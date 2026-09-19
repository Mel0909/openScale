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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;
import com.health.openscale.core.utils.Converters;

import java.util.List;
import java.util.Locale;

/**
 * Tela Perfil.
 *
 * Mostra os quatro campos que alimentam as fórmulas, cada um com a frase
 * do que ele afeta — detalhe do design que conecta o dado pedido ao
 * benefício, em vez de só listar campos.
 *
 * A edição em si ainda abre a tela antiga de usuário: reconstruí-la exigiria
 * replicar validação de data, altura e unidades, e não era o essencial para
 * ver o design de pé. Ver decisão §14.
 */
public class ProfileFragment extends Fragment {

    private TextView avatarInitial;
    private TextView userName;
    private TextView userSubtitle;
    private LinearLayout fieldsContainer;
    private LinearLayout usersContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.rd_fragment_profile, container, false);

        avatarInitial = root.findViewById(R.id.rd_profile_initial);
        userName = root.findViewById(R.id.rd_profile_name);
        userSubtitle = root.findViewById(R.id.rd_profile_subtitle);
        fieldsContainer = root.findViewById(R.id.rd_profile_fields);
        usersContainer = root.findViewById(R.id.rd_profile_users);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recarrega ao voltar: o perfil pode ter sido editado na tela antiga.
        render();
    }

    private void render() {
        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        if (user == null) {
            return;
        }

        renderHeader(user);
        renderFields(user);
        renderUsers(user);
    }

    private void renderHeader(ScaleUser user) {
        final String name = user.getUserName();
        userName.setText(name);
        avatarInitial.setText(name.isEmpty()
                ? "" : name.substring(0, 1).toUpperCase(Locale.getDefault()));

        final List<ScaleMeasurement> all = OpenScale.getInstance().getScaleMeasurementList();
        final int count = all == null ? 0 : all.size();

        final ScaleMeasurement first = OpenScale.getInstance().getFirstScaleMeasurement();
        final String since = first == null
                ? "" : RedesignFormat.monthYear(first.getDateTime());

        userSubtitle.setText(getString(R.string.rd_profile_since, since,
                getResources().getQuantityString(R.plurals.rd_records_count, count, count)));
    }

    private void renderFields(ScaleUser user) {
        fieldsContainer.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(requireContext());

        addField(inflater, R.string.rd_profile_field_name,
                user.getUserName(), R.string.rd_profile_feeds_identity);

        addField(inflater, R.string.rd_profile_field_birthday,
                RedesignFormat.shortDate(requireContext(), user.getBirthday()),
                R.string.rd_profile_feeds_age);

        final String height = RedesignFormat.value(
                Converters.fromCentimeter(user.getBodyHeight(), user.getMeasureUnit()), 0)
                + " " + user.getMeasureUnit().toString();
        addField(inflater, R.string.rd_profile_field_height,
                height, R.string.rd_profile_feeds_height);

        addField(inflater, R.string.rd_profile_field_sex,
                getString(user.getGender().isMale()
                        ? R.string.label_male : R.string.label_female),
                R.string.rd_profile_feeds_age);
    }

    private void addField(LayoutInflater inflater, int labelRes, String value, int feedsRes) {
        final View row = inflater.inflate(R.layout.rd_item_profile_field, fieldsContainer, false);
        ((TextView) row.findViewById(R.id.rd_field_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.rd_field_value)).setText(value);
        ((TextView) row.findViewById(R.id.rd_field_feeds)).setText(feedsRes);

        // A edição continua na tela antiga — ver decisão §14.
        row.setOnClickListener(v -> openLegacyUserSettings());

        fieldsContainer.addView(row);
    }

    private void renderUsers(ScaleUser selected) {
        usersContainer.removeAllViews();
        final LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (ScaleUser user : OpenScale.getInstance().getScaleUserList()) {
            final View row = inflater.inflate(R.layout.rd_item_user, usersContainer, false);

            final TextView initial = row.findViewById(R.id.rd_user_row_initial);
            final TextView name = row.findViewById(R.id.rd_user_row_name);
            final TextView subtitle = row.findViewById(R.id.rd_user_row_subtitle);
            final TextView tag = row.findViewById(R.id.rd_user_row_tag);
            final MaterialCardView card = row.findViewById(R.id.rd_user_row_card);

            final String userName = user.getUserName();
            name.setText(userName);
            initial.setText(userName.isEmpty()
                    ? "" : userName.substring(0, 1).toUpperCase(Locale.getDefault()));

            final ScaleMeasurement last =
                    OpenScale.getInstance().getLastScaleMeasurement(user.getId());
            subtitle.setText(last == null
                    ? getString(R.string.rd_records_empty)
                    : RedesignFormat.shortDate(requireContext(), last.getDateTime()));

            final boolean isSelected = user.getId() == selected.getId();
            if (isSelected) {
                tag.setVisibility(View.VISIBLE);
                tag.setText(R.string.rd_profile_active);
                card.setStrokeColor(RedesignTheme.color(requireContext(),
                        com.google.android.material.R.attr.colorPrimary));
                initial.setBackgroundResource(R.drawable.bg_rd_avatar);
            } else {
                tag.setVisibility(View.GONE);
                initial.setBackgroundResource(R.drawable.bg_rd_avatar_muted);
            }

            card.setOnClickListener(v -> {
                OpenScale.getInstance().selectScaleUser(user.getId());
                render();
            });

            usersContainer.addView(row);
        }
    }

    /**
     * Abre a tela de usuário da UI antiga.
     *
     * Ponte temporária: a edição de perfil não foi reconstruída ainda, e a
     * tela antiga já resolve validação de data, altura e unidades.
     */
    private void openLegacyUserSettings() {
        LegacyBridge.openUserSettings(requireActivity(),
                OpenScale.getInstance().getSelectedScaleUserId());
    }
}
