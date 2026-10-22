/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.tiles;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.AppIntegrity;
import com.revror.kernelmanager.utils.kernel.screen.KLapse;

/** REVROR-branded night color cycle backed by the device kernel when supported. */
public class RevrorNightTile extends TileService {
    public static final String REVROR_SIGNATURE = "revror";

    @Override public void onStartListening() { render(); }

    @Override public void onClick() {
        AppIntegrity.enforce(this);
        if (!KLapse.supported()) { render(); return; }
        int status = KLapse.getklapseEnable();
        int next = status == 0 ? 1 : status == 1 ? 2 : 0;
        KLapse.setklapseEnable(next, this);
        if (next != 0) {
            KLapse.setklapseRed(KLapse.getklapseRed(), this);
            KLapse.setklapseGreen(KLapse.getklapseGreen(), this);
            KLapse.setklapseBlue(KLapse.getklapseBlue(), this);
        }
        render();
    }

    private void render() {
        Tile tile = getQsTile();
        if (tile == null) return;
        int status = KLapse.supported() ? KLapse.getklapseEnable() : 0;
        String label = !KLapse.supported() ? "REVROR Night Unsupported"
                : status == 1 ? "REVROR Night • Time"
                : status == 2 ? "REVROR Night • Brightness" : "REVROR Night";
        tile.setLabel(label);
        tile.setIcon(Icon.createWithResource(this, R.drawable.ic_revror_qs_night));
        tile.setState(status == 0 ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
    }
}
