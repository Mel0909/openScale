/* Copyright (C) 2026  openScale contributors
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>
*/
package com.health.openscale.gui.redesign;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.card.MaterialCardView;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;

import java.util.List;
import java.util.Locale;

/**
 * Sheet "Trocar de pessoa".
 *
 * Lista os perfis com o último peso de cada um e marca o ativo. Trocar de
 * perfil dispara OpenScale.selectScaleUser, e o LiveData das telas reage
 * sozinho — não é preciso avisar ninguém.
 */
public class UserPickerSheet extends BottomSheetDialogFragment {

    public static final String TAG = "rd_user_picker";

    public static UserPickerSheet newInstance() {
        return new UserPickerSheet();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), R.style.ThemeOverlay_OpenScale_BottomSheet);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.rd_sheet_user_picker, container, false);

        final LinearLayout list = root.findViewById(R.id.rd_user_list);
        final OpenScale openScale = OpenScale.getInstance();
        final ScaleUser selected = openScale.getSelectedScaleUser();
        final int selectedId = selected == null ? -1 : selected.getId();

        for (ScaleUser user : openScale.getScaleUserList()) {
            list.addView(buildUserRow(inflater, list, user, user.getId() == selectedId));
        }

        root.findViewById(R.id.rd_user_close).setOnClickListener(v -> dismiss());

        return root;
    }

    private View buildUserRow(LayoutInflater inflater, ViewGroup parent,
                              ScaleUser user, boolean isSelected) {
        final View row = inflater.inflate(R.layout.rd_item_user, parent, false);

        final TextView initial = row.findViewById(R.id.rd_user_row_initial);
        final TextView name = row.findViewById(R.id.rd_user_row_name);
        final TextView subtitle = row.findViewById(R.id.rd_user_row_subtitle);
        final TextView tag = row.findViewById(R.id.rd_user_row_tag);
        final MaterialCardView card = row.findViewById(R.id.rd_user_row_card);

        final String userName = user.getUserName();
        name.setText(userName);
        initial.setText(userName.isEmpty()
                ? "" : userName.substring(0, 1).toUpperCase(Locale.getDefault()));
        subtitle.setText(buildSubtitle(user));

        if (isSelected) {
            tag.setVisibility(View.VISIBLE);
            tag.setText(R.string.rd_profile_active);
            card.setCardBackgroundColor(RedesignTheme.color(
                    requireContext(), com.google.android.material.R.attr.colorPrimaryContainer));
            card.setStrokeColor(RedesignTheme.color(
                    requireContext(), com.google.android.material.R.attr.colorPrimary));
            initial.setBackgroundResource(R.drawable.bg_rd_avatar);
        } else {
            tag.setVisibility(View.GONE);
            initial.setBackgroundResource(R.drawable.bg_rd_avatar_muted);
        }

        card.setOnClickListener(v -> {
            OpenScale.getInstance().selectScaleUser(user.getId());
            dismiss();
        });

        return row;
    }

    /** "72,4 kg · medida há 2 dias" — ou vazio se o perfil não tem medição. */
    private String buildSubtitle(ScaleUser user) {
        final ScaleMeasurement last =
                OpenScale.getInstance().getLastScaleMeasurement(user.getId());
        if (last == null) {
            return getString(R.string.rd_records_empty);
        }

        final MetricCatalog.Metric weight = MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);
        if (weight == null) {
            return "";
        }

        final float value = weight.toUserUnit(weight.valueOf(last, user), user);
        return RedesignFormat.value(value, weight.decimals)
                + " " + weight.unitLabel(requireContext(), user)
                + " · " + RedesignFormat.shortDate(requireContext(), last.getDateTime());
    }
}
