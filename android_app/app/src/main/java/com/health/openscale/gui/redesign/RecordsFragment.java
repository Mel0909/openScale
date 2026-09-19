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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Tela Medições: todas as pesagens, agrupadas por mês.
 *
 * Usa RecyclerView com dois tipos de item — cabeçalho de mês e registro —
 * em vez de uma lista simples, porque o design agrupa visualmente
 * ("Setembro 2026" acima das medições daquele mês).
 */
public class RecordsFragment extends Fragment {

    private RecyclerView list;
    private TextView countLabel;
    private TextView emptyState;

    private final RecordsAdapter adapter = new RecordsAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.rd_fragment_records, container, false);

        list = root.findViewById(R.id.rd_records_list);
        countLabel = root.findViewById(R.id.rd_records_count);
        emptyState = root.findViewById(R.id.rd_records_empty);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

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
        final boolean hasData = measurements != null && !measurements.isEmpty();

        emptyState.setVisibility(hasData ? View.GONE : View.VISIBLE);
        list.setVisibility(hasData ? View.VISIBLE : View.GONE);

        if (!hasData) {
            countLabel.setText("");
            adapter.submit(new ArrayList<>());
            return;
        }

        countLabel.setText(getResources().getQuantityString(
                R.plurals.rd_records_count, measurements.size(), measurements.size()));

        adapter.submit(buildRows(measurements));
    }

    /**
     * Intercala cabeçalhos de mês entre os registros.
     * A lista chega do mais recente para o mais antigo, que é a ordem do design.
     */
    private List<Row> buildRows(List<ScaleMeasurement> measurements) {
        final List<Row> rows = new ArrayList<>();
        int lastMonth = -1;
        int lastYear = -1;

        for (int i = 0; i < measurements.size(); i++) {
            final ScaleMeasurement m = measurements.get(i);
            if (m.getDateTime() == null) {
                continue;
            }

            final Calendar cal = Calendar.getInstance();
            cal.setTime(m.getDateTime());
            final int month = cal.get(Calendar.MONTH);
            final int year = cal.get(Calendar.YEAR);

            if (month != lastMonth || year != lastYear) {
                rows.add(Row.header(RedesignFormat.monthHeader(m.getDateTime())));
                lastMonth = month;
                lastYear = year;
            }

            final ScaleMeasurement previous =
                    (i + 1 < measurements.size()) ? measurements.get(i + 1) : null;
            rows.add(Row.record(m, previous));
        }

        return rows;
    }

    /** Linha da lista: cabeçalho de mês ou registro. */
    private static final class Row {
        static final int TYPE_HEADER = 0;
        static final int TYPE_RECORD = 1;

        final int type;
        final String headerText;
        final ScaleMeasurement measurement;
        final ScaleMeasurement previous;

        private Row(int type, String headerText,
                    ScaleMeasurement measurement, ScaleMeasurement previous) {
            this.type = type;
            this.headerText = headerText;
            this.measurement = measurement;
            this.previous = previous;
        }

        static Row header(String text) {
            return new Row(TYPE_HEADER, text, null, null);
        }

        static Row record(ScaleMeasurement m, ScaleMeasurement previous) {
            return new Row(TYPE_RECORD, null, m, previous);
        }
    }

    private class RecordsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final List<Row> rows = new ArrayList<>();

        void submit(List<Row> newRows) {
            rows.clear();
            rows.addAll(newRows);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return rows.get(position).type;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == Row.TYPE_HEADER) {
                return new HeaderHolder(
                        inflater.inflate(R.layout.rd_item_month_header, parent, false));
            }
            return new RecordHolder(
                    inflater.inflate(R.layout.rd_item_record, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final Row row = rows.get(position);
            if (holder instanceof HeaderHolder) {
                ((HeaderHolder) holder).title.setText(row.headerText);
            } else {
                ((RecordHolder) holder).bind(row.measurement, row.previous);
            }
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static class HeaderHolder extends RecyclerView.ViewHolder {
        final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.rd_month_header);
        }
    }

    private class RecordHolder extends RecyclerView.ViewHolder {
        final TextView day;
        final TextView time;
        final TextView weight;
        final TextView unit;
        final TextView subtitle;
        final TextView delta;

        RecordHolder(@NonNull View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.rd_record_day);
            time = itemView.findViewById(R.id.rd_record_time);
            weight = itemView.findViewById(R.id.rd_record_weight);
            unit = itemView.findViewById(R.id.rd_record_unit);
            subtitle = itemView.findViewById(R.id.rd_record_subtitle);
            delta = itemView.findViewById(R.id.rd_record_delta);
        }

        void bind(ScaleMeasurement measurement, ScaleMeasurement previous) {
            final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
            final MetricCatalog.Metric weightMetric =
                    MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);
            if (weightMetric == null) {
                return;
            }

            day.setText(RedesignFormat.dayLabel(requireContext(), measurement.getDateTime()));
            time.setText(RedesignFormat.time(requireContext(), measurement.getDateTime()));

            final float value =
                    weightMetric.toUserUnit(weightMetric.valueOf(measurement, user), user);
            weight.setText(RedesignFormat.value(value, weightMetric.decimals));
            unit.setText(weightMetric.unitLabel(requireContext(), user));

            subtitle.setText(buildSubtitle(measurement, user));

            if (previous == null) {
                delta.setText("");
            } else {
                final float prev =
                        weightMetric.toUserUnit(weightMetric.valueOf(previous, user), user);
                final String text = RedesignFormat.delta(
                        requireContext(), value, prev, weightMetric.decimals);
                delta.setText(text == null ? "" : text);
            }

            itemView.setOnClickListener(v ->
                    RecordDetailSheet.newInstance(measurement.getId())
                            .show(getParentFragmentManager(), RecordDetailSheet.TAG));
        }

        /** "23,8% gordura · 54,1% água · anotação" */
        private String buildSubtitle(ScaleMeasurement measurement, ScaleUser user) {
            final StringBuilder builder = new StringBuilder();
            int shown = 0;

            for (MetricCatalog.Metric metric :
                    MetricPreferences.enabledMetrics(requireContext())) {
                if (MetricCatalog.KEY_WEIGHT.equals(metric.key) || shown >= 2) {
                    continue;
                }
                final float value = metric.toUserUnit(metric.valueOf(measurement, user), user);
                if (Float.isNaN(value) || value == 0f) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(" · ");
                }
                builder.append(RedesignFormat.value(value, metric.decimals))
                        .append(metric.unitLabel(requireContext(), user))
                        .append(' ')
                        .append(getString(metric.labelRes).toLowerCase());
                shown++;
            }

            final String comment = measurement.getComment();
            if (comment != null && !comment.trim().isEmpty()) {
                if (builder.length() > 0) {
                    builder.append(" · ");
                }
                builder.append(comment.trim());
            }

            return builder.toString();
        }
    }
}
