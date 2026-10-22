/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.tiles;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.AppIntegrity;
import com.revror.kernelmanager.utils.GameOptimizer;
import com.revror.kernelmanager.utils.GsmProfileManager;

/** Cycles every built-in REVROR profile from Android Quick Settings. */
public class RevrorProfileTile extends TileService {
    public static final String REVROR_SIGNATURE = "revror";

    @Override public void onStartListening() {
        AppIntegrity.enforce(this);
        render(GsmProfileManager.selectedProfile(this), Tile.STATE_ACTIVE);
    }

    @Override public void onClick() {
        AppIntegrity.enforce(this);
        int next = (GsmProfileManager.selectedProfile(this) + 1) % 5;
        render(next, Tile.STATE_UNAVAILABLE);
        GameOptimizer.applyProfile(this, next, (success, detail) -> {
            render(next, success ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            Toast.makeText(this, detail, Toast.LENGTH_SHORT).show();
        });
    }

    private void render(int profile, int state) {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel("REVROR • " + GsmProfileManager.getProfileName(profile));
        tile.setIcon(Icon.createWithResource(this, iconFor(profile)));
        tile.setState(state);
        tile.updateTile();
    }

    private int iconFor(int profile) {
        if (profile == 2) return R.drawable.ic_revror_qs_battery;
        if (profile == 3) return R.drawable.ic_revror_qs_cooldown;
        if (profile == 4) return R.drawable.ic_revror_qs_gaming;
        return R.drawable.ic_revror_qs_profile;
    }
}
