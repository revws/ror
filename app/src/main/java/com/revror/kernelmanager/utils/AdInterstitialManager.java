package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import com.revror.kernelmanager.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Alternating Fullscreen Interstitial Ad System for ROR.
 *
 * Implements:
 * 1. Strict alternating sequence: Ad A -> Ad B -> Ad A -> Ad B
 *    - Ad A: https://auctionr.org/4/d8efb756fa667f3e7f796b40ad64f491 (Adsterra/Bauval banners)
 *    - Ad B: https://uplcm.com/4/12097615 (Sponsored gaming destination)
 * 2. Centralized rotation counter (ad_rotation_index) incremented only on dialog.show()
 * 3. 5-second countdown timer badge (5s, 4s, 3s, 2s, 1s)
 * 4. Close (X) icon appears after 5s:
 *    - First click opens active ad URL in device browser
 *    - Second click dismisses ad dialog
 * 5. Dedicated "VISIT OFFER" action button in ad top header
 * 6. Full VIP bypass via RorLicenseManager.isPremiumActive()
 */
public final class AdInterstitialManager {

    private static final String PREFS_NAME = "ror_ad_interstitial_prefs";
    private static final String KEY_DATE = "last_interstitial_date";
    private static final String KEY_COUNT = "daily_interstitial_count";
    private static final String KEY_ROTATION_INDEX = "ad_rotation_index";

    public static final String AD_A_URL = "https://auctionr.org/4/d8efb756fa667f3e7f796b40ad64f491";
    public static final String AD_B_URL = "https://uplcm.com/4/12097615";

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

        // Allow up to 2 interstitial promotions per day for free users
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

        SharedPreferences prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int rotationIndex = prefs.getInt(KEY_ROTATION_INDEX, 0);
        final boolean isAdB = (rotationIndex % 2 != 0);
        final String activeUrl = isAdB ? AD_B_URL : AD_A_URL;
        final String activeTitle = isAdB ? "EXCLUSIVE SPONSORED OFFER" : "GAMING REWARDS & OFFERS";

        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.ror_dialog_fullscreen_ad);
        dialog.setCancelable(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        }

        TextView titleView = dialog.findViewById(R.id.ad_title_text);
        if (titleView != null) {
            titleView.setText(activeTitle);
        }

        TextView visitBtn = dialog.findViewById(R.id.ad_visit_btn);
        if (visitBtn != null) {
            visitBtn.setOnClickListener(v -> {
                openAdUrl(activity, activeUrl);
            });
        }

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

        dialog.setOnDismissListener(d -> {
            timerHandler.removeCallbacksAndMessages(null);
            if (webView != null) {
                try { webView.destroy(); } catch (Throwable ignored) {}
            }
        });

        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> {
                if (!firstCloseHandled[0]) {
                    firstCloseHandled[0] = true;
                    // First click opens active ad destination redirect
                    openAdUrl(activity, activeUrl);
                } else {
                    // Second click dismisses ad dialog
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
                        openAdUrl(activity, url);
                        return true;
                    }
                    return false;
                }
            });

            String html;
            if (isAdB) {
                // Ad B: Sponsored gaming destination showcase
                html = "<!DOCTYPE html>"
                        + "<html><head>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>"
                        + "<style>"
                        + "* { box-sizing: border-box; margin: 0; padding: 0; }"
                        + "body { background-color: #0A0B12; color: #FFFFFF; font-family: -apple-system, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 90vh; padding: 16px; gap: 16px; text-align: center; }"
                        + ".hero-card { background: linear-gradient(135deg, #181A2A 0%, #0F111E 100%); border: 1.5px solid #FF9800; border-radius: 14px; padding: 22px 16px; width: 100%; max-width: 480px; box-shadow: 0 8px 32px rgba(255, 152, 0, 0.18); display: flex; flex-direction: column; align-items: center; gap: 12px; }"
                        + ".badge { background: rgba(255, 152, 0, 0.15); color: #FFA726; padding: 4px 12px; border-radius: 20px; font-size: 11px; font-weight: bold; letter-spacing: 1px; }"
                        + ".title { font-size: 20px; font-weight: 800; color: #FFFFFF; line-height: 1.3; }"
                        + ".desc { font-size: 13px; color: #9E9EB2; line-height: 1.4; }"
                        + ".cta-btn { background: linear-gradient(90deg, #FF6F00 0%, #FFA000 100%); color: #000000; font-weight: 800; font-size: 14px; text-decoration: none; padding: 13px 28px; border-radius: 25px; margin-top: 6px; display: inline-block; box-shadow: 0 4px 14px rgba(255, 152, 0, 0.4); text-transform: uppercase; letter-spacing: 0.5px; }"
                        + ".sub-card { background: #131522; border: 1px solid #23273D; border-radius: 10px; padding: 10px; width: 100%; max-width: 480px; display: flex; justify-content: center; align-items: center; overflow: hidden; }"
                        + "</style>"
                        + "</head><body>"
                        + "<div class='hero-card'>"
                        + "<div class='badge'>LIMITED TIME PARTNER OFFER</div>"
                        + "<div class='title'>⚡ BOOST YOUR GAMING PERFORMANCE</div>"
                        + "<div class='desc'>Claim exclusive game items, high-speed optimizations, and sponsored gaming benefits today.</div>"
                        + "<a href='" + AD_B_URL + "' class='cta-btn'>CLAIM REWARD NOW</a>"
                        + "</div>"
                        + "<div class='sub-card'>"
                        + "<script>atOptions = {'key' : '62a963ca90f7bb8b00e069773c757d6c','format' : 'iframe','height' : 60,'width' : 468,'params' : {}};</script>"
                        + "<script src='https://bauval.org/22/62a963ca90f7bb8b00e069773c757d6c'></script>"
                        + "</div>"
                        + "</body></html>";
                webView.loadDataWithBaseURL("https://bauval.org/", html, "text/html", "UTF-8", null);
            } else {
                // Ad A: Existing Adsterra / Bauval banner placements
                html = "<!DOCTYPE html>"
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
        }

        try {
            dialog.show();
            // Increment rotation counter and impression count strictly on actual display
            prefs.edit().putInt(KEY_ROTATION_INDEX, rotationIndex + 1).apply();
            recordImpression(activity);
        } catch (Throwable ignored) {}
    }

    private static void openAdUrl(Activity activity, String url) {
        if (activity == null || url == null) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            Toast.makeText(activity, "Opening partner offer...", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(intent);
            } catch (Throwable ignored) {}
        }
    }
}
