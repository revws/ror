/* Copyright (C) 2026 REVROR contributors - GPL-3.0-or-later */
package com.revror.kernelmanager.charging;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Automatically activates Smart Fast Charging on charger connection
 * and restores previous profiles and system parameters upon disconnect.
 */
public class ChargingCooldownReceiver extends BroadcastReceiver {

    private static final String TAG = "ChargingCooldownReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        SmartChargingManager manager = SmartChargingManager.getInstance(context);

        if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
            Log.i(TAG, "Charger connected. Auto Fast & Cool enabled=" + manager.isAutoFastCoolEnabled());
            if (manager.isAutoFastCoolEnabled()) {
                int mode = manager.getSelectedMode();
                manager.activateMode(mode, false);
            }
        } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
            Log.i(TAG, "Charger disconnected. Restoring settings.");
            manager.stopAndRestore();
        }
    }
}
