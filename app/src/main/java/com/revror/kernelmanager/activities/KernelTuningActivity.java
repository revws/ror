/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.text.InputType;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.root.RootUtils;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Only exposes nodes and choices actually discovered on this kernel. */
public class KernelTuningActivity extends AppCompatActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout controls;
    private TextView status;
    private boolean loading;

    @Override protected void onCreate(@Nullable Bundle state) {
        setTheme(R.style.RorTheme);
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(0xff070914);
        Button back = new Button(this); back.setText("Back"); back.setOnClickListener(v -> finish()); root.addView(back);
        status = new TextView(this); status.setTextColor(Color.WHITE); status.setTextSize(18);
        root.addView(status);
        Button refresh = new Button(this); refresh.setText("Refresh device controls");
        refresh.setOnClickListener(v -> refresh()); root.addView(refresh);
        ScrollView scroll = new ScrollView(this);
        controls = new LinearLayout(this); controls.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(controls); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root); refresh();
    }
    private void submit(Runnable action) {
        if (isDestroyed() || isFinishing() || executor.isShutdown()) return;
        try { executor.execute(action); } catch (RejectedExecutionException ignored) {}
    }
    private void ui(Runnable action) {
        runOnUiThread(() -> { if (!isDestroyed() && !isFinishing()) action.run(); });
    }
    private void refresh() {
        if (loading) return;
        loading = true; status.setText("Detecting this device's kernel controls…");
        submit(() -> {
            try {
                File detector = new File(getFilesDir(), "kernel_controls.sh");
                try (InputStream in = getAssets().open("kernel_controls.sh"); FileOutputStream out = new FileOutputStream(detector)) {
                    byte[] b = new byte[8192]; int n; while ((n=in.read(b)) != -1) out.write(b,0,n);
                }
                boolean rooted = RootUtils.rootAccess();
                String result = rooted ? RootUtils.runAndGetOutput("/system/bin/sh " + quote(detector.getAbsolutePath())) : "";
                List<Control> found = new ArrayList<>(); Set<String> paths = new HashSet<>();
                for (String line : result.split("\n")) {
                    String[] f = line.split("\\|", -1);
                    if (f.length == 6 && f[1].matches("/(sys|proc)/[A-Za-z0-9_./:@,+-]+") && paths.add(f[1])) found.add(new Control(f));
                }
                ui(() -> {
                    loading=false; controls.removeAllViews();
                    status.setText(rooted ? "Kernel tuning • " + found.size() + " detected controls" : "Kernel tuning requires root");
                    for (Control c : found) addControl(c);
                    if (rooted && found.isEmpty()) status.setText("No readable tuning controls exposed by this kernel");
                });
            } catch (Exception e) { ui(() -> { loading=false; status.setText("Detection failed: " + e.getMessage()); }); }
        });
    }
    private void addControl(Control c) {
        Button row = new Button(this); row.setAllCaps(false);
        row.setText(c.label + "\n" + c.current + (c.editable() ? "" : " • read only"));
        row.setOnClickListener(v -> edit(c)); controls.addView(row);
    }
    private void edit(Control c) {
        if (!c.editable()) { Toast.makeText(this,"This kernel does not expose editable values for this control",Toast.LENGTH_SHORT).show(); return; }
        if (!c.kind.equals("number")) {
            new AlertDialog.Builder(this, R.style.RorDialogTheme).setTitle(c.label)
                    .setItems(c.choices, (d, w) -> apply(c,c.choices[w])).setNegativeButton("Cancel",null).show();
        } else {
            EditText input = new EditText(this); input.setInputType(InputType.TYPE_CLASS_NUMBER); input.setText(c.current);
            AlertDialog dialog = new AlertDialog.Builder(this,R.style.RorDialogTheme).setTitle(c.label)
                    .setView(input).setPositiveButton("Apply",null).setNegativeButton("Cancel",null).create();
            dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value=input.getText().toString().trim();
                try {
                    long n=Long.parseLong(value);
                    if(n<0 || n>Integer.MAX_VALUE || (c.path.endsWith("swappiness") && n>200)
                            || (c.path.endsWith("ratio") && n>100)) throw new NumberFormatException();
                    dialog.dismiss(); apply(c,value);
                } catch(NumberFormatException e) { input.setError("Enter a valid value for this kernel control"); }
            })); dialog.show();
        }
    }
    private void apply(Control c, String value) {
        submit(() -> {
            String command = "";
            if (c.path.contains("/cpufreq/")) command += "setprop persist.revror.manual_cpu 1; ";
            if (c.path.contains("/devfreq/") || c.path.contains("/kgsl/")) command += "setprop persist.revror.manual_gpu 1; ";
            if (c.kind.equals("freq")) {
                boolean minimum=c.path.endsWith("min_freq");
                String opposite=c.path.substring(0,c.path.lastIndexOf('/')+1)
                        + (c.path.contains("/cpufreq/") ? (minimum?"scaling_max_freq":"scaling_min_freq") : (minimum?"max_freq":"min_freq"));
                command += "bound=$(cat " + quote(opposite) + " 2>/dev/null); if [ -z \"$bound\" ] || [ " + quote(value)
                        + (minimum?" -le ":" -ge ") + "\"$bound\" ]; then ";
            }
            command += "if printf '%s\\n' " + quote(value) + " > " + quote(c.path) + "; then echo ROR_WRITE_OK; fi; ";
            if(c.kind.equals("freq")) command += "else echo ROR_INVALID_BOUNDS; fi; ";
            command += "cat " + quote(c.path) + " 2>/dev/null";
            String result=RootUtils.runAndGetOutput(command);
            ui(() -> {
                Toast.makeText(this,result.contains("ROR_WRITE_OK")?"Applied; refreshing kernel values":
                        result.contains("ROR_INVALID_BOUNDS")?"Minimum must not exceed maximum":"Kernel rejected the change",Toast.LENGTH_LONG).show();
                refresh();
            });
        });
    }
    private static String quote(String s) { return "'"+s.replace("'","'\\''")+"'"; }
    private static class Control {
        final String kind,path,label,current; final String[] choices; final boolean writable;
        Control(String[] f) {
            kind=f[0]; path=f[1]; label=f[2]; current=f[3].trim(); writable=f[5].equals("1");
            Set<String> values=new LinkedHashSet<>();
            for(String s:f[4].trim().split("\\s+")) if(s.matches(kind.equals("freq")?"[0-9]+":"[A-Za-z0-9_-]+")) values.add(s);
            choices=values.toArray(new String[0]);
        }
        boolean editable() { return writable && !kind.equals("read") && (kind.equals("number") || choices.length>0); }
    }
    @Override protected void onDestroy() { executor.shutdownNow(); super.onDestroy(); }
}
