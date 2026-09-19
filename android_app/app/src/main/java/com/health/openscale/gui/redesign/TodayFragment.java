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
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.google.android.material.card.MaterialCardView;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;
import com.health.openscale.gui.redesign.widget.SparklineView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tela Hoje — a inicial do design.
 *
 * Mostra a última medição do perfil ativo: peso em destaque, variação em
 * relação à anterior, sparkline dos últimos 30 dias e uma grade com as
 * demais métricas habilitadas.
 *
 * Lê tudo de OpenScale.getScaleMeasurementsLiveData(), que já devolve as
 * medições do usuário selecionado em ordem decrescente de data.
 */
public class TodayFragment extends Fragment {

    /** Quantos pontos o sparkline mostra, no máximo. */
    private static final int SPARKLINE_MAX_POINTS = 30;

    /** Cartões de métrica por linha, como no protótipo. */
    private static final int METRIC_COLUMNS = 2;

    private TextView userInitial;
    private TextView userName;
    private TextView heroLabel;
    private TextView heroValue;
    private TextView heroUnit;
    private TextView heroDelta;
    private TextView heroSince;
    private TextView sparkStart;
    private TextView sparkMid;
    private TextView sparkEnd;
    private SparklineView sparkline;
    private MaterialCardView sparklineCard;
    private LinearLayout metricGrid;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.rd_fragment_today, container, false);

        userInitial = root.findViewById(R.id.rd_user_initial);
        userName = root.findViewById(R.id.rd_user_name);
        heroLabel = root.findViewById(R.id.rd_hero_label);
        heroValue = root.findViewById(R.id.rd_hero_value);
        heroUnit = root.findViewById(R.id.rd_hero_unit);
        heroDelta = root.findViewById(R.id.rd_hero_delta);
        heroSince = root.findViewById(R.id.rd_hero_since);
        sparkStart = root.findViewById(R.id.rd_spark_start);
        sparkMid = root.findViewById(R.id.rd_spark_mid);
        sparkEnd = root.findViewById(R.id.rd_spark_end);
        sparkline = root.findViewById(R.id.rd_sparkline);
        sparklineCard = root.findViewById(R.id.rd_sparkline_card);
        metricGrid = root.findViewById(R.id.rd_metric_grid);
        emptyState = root.findViewById(R.id.rd_empty_state);

        root.findViewById(R.id.rd_settings_button).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.rd_action_today_to_settings));

        root.findViewById(R.id.rd_choose_metrics).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.rd_action_today_to_settings));

        sparklineCard.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.rd_action_today_to_history));

        root.findViewById(R.id.rd_user_chip).setOnClickListener(v ->
                UserPickerSheet.newInstance().show(
                        getParentFragmentManager(), UserPickerSheet.TAG));

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        OpenScale.getInstance().getScaleMeasurementsLiveData().observe(
                getViewLifecycleOwner(), new Observer<List<ScaleMeasurement>>() {
                    @Override
                    public void onChanged(List<ScaleMeasurement> measurements) {
                        render(measurements);
                    }
                });
    }

    private void render(List<ScaleMeasurement> measurements) {
        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        renderUser(user);

        final boolean hasData = measurements != null && !measurements.isEmpty();
        emptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);
        sparklineCard.setVisibility(hasData ? View.VISIBLE : View.GONE);
        metricGrid.setVisibility(hasData ? View.VISIBLE : View.GONE);

        if (!hasData) {
            heroLabel.setText(R.string.rd_today_no_data);
            heroValue.setText(R.string.rd_weigh_placeholder);
            heroUnit.setText("");
            heroDelta.setVisibility(View.GONE);
            heroSince.setVisibility(View.GONE);
            metricGrid.removeAllViews();
            return;
        }

        // A lista vem do mais recente para o mais antigo.
        final ScaleMeasurement latest = measurements.get(0);
        final ScaleMeasurement previous = measurements.size() > 1 ? measurements.get(1) : null;

        renderHero(latest, previous, user);
        renderSparkline(measurements, user);
        renderMetrics(latest, previous, user);
    }

    private void renderUser(ScaleUser user) {
        if (user == null) {
            userInitial.setText("");
            userName.setText("");
            return;
        }
        final String name = user.getUserName();
        userName.setText(name);
        userInitial.setText(name.isEmpty()
                ? "" : name.substring(0, 1).toUpperCase(Locale.getDefault()));
    }

    private void renderHero(ScaleMeasurement latest, ScaleMeasurement previous, ScaleUser user) {
        final MetricCatalog.Metric weight = MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);
        if (weight == null) {
            return;
        }

        final float current = weight.toUserUnit(weight.valueOf(latest, user), user);
        final String unit = weight.unitLabel(requireContext(), user);

        heroValue.setText(RedesignFormat.value(current, weight.decimals));
        heroUnit.setText(unit);
        heroLabel.setText(getString(R.string.rd_today_weight_at,
                RedesignFormat.dateWithTime(requireContext(), latest.getDateTime())));

        if (previous == null) {
            heroDelta.setVisibility(View.GONE);
            heroSince.setVisibility(View.GONE);
            return;
        }

        final float prev = weight.toUserUnit(weight.valueOf(previous, user), user);
        final String delta = RedesignFormat.deltaWithUnit(
                requireContext(), current, prev, weight.decimals, unit);

        if (delta == null) {
            heroDelta.setVisibility(View.GONE);
            heroSince.setVisibility(View.GONE);
        } else {
            heroDelta.setVisibility(View.VISIBLE);
            heroDelta.setText(delta);
            heroSince.setVisibility(View.VISIBLE);
            heroSince.setText(getString(R.string.rd_today_since,
                    RedesignFormat.shortDate(requireContext(), previous.getDateTime())));
        }
    }

    private void renderSparkline(List<ScaleMeasurement> measurements, ScaleUser user) {
        final MetricCatalog.Metric weight = MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);
        if (weight == null) {
            return;
        }

        final int count = Math.min(measurements.size(), SPARKLINE_MAX_POINTS);
        final List<Float> values = new ArrayList<>(count);
        final List<ScaleMeasurement> window = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            final ScaleMeasurement m = measurements.get(i);
            final float v = weight.toUserUnit(weight.valueOf(m, user), user);
            if (!Float.isNaN(v)) {
                values.add(v);
                window.add(m);
            }
        }

        // A lista vem do mais recente para o mais antigo; o gráfico é
        // cronológico, então inverte.
        Collections.reverse(values);
        Collections.reverse(window);

        sparkline.setValues(values);

        if (window.isEmpty()) {
            sparkStart.setText("");
            sparkMid.setText("");
            sparkEnd.setText("");
            return;
        }

        sparkStart.setText(RedesignFormat.shortDate(
                requireContext(), window.get(0).getDateTime()));
        sparkMid.setText(RedesignFormat.shortDate(
                requireContext(), window.get(window.size() / 2).getDateTime()));
        sparkEnd.setText(RedesignFormat.shortDate(
                requireContext(), window.get(window.size() - 1).getDateTime()));
    }

    private void renderMetrics(ScaleMeasurement latest, ScaleMeasurement previous, ScaleUser user) {
        metricGrid.removeAllViews();

        final List<MetricCatalog.Metric> enabled =
                MetricPreferences.enabledMetrics(requireContext());

        final LayoutInflater inflater = LayoutInflater.from(requireContext());
        LinearLayout row = null;

        for (int i = 0; i < enabled.size(); i++) {
            final MetricCatalog.Metric metric = enabled.get(i);

            // O peso já é o número herói; repeti-lo num cartão seria redundante.
            if (MetricCatalog.KEY_WEIGHT.equals(metric.key)) {
                continue;
            }

            final float value = metric.toUserUnit(metric.valueOf(latest, user), user);
            if (Float.isNaN(value)) {
                continue;
            }

            if (row == null || row.getChildCount() >= METRIC_COLUMNS) {
                row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                final LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = getResources()
                        .getDimensionPixelSize(R.dimen.rd_gap);
                row.setLayoutParams(rowParams);
                metricGrid.addView(row);
            }

            row.addView(buildMetricCard(inflater, row, metric, latest, previous, user, value));
        }

        // Última linha ímpar: preenche o vão para o cartão não esticar.
        if (row != null && row.getChildCount() == 1) {
            final View filler = new View(requireContext());
            final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, 1, 1f);
            filler.setLayoutParams(params);
            row.addView(filler);
        }
    }

    private View buildMetricCard(LayoutInflater inflater, ViewGroup parent,
                                 MetricCatalog.Metric metric, ScaleMeasurement latest,
                                 ScaleMeasurement previous, ScaleUser user, float value) {
        final View card = inflater.inflate(R.layout.rd_item_metric_card, parent, false);

        final TextView label = card.findViewById(R.id.rd_metric_label);
        final TextView valueView = card.findViewById(R.id.rd_metric_value);
        final TextView unitView = card.findViewById(R.id.rd_metric_unit);
        final TextView deltaView = card.findViewById(R.id.rd_metric_delta);

        label.setText(metric.labelRes);
        valueView.setText(RedesignFormat.value(value, metric.decimals));
        unitView.setText(metric.unitLabel(requireContext(), user));

        String delta = null;
        if (previous != null) {
            final float prev = metric.toUserUnit(metric.valueOf(previous, user), user);
            delta = RedesignFormat.delta(requireContext(), value, prev, metric.decimals);
        }

        if (delta == null) {
            deltaView.setVisibility(View.INVISIBLE);
        } else {
            deltaView.setVisibility(View.VISIBLE);
            deltaView.setText(delta);
        }

        card.setOnClickListener(v -> {
            final Bundle args = new Bundle();
            args.putString("metricKey", metric.key);
            Navigation.findNavController(v)
                    .navigate(R.id.rd_action_today_to_history, args);
        });

        return card;
    }
}
