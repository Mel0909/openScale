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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.health.openscale.R;
import com.health.openscale.gui.preferences.BluetoothSettingsFragment;

import java.util.List;

/**
 * Tela Ajustes.
 *
 * Cobre o que o design especifica: balança pareada, métricas exibidas,
 * fórmula de gordura, import/export e aparência.
 *
 * O que o design não cobre — lembretes, backup automático, idioma — continua
 * acessível pela UI antiga via LegacyBridge, para não sumir sem decisão.
 */
public class SettingsFragment extends Fragment {

    private LinearLayout metricToggles;
    private TextView scaleName;
    private TextView scaleStatus;
    private View themeLight;
    private View themeDark;
    private View themeSystem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.rd_fragment_settings, container, false);

        metricToggles = root.findViewById(R.id.rd_metric_toggles);
        scaleName = root.findViewById(R.id.rd_scale_name);
        scaleStatus = root.findViewById(R.id.rd_scale_status);
        themeLight = root.findViewById(R.id.rd_theme_light);
        themeDark = root.findViewById(R.id.rd_theme_dark);
        themeSystem = root.findViewById(R.id.rd_theme_system);

        themeLight.setOnClickListener(v -> setTheme("Light"));
        themeDark.setOnClickListener(v -> setTheme("Dark"));
        themeSystem.setOnClickListener(v -> setTheme("System"));

        // Pareamento de balança, import/export e as telas não reconstruídas
        // continuam na UI antiga.
        root.findViewById(R.id.rd_scale_search).setOnClickListener(v -> openLegacy());
        root.findViewById(R.id.rd_import_csv).setOnClickListener(v -> openLegacy());
        root.findViewById(R.id.rd_export_csv).setOnClickListener(v -> openLegacy());
        root.findViewById(R.id.rd_more_settings).setOnClickListener(v -> openLegacy());

        // A paleta azul aparece no protótipo mas não tem contraste verificado
        // nos tokens — ver decisão §4.
        root.findViewById(R.id.rd_accent_blue).setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        R.string.rd_settings_accent_blue_unavailable,
                        Toast.LENGTH_SHORT).show());

        renderScale();
        renderThemeSelection();
        renderMetricToggles(inflater);

        return root;
    }

    private void renderScale() {
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(requireContext());
        final String name = prefs.getString(
                BluetoothSettingsFragment.PREFERENCE_KEY_BLUETOOTH_DEVICE_NAME, "");

        if (name.isEmpty()) {
            scaleName.setText(R.string.rd_settings_scale_none);
            scaleStatus.setText("");
        } else {
            scaleName.setText(name);
            scaleStatus.setText(R.string.label_bluetooth_title);
        }
    }

    private void setTheme(String value) {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .edit().putString("app_theme", value).apply();

        if ("Dark".equals(value)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("Light".equals(value)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void renderThemeSelection() {
        final String theme = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("app_theme", "System");

        styleSegment(themeLight, "Light".equals(theme));
        styleSegment(themeDark, "Dark".equals(theme));
        styleSegment(themeSystem, "System".equals(theme));
    }

    private void styleSegment(View segment, boolean selected) {
        segment.setBackgroundResource(selected ? R.drawable.bg_rd_segment_selected : 0);
        ((TextView) segment).setTextColor(RedesignTheme.color(requireContext(),
                selected
                        ? com.google.android.material.R.attr.colorOnSurface
                        : com.google.android.material.R.attr.colorOnSurfaceVariant));
    }

    /**
     * Lista de métricas com interruptor.
     *
     * Mostra TODAS as métricas do catálogo, não só as habilitadas — é aqui
     * que o usuário liga as que quiser. O design não corta métricas, apenas
     * escolhe um padrão (ver decisão §8).
     */
    private void renderMetricToggles(LayoutInflater inflater) {
        metricToggles.removeAllViews();

        final List<MetricCatalog.Metric> all = MetricCatalog.all();

        for (MetricCatalog.Metric metric : all) {
            final View row = inflater.inflate(
                    R.layout.rd_item_metric_toggle, metricToggles, false);

            final TextView label = row.findViewById(R.id.rd_toggle_label);
            final TextView source = row.findViewById(R.id.rd_toggle_source);
            final android.widget.Switch toggle = row.findViewById(R.id.rd_toggle_switch);

            label.setText(metric.labelRes);
            source.setText(metric.source.labelRes);
            toggle.setChecked(MetricPreferences.isEnabled(requireContext(), metric));

            toggle.setOnCheckedChangeListener((button, checked) ->
                    MetricPreferences.setEnabled(requireContext(), metric, checked));

            row.setOnClickListener(v -> toggle.toggle());

            metricToggles.addView(row);
        }
    }

    private void openLegacy() {
        LegacyBridge.openSettings(requireActivity());
    }
}
