/*
 * Copyright (C) 2020-2021 sunilpaulmathew <sunil.kde@gmail.com>
 *
 * This file is part of REVROR Kernel Manager, which is a heavily modified version of Kernel Adiutor,
 * originally developed by Willi Ye <williye97@gmail.com>
 *
 * Both REVROR Kernel Manager & Kernel Adiutor are free softwares: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REVROR Kernel Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REVROR Kernel Manager.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.revror.kernelmanager.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.fragments.kernel.boost.CPUBoostFragment;
import com.revror.kernelmanager.fragments.kernel.boost.PowerHalFragment;
import com.revror.kernelmanager.fragments.kernel.boost.StuneBoostFragment;
import com.revror.kernelmanager.utils.kernel.cpu.boost.CPUBoost;
import com.revror.kernelmanager.utils.kernel.cpu.boost.CPUInputBoost;
import com.revror.kernelmanager.utils.kernel.cpu.boost.StuneBoost;
import com.revror.kernelmanager.utils.kernel.cpu.boost.VoxPopuli;

import in.sunilpaulmathew.sCommon.Adapters.sPagerAdapter;

/**
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on May 19, 2020
 */

public class CPUBoostActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tablayout);

        TabLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.view_pager);

        sPagerAdapter adapter = new sPagerAdapter(getSupportFragmentManager());
        if (CPUBoost.getInstance().supported() || CPUInputBoost.getInstance().supported()) {
            adapter.AddFragment(new CPUBoostFragment(), getString(R.string.cpu_boost));
        }
        if (VoxPopuli.hasVoxpopuliTunable()) {
            adapter.AddFragment(new PowerHalFragment(), getString(R.string.powerhal));
        }
        if (StuneBoost.supported()) {
            adapter.AddFragment(new StuneBoostFragment(), getString(R.string.stune_boost));
        }

        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
