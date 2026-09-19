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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Tela Histórico.
 *
 * Chips para escolher a métrica, seletor de período (semana/mês/ano) e um
 * gráfico de linha com estatísticas abaixo.
 *
 * Usa MPAndroidChart — a lib que já estava no projeto e para a qual o próprio
 * design especifica as cores (gridColor, axisLineColor, LimitLine). Ver
 * decisão §10.
 */
public class HistoryFragment extends Fragment {

    private enum Period {
        WEEK(7, R.string.rd_history_by_week, R.string.rd_history_note_week),
        MONTH(30, R.string.rd_history_by_month, R.string.rd_history_note_month),
        YEAR(365, R.string.rd_history_by_year, R.string.rd_history_note_year);

        final int days;
        final int labelRes;
        final int noteRes;

        Period(int days, int labelRes, int noteRes) {
            this.days = days;
            this.labelRes = labelRes;
            this.noteRes = noteRes;
        }
    }

    private ChipGroup metricChips;
    private LineChart chart;
    private TextView chartTitle;
    private TextView chartValue;
    private TextView chartUnit;
    private TextView chartDelta;
    private TextView aggregationNote;
    private LinearLayout statsRow;
    private TextView emptyState;

    private final View[] periodButtons = new View[3];
    private Period selectedPeriod = Period.MONTH;
    private String selectedMetricKey = MetricCatalog.KEY_WEIGHT;

    private List<ScaleMeasurement> measurements = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.rd_fragment_history, container, false);

        metricChips = root.findViewById(R.id.rd_metric_chips);
        chart = root.findViewById(R.id.rd_chart);
        chartTitle = root.findViewById(R.id.rd_chart_title);
        chartValue = root.findViewById(R.id.rd_chart_value);
        chartUnit = root.findViewById(R.id.rd_chart_unit);
        chartDelta = root.findViewById(R.id.rd_chart_delta);
        aggregationNote = root.findViewById(R.id.rd_aggregation_note);
        statsRow = root.findViewById(R.id.rd_chart_stats);
        emptyState = root.findViewById(R.id.rd_history_empty);

        periodButtons[0] = root.findViewById(R.id.rd_period_week);
        periodButtons[1] = root.findViewById(R.id.rd_period_month);
        periodButtons[2] = root.findViewById(R.id.rd_period_year);

        periodButtons[0].setOnClickListener(v -> selectPeriod(Period.WEEK));
        periodButtons[1].setOnClickListener(v -> selectPeriod(Period.MONTH));
        periodButtons[2].setOnClickListener(v -> selectPeriod(Period.YEAR));

        // Métrica vinda do cartão tocado na tela Hoje.
        if (getArguments() != null) {
            final String key = getArguments().getString("metricKey", "");
            if (!key.isEmpty() && MetricCatalog.byKey(key) != null) {
                selectedMetricKey = key;
            }
        }

        styleChart();
        buildMetricChips();
        updatePeriodButtons();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        OpenScale.getInstance().getScaleMeasurementsLiveData().observe(
                getViewLifecycleOwner(), new Observer<List<ScaleMeasurement>>() {
                    @Override
                    public void onChanged(List<ScaleMeasurement> list) {
                        measurements = list == null ? new ArrayList<>() : list;
                        render();
                    }
                });
    }

    /**
     * Estilo do gráfico conforme os tokens.
     * O design pede grade quase invisível e uma série por vez.
     */
    private void styleChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBorders(false);
        chart.setScaleEnabled(false);
        chart.setExtraBottomOffset(8f);
        chart.setNoDataText("");

        final int gridColor = ContextCompat.getColor(requireContext(), R.color.chart_grid);
        final int axisTextColor =
                ContextCompat.getColor(requireContext(), R.color.chart_axis_text);

        final XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(axisTextColor);
        xAxis.setTextSize(10.5f);
        xAxis.setGranularity(1f);

        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getAxisLeft().setGridColor(gridColor);
        chart.getAxisLeft().setTextColor(axisTextColor);
        chart.getAxisLeft().setTextSize(10.5f);
        chart.getAxisLeft().setGridLineWidth(1f);

        // O design mostra rótulos só de um lado.
        chart.getAxisRight().setEnabled(false);
    }

    private void buildMetricChips() {
        metricChips.removeAllViews();

        for (MetricCatalog.Metric metric : MetricPreferences.enabledMetrics(requireContext())) {
            final Chip chip = new Chip(requireContext());
            chip.setText(metric.labelRes);
            chip.setCheckable(true);
            chip.setChecked(metric.key.equals(selectedMetricKey));
            chip.setTag(metric.key);
            chip.setOnClickListener(v -> {
                selectedMetricKey = (String) v.getTag();
                buildMetricChips();
                render();
            });
            metricChips.addView(chip);
        }
    }

    private void selectPeriod(Period period) {
        selectedPeriod = period;
        updatePeriodButtons();
        render();
    }

    private void updatePeriodButtons() {
        final Period[] all = Period.values();
        for (int i = 0; i < periodButtons.length; i++) {
            final boolean selected = all[i] == selectedPeriod;
            periodButtons[i].setBackgroundResource(
                    selected ? R.drawable.bg_rd_segment_selected : 0);
            ((TextView) periodButtons[i]).setTextColor(RedesignTheme.color(
                    requireContext(),
                    selected
                            ? com.google.android.material.R.attr.colorOnSurface
                            : com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
    }

    private void render() {
        final MetricCatalog.Metric metric = MetricCatalog.byKey(selectedMetricKey);
        if (metric == null) {
            return;
        }

        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        final List<ScaleMeasurement> window = measurementsInPeriod();

        final List<Entry> entries = new ArrayList<>();
        final List<Float> values = new ArrayList<>();
        final List<Date> dates = new ArrayList<>();

        for (int i = 0; i < window.size(); i++) {
            final ScaleMeasurement m = window.get(i);
            final float value = metric.toUserUnit(metric.valueOf(m, user), user);
            if (Float.isNaN(value)) {
                continue;
            }
            entries.add(new Entry(values.size(), value));
            values.add(value);
            dates.add(m.getDateTime());
        }

        final boolean hasData = values.size() >= 2;
        emptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);
        chart.setVisibility(hasData ? View.VISIBLE : View.INVISIBLE);
        statsRow.setVisibility(hasData ? View.VISIBLE : View.GONE);

        chartTitle.setText(getString(R.string.rd_history_chart_title,
                getString(metric.labelRes), getString(selectedPeriod.labelRes)));
        aggregationNote.setText(selectedPeriod.noteRes);
        chartUnit.setText(metric.unitLabel(requireContext(), user));

        if (values.isEmpty()) {
            chartValue.setText("–");
            chartDelta.setText("");
            chart.clear();
            return;
        }

        final float latest = values.get(values.size() - 1);
        chartValue.setText(RedesignFormat.value(latest, metric.decimals));

        final String delta = values.size() >= 2
                ? RedesignFormat.delta(requireContext(), latest, values.get(0), metric.decimals)
                : null;
        chartDelta.setText(delta == null ? "" : delta);

        if (!hasData) {
            chart.clear();
            return;
        }

        renderChart(entries, dates, metric);
        renderStats(values, metric, user);
    }

    /** Recorta as medições dentro do período selecionado, em ordem cronológica. */
    private List<ScaleMeasurement> measurementsInPeriod() {
        final Calendar cutoff = Calendar.getInstance();
        cutoff.add(Calendar.DAY_OF_YEAR, -selectedPeriod.days);
        final Date cutoffDate = cutoff.getTime();

        final List<ScaleMeasurement> window = new ArrayList<>();
        // measurements vem do mais recente para o mais antigo.
        for (ScaleMeasurement m : measurements) {
            if (m.getDateTime() != null && m.getDateTime().before(cutoffDate)) {
                break;
            }
            window.add(m);
        }
        Collections.reverse(window);
        return window;
    }

    private void renderChart(List<Entry> entries, List<Date> dates,
                             MetricCatalog.Metric metric) {
        final LineDataSet dataSet = new LineDataSet(entries, getString(metric.labelRes));

        final int lineColor = ContextCompat.getColor(requireContext(), R.color.chart_line);
        final int fillColor = ContextCompat.getColor(requireContext(), R.color.chart_fill);
        final int highlightColor =
                ContextCompat.getColor(requireContext(), R.color.chart_highlight);
        final int surfaceColor = RedesignTheme.color(requireContext(),
                com.google.android.material.R.attr.colorSurface);

        dataSet.setColor(lineColor);
        dataSet.setLineWidth(2.6f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setCubicIntensity(0.16f);
        dataSet.setDrawValues(false);

        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(lineColor);
        dataSet.setCircleRadius(3.4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(surfaceColor);
        dataSet.setCircleHoleRadius(1.8f);

        dataSet.setDrawFilled(true);
        dataSet.setFillColor(fillColor);
        // O design pede preenchimento chapado, sem gradiente — decisão de
        // performance registrada nos tokens.
        dataSet.setFillAlpha(128);

        dataSet.setHighLightColor(highlightColor);
        dataSet.setHighlightLineWidth(1f);
        dataSet.setDrawHorizontalHighlightIndicator(false);

        chart.setData(new LineData(dataSet));

        final List<Date> axisDates = new ArrayList<>(dates);
        chart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                final int index = Math.round(value);
                if (index < 0 || index >= axisDates.size()) {
                    return "";
                }
                return RedesignFormat.shortDate(requireContext(), axisDates.get(index));
            }
        });
        chart.getXAxis().setLabelCount(Math.min(6, axisDates.size()), false);

        chart.invalidate();
        chart.animateY(600);
    }

    private void renderStats(List<Float> values, MetricCatalog.Metric metric, ScaleUser user) {
        float min = values.get(0);
        float max = values.get(0);
        float sum = 0f;
        for (float v : values) {
            min = Math.min(min, v);
            max = Math.max(max, v);
            sum += v;
        }
        final float avg = sum / values.size();

        final String unit = metric.unitLabel(requireContext(), user);
        setStat(R.id.rd_stat_min, R.string.rd_history_stat_min, min, metric, unit);
        setStat(R.id.rd_stat_avg, R.string.rd_history_stat_avg, avg, metric, unit);
        setStat(R.id.rd_stat_max, R.string.rd_history_stat_max, max, metric, unit);
    }

    private void setStat(int containerId, int labelRes, float value,
                         MetricCatalog.Metric metric, String unit) {
        final View container = statsRow.findViewById(containerId);
        final TextView label = container.findViewById(R.id.rd_stat_label);
        final TextView valueView = container.findViewById(R.id.rd_stat_value);

        label.setText(labelRes);
        final String formatted = RedesignFormat.value(value, metric.decimals);
        valueView.setText(unit.isEmpty() ? formatted : formatted + " " + unit);
    }
}
