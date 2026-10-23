package com.revror.kernelmanager.views.dialog;

import android.app.Activity;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.touch.AimBoostEngine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Interactive Aim Boost & Touch Precision Dialog with Live Touch Diagnostic Pad.
 */
public final class AimBoostDialog {

    private AimBoostDialog() {}

    public static void show(@NonNull Activity activity) {
        if (activity.isFinishing()) return;

        if (!com.revror.kernelmanager.utils.RorLicenseManager.isPremiumActive(activity)) {
            com.revror.kernelmanager.utils.RorLicenseManager.showVipRequiredDialog(activity, "Aim Boost (Phoenix VIP)", () -> {
                show(activity);
            });
            return;
        }

        View root = LayoutInflater.from(activity).inflate(R.layout.dialog_aim_boost, null);

        TextView activeModeDesc = root.findViewById(R.id.aim_active_mode_desc);

        LinearLayout cardCompetitive = root.findViewById(R.id.aim_card_competitive);
        LinearLayout cardBalanced = root.findViewById(R.id.aim_card_balanced);
        LinearLayout cardEfficient = root.findViewById(R.id.aim_card_efficient);

        TextView titleComp = root.findViewById(R.id.aim_title_competitive);
        TextView titleBal = root.findViewById(R.id.aim_title_balanced);
        TextView titleEff = root.findViewById(R.id.aim_title_efficient);

        TextView badgeComp = root.findViewById(R.id.aim_badge_competitive);
        TextView badgeBal = root.findViewById(R.id.aim_badge_balanced);
        TextView badgeEff = root.findViewById(R.id.aim_badge_efficient);

        TextView valDelay = root.findViewById(R.id.touch_val_delay);
        TextView valHz = root.findViewById(R.id.touch_val_hz);
        TextView valJitter = root.findViewById(R.id.touch_val_jitter);
        TextView valPointers = root.findViewById(R.id.touch_val_pointers);

        FrameLayout touchArea = root.findViewById(R.id.touch_test_area);
        TextView touchHint = root.findViewById(R.id.touch_hint_text);

        Button btnApply = root.findViewById(R.id.btn_aim_apply);
        Button btnClose = root.findViewById(R.id.btn_aim_close);

        final int[] selectedMode = new int[]{AimBoostEngine.getCurrentMode(activity)};

        Runnable updateCardsUi = () -> {
            int cur = selectedMode[0];

            cardCompetitive.setBackgroundResource(cur == AimBoostEngine.MODE_COMPETITIVE ? R.drawable.ror_active_banner : R.drawable.ror_tool_card_bg);
            titleComp.setTextColor(cur == AimBoostEngine.MODE_COMPETITIVE ? 0xFFFF0055 : 0xFFFFFFFF);
            badgeComp.setText(cur == AimBoostEngine.MODE_COMPETITIVE ? "ACTIVE" : "PRO GAMING");
            badgeComp.setTextColor(cur == AimBoostEngine.MODE_COMPETITIVE ? 0xFF00F5D4 : 0xFFFF0055);

            cardBalanced.setBackgroundResource(cur == AimBoostEngine.MODE_BALANCED ? R.drawable.ror_active_banner : R.drawable.ror_tool_card_bg);
            titleBal.setTextColor(cur == AimBoostEngine.MODE_BALANCED ? 0xFF00F5D4 : 0xFFFFFFFF);
            badgeBal.setText(cur == AimBoostEngine.MODE_BALANCED ? "ACTIVE" : "RECOMMENDED");
            badgeBal.setTextColor(cur == AimBoostEngine.MODE_BALANCED ? 0xFF00F5D4 : 0xFF2DE49A);

            cardEfficient.setBackgroundResource(cur == AimBoostEngine.MODE_EFFICIENT ? R.drawable.ror_active_banner : R.drawable.ror_tool_card_bg);
            titleEff.setTextColor(cur == AimBoostEngine.MODE_EFFICIENT ? 0xFF2DE49A : 0xFFFFFFFF);
            badgeEff.setText(cur == AimBoostEngine.MODE_EFFICIENT ? "ACTIVE" : "BATTERY");
            badgeEff.setTextColor(cur == AimBoostEngine.MODE_EFFICIENT ? 0xFF00F5D4 : 0xFF7A85A8);

            switch (cur) {
                case AimBoostEngine.MODE_COMPETITIVE:
                    activeModeDesc.setText("Active: Competitive Mode (480Hz Raw Touch • Zero Delay)");
                    break;
                case AimBoostEngine.MODE_EFFICIENT:
                    activeModeDesc.setText("Active: Efficient Mode (120Hz Standard • Battery Saver)");
                    break;
                case AimBoostEngine.MODE_BALANCED:
                default:
                    activeModeDesc.setText("Active: Balanced Mode (240Hz High Polling • Responsive)");
                    break;
            }
        };

        updateCardsUi.run();

        cardCompetitive.setOnClickListener(v -> {
            selectedMode[0] = AimBoostEngine.MODE_COMPETITIVE;
            updateCardsUi.run();
        });

        cardBalanced.setOnClickListener(v -> {
            selectedMode[0] = AimBoostEngine.MODE_BALANCED;
            updateCardsUi.run();
        });

        cardEfficient.setOnClickListener(v -> {
            selectedMode[0] = AimBoostEngine.MODE_EFFICIENT;
            updateCardsUi.run();
        });

        // Interactive Touch Diagnostic Engine
        final Deque<Long> touchTimes = new ArrayDeque<>();
        final Deque<Double> intervals = new ArrayDeque<>();

        touchArea.setOnTouchListener((v, event) -> {
            long nowUptime = SystemClock.uptimeMillis();
            long eventTime = event.getEventTime();
            long dispatchLatency = Math.max(0, nowUptime - eventTime);
            int pointers = event.getPointerCount();

            touchTimes.addLast(eventTime);
            while (!touchTimes.isEmpty() && (eventTime - touchTimes.peekFirst()) > 1000) {
                touchTimes.pollFirst();
            }

            if (touchTimes.size() >= 2) {
                double rateHz = (touchTimes.size() - 1) * 1000.0 / Math.max(1, eventTime - touchTimes.peekFirst());
                valHz.setText(String.format(Locale.US, "%.0f Hz", rateHz));
            }

            int historySize = event.getHistorySize();
            long lastT = eventTime;
            for (int h = 0; h < historySize; h++) {
                long ht = event.getHistoricalEventTime(h);
                double dt = Math.abs(lastT - ht);
                if (dt > 0 && dt < 100) {
                    intervals.addLast(dt);
                }
                lastT = ht;
            }
            while (intervals.size() > 30) {
                intervals.pollFirst();
            }

            double jitter = 0;
            if (!intervals.isEmpty()) {
                double sum = 0;
                for (double iv : intervals) sum += iv;
                double mean = sum / intervals.size();
                double sumSq = 0;
                for (double iv : intervals) {
                    double d = iv - mean;
                    sumSq += d * d;
                }
                jitter = Math.sqrt(sumSq / intervals.size());
            }

            valDelay.setText(String.format(Locale.US, "%d ms", dispatchLatency));
            valJitter.setText(String.format(Locale.US, "%.2f ms", jitter));
            valPointers.setText(String.valueOf(pointers));

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    touchHint.setText(pointers > 1 ? (pointers + " pointers tracking...") : "Tracking touch input...");
                    touchArea.setBackgroundColor(0x3500F5D4);
                    break;
                case MotionEvent.ACTION_MOVE:
                    touchHint.setText(String.format(Locale.US, "Active input • %d ms delay • %d pointers", dispatchLatency, pointers));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    touchArea.setBackgroundColor(0x15FFFFFF);
                    touchHint.setText("Touch, swipe, or aim here to measure");
                    valPointers.setText("0");
                    break;
            }

            return true;
        });

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.RorDialogTheme)
                .setView(root)
                .create();

        btnApply.setOnClickListener(v -> {
            btnApply.setEnabled(false);
            btnApply.setText("Applying Mode...");
            AimBoostEngine.applyMode(activity, selectedMode[0], () -> {
                btnApply.setEnabled(true);
                btnApply.setText("Apply Aim Mode");
                updateCardsUi.run();
                Toast.makeText(activity, "Aim Boost configuration applied successfully!", Toast.LENGTH_SHORT).show();
            });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            DisplayMetrics dm = activity.getResources().getDisplayMetrics();
            int width = (int) (dm.widthPixels * 0.92);
            dialog.getWindow().setLayout(width, -2);
        }
    }
}
