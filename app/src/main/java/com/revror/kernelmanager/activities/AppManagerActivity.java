/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.root.RootUtils;
import com.revror.kernelmanager.utils.ShizukuBridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppManagerActivity extends AppCompatActivity {

    public static class AppModel {
        public String name;
        public String pkg;
        public Drawable icon;
        public boolean isSystem;
        public boolean isEnabled;

        public AppModel(String name, String pkg, Drawable icon, boolean isSystem, boolean isEnabled) {
            this.name = name;
            this.pkg = pkg;
            this.icon = icon;
            this.isSystem = isSystem;
            this.isEnabled = isEnabled;
        }
    }

    private final List<AppModel> fullList = new ArrayList<>();
    private final List<AppModel> filteredList = new ArrayList<>();
    private AppAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private ProgressBar progressBar;
    private TextView countText;
    private EditText searchInput;
    private TextView filterAll, filterUser, filterSystem;
    private int currentFilter = 0; // 0=All, 1=User, 2=System
    private String queryText = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(R.style.RorTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_manager);

        progressBar = findViewById(R.id.app_manager_progress);
        countText = findViewById(R.id.app_manager_count);
        searchInput = findViewById(R.id.app_search_input);
        filterAll = findViewById(R.id.filter_all);
        filterUser = findViewById(R.id.filter_user);
        filterSystem = findViewById(R.id.filter_system);
        ListView listView = findViewById(R.id.apps_list_view);

        adapter = new AppAdapter();
        listView.setAdapter(adapter);

        findViewById(R.id.app_manager_back).setOnClickListener(v -> finish());
        findViewById(R.id.app_manager_refresh).setOnClickListener(v -> loadApps());

        filterAll.setOnClickListener(v -> setFilter(0));
        filterUser.setOnClickListener(v -> setFilter(1));
        filterSystem.setOnClickListener(v -> setFilter(2));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryText = s.toString().toLowerCase(Locale.US).trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        loadApps();
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        filterAll.setTextColor(filter == 0 ? 0xFF00E5FF : 0xFF8892B0);
        filterUser.setTextColor(filter == 1 ? 0xFF00E5FF : 0xFF8892B0);
        filterSystem.setTextColor(filter == 2 ? 0xFF00E5FF : 0xFF8892B0);
        applyFilter();
    }

    private void loadApps() {
        progressBar.setVisibility(View.VISIBLE);
        countText.setText("Scanning packages...");

        executor.execute(() -> {
            PackageManager pm = getPackageManager();
            List<PackageInfo> installed = pm.getInstalledPackages(0);
            List<AppModel> loaded = new ArrayList<>();

            for (PackageInfo pi : installed) {
                ApplicationInfo ai = pi.applicationInfo;
                if (ai == null) continue;
                String name = ai.loadLabel(pm).toString();
                String pkg = ai.packageName;
                Drawable icon = ai.loadIcon(pm);
                boolean isSystem = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                boolean isEnabled = ai.enabled;

                loaded.add(new AppModel(name, pkg, icon, isSystem, isEnabled));
            }

            Collections.sort(loaded, (a, b) -> a.name.compareToIgnoreCase(b.name));

            runOnUiThread(() -> {
                fullList.clear();
                fullList.addAll(loaded);
                progressBar.setVisibility(View.GONE);
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        filteredList.clear();
        for (AppModel app : fullList) {
            if (currentFilter == 1 && app.isSystem) continue;
            if (currentFilter == 2 && !app.isSystem) continue;
            if (!queryText.isEmpty()) {
                if (!app.name.toLowerCase(Locale.US).contains(queryText) && !app.pkg.toLowerCase(Locale.US).contains(queryText)) {
                    continue;
                }
            }
            filteredList.add(app);
        }
        countText.setText(String.format(Locale.US, "%d apps listed (%d total)", filteredList.size(), fullList.size()));
        adapter.notifyDataSetChanged();
    }

    private boolean isProtectedApp(String pkg) {
        if (pkg == null) return false;
        String lower = pkg.toLowerCase(Locale.ROOT);
        return lower.contains("shizuku") || lower.contains("revror") || getPackageName().equals(pkg);
    }

    private void runPrivileged(String actionName, String cmd, Runnable onDone) {
        executor.execute(() -> {
            boolean success = false;
            String out = "";
            if (RootUtils.rootAccess()) {
                RootUtils.runCommand(cmd);
                success = true;
            } else if (ShizukuBridge.granted()) {
                ShizukuBridge.Result r = ShizukuBridge.run(cmd);
                success = r.success;
                out = r.output;
            }

            final boolean finalSuccess = success;
            final String finalOut = out;
            runOnUiThread(() -> {
                if (finalSuccess) {
                    Toast.makeText(this, actionName + " succeeded", Toast.LENGTH_SHORT).show();
                } else if (ShizukuBridge.granted()) {
                    Toast.makeText(this, actionName + " failed: " + (finalOut.isEmpty() ? "Operation rejected" : finalOut), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, actionName + " requires Root or Shizuku authorization", Toast.LENGTH_SHORT).show();
                }
                if (onDone != null) onDone.run();
            });
        });
    }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredList.size(); }
        @Override public Object getItem(int position) { return filteredList.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AppManagerActivity.this).inflate(R.layout.item_app_manager, parent, false);
            }

            AppModel app = filteredList.get(position);

            ImageView icon = convertView.findViewById(R.id.app_item_icon);
            TextView name = convertView.findViewById(R.id.app_item_name);
            TextView pkg = convertView.findViewById(R.id.app_item_pkg);
            TextView badge = convertView.findViewById(R.id.app_item_badge);

            TextView btnLaunch = convertView.findViewById(R.id.btn_app_launch);
            TextView btnClear = convertView.findViewById(R.id.btn_app_clear);
            TextView btnDisable = convertView.findViewById(R.id.btn_app_disable);
            TextView btnStop = convertView.findViewById(R.id.btn_app_stop);

            icon.setImageDrawable(app.icon);
            name.setText(app.name);
            pkg.setText(app.pkg);

            if (!app.isEnabled) {
                badge.setText("DISABLED");
                badge.setTextColor(0xFFFF4D6D);
                btnDisable.setText("ENABLE");
                btnDisable.setTextColor(0xFF00E5FF);
            } else if (app.isSystem) {
                badge.setText("SYSTEM");
                badge.setTextColor(0xFFBAB6D2);
                btnDisable.setText("DISABLE");
                btnDisable.setTextColor(0xFFFB8500);
            } else {
                badge.setText("USER");
                badge.setTextColor(0xFF00E5FF);
                btnDisable.setText("DISABLE");
                btnDisable.setTextColor(0xFFFB8500);
            }

            btnLaunch.setOnClickListener(v -> {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.pkg);
                if (launchIntent != null) {
                    startActivity(launchIntent);
                } else {
                    Toast.makeText(AppManagerActivity.this, "Cannot launch " + app.name, Toast.LENGTH_SHORT).show();
                }
            });

            btnStop.setOnClickListener(v -> {
                if (isProtectedApp(app.pkg)) {
                    Toast.makeText(AppManagerActivity.this, app.name + " is protected from termination", Toast.LENGTH_SHORT).show();
                    return;
                }
                runPrivileged("Force stop " + app.name, "am force-stop " + app.pkg, null);
            });

            btnDisable.setOnClickListener(v -> {
                if (isProtectedApp(app.pkg)) {
                    Toast.makeText(AppManagerActivity.this, app.name + " is protected from being disabled", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (app.isEnabled) {
                    runPrivileged("Disable " + app.name, "pm disable-user --user 0 " + app.pkg, () -> {
                        app.isEnabled = false;
                        notifyDataSetChanged();
                    });
                } else {
                    runPrivileged("Enable " + app.name, "pm enable " + app.pkg, () -> {
                        app.isEnabled = true;
                        notifyDataSetChanged();
                    });
                }
            });

            btnClear.setOnClickListener(v -> {
                if (isProtectedApp(app.pkg)) {
                    Toast.makeText(AppManagerActivity.this, app.name + " is protected from data clearing", Toast.LENGTH_SHORT).show();
                    return;
                }
                runPrivileged("Clear data " + app.name, "pm clear " + app.pkg, null);
            });

            return convertView;
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}

