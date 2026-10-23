/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.tiles;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.charging.SmartChargingManager;
import com.revror.kernelmanager.utils.AppIntegrity;
import com.revror.kernelmanager.utils.GameOptimizer;
import com.revror.kernelmanager.utils.GsmProfileManager;

/** One-tap protected cleanup, Smart Fast Charging, and CoolDown profile. */
public class RevrorCooldownTile extends TileService {
    public static final String REVROR_SIGNATURE = "revror";

    @Override public void onStartListening() {
        Tile tile = getQsTile();
        if (tile == null) return;

        SmartChargingManager scm = SmartChargingManager.getInstance(this);
        if (scm.isCharging()) {
            boolean active = scm.isActive();
            tile.setLabel(active ? "Fast & Cool (Active)" : "Fast & Cool");
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_flash));
            tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        } else {
            tile.setLabel("REVROR CoolDown");
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_revror_qs_cooldown));
            tile.setState(GsmProfileManager.selectedProfile(this) == 3 ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        }
        tile.updateTile();
    }

    @Override public void onClick() {
        AppIntegrity.enforce(this);
        SmartChargingManager scm = SmartChargingManager.getInstance(this);
        if (scm.isCharging()) {
            if (scm.isActive()) {
                scm.stopAndRestore();
                Toast.makeText(this, "Fast & Cool stopped • Settings restored", Toast.LENGTH_SHORT).show();
            } else {
                int mode = scm.getSelectedMode();
                scm.activateMode(mode, true);
                Toast.makeText(this, SmartChargingManager.getModeName(mode) + " active", Toast.LENGTH_SHORT).show();
            }
            onStartListening();
        } else {
            GameOptimizer.applyProfile(this, 3, (success, detail) -> {
                onStartListening();
                Toast.makeText(this, detail, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
