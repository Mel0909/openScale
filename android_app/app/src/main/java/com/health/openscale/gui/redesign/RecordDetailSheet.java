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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;

import java.util.List;

/**
 * Sheet de detalhe de uma medição: todas as métricas, a anotação, e as
 * ações de editar e apagar.
 */
public class RecordDetailSheet extends BottomSheetDialogFragment {

    public static final String TAG = "rd_record_detail";
    private static final String ARG_ID = "measurementId";

    private ScaleMeasurement measurement;

    public static RecordDetailSheet newInstance(int measurementId) {
        final RecordDetailSheet sheet = new RecordDetailSheet();
        final Bundle args = new Bundle();
        args.putInt(ARG_ID, measurementId);
        sheet.setArguments(args);
        return sheet;
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
        final View root = inflater.inflate(R.layout.rd_sheet_record_detail, container, false);

        final int id = getArguments() == null ? -1 : getArguments().getInt(ARG_ID, -1);
        measurement = findMeasurement(id);
        if (measurement == null) {
            dismiss();
            return root;
        }

        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        bindHeader(root, user);
        bindMetrics(root, inflater, user);
        bindNote(root);

        root.findViewById(R.id.rd_detail_edit).setOnClickListener(v -> {
            dismiss();
            AddMeasurementSheet.newInstanceForEdit(measurement.getId())
                    .show(getParentFragmentManager(), AddMeasurementSheet.TAG);
        });

        root.findViewById(R.id.rd_detail_delete).setOnClickListener(v -> confirmDelete());

        return root;
    }

    private ScaleMeasurement findMeasurement(int id) {
        final List<ScaleMeasurement> all = OpenScale.getInstance().getScaleMeasurementList();
        if (all == null) {
            return null;
        }
        for (ScaleMeasurement m : all) {
            if (m.getId() == id) {
                return m;
            }
        }
        return null;
    }

    private void bindHeader(View root, ScaleUser user) {
        final TextView dateView = root.findViewById(R.id.rd_detail_date);
        final TextView valueView = root.findViewById(R.id.rd_detail_weight);
        final TextView unitView = root.findViewById(R.id.rd_detail_unit);
        final TextView deltaView = root.findViewById(R.id.rd_detail_delta);

        dateView.setText(RedesignFormat.dateWithTime(
                requireContext(), measurement.getDateTime()));

        final MetricCatalog.Metric weight = MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);
        if (weight == null) {
            return;
        }

        final float value = weight.toUserUnit(weight.valueOf(measurement, user), user);
        valueView.setText(RedesignFormat.value(value, weight.decimals));
        unitView.setText(weight.unitLabel(requireContext(), user));

        // Compara com a medição imediatamente anterior a esta.
        final ScaleMeasurement[] tuple =
                OpenScale.getInstance().getTupleOfScaleMeasurement(measurement.getId());
        final ScaleMeasurement previous = tuple == null ? null : tuple[0];

        if (previous == null) {
            deltaView.setText("");
        } else {
            final float prev = weight.toUserUnit(weight.valueOf(previous, user), user);
            final String delta = RedesignFormat.deltaWithUnit(requireContext(), value, prev,
                    weight.decimals, weight.unitLabel(requireContext(), user));
            deltaView.setText(delta == null ? "" : delta);
        }
    }

    private void bindMetrics(View root, LayoutInflater inflater, ScaleUser user) {
        final LinearLayout container = root.findViewById(R.id.rd_detail_metrics);
        container.removeAllViews();

        for (MetricCatalog.Metric metric : MetricPreferences.enabledMetrics(requireContext())) {
            if (MetricCatalog.KEY_WEIGHT.equals(metric.key)) {
                continue;
            }

            final float value = metric.toUserUnit(metric.valueOf(measurement, user), user);
            if (Float.isNaN(value) || value == 0f) {
                continue;
            }

            final View row = inflater.inflate(R.layout.rd_item_detail_row, container, false);
            ((TextView) row.findViewById(R.id.rd_detail_row_label)).setText(metric.labelRes);

            final String unit = metric.unitLabel(requireContext(), user);
            final String formatted = RedesignFormat.value(value, metric.decimals);
            ((TextView) row.findViewById(R.id.rd_detail_row_value))
                    .setText(unit.isEmpty() ? formatted : formatted + " " + unit);

            container.addView(row);
        }
    }

    private void bindNote(View root) {
        final TextView note = root.findViewById(R.id.rd_detail_note);
        final String comment = measurement.getComment();
        note.setText(comment == null || comment.trim().isEmpty()
                ? getString(R.string.rd_record_no_note) : comment.trim());
    }

    /**
     * Confirmação antes de apagar.
     * Respeita a preferência "deleteConfirmationEnable" da UI antiga.
     */
    private void confirmDelete() {
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(requireContext());

        if (!prefs.getBoolean("deleteConfirmationEnable", true)) {
            performDelete();
            return;
        }

        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.question_really_delete)
                .setPositiveButton(R.string.label_yes, (dialog, which) -> performDelete())
                .setNegativeButton(R.string.label_no, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void performDelete() {
        OpenScale.getInstance().deleteScaleMeasurement(measurement.getId());
        dismiss();
    }
}
