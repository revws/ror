package com.revror.kernelmanager.utils;

import android.content.Context;
import android.os.Process;

import com.revror.kernelmanager.utils.root.RootUtils;

import java.io.File;
import java.io.FileOutputStream;

/** Launches the bundled version directly; never falls back to an old shared payload. */
final class ProfileWorkerRunner {
    private ProfileWorkerRunner() {}

    static void start(Context context, File payload, boolean binary, String properties) throws Exception {
        String prefix = context.getFilesDir().getAbsolutePath() + "/";
        String pid = new File(context.getFilesDir(), "ror_worker.pid").getAbsolutePath();
        File policy = new File(context.getFilesDir(), "cpu_policy_" + com.revror.kernelmanager.BuildConfig.VERSION_CODE + ".sh");
        try (java.io.InputStream input = context.getAssets().open("cpu_profile_policy.sh");
             FileOutputStream output = new FileOutputStream(policy)) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        File log = new File(context.getFilesDir(), "ror_worker_output.log");
        if (!log.exists() || log.length() > 1024 * 1024) {
            try (FileOutputStream ignored = new FileOutputStream(log)) { /* Bound the worker log. */ }
        }
        // Freeze each identified worker before walking its descendants so it cannot fork a replacement.
        String command = "stop_tree() { [ \"$1\" = \"" + Process.myPid() + "\" ] && return; "
                + "kill -STOP \"$1\" 2>/dev/null || return; "
                + "for child in $(ps -A -o PID,PPID | awk -v parent=\"$1\" '$2 == parent {print $1}'); do stop_tree \"$child\"; done; "
                + "kill -KILL \"$1\" 2>/dev/null; }; "
                + "for p in $(ps -A -o PID,ARGS 2>/dev/null | awk -v prefix=" + quote(prefix)
                + " '($2 == \"sh\" || $2 == \"/system/bin/sh\") && "
                + "((index($3,prefix)==1 && $3 ~ /\\/(ror_(regular|phoenix)_[0-9]+|cpu_policy_[0-9]+|codex_[0-9]+|library_[0-9]+)[.]sh$/) "
                + "|| $3 == \"/data/local/tmp/phx\" || $3 == \"/data/local/tmp/library\") {print $1; next} "
                + "$2 ~ /^(codex|library)_[0-9]+[.]sh$/ {print $1}'); do stop_tree \"$p\"; done; "
                + properties + "; "
                + "if [ ! -r " + quote(payload.getAbsolutePath()) + " ]; then echo ROR_PAYLOAD_UNREADABLE; "
                + "else "
                + (binary ? "chmod 700 " + quote(payload.getAbsolutePath()) + "; " : "")
                + "nohup " + (binary ? "" : "/system/bin/sh ") + quote(payload.getAbsolutePath())
                + " >> " + quote(log.getAbsolutePath()) + " 2>&1 < /dev/null & "
                + "p=$!; echo \"$p\" > " + quote(pid) + "; sleep 1; "
                + "if kill -0 \"$p\" 2>/dev/null; then echo ROR_WORKER_STARTED:$p; "
                + "nohup /system/bin/sh " + quote(policy.getAbsolutePath()) + " $(getprop persist.revror.app.profile) >> "
                + quote(log.getAbsolutePath()) + " 2>&1 < /dev/null & "
                + "else echo ROR_WORKER_FAILED; tail -n 8 " + quote(log.getAbsolutePath()) + "; fi; fi";
        String result = RootUtils.runAndGetOutput(command);
        if (!result.contains("ROR_WORKER_STARTED:")) {
            throw new IllegalStateException("Profile worker did not start: " + result);
        }
        RorFeedback.append(context, result.trim());
    }

    private static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
