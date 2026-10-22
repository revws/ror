/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.tiles;

import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.services.CpuOverlayService;
import com.revror.kernelmanager.utils.AppIntegrity;

/** Starts and stops the REVROR live performance overlay. */
public class RevrorOverlayTile extends TileService {
    public static final String REVROR_SIGNATURE = "revror";
    private static final String PREFS = "ror_tools";

    @Override public void onStartListening() { render(isActive()); }

    @Override public void onClick() {
        AppIntegrity.enforce(this);
        if (isActive()) {
            startService(new Intent(this, CpuOverlayService.class).setAction(CpuOverlayService.ACTION_STOP));
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("overlay", false).apply();
            render(false);
            return;
        }
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Allow overlay access in REVROR first", Toast.LENGTH_LONG).show();
            render(false);
            return;
        }
        Intent intent = new Intent(this, CpuOverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean("overlay", true).apply();
        render(true);
    }

    private boolean isActive() { return getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean("overlay", false); }

    private void render(boolean active) {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(active ? "REVROR HUD On" : "REVROR HUD");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_revror_qs_overlay));
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
