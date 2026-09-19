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
import android.util.TypedValue;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;

/**
 * Acesso às cores do tema a partir de código.
 *
 * A UI nova resolve cor por atributo (?attr/colorPrimary) em vez de por
 * recurso fixo (@color/...). Isso é o que faz claro e escuro funcionarem
 * sem nenhum if espalhado pelo código — foi justamente o problema do app
 * antigo, que tinha cor decidida em quatro lugares diferentes.
 */
public final class RedesignTheme {

    /** Resolve um atributo de cor do tema atual. */
    @ColorInt
    public static int color(Context context, @AttrRes int attr) {
        final TypedValue value = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return androidx.core.content.ContextCompat.getColor(context, value.resourceId);
            }
            return value.data;
        }
        // Se o atributo não existe no tema, é sinal de que o parent não é
        // Material 3 — cinza é melhor que crash.
        return 0xFF888888;
    }

    private RedesignTheme() {
    }
}
