/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.revror.kernelmanager.utils.RorFeedback;
import java.io.File;

public class CleanupReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        removeChildren(context.getCacheDir());
        RorFeedback.append(context, "Automatic cleanup completed");
    }
    private static void removeChildren(File dir) {
        File[] files = dir == null ? null : dir.listFiles(); if (files == null) return;
        for (File file : files) { if (file.isDirectory()) removeChildren(file); file.delete(); }
    }
}
