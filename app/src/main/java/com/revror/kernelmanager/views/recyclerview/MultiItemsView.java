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

package com.revror.kernelmanager.views.recyclerview;

import android.content.Intent;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.card.MaterialCardView;
import com.revror.kernelmanager.R;
import com.revror.kernelmanager.activities.LaunchFragmentActivity;
import com.revror.kernelmanager.activities.TerminalActivity;
import com.revror.kernelmanager.activities.ToolsActivity;
import com.revror.kernelmanager.fragments.other.AboutFragment;
import com.revror.kernelmanager.utils.Common;
import com.revror.kernelmanager.utils.ViewUtils;

/*
 * Created by sunilpaulmathew <sunil.kde@gmail.com> on December 24, 2020
 */

public class MultiItemsView extends RecyclerViewItem {

    @Override
    public int getLayoutRes() {
        return R.layout.rv_multiitems_view;
    }

    @Override
    public void onCreateView(View view) {
        AppCompatImageButton mInfo = view.findViewById(R.id.icon_info);
        MaterialCardView mTerminal = view.findViewById(R.id.card_terminal);
        MaterialCardView mTools = view.findViewById(R.id.card_tools);

        mTerminal.setCardBackgroundColor(ViewUtils.getThemeAccentColor(view.getContext()));
        mTerminal.setStrokeColor(ViewUtils.getThemeAccentColor(view.getContext()));
        mTools.setCardBackgroundColor(ViewUtils.getThemeAccentColor(view.getContext()));
        mTools.setStrokeColor(ViewUtils.getThemeAccentColor(view.getContext()));

        mTerminal.setOnClickListener(v -> {
            Intent intent = new Intent(view.getContext(), TerminalActivity.class);
            view.getContext().startActivity(intent);
        });

        mTools.setOnClickListener(v -> {
            Intent intent = new Intent(view.getContext(), ToolsActivity.class);
            view.getContext().startActivity(intent);
        });

        mInfo.setOnClickListener(v -> {
            Common.setSelectedFragment(new AboutFragment());
            Intent intent = new Intent(view.getContext(), LaunchFragmentActivity.class);
            view.getContext().startActivity(intent);
        });
        super.onCreateView(view);
    }

    @Override
    protected boolean cardCompatible() {
        return false;
    }

}
