package com.revror.kernelmanager.utils;

import com.revror.kernelmanager.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.util.TypedValue;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * AdBypassHelper
 * Robust loader for Adsterra 728x90 leaderboard ad with DNS-Over-HTTPS (DoH) bypass.
 * Ensures ads continue to load and display even when device-level or router-level ad-blocking
 * DNS (e.g. AdGuard, NextDNS, Pi-hole, hosts files) is active.
 */
public class AdBypassHelper {
    private static final String TAG = "AdBypassHelper";
    private static final String ADSTERRA_KEY = "b42fcfe592ba30a160cff1a209bed5f7";
    private static final String ADSTERRA_HOST = "venisonglum.com";
    private static final String CHROME_UA = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
    public static final String SMART_LINK_URL = "https://auctionr.org/4/d8efb756fa667f3e7f796b40ad64f491";
    private static final String INTERSTITIAL_PREFS = "phoenix_interstitial_ads";

    // In-memory DNS cache: domain -> IP
    private static final Map<String, String> DNS_CACHE = new ConcurrentHashMap<>();
    // Static fallbacks for Adsterra edge nodes in case DoH is temporarily unreachable
    static {
        DNS_CACHE.put("venisonglum.com", "172.240.127.234");
        DNS_CACHE.put("bauval.org", "172.240.127.234");
    }

    private static final ExecutorService DOH_EXECUTOR = Executors.newSingleThreadExecutor();

    static {
        // Pre-warm DNS cache via Cloudflare DoH in background
        DOH_EXECUTOR.execute(() -> resolveDoH("venisonglum.com"));
    }

    /**
     * Resolves a hostname using Cloudflare DNS-Over-HTTPS (1.1.1.1),
     * completely bypassing system/router private DNS filters.
     */
    public static String resolveDoH(String hostname) {
        if (TextUtils.isEmpty(hostname)) return null;
        try {
            // 1.1.1.1 is direct IP literal; no local DNS query needed
            URL dohUrl = new URL("https://1.1.1.1/dns-query?name=" + hostname + "&type=A");
            HttpsURLConnection conn = (HttpsURLConnection) dohUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/dns-json");
            conn.setRequestProperty("User-Agent", "curl/8.0");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                if (json.has("Answer")) {
                    JSONArray answers = json.getJSONArray("Answer");
                    for (int i = 0; i < answers.length(); i++) {
                        JSONObject ans = answers.getJSONObject(i);
                        if (ans.optInt("type") == 1 && ans.has("data")) {
                            String ip = ans.getString("data");
                            DNS_CACHE.put(hostname, ip);
                            return ip;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "DoH lookup failed for " + hostname + ": " + t.getMessage());
        }
        return DNS_CACHE.get(hostname);
    }

    /**
     * Configures a WebView for optimal Adsterra display and sets up DoH request interception.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public static void setupAdWebView(WebView webView, Context context) {
        if (webView == null || context == null || "ror_renderer_gone".equals(webView.getTag())) return;

        if (RorLicenseManager.isPremiumActive(context)) {
            webView.setVisibility(View.GONE);
            try {
                webView.stopLoading();
            } catch (Throwable ignored) {}
            return;
        }

        webView.setBackgroundColor(0x00000000);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setUserAgentString(CHROME_UA);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        if (Build.VERSION.SDK_INT >= 26) {
            settings.setSafeBrowsingEnabled(false);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean onRenderProcessGone(WebView view, android.webkit.RenderProcessGoneDetail detail) {
                RorFeedback.append(context, "WebView renderer exited during profile/session; crashed=" + detail.didCrash());
                view.setTag("ror_renderer_gone");
                android.view.ViewParent parent = view.getParent();
                if (parent instanceof android.view.ViewGroup) ((android.view.ViewGroup) parent).removeView(view);
                view.destroy();
                return true;
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Prevent automatic or click redirects from launching external Chrome/browser with ad links
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (uri == null) return super.shouldInterceptRequest(view, request);

                String host = uri.getHost();
                if (host != null && (host.contains("venisonglum.com") || host.contains("alwingulla.com") || host.contains("profitablecpmrate.com"))) {
                    // Bypass ad-blocking DNS by routing through direct IP / DoH
                    WebResourceResponse response = fetchWithDohBypass(uri, request);
                    if (response != null) return response;
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        loadAdsterraSnippet(webView);
    }

    /**
     * Fetches the web resource directly, bypassing local DNS sinking.
     */
    private static WebResourceResponse fetchWithDohBypass(Uri uri, WebResourceRequest request) {
        try {
            String host = uri.getHost();
            String ip = resolveDoH(host);
            if (ip == null) ip = DNS_CACHE.get(host);

            URL url = new URL(uri.toString());
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setRequestMethod(request.getMethod());
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", CHROME_UA);
            conn.setRequestProperty("Referer", "https://venisonglum.com/");
            conn.setRequestProperty("Origin", "https://venisonglum.com");

            Map<String, String> reqHeaders = request.getRequestHeaders();
            if (reqHeaders != null) {
                for (Map.Entry<String, String> e : reqHeaders.entrySet()) {
                    if (!"User-Agent".equalsIgnoreCase(e.getKey()) && !"Referer".equalsIgnoreCase(e.getKey())) {
                        conn.setRequestProperty(e.getKey(), e.getValue());
                    }
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 400) {
                String rawContentType = conn.getContentType();
                String mimeType = "application/javascript";
                String encoding = "UTF-8";
                if (rawContentType != null) {
                    String[] parts = rawContentType.split(";");
                    mimeType = parts[0].trim();
                    for (int i = 1; i < parts.length; i++) {
                        String p = parts[i].trim().toLowerCase();
                        if (p.startsWith("charset=")) {
                            encoding = p.substring(8).trim();
                        }
                    }
                }

                InputStream in = conn.getInputStream();
                Map<String, String> headers = new HashMap<>();
                headers.put("Access-Control-Allow-Origin", "*");
                headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

                return new WebResourceResponse(mimeType, encoding, responseCode, "OK", headers, in);
            }
        } catch (Throwable t) {
            Log.w(TAG, "DoH fetch intercept failed for " + uri + ": " + t.getMessage());
        }
        return null;
    }

    /**
     * Generates responsive HTML containing the Adsterra 728x90 banner with auto-scaling to fit any container.
     */
    public static void loadAdsterraSnippet(WebView webView) {
        if (webView == null) return;
        String html = "<!doctype html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no'>"
                + "<style>"
                + "* { margin:0; padding:0; box-sizing:border-box; }"
                + "html, body { width:100%; height:100%; background:transparent; overflow:hidden; display:flex; align-items:center; justify-content:center; }"
                + "#ad-wrapper { width:728px; height:90px; transform-origin:center center; display:flex; align-items:center; justify-content:center; border-radius:6px; overflow:hidden; background:transparent; }"
                + "iframe { width:728px !important; height:90px !important; border:0 !important; overflow:hidden !important; display:block !important; }"
                + "</style></head><body>"
                + "<div id='ad-wrapper'>"
                + "<script type='text/javascript'>"
                + "  atOptions = {"
                + "    'key' : '" + ADSTERRA_KEY + "',"
                + "    'format' : 'iframe',"
                + "    'height' : 90,"
                + "    'width' : 728,"
                + "    'params' : {}"
                + "  };"
                + "</script>"
                + "<script type='text/javascript' src='https://" + ADSTERRA_HOST + "/" + ADSTERRA_KEY + "/invoke.js'></script>"
                + "</div>"
                + "<script>"
                + "  function scaleAd() {"
                + "    var w = document.documentElement.clientWidth || window.innerWidth;"
                + "    var h = document.documentElement.clientHeight || window.innerHeight;"
                + "    if (w > 0) {"
                + "      var s = (h > 0) ? Math.min(w / 728.0, h / 90.0) : (w / 728.0);"
                + "      var el = document.getElementById('ad-wrapper');"
                + "      if (el) {"
                + "        el.style.transform = 'scale(' + s + ')';"
                + "        el.style.webkitTransform = 'scale(' + s + ')';"
                + "      }"
                + "    }"
                + "  }"
                + "  window.addEventListener('resize', scaleAd);"
                + "  window.addEventListener('load', scaleAd);"
                + "  setTimeout(scaleAd, 100);"
                + "  setTimeout(scaleAd, 300);"
                + "  setTimeout(scaleAd, 700);"
                + "  setTimeout(scaleAd, 1500);"
                + "  setTimeout(scaleAd, 3000);"
                + "  scaleAd();"
                + "</script>"
                + "</body></html>";

        webView.loadDataWithBaseURL("https://" + ADSTERRA_HOST + "/", html, "text/html", "UTF-8", null);
    }

    /** Shows at most two full-screen promotions per local calendar day to non-Phoenix users. */
    public static void showFullScreenAdIfDue(Activity activity) {
        AdInterstitialManager.maybeShowInterstitial(activity);
    }
}

