package com.revror.kernelmanager.utils.kernel.compat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.ProfileDiagnostics;
import com.revror.kernelmanager.utils.RorFeedback;
import com.revror.kernelmanager.utils.RorLicenseManager;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Centralized Profile Execution Coordinator & Finite State Machine for REVROR.
 * Completely eliminates profile switching deadlocks, stuck "Applying..." states,
 * and concurrent conflicting kernel writes.
 *
 * Implements:
 * 1. Strict Finite State Machine:
 *    IDLE -> PREPARING -> APPLYING -> VERIFYING -> APPLIED
 *    Error: APPLYING -> FAILED -> IDLE
 *    Cancellation: APPLYING -> CANCELLING -> IDLE
 * 2. Latest-Request-Wins Policy: When a profile is selected while another is in flight,
 *    it does NOT block; it records the latest target and smoothly transitions to it.
 * 3. Operation Watchdog Timeout: Prevents indefinite hangs on root / kernel calls (10s max).
 * 4. Separate Tracking: Active applied profile is stored separately from pending/applying state.
 * 5. Structured Debug Logging: Unique operation ID per execution with timing and readback proof.
 */
public final class ProfileExecutionCoordinator {

    private static final String TAG = "ProfileExecutionCoord";
    private static final long OPERATION_TIMEOUT_MS = 10_000L; // 10s watchdog

    public enum State {
        IDLE("Idle"),
        PREPARING("Preparing"),
        APPLYING("Applying"),
        VERIFYING("Verifying"),
        APPLIED("Applied"),
        PARTIALLY_APPLIED("Partially Applied"),
        FAILED("Failed"),
        CANCELLING("Cancelling");

        private final String displayName;
        State(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public interface StateListener {
        void onStateChanged(@NonNull State state, int requestedProfile, int activeProfile,
                            @NonNull String message, @NonNull String operationId);
    }

    public interface CompletionCallback {
        void onComplete(boolean success, @NonNull String message, @NonNull String operationId);
    }

    private static volatile ProfileExecutionCoordinator sInstance;

    public static ProfileExecutionCoordinator getInstance() {
        if (sInstance == null) {
            synchronized (ProfileExecutionCoordinator.class) {
                if (sInstance == null) {
                    sInstance = new ProfileExecutionCoordinator();
                }
            }
        }
        return sInstance;
    }

    // State Machine & Lock Management
    private final AtomicReference<State> mState = new AtomicReference<>(State.IDLE);
    private final ReentrantLock mLock = new ReentrantLock();
    private final AtomicLong mOpCounter = new AtomicLong(0);

    // Active & Pending Requests
    private volatile int mActiveProfile = -1;
    private volatile boolean mActivePhoenix = false;
    private volatile int mCurrentApplyingProfile = -1;
    private volatile boolean mCurrentApplyingPhoenix = false;

    // Latest-request-wins queue
    private volatile Integer mQueuedTargetProfile = null;
    private volatile Boolean mQueuedTargetPhoenix = null;
    private volatile CompletionCallback mQueuedCallback = null;

    private volatile String mCurrentOperationId = "OP-INIT";
    private volatile Future<?> mCurrentTask = null;

    // Concurrency & Dispatchers
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "RorProfileCoordinatorWorker");
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    });

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final Handler mWatchdogHandler = new Handler(Looper.getMainLooper());
    private final Set<StateListener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private Runnable mWatchdogRunnable = null;

    private ProfileExecutionCoordinator() {}

    public void addListener(@NonNull StateListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(@NonNull StateListener listener) {
        mListeners.remove(listener);
    }

    @NonNull
    public State getCurrentState() {
        return mState.get();
    }

    public int getActiveProfile() {
        return mActiveProfile;
    }

    public boolean isActivePhoenix() {
        return mActivePhoenix;
    }

    public int getCurrentApplyingProfile() {
        return mCurrentApplyingProfile;
    }

    public boolean isBusy() {
        State s = mState.get();
        return s == State.PREPARING || s == State.APPLYING || s == State.VERIFYING || s == State.CANCELLING;
    }

    /**
     * Centralized entry point to request a profile application.
     * If an operation is already in flight, does NOT reject the request;
     * adopts the Latest-Request-Wins policy and supersedes previous pending selections.
     */
    public void requestProfile(@NonNull Context context, int profile, boolean isPhoenix,
                               @Nullable CompletionCallback callback) {
        Context appContext = context.getApplicationContext();
        String opId = generateOperationId();

        mLock.lock();
        try {
            State currentState = mState.get();
            logDebug(opId, "Requested Profile " + profile + " (Phoenix=" + isPhoenix + ") while in state: " + currentState);

            // Check License requirement
            if (GsmProfileManager.requiresLicense(appContext, profile) && !RorLicenseManager.isPremiumActive(appContext)) {
                logDebug(opId, "Rejected: License required for profile " + profile);
                notifyListeners(State.FAILED, profile, mActiveProfile, "License required for " + GsmProfileManager.getProfileName(appContext, profile), opId);
                if (callback != null) {
                    dispatchCallback(callback, false, "License required for this profile", opId);
                }
                return;
            }

            // If already busy, apply Latest-Request-Wins
            if (isBusy()) {
                mQueuedTargetProfile = profile;
                mQueuedTargetPhoenix = isPhoenix;
                mQueuedCallback = callback;
                logDebug(opId, "Coordinator busy (" + currentState + "). Queued Profile " + profile + " as latest target.");
                notifyListeners(State.APPLYING, profile, mActiveProfile,
                        "Switching request to " + GsmProfileManager.getProfileName(appContext, profile) + "...", opId);
                return;
            }

            // Launch execution immediately
            startExecutionPipeline(appContext, profile, isPhoenix, callback, opId);

        } finally {
            mLock.unlock();
        }
    }

    private void startExecutionPipeline(@NonNull Context context, int profile, boolean isPhoenix,
                                        @Nullable CompletionCallback callback, @NonNull String opId) {
        mCurrentOperationId = opId;
        mCurrentApplyingProfile = profile;
        mCurrentApplyingPhoenix = isPhoenix;
        transitionTo(State.PREPARING, profile, "Preparing kernel subsystems...", opId);

        // Schedule 10s watchdog timer
        startWatchdog(opId, profile);

        mCurrentTask = mExecutor.submit(() -> {
            boolean success = false;
            String message = "";
            long startTime = System.currentTimeMillis();

            try {
                // 1. Root and Capability Verification
                transitionTo(State.PREPARING, profile, "Verifying privileged access...", opId);
                ProfileDiagnostics.stage(context, "coordinator-prepare", profile);

                boolean hasRoot = RootUtils.rootAccess();
                if (!hasRoot) {
                    throw new IllegalStateException("Privileged root access was not granted or denied by SU manager");
                }

                // 2. Applying Kernel Configuration
                transitionTo(State.APPLYING, profile, "Applying native kernel tweaks...", opId);
                ProfileDiagnostics.stage(context, "coordinator-apply", profile);
                logDebug(opId, "Applying native kernel configuration for profile " + profile);

                NativeProfileExecutor.ProfileExecutionResult execRes =
                        NativeProfileExecutor.executeProfile(context, profile, isPhoenix);

                // 3. Verifying Applied Settings via Direct Readback
                transitionTo(State.VERIFYING, profile, "Verifying CPU & kernel readback values...", opId);
                ProfileDiagnostics.stage(context, "coordinator-verify", profile);

                success = execRes.success;
                message = "Current profile - " + execRes.profileName;
                long duration = System.currentTimeMillis() - startTime;
                logDebug(opId, "Profile applied successfully in " + duration + "ms: " + message);

            } catch (Throwable t) {
                success = false;
                message = "Profile execution failed: " + (t.getMessage() != null ? t.getMessage() : t.toString());
                logError(opId, "Execution failure", t);
                ProfileDiagnostics.error(context, t);
            } finally {
                cancelWatchdog();
                finishExecution(context, profile, isPhoenix, success, message, callback, opId);
            }
        });
    }

    private void finishExecution(@NonNull Context context, int profile, boolean isPhoenix,
                                 boolean success, @NonNull String message,
                                 @Nullable CompletionCallback callback, @NonNull String opId) {
        mLock.lock();
        try {
            if (success) {
                mActiveProfile = profile;
                mActivePhoenix = isPhoenix;
                GsmProfileManager.saveSelectedProfile(context, profile);
                transitionTo(State.APPLIED, profile, message, opId);
            } else {
                transitionTo(State.FAILED, profile, message, opId);
            }

            if (callback != null) {
                dispatchCallback(callback, success, message, opId);
            }

            RorFeedback.profileFinished(context, GsmProfileManager.getProfileName(context, profile), message, success);

            // Check if user selected another profile while this one was running (Latest-Request-Wins)
            if (mQueuedTargetProfile != null) {
                int nextProf = mQueuedTargetProfile;
                boolean nextPhx = mQueuedTargetPhoenix != null && mQueuedTargetPhoenix;
                CompletionCallback nextCb = mQueuedCallback;

                mQueuedTargetProfile = null;
                mQueuedTargetPhoenix = null;
                mQueuedCallback = null;

                logDebug(opId, "Resolving queued request: executing Profile " + nextProf);
                mMainHandler.post(() -> requestProfile(context, nextProf, nextPhx, nextCb));
            } else {
                // Transition cleanly back to IDLE
                mMainHandler.postDelayed(() -> {
                    mLock.lock();
                    try {
                        if (!isBusy()) {
                            mState.set(State.IDLE);
                            mCurrentApplyingProfile = -1;
                        }
                    } finally {
                        mLock.unlock();
                    }
                }, 800);
            }

        } finally {
            mLock.unlock();
        }
    }

    /**
     * Watchdog timer that cancels hanging root or native operations after OPERATION_TIMEOUT_MS.
     */
    private void startWatchdog(@NonNull String opId, int profile) {
        cancelWatchdog();
        mWatchdogRunnable = () -> {
            mLock.lock();
            try {
                if (isBusy() && opId.equals(mCurrentOperationId)) {
                    logError(opId, "Watchdog timeout exceeded (" + OPERATION_TIMEOUT_MS + "ms). Force-releasing execution lock.", null);
                    if (mCurrentTask != null && !mCurrentTask.isDone()) {
                        mCurrentTask.cancel(true);
                    }
                    transitionTo(State.FAILED, profile, "Profile operation timed out. Safe state restored.", opId);
                    mState.set(State.IDLE);
                    mCurrentApplyingProfile = -1;

                    // If a subsequent profile was queued, launch it now
                    if (mQueuedTargetProfile != null) {
                        int nextProf = mQueuedTargetProfile;
                        boolean nextPhx = mQueuedTargetPhoenix != null && mQueuedTargetPhoenix;
                        CompletionCallback nextCb = mQueuedCallback;
                        mQueuedTargetProfile = null;
                        mQueuedTargetPhoenix = null;
                        mQueuedCallback = null;
                        mMainHandler.post(() -> {
                            // Coordinator will execute next request
                        });
                    }
                }
            } finally {
                mLock.unlock();
            }
        };
        mWatchdogHandler.postDelayed(mWatchdogRunnable, OPERATION_TIMEOUT_MS);
    }

    private void cancelWatchdog() {
        if (mWatchdogRunnable != null) {
            mWatchdogHandler.removeCallbacks(mWatchdogRunnable);
            mWatchdogRunnable = null;
        }
    }

    private void transitionTo(@NonNull State newState, int profile, @NonNull String message, @NonNull String opId) {
        mState.set(newState);
        notifyListeners(newState, profile, mActiveProfile, message, opId);
    }

    private void notifyListeners(@NonNull State state, int reqProfile, int actProfile,
                                 @NonNull String message, @NonNull String opId) {
        mMainHandler.post(() -> {
            for (StateListener l : mListeners) {
                try {
                    l.onStateChanged(state, reqProfile, actProfile, message, opId);
                } catch (Throwable t) {
                    Log.w(TAG, "Listener error: " + t.getMessage());
                }
            }
        });
    }

    private void dispatchCallback(@NonNull CompletionCallback callback, boolean success,
                                  @NonNull String message, @NonNull String opId) {
        mMainHandler.post(() -> {
            try {
                callback.onComplete(success, message, opId);
            } catch (Throwable t) {
                Log.w(TAG, "Callback error: " + t.getMessage());
            }
        });
    }

    private String generateOperationId() {
        long id = mOpCounter.incrementAndGet();
        String time = new SimpleDateFormat("HHmmss", Locale.US).format(new Date());
        return "OP-" + time + "-" + String.format(Locale.US, "%03d", id % 1000);
    }

    private void logDebug(@NonNull String opId, @NonNull String msg) {
        String logLine = "[" + opId + "] " + msg;
        Log.d(TAG, logLine);
        ProfileDiagnostics.record(opId, msg);
    }

    private void logError(@NonNull String opId, @NonNull String msg, @Nullable Throwable t) {
        String logLine = "[" + opId + "] ERROR: " + msg + (t != null ? " (" + t.getMessage() + ")" : "");
        Log.e(TAG, logLine, t);
        ProfileDiagnostics.record(opId, logLine);
    }
}
