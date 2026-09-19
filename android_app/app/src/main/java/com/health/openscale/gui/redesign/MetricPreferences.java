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

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.preference.PreferenceManager;

import com.health.openscale.gui.measurement.MeasurementView;
import com.health.openscale.gui.measurement.MeasurementViewSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Quais métricas estão habilitadas e em que ordem.
 *
 * Deliberadamente reaproveita MeasurementViewSettings e a preferência
 * "measurementOrder" do app antigo, em vez de criar um esquema novo. Duas
 * razões:
 *
 *   1. Quem já usa o app não perde a configuração ao migrar para a UI nova.
 *   2. MeasurementViewSettings já resolve dependências entre métricas — por
 *      exemplo, cintura-quadril só faz sentido com cintura e quadril ligadas.
 *      Reimplementar isso seria duplicar regra de negócio.
 */
public final class MetricPreferences {

    /** Métricas ligadas por padrão, conforme a tela Hoje do protótipo. */
    private static final String[] DEFAULT_ENABLED = {
            MetricCatalog.KEY_WEIGHT,
            MetricCatalog.KEY_FAT,
            MetricCatalog.KEY_WATER,
            MetricCatalog.KEY_MUSCLE,
            MetricCatalog.KEY_BMI,
            MetricCatalog.KEY_WAIST,
    };

    /**
     * Métricas habilitadas, na ordem escolhida pelo usuário.
     *
     * A ordem vem da mesma preferência que a UI antiga usa, então reordenar
     * numa das duas reflete na outra.
     */
    public static List<MetricCatalog.Metric> enabledMetrics(Context context) {
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);

        final List<MetricCatalog.Metric> all = MetricCatalog.all();
        final List<MetricCatalog.Metric> ordered = new ArrayList<>();

        // Primeiro, na ordem salva pelo usuário.
        final String savedOrder = prefs.getString(MeasurementView.PREF_MEASUREMENT_ORDER, "");
        if (!savedOrder.isEmpty()) {
            for (String key : TextUtils.split(savedOrder, ",")) {
                final MetricCatalog.Metric metric = MetricCatalog.byKey(key);
                if (metric != null && all.remove(metric)) {
                    ordered.add(metric);
                }
            }
        }

        // O que sobrou (métricas novas, ou sem ordem salva) vai ao fim, na
        // ordem padrão do catálogo.
        ordered.addAll(all);

        final List<MetricCatalog.Metric> enabled = new ArrayList<>();
        for (MetricCatalog.Metric metric : ordered) {
            if (isEnabled(context, metric)) {
                enabled.add(metric);
            }
        }
        return enabled;
    }

    /**
     * Se a métrica aparece na UI.
     *
     * Usa MeasurementViewSettings, que além da preferência direta também
     * checa as dependências (cintura-quadril precisa de cintura e quadril).
     */
    public static boolean isEnabled(Context context, MetricCatalog.Metric metric) {
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        final MeasurementViewSettings settings =
                new MeasurementViewSettings(prefs, metric.key);

        // Sem preferência gravada ainda: aplica o padrão do design.
        if (!prefs.contains(settings.getEnabledKey())) {
            return isDefaultEnabled(metric.key);
        }
        return settings.isEnabled();
    }

    public static void setEnabled(Context context, MetricCatalog.Metric metric, boolean enabled) {
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        final MeasurementViewSettings settings =
                new MeasurementViewSettings(prefs, metric.key);
        prefs.edit().putBoolean(settings.getEnabledKey(), enabled).apply();
    }

    private static boolean isDefaultEnabled(String key) {
        for (String defaultKey : DEFAULT_ENABLED) {
            if (defaultKey.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private MetricPreferences() {
    }
}
