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
import android.widget.Toast;

import com.health.openscale.R;
import com.health.openscale.gui.MainActivity;

/**
 * Ponte para as telas que ainda não foram reconstruídas.
 *
 * O design novo não cobre tudo o que o app faz: backup, lembretes,
 * pareamento de balança, onboarding e edição de perfil continuam só na UI
 * antiga. Em vez de reimplementá-las às pressas — ou pior, deixá-las
 * inacessíveis — esta classe abre a Activity antiga.
 *
 * É temporário por natureza. Cada item daqui deveria virar tela nova ou
 * uma decisão explícita de corte. Ver .claude/docs/inventario-legado.md.
 */
public final class LegacyBridge {

    /**
     * Abre a UI antiga para criar ou editar um usuário.
     *
     * @param userId id do perfil a editar, ou -1 para criar o primeiro.
     */
    public static void openUserSettings(Activity activity, int userId) {
        openLegacy(activity);
    }

    /** Abre a UI antiga nas preferências (backup, lembretes, bluetooth). */
    public static void openSettings(Activity activity) {
        openLegacy(activity);
    }

    /**
     * Abre a MainActivity antiga.
     *
     * Não navega para um destino específico: a MainActivity antiga decide o
     * fragment inicial pela preferência "lastFragmentId", e forçar um destino
     * exigiria expor a navegação dela. Como é uma ponte temporária, abrir a
     * tela e deixar o usuário chegar ao lugar é suficiente.
     *
     * Sobre o botão voltar: a Overview antiga intercepta o back e chama
     * finish(). Como a Activity nova está embaixo na pilha, isso fecha só a
     * antiga e devolve o usuário à UI nova — que é o comportamento desejado.
     */
    private static void openLegacy(Activity activity) {
        try {
            final Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(activity, R.string.rd_legacy_unavailable,
                    Toast.LENGTH_SHORT).show();
        }
    }

    private LegacyBridge() {
    }
}
