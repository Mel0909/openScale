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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.health.openscale.R;
import com.health.openscale.core.OpenScale;
import com.health.openscale.core.bluetooth.BluetoothCommunication;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;
import com.health.openscale.gui.preferences.BluetoothSettingsFragment;

import timber.log.Timber;

/**
 * Sheet "Pesar".
 *
 * Substitui o ícone de status na toolbar da UI antiga: o estado da conexão
 * passa a aparecer aqui, no momento em que importa.
 *
 * IMPORTANTE — esta classe conversa com a camada Bluetooth. As regras estão
 * em .claude/docs/balancas-bluetooth.md e foram seguidas aqui:
 *
 *   - as 5 validações antes de conectar (§4 do documento);
 *   - o Handler trata os 9 estados de BT_STATUS, não só os felizes;
 *   - a regra de merge com a última medição é preservada, com o mesmo
 *     default (ligado) que a UI antiga usa;
 *   - addScaleMeasurement é chamado com silent=true;
 *   - desconecta ao fechar o sheet.
 */
public class WeighSheet extends BottomSheetDialogFragment {

    public static final String TAG = "rd_weigh";

    private TextView statusText;
    private View statusDot;
    private TextView readingLabel;
    private TextView readingValue;
    private TextView readingUnit;
    private TextView readingHint;
    private MaterialButton saveButton;

    private ScaleMeasurement pendingMeasurement;

    public static WeighSheet newInstance() {
        return new WeighSheet();
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
        final View root = inflater.inflate(R.layout.rd_sheet_weigh, container, false);

        statusText = root.findViewById(R.id.rd_weigh_status);
        statusDot = root.findViewById(R.id.rd_weigh_status_dot);
        readingLabel = root.findViewById(R.id.rd_weigh_label);
        readingValue = root.findViewById(R.id.rd_weigh_value);
        readingUnit = root.findViewById(R.id.rd_weigh_unit);
        readingHint = root.findViewById(R.id.rd_weigh_hint);
        saveButton = root.findViewById(R.id.rd_weigh_save);

        root.findViewById(R.id.rd_weigh_manual).setOnClickListener(v -> {
            dismiss();
            AddMeasurementSheet.newInstance(Float.NaN)
                    .show(getParentFragmentManager(), AddMeasurementSheet.TAG);
        });

        saveButton.setOnClickListener(v -> savePendingMeasurement());

        showWaitingState();
        connectToScale();

        return root;
    }

    @Override
    public void onDestroyView() {
        // Sempre desconecta ao sair: manter a conexão aberta drena bateria e
        // impede outro app de falar com a balança.
        OpenScale.getInstance().disconnectFromBluetoothDevice();
        super.onDestroyView();
    }

    private void showWaitingState() {
        pendingMeasurement = null;
        readingLabel.setText(R.string.rd_weigh_step_on);
        readingValue.setText(R.string.rd_weigh_placeholder);
        readingValue.setTextColor(RedesignTheme.color(requireContext(),
                com.google.android.material.R.attr.colorOutlineVariant));
        readingUnit.setVisibility(View.GONE);
        readingHint.setText(getString(R.string.rd_weigh_waiting, currentUserName()));
        saveButton.setEnabled(false);
        saveButton.setText(R.string.rd_weigh_waiting_cta);
    }

    private void showReadingState(ScaleMeasurement measurement) {
        pendingMeasurement = measurement;

        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        final MetricCatalog.Metric weight = MetricCatalog.byKey(MetricCatalog.KEY_WEIGHT);
        if (weight == null) {
            return;
        }

        final float value = weight.toUserUnit(weight.valueOf(measurement, user), user);

        readingLabel.setText(R.string.rd_weigh_received);
        readingValue.setText(RedesignFormat.value(value, weight.decimals));
        readingValue.setTextColor(RedesignTheme.color(requireContext(),
                com.google.android.material.R.attr.colorOnSurface));
        readingUnit.setVisibility(View.VISIBLE);
        readingUnit.setText(weight.unitLabel(requireContext(), user));
        readingHint.setText(R.string.rd_weigh_received_hint);
        saveButton.setEnabled(true);
        saveButton.setText(R.string.rd_weigh_save);
    }

    private String currentUserName() {
        final ScaleUser user = OpenScale.getInstance().getSelectedScaleUser();
        return user == null ? "" : user.getUserName();
    }

    /**
     * Validações antes de conectar.
     * Espelha MainActivity.invokeConnectToBluetoothDevice — ver §4 do
     * documento das balanças. Pular qualquer uma faz a conexão falhar de
     * forma confusa para o usuário.
     */
    private void connectToScale() {
        final Context context = requireContext();
        final OpenScale openScale = OpenScale.getInstance();

        // 1. Existe usuário selecionado?
        if (openScale.getSelectedScaleUserId() == -1) {
            setStatus(getString(R.string.info_no_selected_user), false);
            return;
        }

        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(context);
        final String deviceName = prefs.getString(
                BluetoothSettingsFragment.PREFERENCE_KEY_BLUETOOTH_DEVICE_NAME, "");
        final String hwAddress = prefs.getString(
                BluetoothSettingsFragment.PREFERENCE_KEY_BLUETOOTH_HW_ADDRESS, "");

        // 2. Existe balança pareada, com MAC válido?
        if (!BluetoothAdapter.checkBluetoothAddress(hwAddress)) {
            setStatus(getString(R.string.rd_settings_scale_none), false);
            return;
        }

        // 3. Bluetooth está ligado?
        final BluetoothManager manager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager == null || manager.getAdapter() == null
                || !manager.getAdapter().isEnabled()) {
            setStatus(getString(R.string.info_bluetooth_no_device), false);
            return;
        }

        setStatus(getString(R.string.rd_weigh_connecting), false);

        // 4. O driver existe para esse nome de dispositivo?
        //    registerCallbackHandler acontece dentro de connectToBluetoothDevice,
        //    antes do connect — como exige o contrato da camada.
        if (!openScale.connectToBluetoothDevice(deviceName, hwAddress, btHandler)) {
            setStatus(deviceName + " "
                    + getString(R.string.label_bt_device_no_support), false);
        }
    }

    private void setStatus(String text, boolean connected) {
        statusText.setText(text);
        statusDot.setVisibility(connected ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Handler dos estados da camada Bluetooth.
     *
     * Trata os 9 valores de BT_STATUS. A ordem do enum é o protocolo entre
     * driver e UI — não inserir valores no meio dele.
     */
    private final Handler btHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!isAdded()) {
                return;
            }

            final BluetoothCommunication.BT_STATUS status =
                    BluetoothCommunication.BT_STATUS.values()[msg.what];

            switch (status) {
                case RETRIEVE_SCALE_DATA:
                    handleMeasurement((ScaleMeasurement) msg.obj);
                    break;

                case CONNECTION_ESTABLISHED:
                    setStatus(getString(R.string.info_bluetooth_connection_successful), true);
                    break;

                case INIT_PROCESS:
                    setStatus(getString(R.string.info_bluetooth_init), true);
                    break;

                case CONNECTION_RETRYING:
                    setStatus(getString(R.string.info_bluetooth_no_device_retrying), false);
                    break;

                case CONNECTION_LOST:
                    setStatus(getString(R.string.info_bluetooth_connection_lost), false);
                    break;

                case CONNECTION_DISCONNECT:
                    setStatus(getString(R.string.info_bluetooth_connection_disconnected), false);
                    break;

                case NO_DEVICE_FOUND:
                    setStatus(getString(R.string.info_bluetooth_no_device), false);
                    break;

                case UNEXPECTED_ERROR:
                    setStatus(getString(R.string.info_bluetooth_connection_error)
                            + ": " + msg.obj, false);
                    Timber.e("Bluetooth unexpected error: %s", msg.obj);
                    break;

                case SCALE_MESSAGE:
                    // msg.arg1 é um string resource id, resolvido com msg.obj
                    // como argumento — contrato da camada Bluetooth.
                    try {
                        final String text = getString(msg.arg1, msg.obj);
                        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
                    } catch (Exception ex) {
                        Timber.e("Bluetooth scale message error: %s", ex);
                    }
                    break;
            }
        }
    };

    /**
     * Medição recebida da balança.
     *
     * A regra de merge é a mesma da UI antiga, com o mesmo default (ligado):
     * completa os campos zerados com os da última medição, para quem pesa na
     * balança e mede a cintura à mão depois.
     */
    private void handleMeasurement(ScaleMeasurement measurement) {
        if (measurement == null) {
            return;
        }

        final OpenScale openScale = OpenScale.getInstance();
        final SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(requireContext());

        if (prefs.getBoolean("mergeWithLastMeasurement", true)
                && !openScale.isScaleMeasurementListEmpty()) {
            measurement.merge(openScale.getLastScaleMeasurement());
        }

        showReadingState(measurement);
    }

    /**
     * Grava a medição.
     *
     * silent=true porque o sheet já dá o retorno visual — o toast da UI
     * antiga viria em duplicidade.
     */
    private void savePendingMeasurement() {
        if (pendingMeasurement == null) {
            return;
        }
        OpenScale.getInstance().addScaleMeasurement(pendingMeasurement, true);
        dismiss();
    }
}
