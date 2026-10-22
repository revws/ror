package com.revror.kernelmanager.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.revror.kernelmanager.utils.GsmProfileManager;

import java.util.Calendar;

public class ProfileScheduleReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int profile = intent == null ? 2 : intent.getIntExtra("profile", 2);
        GsmProfileManager.apply(context, profile, null);
    }

    public static void schedule(Context context, int hour, int minute) {
        Intent intent = new Intent(context, ProfileScheduleReceiver.class).putExtra("profile", 2);
        PendingIntent pending = PendingIntent.getBroadcast(context, 6021, intent, PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, hour); time.set(Calendar.MINUTE, minute); time.set(Calendar.SECOND, 0);
        if (time.before(Calendar.getInstance())) time.add(Calendar.DAY_OF_YEAR, 1);
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null) alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pending);
        context.getSharedPreferences("ror_tools", Context.MODE_PRIVATE).edit()
                .putBoolean("schedule_enabled", true).putInt("schedule_hour", hour).putInt("schedule_minute", minute).apply();
    }

    public static void cancel(Context context) {
        PendingIntent pending = PendingIntent.getBroadcast(context, 6021, new Intent(context, ProfileScheduleReceiver.class),
                PendingIntent.FLAG_NO_CREATE | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarm != null && pending != null) { alarm.cancel(pending); pending.cancel(); }
        context.getSharedPreferences("ror_tools", Context.MODE_PRIVATE).edit().putBoolean("schedule_enabled", false).apply();
    }

    public static void restore(Context context) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("ror_tools", Context.MODE_PRIVATE);
        if (prefs.getBoolean("schedule_enabled", false)) schedule(context,
                prefs.getInt("schedule_hour", 22), prefs.getInt("schedule_minute", 0));
    }
}
