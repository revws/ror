package com.revror.kernelmanager.utils.media;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.activities.RorCapturePermissionActivity;
import com.revror.kernelmanager.services.media.RorScreenRecordService;
import com.revror.kernelmanager.utils.ShizukuBridge;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Universal Screen Capture & Recording Manager for ROR.
 *
 * Implements:
 * 1. Native Android MediaProjection + MediaRecorder engine via RorScreenRecordService
 * 2. Proper consent dialog prompt via RorCapturePermissionActivity
 * 3. Direct Scoped Storage into /storage/emulated/0/Movies/ROR/ (Movies/ROR)
 * 4. IS_PENDING MediaStore management for instant Gallery discovery
 * 5. Hardware playback and metadata verification (MediaMetadataRetriever) before reporting success
 * 6. High-resolution Screenshots into /storage/emulated/0/Pictures/ROR/Screenshots/
 */
public final class ScreenCaptureManager {

    private static final String TAG = "ROR_ScreenCapture";
    private static final String PID_FILE = "/data/local/tmp/ror_record.pid";

    private static volatile ScreenCaptureManager sInstance;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean mIsRecording = false;
    private volatile long mRecordingStartTime = 0;
    private volatile String mCurrentRecordingPath = "";
    private volatile Uri mCurrentRecordingUri = null;

    private RecordingCallback mActiveCallback;
    private int mPendingResolution = 1080;
    private boolean mPendingAudio = true;
    private Context mAppContext;

    public interface RecordingCallback {
        void onRecordingStarted(@NonNull String filePath);
        void onRecordingSuccess(@NonNull File file, @Nullable Uri mediaUri, long sizeBytes);
        void onRecordingError(@NonNull String errorMessage);
    }

    public interface ScreenshotCallback {
        void onScreenshotSuccess(@NonNull File file, @Nullable Uri mediaUri);
        void onScreenshotError(@NonNull String errorMessage);
    }

    private ScreenCaptureManager() {}

    public static ScreenCaptureManager getInstance() {
        if (sInstance == null) {
            synchronized (ScreenCaptureManager.class) {
                if (sInstance == null) {
                    sInstance = new ScreenCaptureManager();
                }
            }
        }
        return sInstance;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public long getRecordingStartTime() {
        return mRecordingStartTime;
    }

    public boolean isAudioRequested() {
        return mPendingAudio;
    }

    @NonNull
    public String getCurrentRecordingPath() {
        return mCurrentRecordingPath;
    }

    @NonNull
    public static File getMoviesDirectory() {
        File dir = new File("/storage/emulated/0/Movies/ROR");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                File pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
                dir = new File(pub, "ROR");
                dir.mkdirs();
            }
        }
        return dir;
    }

    @NonNull
    public static File getScreenshotsDirectory() {
        File dir = new File("/storage/emulated/0/Pictures/ROR/Screenshots");
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                File pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                dir = new File(new File(pub, "ROR"), "Screenshots");
                dir.mkdirs();
            }
        }
        return dir;
    }

    /**
     * Starts native screen recording.
     * Always triggers the standard Android MediaProjection consent request to ensure proper
     * screen-casting permission, audio capture, and foreground service lifecycle.
     */
    public synchronized void startRecording(@NonNull Context context, int resolution, boolean includeSound,
                                            @Nullable RecordingCallback callback) {
        if (mIsRecording) {
            if (callback != null) {
                callback.onRecordingError("A screen recording is already in progress.");
            }
            return;
        }

        mAppContext = context.getApplicationContext();
        mActiveCallback = callback;
        mPendingResolution = resolution;
        mPendingAudio = includeSound;

        // Request standard Android MediaProjection consent
        RorCapturePermissionActivity.requestCapturePermission(context);
    }

    /**
     * Called when RorCapturePermissionActivity obtains MediaProjection consent.
     * Uses callerContext (foreground activity) to launch the foreground service smoothly on Android 14+.
     */
    public synchronized void onProjectionPermissionResult(@NonNull Context callerContext, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            mIsRecording = false;
            if (mActiveCallback != null) {
                mActiveCallback.onRecordingError("Screen capture permission was not granted.");
            }
            return;
        }

        Context ctx = (callerContext != null) ? callerContext : (mAppContext != null ? mAppContext : callerContext);
        Intent serviceIntent = new Intent(ctx, RorScreenRecordService.class);
        serviceIntent.setAction(RorScreenRecordService.ACTION_START);
        serviceIntent.putExtra(RorScreenRecordService.EXTRA_RESULT_CODE, resultCode);
        serviceIntent.putExtra(RorScreenRecordService.EXTRA_DATA, data);
        serviceIntent.putExtra(RorScreenRecordService.EXTRA_RES, mPendingResolution);
        serviceIntent.putExtra(RorScreenRecordService.EXTRA_AUDIO, mPendingAudio);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(serviceIntent);
            } else {
                ctx.startService(serviceIntent);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start RorScreenRecordService: " + t.getMessage(), t);
            mIsRecording = false;
            if (mActiveCallback != null) {
                mActiveCallback.onRecordingError("Failed to start recording service: " + t.getMessage());
            }
        }
    }

    public synchronized void onProjectionPermissionResult(int resultCode, @Nullable Intent data) {
        onProjectionPermissionResult(mAppContext, resultCode, data);
    }

    public void onServiceRecordingStarted(String path) {
        mIsRecording = true;
        mCurrentRecordingPath = path;
        mRecordingStartTime = System.currentTimeMillis();
        mMainHandler.post(() -> {
            if (mActiveCallback != null) {
                mActiveCallback.onRecordingStarted(path);
            }
        });
    }

    public void onServiceRecordingSuccess(@Nullable File file, @Nullable Uri uri, long sizeBytes) {
        mIsRecording = false;
        mMainHandler.post(() -> {
            if (mActiveCallback != null) {
                File target = file != null ? file : new File(mCurrentRecordingPath);
                mActiveCallback.onRecordingSuccess(target, uri, sizeBytes);
            }
        });
    }

    public void onServiceRecordingError(String msg) {
        mIsRecording = false;
        mMainHandler.post(() -> {
            if (mActiveCallback != null) {
                mActiveCallback.onRecordingError(msg);
            }
        });
    }

    /**
     * Cleanly stops screen recording and finalizes output.
     */
    public synchronized void stopRecording(@NonNull Context context, @Nullable RecordingCallback callback) {
        if (!mIsRecording) {
            if (callback != null) {
                callback.onRecordingError("No active screen recording to stop.");
            }
            return;
        }
        mActiveCallback = callback;

        // Stop Foreground Service if active
        Intent serviceIntent = new Intent(context, RorScreenRecordService.class);
        serviceIntent.setAction(RorScreenRecordService.ACTION_STOP);
        try {
            context.startService(serviceIntent);
        } catch (Throwable ignored) {}

        // Fallback root cleanup if any stray process exists
        if (RootUtils.rootAccess()) {
            mExecutor.execute(() -> {
                String killCmd = "p=$(cat " + PID_FILE + " 2>/dev/null); "
                        + "if [ -n \"$p\" ]; then kill -2 \"$p\"; kill -INT \"$p\"; fi; "
                        + "killall -2 screenrecord 2>/dev/null; rm -f " + PID_FILE;
                RootUtils.runCommand(killCmd);
            });
        }
    }

    /**
     * Captures an instant high-resolution PNG screenshot and publishes it to MediaStore.Images.
     */
    public void takeScreenshot(@NonNull Context context, @Nullable ScreenshotCallback callback) {
        final Context appContext = context.getApplicationContext();

        mExecutor.execute(() -> {
            File screenshotsDir = getScreenshotsDirectory();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            String fileName = "ROR_GS_" + timeStamp + ".png";
            File screenFile = new File(screenshotsDir, fileName);

            ContentResolver resolver = appContext.getContentResolver();
            ContentValues cv = new ContentValues();
            cv.put(MediaStore.Images.Media.TITLE, "ROR Screenshot " + timeStamp);
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            cv.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cv.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ROR/Screenshots");
                cv.put(MediaStore.Images.Media.IS_PENDING, 1);
            }

            Uri insertedUri = null;
            try {
                insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            } catch (Throwable ignored) {}

            String cmd = "screencap -p '" + screenFile.getAbsolutePath() + "'";
            if (RootUtils.rootAccess()) {
                RootUtils.runCommand(cmd);
            } else if (ShizukuBridge.granted()) {
                ShizukuBridge.run(cmd);
            } else {
                RootUtils.runCommand(cmd);
            }

            boolean success = false;
            for (int i = 0; i < 6; i++) {
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}

                if (screenFile.exists() && screenFile.length() > 1024) {
                    success = true;
                    break;
                }
            }

            final Uri finalUri = insertedUri;
            if (success) {
                long sizeBytes = screenFile.length();
                if (finalUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        ContentValues updateCv = new ContentValues();
                        updateCv.put(MediaStore.Images.Media.IS_PENDING, 0);
                        updateCv.put(MediaStore.Images.Media.SIZE, sizeBytes);
                        resolver.update(finalUri, updateCv, null, null);
                    } catch (Throwable ignored) {}
                }

                try {
                    MediaScannerConnection.scanFile(appContext,
                            new String[]{screenFile.getAbsolutePath()},
                            new String[]{"image/png"}, null);
                } catch (Throwable ignored) {}

                mMainHandler.post(() -> {
                    if (callback != null) {
                        callback.onScreenshotSuccess(screenFile, finalUri);
                    }
                });
            } else {
                if (screenFile.exists() && screenFile.length() == 0) {
                    screenFile.delete();
                }
                if (finalUri != null) {
                    try { resolver.delete(finalUri, null, null); } catch (Throwable ignored) {}
                }

                mMainHandler.post(() -> {
                    if (callback != null) {
                        callback.onScreenshotError("Screenshot capture failed or produced a 0-byte file.");
                    }
                });
            }
        });
    }
}
