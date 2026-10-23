package com.revror.kernelmanager.utils.kernel.compat;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.revror.kernelmanager.utils.GsmProfileManager;
import com.revror.kernelmanager.utils.RorLicenseManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Centralized Profile Registry for REVROR.
 *
 * Defines stable profile IDs, display names, engine mappings, and categories
 * across the entire application:
 * - Regular Profiles (Daily, Balance, Battery, CoolDown, Gaming, Ultra Gaming)
 * - Phoenix+ Profiles (Battery+, Balance+, Thermal Cooldown, Gaming Max, Smooth Gaming+, Ultra+, Esports, Apex)
 *
 * Synchronizes Game Space floating bubble, DashboardActivity, and native kernel engine.
 */
public final class ProfileRegistry {

    public static final class ProfileDescriptor {
        public final int id;
        public final String key;
        public final String displayName;
        public final String shortName;
        public final boolean isPhoenix;
        public final boolean requiresVip;
        public final int gsmwsWorkerId;
        public final String description;

        public ProfileDescriptor(int id, String key, String displayName, String shortName,
                                 boolean isPhoenix, boolean requiresVip, int gsmwsWorkerId,
                                 String description) {
            this.id = id;
            this.key = key;
            this.displayName = displayName;
            this.shortName = shortName;
            this.isPhoenix = isPhoenix;
            this.requiresVip = requiresVip;
            this.gsmwsWorkerId = gsmwsWorkerId;
            this.description = description;
        }
    }

    // Regular Profiles (IDs 0 - 5)
    public static final ProfileDescriptor REGULAR_DAILY = new ProfileDescriptor(
            0, "daily", "Daily Use", "Daily", false, false, 0, "Balanced everyday efficiency and responsiveness");
    public static final ProfileDescriptor REGULAR_BALANCE = new ProfileDescriptor(
            1, "balance", "Balance", "Balance", false, false, 1, "Optimized CPU scaling and power balance");
    public static final ProfileDescriptor REGULAR_BATTERY = new ProfileDescriptor(
            2, "battery", "Battery", "Battery", false, false, 2, "Aggressive power-saving and low frequency lock");
    public static final ProfileDescriptor REGULAR_COOLDOWN = new ProfileDescriptor(
            3, "cooldown", "CoolDown", "Cool", false, false, 4, "Thermal mitigation, governor throttle, cache drop");
    public static final ProfileDescriptor REGULAR_GAMING = new ProfileDescriptor(
            4, "gaming", "Gaming", "Gaming", false, false, 3, "High responsiveness, 85-100% dynamic frequency targets");
    public static final ProfileDescriptor REGULAR_ULTRA = new ProfileDescriptor(
            5, "ultra", "Ultra Gaming", "Ultra", false, true, 6, "Maximum sustained CPU/GPU clocks with locked permissions");

    // Phoenix+ Profiles (IDs 0 - 7)
    public static final ProfileDescriptor PHOENIX_BATTERY = new ProfileDescriptor(
            0, "phoenix_battery", "Battery+", "Battery+", true, true, 1, "Deep sleep optimization and sub-GHz frequency cap");
    public static final ProfileDescriptor PHOENIX_BALANCE = new ProfileDescriptor(
            1, "phoenix_balance", "Balance+", "Balance+", true, true, 1, "Next-gen intelligent kernel scheduler tuning");
    public static final ProfileDescriptor PHOENIX_COOLING = new ProfileDescriptor(
            2, "phoenix_cooling", "Thermal Cooldown", "Cool+", true, true, 1, "Intense thermal recovery while preserving UI smoothness");
    public static final ProfileDescriptor PHOENIX_GAMING_MAX = new ProfileDescriptor(
            3, "phoenix_gaming_max", "Gaming Max", "Gaming+", true, true, 2, "Pro esports tuning, schedutil governor, 85% min clock");
    public static final ProfileDescriptor PHOENIX_SMOOTH = new ProfileDescriptor(
            4, "phoenix_smooth", "Smooth Gaming+", "Smooth+", true, true, 3, "Frame-drop prevention with dynamic cluster migration");
    public static final ProfileDescriptor PHOENIX_ULTRA = new ProfileDescriptor(
            5, "phoenix_ultra", "Ultra+", "Ultra+", true, true, 2, "Uncapped frequencies, maximum GPU clock lock, 0444 sysfs lock");
    public static final ProfileDescriptor PHOENIX_ESPORTS = new ProfileDescriptor(
            6, "phoenix_esports", "Esports", "Esports", true, true, 3, "Ultra low input-to-display latency with touch 480Hz burst");
    public static final ProfileDescriptor PHOENIX_APEX = new ProfileDescriptor(
            7, "phoenix_apex", "Apex", "Apex", true, true, 2, "Extreme performance profile for demanding flagship titles");

    private static final List<ProfileDescriptor> REGULAR_LIST;
    private static final List<ProfileDescriptor> PHOENIX_LIST;

    static {
        List<ProfileDescriptor> reg = new ArrayList<>(6);
        reg.add(REGULAR_DAILY);
        reg.add(REGULAR_BALANCE);
        reg.add(REGULAR_BATTERY);
        reg.add(REGULAR_COOLDOWN);
        reg.add(REGULAR_GAMING);
        reg.add(REGULAR_ULTRA);
        REGULAR_LIST = Collections.unmodifiableList(reg);

        List<ProfileDescriptor> phx = new ArrayList<>(8);
        phx.add(PHOENIX_BATTERY);
        phx.add(PHOENIX_BALANCE);
        phx.add(PHOENIX_COOLING);
        phx.add(PHOENIX_GAMING_MAX);
        phx.add(PHOENIX_SMOOTH);
        phx.add(PHOENIX_ULTRA);
        phx.add(PHOENIX_ESPORTS);
        phx.add(PHOENIX_APEX);
        PHOENIX_LIST = Collections.unmodifiableList(phx);
    }

    private ProfileRegistry() {}

    /**
     * Returns all profile descriptors for the specified mode.
     */
    @NonNull
    public static List<ProfileDescriptor> getProfiles(boolean isPhoenix) {
        return isPhoenix ? PHOENIX_LIST : REGULAR_LIST;
    }

    /**
     * Resolves a ProfileDescriptor by its numeric ID and mode.
     */
    @NonNull
    public static ProfileDescriptor getProfile(int id, boolean isPhoenix) {
        List<ProfileDescriptor> list = getProfiles(isPhoenix);
        if (id >= 0 && id < list.size()) {
            return list.get(id);
        }
        return isPhoenix ? PHOENIX_BALANCE : REGULAR_BALANCE;
    }

    /**
     * Returns the human-readable display name for the given profile ID and mode.
     */
    @NonNull
    public static String getProfileName(int id, boolean isPhoenix) {
        return getProfile(id, isPhoenix).displayName;
    }

    /**
     * Returns the human-readable display name of the currently selected profile.
     */
    @NonNull
    public static String getActiveProfileName(@NonNull Context context) {
        boolean phoenix = GsmProfileManager.isPhoenixSelected(context);
        int id = GsmProfileManager.selectedProfile(context);
        return getProfileName(id, phoenix);
    }

    /**
     * Translates application profile ID to native gsmws worker ID.
     */
    public static int toNativeWorkerId(int profileId, boolean isPhoenix) {
        ProfileDescriptor desc = getProfile(profileId, isPhoenix);
        return desc.gsmwsWorkerId;
    }
}
