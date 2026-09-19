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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;
import com.health.openscale.core.utils.Converters;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Sheet "Nova medição" — entrada manual.
 *
 * Preserva o fluxo eficiente da UI antiga: peso com botões +/- de 0,1 em 0,1,
 * delta em relação à última medição, e os campos manuais (cintura, quadril,
 * pescoço) logo abaixo.
 *
 * Serve tanto para criar quanto para editar: newInstanceForEdit carrega uma
 * medição existente.
 */
public class AddMeasurementSheet extends BottomSheetDialogFragment {

    public static final String TAG = "rd_add_measurement";
    private static final String ARG_WEIGHT = "initialWeight";
    private static final String ARG_EDIT_ID = "editId";

    /** Passo dos botões +/-, na unidade do usuário. */
    private static final float WEIGHT_STEP = 0.1f;

    private TextView weightValue;
    private TextView weightUnit;
    private TextView weightDelta;
    private TextView dateValue;
    private TextView timeValue;
    private EditText noteInput;
    private LinearLayout manualFields;

    private final Calendar dateTime = Calendar.getInstance();

    /** Peso na unidade do usuário (não em kg). */
    private float weightInUserUnit = 0f;

    private ScaleMeasurement editing;

    public static AddMeasurementSheet newInstance(float initialWeight) {
        final AddMeasurementSheet sheet = new AddMeasurementSheet();
        final Bundle args = new Bundle();
        args.putFloat(ARG_WEIGHT, initialWeight);
        sheet.setArguments(args);
        return sheet;
    }

    public static AddMeasurementSheet newInstanceForEdit(int measurementId) {
        final AddMeasurementSheet sheet = new AddMeasurementSheet();
        final Bundle args = new Bundle();
        args.putInt(ARG_EDIT_ID, measurementId);
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
        final View root = inflater.inflate(R.layout.rd_sheet_add_measurement, container, false);

        weightValue = root.findViewById(R.id.rd_add_weight_value);
        weightUnit = root.findViewById(R.id.rd_add_weight_unit);
        weightDelta = root.findViewById(R.id.rd_add_weight_delta);
        dateValue = root.findViewById(R.id.rd_add_date_value);
        timeValue = root.findViewById(R.id.rd_add_time_value);
        noteInput = root.findViewById(R.id.rd_add_note_input);
        manualFields = root.findViewById(R.id.rd_add_manual_fields);

        loadInitialState();

        root.findViewById(R.id.rd_add_decrease).setOnClickListener(v -> changeWeight(-WEIGHT_STEP));
        root.findViewById(R.id.rd_add_increase).setOnClickListener(v -> changeWeight(WEIGHT_STEP));
        root.findViewById(R.id.rd_add_date).setOnClickListener(v -> pickDate());
        root.findViewById(R.id.rd_add_time).setOnClickListener(v -> pickTime());
        root.findViewById(R.id.rd_add_save).setOnClickListener(v -> save());
        root.findViewById(R.id.rd_add_close).setOnClickListener(v -> dismiss());

        buildManualFields(inflater);
        updateWeightViews();
        updateDateTimeViews();

        return root;
    }

    private void loadInitialState() {
        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        final MetricCatalog.Metric weight = MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);

        final int editId = getArguments() == null ? -1 : getArguments().getInt(ARG_EDIT_ID, -1);
        if (editId != -1) {
            editing = findMeasurement(editId);
        }

        if (editing != null) {
            dateTime.setTime(editing.getDateTime());
            if (weight != null) {
                weightInUserUnit = weight.toUserUnit(weight.valueOf(editing, user), user);
            }
            if (editing.getComment() != null) {
                noteInput.setText(editing.getComment());
            }
            return;
        }

        // Peso vindo da balança pelo sheet de pesagem.
        final float provided = getArguments() == null
                ? Float.NaN : getArguments().getFloat(ARG_WEIGHT, Float.NaN);
        if (!Float.isNaN(provided) && provided > 0f) {
            weightInUserUnit = provided;
            return;
        }

        // Sem ponto de partida: começa na última medição, que é o palpite
        // mais útil — o peso raramente muda muito entre pesagens.
        final ScaleMeasurement last = OpenScale.getInstance().getLastScaleMeasurement();
        if (last != null && weight != null) {
            weightInUserUnit = weight.toUserUnit(weight.valueOf(last, user), user);
        } else {
            weightInUserUnit = 70f;
        }
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

    private void changeWeight(float delta) {
        weightInUserUnit = Math.max(0f,
                Math.round((weightInUserUnit + delta) * 10f) / 10f);
        updateWeightViews();
    }

    private void updateWeightViews() {
        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        final MetricCatalog.Metric weight = MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);
        if (weight == null) {
            return;
        }

        weightValue.setText(RedesignFormat.value(weightInUserUnit, weight.decimals));
        weightUnit.setText(weight.unitLabel(requireContext(), user));

        final ScaleMeasurement last = OpenScale.getInstance().getLastScaleMeasurement();
        if (last == null) {
            weightDelta.setVisibility(View.GONE);
            return;
        }

        final float previous = weight.toUserUnit(weight.valueOf(last, user), user);
        final String delta = RedesignFormat.deltaWithUnit(requireContext(),
                weightInUserUnit, previous, weight.decimals,
                weight.unitLabel(requireContext(), user));

        if (delta == null) {
            weightDelta.setVisibility(View.GONE);
        } else {
            weightDelta.setVisibility(View.VISIBLE);
            weightDelta.setText(getString(R.string.rd_add_delta_from, delta,
                    RedesignFormat.shortDate(requireContext(), last.getDateTime())));
        }
    }

    private void updateDateTimeViews() {
        dateValue.setText(RedesignFormat.shortDate(requireContext(), dateTime.getTime()));
        timeValue.setText(RedesignFormat.time(requireContext(), dateTime.getTime()));
    }

    private void pickDate() {
        new DatePickerDialog(requireContext(), (view, year, month, day) -> {
            dateTime.set(Calendar.YEAR, year);
            dateTime.set(Calendar.MONTH, month);
            dateTime.set(Calendar.DAY_OF_MONTH, day);
            updateDateTimeViews();
        }, dateTime.get(Calendar.YEAR), dateTime.get(Calendar.MONTH),
                dateTime.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        new TimePickerDialog(requireContext(), (view, hour, minute) -> {
            dateTime.set(Calendar.HOUR_OF_DAY, hour);
            dateTime.set(Calendar.MINUTE, minute);
            updateDateTimeViews();
        }, dateTime.get(Calendar.HOUR_OF_DAY), dateTime.get(Calendar.MINUTE),
                android.text.format.DateFormat.is24HourFormat(requireContext())).show();
    }

    /**
     * Campos manuais (cintura, quadril, pescoço…) — só os que estão
     * habilitados e cuja origem é entrada manual.
     */
    private void buildManualFields(LayoutInflater inflater) {
        manualFields.removeAllViews();

        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();

        for (MetricCatalog.Metric metric : MetricPreferences.enabledMetrics(requireContext())) {
            if (metric.source != MetricCatalog.Source.MANUAL) {
                continue;
            }

            final View row = inflater.inflate(R.layout.rd_item_manual_field, manualFields, false);
            ((TextView) row.findViewById(R.id.rd_manual_label)).setText(metric.labelRes);
            ((TextView) row.findViewById(R.id.rd_manual_unit))
                    .setText(metric.unitLabel(requireContext(), user));

            final EditText input = row.findViewById(R.id.rd_manual_input);
            input.setTag(metric.key);

            if (editing != null) {
                final float value = metric.toUserUnit(metric.valueOf(editing, user), user);
                if (!Float.isNaN(value) && value != 0f) {
                    input.setText(RedesignFormat.value(value, metric.decimals));
                }
            }

            manualFields.addView(row);
        }
    }

    /**
     * Grava a medição.
     *
     * Converte de volta para a unidade base (kg/cm) antes de salvar — o core
     * armazena sempre em unidade base, e é a UI que converte para exibir.
     */
    private void save() {
        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        if (user == null) {
            dismiss();
            return;
        }

        final ScaleMeasurement measurement =
                editing != null ? editing : new ScaleMeasurement();

        measurement.setUserId(user.getId());
        measurement.setDateTime(dateTime.getTime());
        measurement.setWeight(
                Converters.toKilogram(weightInUserUnit, user.getScaleUnit()));

        applyManualFields(measurement, user);

        final String note = noteInput.getText().toString().trim();
        measurement.setComment(note.isEmpty() ? null : note);

        if (editing != null) {
            OpenScale.getInstance().updateScaleMeasurement(measurement);
        } else {
            // silent=false aqui: entrada manual não tem outro retorno visual
            // além do toast, diferente do sheet de pesagem.
            OpenScale.getInstance().addScaleMeasurement(measurement, false);
        }

        dismiss();
    }

    private void applyManualFields(ScaleMeasurement measurement, ScaleUser user) {
        for (int i = 0; i < manualFields.getChildCount(); i++) {
            final View row = manualFields.getChildAt(i);
            final EditText input = row.findViewById(R.id.rd_manual_input);
            if (input == null || input.getTag() == null) {
                continue;
            }

            final String text = input.getText().toString().trim().replace(',', '.');
            if (text.isEmpty()) {
                continue;
            }

            final float value;
            try {
                value = Float.parseFloat(text);
            } catch (NumberFormatException ex) {
                continue;
            }

            applyMetricValue(measurement, (String) input.getTag(), value, user);
        }
    }

    /**
     * Escreve um valor manual no ScaleMeasurement, convertendo para a
     * unidade base.
     */
    private void applyMetricValue(ScaleMeasurement measurement, String key,
                                  float userValue, ScaleUser user) {
        final float base = Converters.toCentimeter(userValue, user.getMeasureUnit());

        switch (key) {
            case MetricCatalog.KEY_WAIST:
                measurement.setWaist(base);
                break;
            case MetricCatalog.KEY_HIP:
                measurement.setHip(base);
                break;
            case MetricCatalog.KEY_NECK:
                measurement.setNeck(base);
                break;
            case MetricCatalog.KEY_CHEST:
                measurement.setChest(base);
                break;
            case MetricCatalog.KEY_THIGH:
                measurement.setThigh(base);
                break;
            case MetricCatalog.KEY_BICEPS:
                measurement.setBiceps(base);
                break;
            case MetricCatalog.KEY_CALIPER1:
                measurement.setCaliper1(base);
                break;
            case MetricCatalog.KEY_CALIPER2:
                measurement.setCaliper2(base);
                break;
            case MetricCatalog.KEY_CALIPER3:
                measurement.setCaliper3(base);
                break;
            default:
                break;
        }
    }
}
