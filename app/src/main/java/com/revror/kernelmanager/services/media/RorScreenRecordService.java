package com.revror.kernelmanager.services.media;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.media.ScreenCaptureManager;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dedicated Screen Recording Foreground Service for ROR.
 *
 * Implements Android 14+ MediaProjection architecture:
 * 1. Started as FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION before getMediaProjection()
 * 2. MediaProjection.Callback registered prior to createVirtualDisplay()
 * 3. MediaRecorder writing directly to MediaStore ParcelFileDescriptor
 * 4. Automatic finalization, IS_PENDING management, and MediaMetadataRetriever verification
 * 5. Notification with "OPEN RECORDING" action
 */
public class RorScreenRecordService extends Service {

    private static final String TAG = "ROR_RecordService";
    private static final String CHANNEL_ID = "ror_screen_recorder_channel";
    private static final int NOTIF_ID = 9812;

    public static final String ACTION_START = "com.revror.action.START_RECORDING";
    public static final String ACTION_STOP = "com.revror.action.STOP_RECORDING";
    public static final String EXTRA_RESULT_CODE = "extra_result_code";
    public static final String EXTRA_DATA = "extra_data";
    public static final String EXTRA_RES = "extra_res";
    public static final String EXTRA_AUDIO = "extra_audio";

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaRecorder mMediaRecorder;
    private ParcelFileDescriptor mPfd;
    private Uri mCurrentVideoUri;
    private File mCurrentRawFile;
    private String mCurrentFileName = "";
    private boolean mIsRecording = false;

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0);
            Intent data = intent.getParcelableExtra(EXTRA_DATA);
            int res = intent.getIntExtra(EXTRA_RES, 1080);
            boolean audio = intent.getBooleanExtra(EXTRA_AUDIO, true);

            startForegroundServiceWithNotification();
            startCapture(resultCode, data, res, audio);
        } else if (ACTION_STOP.equals(action)) {
            stopCapture();
        }

        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "ROR Screen Recorder",
                    NotificationManager.IMPORTANCE_LOW
            );
            chan.setDescription("Active in-game screen recording session");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(chan);
        }
    }

    private void startForegroundServiceWithNotification() {
        Intent stopIntent = new Intent(this, RorScreenRecordService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pi = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ROR Screen Recorder Active")
                .setContentText("Recording in-game session to Movies/ROR")
                .setSmallIcon(R.drawable.ic_record_dot)
                .setOngoing(true)
                .addAction(R.drawable.ic_clear, "STOP", pi)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    private void startCapture(int resultCode, Intent data, int resolution, boolean includeAudio) {
        if (mIsRecording) return;

        MediaProjectionManager mpm = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mpm == null || data == null) {
            notifyError("MediaProjection service unavailable");
            stopSelf();
            return;
        }

        try {
            mMediaProjection = mpm.getMediaProjection(resultCode, data);
        } catch (Throwable t) {
            notifyError("Failed to initialize MediaProjection: " + t.getMessage());
            stopSelf();
            return;
        }

        if (mMediaProjection == null) {
            notifyError("Screen capture permission was not granted");
            stopSelf();
            return;
        }

        // Register mandatory callback on Android 14+
        mMediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                Log.w(TAG, "MediaProjection session stopped by system");
                stopCapture();
            }
        }, mHandler);

        // Get true display dimensions and aspect ratio
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        int screenWidth = 1080;
        int screenHeight = 1920;
        int dpi = 420;
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Rect bounds = wm.getMaximumWindowMetrics().getBounds();
                screenWidth = bounds.width();
                screenHeight = bounds.height();
                dpi = getResources().getConfiguration().densityDpi;
            } else {
                DisplayMetrics realMetrics = new DisplayMetrics();
                wm.getDefaultDisplay().getRealMetrics(realMetrics);
                screenWidth = realMetrics.widthPixels;
                screenHeight = realMetrics.heightPixels;
                dpi = realMetrics.densityDpi;
            }
        } else {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            screenWidth = dm.widthPixels;
            screenHeight = dm.heightPixels;
            dpi = dm.densityDpi;
        }

        boolean isPortrait = screenHeight > screenWidth;
        int shortEdge = Math.min(resolution, Math.min(screenWidth, screenHeight));
        double aspect = (double) Math.max(screenWidth, screenHeight) / (double) Math.min(screenWidth, screenHeight);
        int longEdge = (int) Math.round(shortEdge * aspect);

        int targetW = isPortrait ? shortEdge : longEdge;
        int targetH = isPortrait ? longEdge : shortEdge;

        // Ensure even dimensions for H.264 encoder
        targetW = (targetW / 2) * 2;
        targetH = (targetH / 2) * 2;
        int bitrate = (resolution >= 1080) ? 16000000 : ((resolution >= 720) ? 8000000 : 4000000);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "ROR_REC_" + resolution + "p_" + timeStamp + ".mp4";
        mCurrentFileName = fileName;

        File moviesDir = ScreenCaptureManager.getMoviesDirectory();
        mCurrentRawFile = new File(moviesDir, fileName);

        ContentResolver resolver = getContentResolver();
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Video.Media.TITLE, "ROR Recording " + timeStamp);
        cv.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        cv.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        cv.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cv.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ROR");
            cv.put(MediaStore.Video.Media.IS_PENDING, 1);
        }

        try {
            mCurrentVideoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cv);
            if (mCurrentVideoUri != null) {
                mPfd = resolver.openFileDescriptor(mCurrentVideoUri, "rw");
            }
        } catch (Throwable t) {
            Log.w(TAG, "MediaStore insert error: " + t.getMessage());
        }

        try {
            mMediaRecorder = new MediaRecorder();

            boolean canRecordAudio = includeAudio &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
            boolean audioConfigured = false;
            if (canRecordAudio) {
                try {
                    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    audioConfigured = true;
                } catch (Throwable t) {
                    Log.w(TAG, "Audio source setup notice: " + t.getMessage());
                    audioConfigured = false;
                }
            }

            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

            if (audioConfigured) {
                try {
                    mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mMediaRecorder.setAudioEncodingBitRate(128000);
                    mMediaRecorder.setAudioSamplingRate(44100);
                } catch (Throwable t) {
                    Log.w(TAG, "Audio encoder setup notice: " + t.getMessage());
                }
            }

            mMediaRecorder.setVideoSize(targetW, targetH);
            mMediaRecorder.setVideoFrameRate(60);
            mMediaRecorder.setVideoEncodingBitRate(bitrate);

            if (mPfd != null) {
                mMediaRecorder.setOutputFile(mPfd.getFileDescriptor());
            } else {
                mMediaRecorder.setOutputFile(mCurrentRawFile.getAbsolutePath());
            }

            mMediaRecorder.prepare();
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(
                    "ROR_VirtualDisplay",
                    targetW, targetH, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mMediaRecorder.getSurface(),
                    null, null
            );

            mMediaRecorder.start();
            mIsRecording = true;

            String path = (mCurrentRawFile != null) ? mCurrentRawFile.getAbsolutePath() : "/storage/emulated/0/Movies/ROR/" + fileName;
            ScreenCaptureManager.getInstance().onServiceRecordingStarted(path);
            Log.i(TAG, "Screen recording started successfully (" + targetW + "x" + targetH + ", " + resolution + "p)");
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start MediaRecorder: " + t.getMessage(), t);
            notifyError("MediaRecorder error: " + t.getMessage());
            cleanup();
            stopSelf();
        }
    }

    private void stopCapture() {
        if (!mIsRecording) {
            stopSelf();
            return;
        }
        mIsRecording = false;

        new Thread(() -> {
            try {
                if (mMediaRecorder != null) {
                    try {
                        mMediaRecorder.stop();
                    } catch (Throwable t) {
                        Log.w(TAG, "MediaRecorder stop note: " + t.getMessage());
                    }
                    mMediaRecorder.reset();
                    mMediaRecorder.release();
                    mMediaRecorder = null;
                }
            } catch (Throwable ignored) {}

            try {
                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                    mVirtualDisplay = null;
                }
            } catch (Throwable ignored) {}

            try {
                if (mMediaProjection != null) {
                    mMediaProjection.stop();
                    mMediaProjection = null;
                }
            } catch (Throwable ignored) {}

            try {
                if (mPfd != null) {
                    mPfd.close();
                    mPfd = null;
                }
            } catch (Throwable ignored) {}

            // Verify file and publish
            boolean valid = verifyAndPublish();
            if (valid) {
                showSavedNotification(mCurrentFileName);
            } else {
                notifyError("Screen recording failed or video file is empty.");
            }
            stopForeground(true);
            stopSelf();
        }).start();
    }

    private boolean verifyAndPublish() {
        ContentResolver resolver = getContentResolver();
        File targetFile = mCurrentRawFile;

        if (targetFile == null || !targetFile.exists()) {
            File expectedFile = new File(ScreenCaptureManager.getMoviesDirectory(), mCurrentFileName);
            if (expectedFile.exists() && expectedFile.length() > 0) {
                targetFile = expectedFile;
            }
        }

        if (mCurrentVideoUri != null) {
            try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
                mmr.setDataSource(this, mCurrentVideoUri);
                String hasVideo = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO);
                String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

                if ("yes".equalsIgnoreCase(hasVideo) || (duration != null && Long.parseLong(duration) > 0)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues updateCv = new ContentValues();
                        updateCv.put(MediaStore.Video.Media.IS_PENDING, 0);
                        if (targetFile != null && targetFile.exists()) {
                            updateCv.put(MediaStore.Video.Media.SIZE, targetFile.length());
                        }
                        resolver.update(mCurrentVideoUri, updateCv, null, null);
                    }
                    long size = (targetFile != null && targetFile.exists()) ? targetFile.length() : (1024 * 1024);
                    MediaScannerConnection.scanFile(this,
                            new String[]{targetFile != null ? targetFile.getAbsolutePath() : "/storage/emulated/0/Movies/ROR/" + mCurrentFileName},
                            new String[]{"video/mp4"}, null);
                    ScreenCaptureManager.getInstance().onServiceRecordingSuccess(targetFile, mCurrentVideoUri, size);
                    return true;
                }
            } catch (Throwable t) {
                Log.w(TAG, "MediaMetadataRetriever check note: " + t.getMessage());
            }
        }

        if (targetFile != null && targetFile.exists() && targetFile.length() > 2048) {
            long sizeBytes = targetFile.length();
            if (mCurrentVideoUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues updateCv = new ContentValues();
                updateCv.put(MediaStore.Video.Media.IS_PENDING, 0);
                updateCv.put(MediaStore.Video.Media.SIZE, sizeBytes);
                resolver.update(mCurrentVideoUri, updateCv, null, null);
            }
            MediaScannerConnection.scanFile(this, new String[]{targetFile.getAbsolutePath()}, new String[]{"video/mp4"}, null);
            ScreenCaptureManager.getInstance().onServiceRecordingSuccess(targetFile, mCurrentVideoUri, sizeBytes);
            return true;
        }

        // Clean up broken recording
        if (mCurrentVideoUri != null) {
            try { resolver.delete(mCurrentVideoUri, null, null); } catch (Throwable ignored) {}
        }
        if (targetFile != null && targetFile.exists()) {
            targetFile.delete();
        }
        return false;
    }

    private void showSavedNotification(String fileName) {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        Uri playUri = mCurrentVideoUri;
        if (playUri == null && mCurrentRawFile != null && mCurrentRawFile.exists()) {
            try {
                playUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", mCurrentRawFile);
            } catch (Throwable ignored) {}
        }

        PendingIntent openPi = null;
        if (playUri != null) {
            viewIntent.setDataAndType(playUri, "video/mp4");
            viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            openPi = PendingIntent.getActivity(this, 1002, viewIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Recording Saved")
                .setContentText("Saved to Movies/ROR/" + fileName)
                .setSmallIcon(R.drawable.ic_record_dot)
                .setAutoCancel(true);

        if (openPi != null) {
            builder.setContentIntent(openPi);
            builder.addAction(R.drawable.ic_play_arrow, "OPEN RECORDING", openPi);
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIF_ID + 1, builder.build());
        }
    }

    private void notifyError(String msg) {
        ScreenCaptureManager.getInstance().onServiceRecordingError(msg);
    }

    private void cleanup() {
        try { if (mMediaRecorder != null) { mMediaRecorder.release(); mMediaRecorder = null; } } catch (Throwable ignored) {}
        try { if (mVirtualDisplay != null) { mVirtualDisplay.release(); mVirtualDisplay = null; } } catch (Throwable ignored) {}
        try { if (mMediaProjection != null) { mMediaProjection.stop(); mMediaProjection = null; } } catch (Throwable ignored) {}
        try { if (mPfd != null) { mPfd.close(); mPfd = null; } } catch (Throwable ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanup();
    }
}
