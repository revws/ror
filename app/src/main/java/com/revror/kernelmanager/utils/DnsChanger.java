package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.root.RootUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * System-wide DNS Changer with built-in adblocking, privacy, security, and gaming DNS providers.
 */
public class DnsChanger {

    private static final String PREF_NAME = "ror_dns_changer";
    private static final String KEY_ACTIVE_DNS = "active_dns";
    private static final String KEY_DNS_HOSTNAME = "dns_hostname";
    private static final String KEY_DNS_PRIMARY = "dns_primary";
    private static final String KEY_DNS_SECONDARY = "dns_secondary";

    public static class DnsProfile {
        public final String name;
        public final String badge;
        public final String description;
        public final String tlsHostname;
        public final String primaryIp;
        public final String secondaryIp;

        public DnsProfile(String name, String badge, String description, String tlsHostname, String primaryIp, String secondaryIp) {
            this.name = name;
            this.badge = badge;
            this.description = description;
            this.tlsHostname = tlsHostname;
            this.primaryIp = primaryIp;
            this.secondaryIp = secondaryIp;
        }
    }

    public static List<DnsProfile> getProfiles() {
        List<DnsProfile> list = new ArrayList<>();
        list.add(new DnsProfile("Cloudflare 1.1.1.1", "LOWEST PING", "Ultra-fast response, zero logging, lowest latency for gaming", "1dot1dot1dot1.cloudflare-dns.com", "1.1.1.1", "1.0.0.1"));
        list.add(new DnsProfile("AdGuard DNS AdBlocker", "AD-BLOCK", "System-wide ad, popup, and tracker blocking in apps & games", "dns.adguard.com", "94.140.14.14", "94.140.15.15"));
        list.add(new DnsProfile("Cloudflare Family & Anti-Malware", "FAMILY", "Blocks malware, phishing, and adult content automatically", "family.cloudflare-dns.com", "1.1.1.3", "1.0.0.3"));
        list.add(new DnsProfile("Google Public DNS", "GLOBAL", "Global high availability, speed, and anycast routing", "dns.google", "8.8.8.8", "8.8.4.4"));
        list.add(new DnsProfile("Quad9 DNS Threat Defense", "SECURITY", "Privacy-focused with malicious domain blocking", "dns.quad9.net", "9.9.9.9", "149.112.112.112"));
        list.add(new DnsProfile("AdGuard Family Protection", "PROTECT", "Blocks ads, trackers, and explicit content with SafeSearch", "family.adguard-dns.com", "94.140.14.15", "94.140.15.16"));
        list.add(new DnsProfile("Cisco OpenDNS Home", "FAST", "Reliable anycast DNS with intelligent phishing defense", "dns.opendns.com", "208.67.222.222", "208.67.220.220"));
        list.add(new DnsProfile("CleanBrowsing Security", "SECURE", "Blocks malicious, scam, cryptojacking, and malware domains", "security-filter-dns.cleanbrowsing.org", "185.228.168.9", "185.228.169.9"));
        list.add(new DnsProfile("AliDNS Alibaba Cloud", "ASIA FAST", "Ultra-fast routing in Asia-Pacific region", "dns.alidns.com", "223.5.5.5", "223.6.6.6"));
        return list;
    }

    public static String getActiveDns(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_DNS, "Default ISP DNS");
    }

    /**
     * Shows the modern DNS selection dialog with custom card views.
     */
    public static void showDialog(Activity activity) {
        if (activity == null || activity.isFinishing()) return;

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_dns_changer, null);
        TextView activeStatus = root.findViewById(R.id.dns_active_status);
        LinearLayout container = root.findViewById(R.id.dns_profiles_container);
        Button btnCustom = root.findViewById(R.id.btn_dns_custom);
        Button btnRestore = root.findViewById(R.id.btn_dns_restore);
        Button btnClose = root.findViewById(R.id.btn_dns_close);

        String active = getActiveDns(activity);
        activeStatus.setText("Active: " + active);

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setView(root)
                .create();

        DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        List<DnsProfile> profiles = getProfiles();

        for (DnsProfile profile : profiles) {
            boolean isActive = profile.name.equals(active);

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12, dm), dp(10, dm), dp(12, dm), dp(10, dm));
            card.setBackgroundResource(R.drawable.ror_tool_card_bg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(8, dm);
            card.setLayoutParams(lp);

            LinearLayout titleRow = new LinearLayout(activity);
            titleRow.setOrientation(LinearLayout.HORIZONTAL);

            TextView name = new TextView(activity);
            name.setText((isActive ? " " : "") + profile.name);
            name.setTextColor(isActive ? 0xFF00E5FF : 0xFFFFFFFF);
            name.setTextSize(13f);
            name.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            titleRow.addView(name, nameLp);

            TextView badge = new TextView(activity);
            badge.setText(profile.badge);
            badge.setTextSize(9f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextColor(isActive ? 0xFF00E5FF : 0xFFD1A2FF);
            badge.setBackgroundResource(R.drawable.ror_chip);
            badge.setPadding(dp(6, dm), dp(2, dm), dp(6, dm), dp(2, dm));
            titleRow.addView(badge);

            card.addView(titleRow);

            TextView desc = new TextView(activity);
            desc.setText(profile.description);
            desc.setTextColor(0xFF8B8EA8);
            desc.setTextSize(10.5f);
            desc.setPadding(0, dp(3, dm), 0, 0);
            card.addView(desc);

            TextView ipInfo = new TextView(activity);
            ipInfo.setText("TLS: " + profile.tlsHostname + " • IP: " + profile.primaryIp);
            ipInfo.setTextColor(0xFF5A6078);
            ipInfo.setTextSize(9f);
            ipInfo.setPadding(0, dp(2, dm), 0, 0);
            card.addView(ipInfo);

            card.setOnClickListener(v -> {
                dialog.dismiss();
                applyProfile(activity, profile);
            });

            container.addView(card);
        }

        btnCustom.setOnClickListener(v -> {
            dialog.dismiss();
            promptCustomDns(activity);
        });

        btnRestore.setOnClickListener(v -> {
            dialog.dismiss();
            resetToDefault(activity);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private static int dp(int val, DisplayMetrics dm) {
        return Math.round(val * dm.density);
    }

    private static void promptCustomDns(Activity activity) {
        EditText input = new EditText(activity);
        input.setHint("e.g. dns.adguard.com or 1.1.1.1");
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF7A85A8);

        new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setTitle("Custom DNS")
                .setMessage("Enter a Private DNS hostname (e.g. dns.quad9.net) or Primary DNS IP:")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Apply", (d, w) -> {
                    String custom = input.getText().toString().trim();
                    if (!custom.isEmpty()) {
                        applyCustom(activity, custom);
                    }
                })
                .show();
    }

    public static void applyProfile(Activity activity, DnsProfile profile) {
        new Thread(() -> {
            executeDnsChange(profile.tlsHostname, profile.primaryIp, profile.secondaryIp);
            activity.runOnUiThread(() -> {
                activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                        .putString(KEY_ACTIVE_DNS, profile.name)
                        .putString(KEY_DNS_HOSTNAME, profile.tlsHostname)
                        .putString(KEY_DNS_PRIMARY, profile.primaryIp)
                        .putString(KEY_DNS_SECONDARY, profile.secondaryIp)
                        .apply();
                Toast.makeText(activity, "DNS changed to: " + profile.name, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    public static void applyCustom(Activity activity, String custom) {
        new Thread(() -> {
            String tls = custom.contains(".") && !custom.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$") ? custom : null;
            String ip = tls == null ? custom : "1.1.1.1";
            executeDnsChange(tls, ip, ip);
            activity.runOnUiThread(() -> {
                activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                        .putString(KEY_ACTIVE_DNS, "Custom - " + custom)
                        .putString(KEY_DNS_HOSTNAME, tls != null ? tls : "")
                        .putString(KEY_DNS_PRIMARY, ip)
                        .putString(KEY_DNS_SECONDARY, ip)
                        .apply();
                Toast.makeText(activity, "Custom DNS applied: " + custom, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    public static void resetToDefault(Activity activity) {
        new Thread(() -> {
            String script = "settings put global private_dns_mode opportunistic; " +
                    "settings delete global private_dns_specifier; " +
                    "iptables -t nat -F OUTPUT 2>/dev/null";
            RootUtils.runCommand(script);
            activity.runOnUiThread(() -> {
                activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply();
                Toast.makeText(activity, "Default Carrier / Wi-Fi DNS restored", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    public static void applyOnBoot(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (sp.contains(KEY_ACTIVE_DNS)) {
            String tls = sp.getString(KEY_DNS_HOSTNAME, null);
            String p1 = sp.getString(KEY_DNS_PRIMARY, null);
            String p2 = sp.getString(KEY_DNS_SECONDARY, null);
            if ((tls != null && !tls.isEmpty()) || (p1 != null && !p1.isEmpty())) {
                executeDnsChange(tls, p1, p2);
            }
        }
    }

    private static void executeDnsChange(String tlsHostname, String primaryIp, String secondaryIp) {
        StringBuilder sb = new StringBuilder();

        // 1. Android 9+ Private DNS (DNS-over-TLS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && tlsHostname != null && !tlsHostname.isEmpty()) {
            sb.append("settings put global private_dns_mode hostname; ");
            sb.append("settings put global private_dns_specifier ").append(tlsHostname).append("; ");
        } else {
            sb.append("settings put global private_dns_mode off; ");
        }

        // 2. Set system properties for network daemons
        if (primaryIp != null && !primaryIp.isEmpty()) {
            sb.append("setprop net.dns1 ").append(primaryIp).append("; ");
            if (secondaryIp != null && !secondaryIp.isEmpty()) {
                sb.append("setprop net.dns2 ").append(secondaryIp).append("; ");
            }
        }

        // 3. Kernel iptables redirection for port 53 (redirect all standard DNS traffic)
        if (primaryIp != null && !primaryIp.isEmpty()) {
            int myUid = android.os.Process.myUid();
            sb.append("iptables -t nat -F OUTPUT 2>/dev/null; ");
            // Whitelist REVROR app traffic so adblock DNS never intercepts or breaks REVROR connections
            sb.append("iptables -t nat -A OUTPUT -m owner --uid-owner ").append(myUid).append(" -j ACCEPT 2>/dev/null; ");
            sb.append("iptables -t nat -A OUTPUT -p udp --dport 53 -j DNAT --to-destination ").append(primaryIp).append(":53 2>/dev/null; ");
            sb.append("iptables -t nat -A OUTPUT -p tcp --dport 53 -j DNAT --to-destination ").append(primaryIp).append(":53 2>/dev/null; ");
        }

        // 4. Flush Android DNS resolver cache
        sb.append("ndc resolver flushdefaultif 2>/dev/null; ");
        sb.append("ndc resolver flushif wlan0 2>/dev/null; ");
        sb.append("ndc resolver flushif rmnet0 2>/dev/null; ");
        sb.append("ndc resolver flushif rmnet_data0 2>/dev/null; ");

        RootUtils.runCommand(sb.toString());
    }
}
