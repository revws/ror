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

/** One-tap gaming profile toggle. */
public class RevrorGamingTile extends TileService {
    public static final String REVROR_SIGNATURE = "revror";

    @Override public void onStartListening() { render(GsmProfileManager.selectedProfile(this) == 4); }

    @Override public void onClick() {
        AppIntegrity.enforce(this);
        boolean enable = GsmProfileManager.selectedProfile(this) != 4;
        int target = enable ? 4 : 0;
        GameOptimizer.applyProfile(this, target, (success, detail) -> {
            render(success && enable);
            Toast.makeText(this, detail, Toast.LENGTH_SHORT).show();
        });
    }

    private void render(boolean active) {
        Tile tile = getQsTile();
        if (tile == null) return;
        tile.setLabel(active ? "REVROR Gaming On" : "REVROR Gaming");
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_revror_qs_gaming));
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
