package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import com.revror.kernelmanager.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AdInterstitialManager {
    private static final String PREFS_NAME = "ror_ad_interstitial_prefs";
    private static final String KEY_DATE = "last_interstitial_date";
    private static final String KEY_COUNT = "daily_interstitial_count";
    private static final String SMART_LINK_URL = "https://auctionr.org/4/d8efb756fa667f3e7f796b40ad64f491";

    private AdInterstitialManager() {}

    public static boolean canShowToday(Context context) {
        if (context == null) return false;
        if (RorLicenseManager.isPremiumActive(context)) return false;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String savedDate = prefs.getString(KEY_DATE, "");
        int count = prefs.getInt(KEY_COUNT, 0);

        if (!today.equals(savedDate)) {
            prefs.edit().putString(KEY_DATE, today).putInt(KEY_COUNT, 0).apply();
            return true;
        }

        // Limit to 2 per day for non-Phoenix users
        return count < 2;
    }

    public static void recordImpression(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String savedDate = prefs.getString(KEY_DATE, "");
        int count = today.equals(savedDate) ? prefs.getInt(KEY_COUNT, 0) : 0;
        prefs.edit().putString(KEY_DATE, today).putInt(KEY_COUNT, count + 1).apply();
    }

    public static void maybeShowInterstitial(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (!canShowToday(activity)) return;

        activity.runOnUiThread(() -> showFullscreenAd(activity));
    }

    public static void showFullscreenAd(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (RorLicenseManager.isPremiumActive(activity)) return;

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.ror_dialog_fullscreen_ad);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        recordImpression(activity);

        WebView webView = dialog.findViewById(R.id.ad_webview);
        View closeBtn = dialog.findViewById(R.id.ad_fake_close_btn);
        TextView countdownView = dialog.findViewById(R.id.ad_countdown_text);

        final boolean[] firstCloseHandled = {false};

        if (closeBtn != null) {
            closeBtn.setVisibility(View.GONE);
        }

        final int[] countdown = {5};
        Handler timerHandler = new Handler(Looper.getMainLooper());
        Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!dialog.isShowing() || activity.isFinishing() || activity.isDestroyed()) return;
                countdown[0]--;
                if (countdown[0] > 0) {
                    if (countdownView != null) countdownView.setText(countdown[0] + "s");
                    timerHandler.postDelayed(this, 1000);
                } else {
                    if (countdownView != null) countdownView.setVisibility(View.GONE);
                    if (closeBtn != null) {
                        closeBtn.setVisibility(View.VISIBLE);
                        closeBtn.setAlpha(0f);
                        closeBtn.animate().alpha(1f).setDuration(250).start();
                    }
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);

        dialog.setOnDismissListener(d -> timerHandler.removeCallbacksAndMessages(null));

        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                if (!firstCloseHandled[0]) {
                    firstCloseHandled[0] = true;
                    // First click opens the smart link redirect
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SMART_LINK_URL));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                        Toast.makeText(activity, "Opening partner offer...", Toast.LENGTH_SHORT).show();
                    } catch (Throwable t) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(SMART_LINK_URL));
                            activity.startActivity(intent);
                        } catch (Throwable ignored) {}
                    }
                } else {
                    // Second click dismisses the ad dialog
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            });
        }

        if (webView != null) {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setAllowFileAccess(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            webView.setBackgroundColor(0xFF0A0B12);

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url != null && !url.startsWith("about:") && !url.startsWith("data:")) {
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            activity.startActivity(intent);
                        } catch (Throwable ignored) {}
                        return true;
                    }
                    return false;
                }
            });

            String html = "<!DOCTYPE html>"
                    + "<html><head>"
                    + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>"
                    + "<style>"
                    + "* { box-sizing: border-box; margin: 0; padding: 0; }"
                    + "body { background-color: #0A0B12; color: #FFFFFF; font-family: -apple-system, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 90vh; padding: 12px; gap: 14px; text-align: center; }"
                    + ".ad-card { background: #131522; border: 1px solid #23273D; border-radius: 12px; padding: 10px; width: 100%; max-width: 500px; display: flex; justify-content: center; align-items: center; overflow: hidden; }"
                    + "</style>"
                    + "</head><body>"
                    + "<div class='ad-card'>"
                    + "<script>atOptions = {'key' : '596f557432272eeedda31e1200f5a606','format' : 'iframe','height' : 250,'width' : 300,'params' : {}};</script>"
                    + "<script src='https://bauval.org/22/596f557432272eeedda31e1200f5a606'></script>"
                    + "</div>"
                    + "<div class='ad-card'>"
                    + "<script>atOptions = {'key' : '62a963ca90f7bb8b00e069773c757d6c','format' : 'iframe','height' : 60,'width' : 468,'params' : {}};</script>"
                    + "<script src='https://bauval.org/22/62a963ca90f7bb8b00e069773c757d6c'></script>"
                    + "</div>"
                    + "<div class='ad-card'>"
                    + "<script>atOptions = {'key' : '62a963ca90f7bb8b00e069773c757d6c','format' : 'iframe','height' : 60,'width' : 468,'params' : {}};</script>"
                    + "<script src='https://bauval.org/22/62a963ca90f7bb8b00e069773c757d6c'></script>"
                    + "</div>"
                    + "</body></html>";

            webView.loadDataWithBaseURL("https://bauval.org/", html, "text/html", "UTF-8", null);
        }

        try {
            dialog.show();
        } catch (Throwable ignored) {}
    }
}

