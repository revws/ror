package com.revror.kernelmanager.activities;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.revror.kernelmanager.utils.media.ScreenCaptureManager;

/**
 * Transparent helper activity to prompt Android MediaProjection screen capture consent dialog.
 * Uses modern Activity Result API, handles runtime audio permission, and adheres to Android 14+ FGS restrictions.
 */
public class RorCapturePermissionActivity extends AppCompatActivity {

    private static final String TAG = "ROR_CaptureConsent";
    private boolean mResultDelivered = false;

    private final ActivityResultLauncher<Intent> mCaptureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                int resultCode = result.getResultCode();
                Intent data = result.getData();
                Log.i(TAG, "MediaProjection consent result: " + resultCode);
                mResultDelivered = true;
                if (resultCode == Activity.RESULT_OK && data != null) {
                    ScreenCaptureManager.getInstance().onProjectionPermissionResult(this, resultCode, data);
                } else {
                    ScreenCaptureManager.getInstance().onProjectionPermissionResult(this, Activity.RESULT_CANCELED, null);
                }
                finish();
            }
    );

    private final ActivityResultLauncher<String> mAudioPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                launchMediaProjectionConsent();
            }
    );

    public static void requestCapturePermission(@NonNull Context context) {
        Intent intent = new Intent(context, RorCapturePermissionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        Bundle optionsBundle = null;
        if (Build.VERSION.SDK_INT >= 34) {
            ActivityOptions options = ActivityOptions.makeBasic();
            try {
                options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                optionsBundle = options.toBundle();
            } catch (Throwable ignored) {}
        }

        try {
            if (optionsBundle != null) {
                context.startActivity(intent, optionsBundle);
            } else {
                context.startActivity(intent);
            }
        } catch (Throwable t) {
            Log.w(TAG, "Direct startActivity failed, falling back to PendingIntent: " + t.getMessage());
            try {
                PendingIntent pi = PendingIntent.getActivity(
                        context,
                        1992,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
                );
                if (optionsBundle != null) {
                    pi.send(context, 0, null, null, null, null, optionsBundle);
                } else {
                    pi.send();
                }
            } catch (Throwable t2) {
                Log.e(TAG, "PendingIntent launch also failed: " + t2.getMessage());
                ScreenCaptureManager.getInstance().onProjectionPermissionResult(context, Activity.RESULT_CANCELED, null);
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep screen on during consent
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        View dummy = new View(this);
        dummy.setBackgroundColor(Color.TRANSPARENT);
        setContentView(dummy);

        // If audio recording is requested and not granted, request audio permission first
        boolean needsAudio = ScreenCaptureManager.getInstance().isAudioRequested();
        if (needsAudio && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        } else {
            launchMediaProjectionConsent();
        }
    }

    private void launchMediaProjectionConsent() {
        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mpm != null) {
            try {
                Intent captureIntent = mpm.createScreenCaptureIntent();
                mCaptureLauncher.launch(captureIntent);
            } catch (Throwable t) {
                Log.e(TAG, "Failed to launch screen capture intent: " + t.getMessage(), t);
                mResultDelivered = true;
                ScreenCaptureManager.getInstance().onProjectionPermissionResult(this, Activity.RESULT_CANCELED, null);
                finish();
            }
        } else {
            Log.e(TAG, "MediaProjectionManager is null on this device");
            mResultDelivered = true;
            ScreenCaptureManager.getInstance().onProjectionPermissionResult(this, Activity.RESULT_CANCELED, null);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mResultDelivered) {
            mResultDelivered = true;
            ScreenCaptureManager.getInstance().onProjectionPermissionResult(this, Activity.RESULT_CANCELED, null);
        }
    }
}
