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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.health.openscale.R;
import com.health.openscale.gui.MainActivity;
import com.health.openscale.gui.preferences.UserSettingsFragment;

/**
 * Ponte para as telas que ainda não foram reconstruídas.
 *
 * O design novo não cobre tudo o que o app faz: pareamento de balança,
 * backup, lembretes e edição de perfil continuam só na UI antiga. Em vez de
 * reimplementá-las às pressas — ou pior, deixá-las inacessíveis — esta
 * classe abre a Activity antiga **no destino certo**.
 *
 * É temporário por natureza. Cada item daqui deveria virar tela nova ou
 * uma decisão explícita de corte. Ver .claude/docs/inventario-legado.md.
 */
public final class LegacyBridge {

    /** Escaneamento e pareamento de balança Bluetooth. */
    public static void openScaleSearch(Activity activity) {
        openLegacy(activity, R.id.nav_bluetooth_settings, null);
    }

    /** Preferências de Bluetooth (assistente de usuário, merge, etc). */
    public static void openBluetoothPreferences(Activity activity) {
        openLegacy(activity, R.id.nav_bluetooth_preferences, null);
    }

    /** Backup e import/export do banco. */
    public static void openBackup(Activity activity) {
        openLegacy(activity, R.id.nav_backup_preferences, null);
    }

    /** Lembretes de pesagem. */
    public static void openReminders(Activity activity) {
        openLegacy(activity, R.id.nav_reminder_preferences, null);
    }

    /** Tela raiz de preferências, para o que não tem atalho próprio. */
    public static void openSettings(Activity activity) {
        openLegacy(activity, R.id.nav_main_preferences, null);
    }

    /**
     * Criação ou edição de um perfil.
     *
     * @param userId id do perfil a editar, ou -1 para criar um novo.
     */
    public static void openUserSettings(Activity activity, int userId) {
        // UserSettingsFragment le os argumentos via SafeArgs, entao o enum
        // precisa ir como Serializable e as chaves com o nome exato do grafo.
        final Bundle args = new Bundle();
        final boolean isNew = userId == -1;
        args.putSerializable("mode", isNew
                ? UserSettingsFragment.USER_SETTING_MODE.ADD
                : UserSettingsFragment.USER_SETTING_MODE.EDIT);
        args.putInt("userId", userId);
        args.putString("title", activity.getString(
                isNew ? R.string.label_add_user : R.string.label_title_user));
        openLegacy(activity, R.id.nav_usersettings, args);
    }

    /**
     * Abre a MainActivity antiga num destino do grafo dela.
     *
     * Sobre o botão voltar: a Overview antiga intercepta o back e chama
     * finish(). Como a Activity nova está embaixo na pilha, isso fecha só a
     * antiga e devolve o usuário à UI nova — que é o comportamento desejado.
     */
    private static void openLegacy(Activity activity, int destinationId, Bundle args) {
        try {
            final Intent intent = new Intent(activity, MainActivity.class);
            intent.putExtra(MainActivity.EXTRA_DESTINATION, destinationId);
            if (args != null) {
                intent.putExtra(MainActivity.EXTRA_DESTINATION_ARGS, args);
            }
            activity.startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(activity, R.string.rd_legacy_unavailable,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private LegacyBridge() {
    }
}
