package com.revror.kernelmanager.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.utils.RorLicenseManager;

public class RorProBenefitsActivity extends AppCompatActivity {

    private TextView statusBadge;
    private TextView heroStatus;
    private ImageView heroIcon;
    private MaterialButton btnActionActivate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ror_pro_benefits);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        statusBadge = findViewById(R.id.benefits_status_badge);
        heroStatus = findViewById(R.id.benefits_hero_status);
        heroIcon = findViewById(R.id.benefits_hero_icon);
        btnActionActivate = findViewById(R.id.btn_action_activate);

        updateStatus();

        if (btnActionActivate != null) {
            btnActionActivate.setOnClickListener(v -> {
                RorLicenseManager.showVipRequiredDialog(this, "Phoenix Benefits", this::updateStatus);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        boolean active = RorLicenseManager.isPremiumActive(this);

        if (statusBadge != null) {
            if (active) {
                statusBadge.setText("PHOENIX ACTIVE");
                statusBadge.setTextColor(Color.rgb(255, 215, 0));
                statusBadge.setBackgroundColor(Color.rgb(36, 30, 8));
            } else {
                statusBadge.setText("FREE TIER");
                statusBadge.setTextColor(Color.rgb(255, 82, 119));
                statusBadge.setBackgroundColor(Color.rgb(36, 21, 37));
            }
        }

        if (heroIcon != null) {
            heroIcon.setImageResource(active ? R.drawable.ic_vip_active_badge : R.drawable.ic_vip_badge_inactive);
        }

        if (heroStatus != null) {
            if (active) {
                heroStatus.setText(RorLicenseManager.getLicenseStatusDescription(this));
            } else {
                heroStatus.setText("Unlock Full Hardware Power • Zero Ads • Esports Optimization");
            }
        }

        View pricingCard = findViewById(R.id.ror_pro_pricing_card);
        if (pricingCard != null) {
            pricingCard.setVisibility(active ? View.GONE : View.VISIBLE);
        }

        if (btnActionActivate != null) {
            if (active) {
                btnActionActivate.setText("PHOENIX IS ACTIVE");
                btnActionActivate.setEnabled(false);
                btnActionActivate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1B5E20")));
            } else {
                btnActionActivate.setText("ACTIVATE PHOENIX KEY");
                btnActionActivate.setEnabled(true);
                btnActionActivate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#7220A8")));
            }
        }
    }
}

