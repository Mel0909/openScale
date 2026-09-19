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
import android.text.format.DateFormat;

import com.health.openscale.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formatação de números e datas para a UI nova.
 *
 * Tudo passa por Locale.getDefault(), então o separador decimal segue o
 * idioma do aparelho — "72,4" em pt-BR e "72.4" em inglês, como no protótipo,
 * que foi desenhado em português.
 */
public final class RedesignFormat {

    /** Valor numérico com o número de casas da métrica. */
    public static String value(float v, int decimals) {
        if (Float.isNaN(v)) {
            return "–";
        }
        return String.format(Locale.getDefault(), "%." + decimals + "f", v);
    }

    /**
     * Variação em relação à medição anterior, já com a seta.
     * Devolve null quando não há anterior — o chamador esconde o chip.
     */
    public static String delta(Context context, float current, float previous, int decimals) {
        if (Float.isNaN(current) || Float.isNaN(previous)) {
            return null;
        }

        final float diff = current - previous;

        // Diferença abaixo da resolução exibida conta como "sem mudança":
        // mostrar "↓ 0,0 kg" seria ruído.
        final float threshold = (float) Math.pow(10, -decimals) / 2f;
        if (Math.abs(diff) < threshold) {
            return context.getString(R.string.rd_delta_none);
        }

        final String magnitude = value(Math.abs(diff), decimals);
        return context.getString(
                diff < 0 ? R.string.rd_delta_down : R.string.rd_delta_up, magnitude);
    }

    /** Variação com unidade: "↓ 0,6 kg". */
    public static String deltaWithUnit(Context context, float current, float previous,
                                       int decimals, String unit) {
        final String base = delta(context, current, previous, decimals);
        if (base == null) {
            return null;
        }
        if (unit == null || unit.isEmpty()
                || base.equals(context.getString(R.string.rd_delta_none))) {
            return base;
        }
        return base + " " + unit;
    }

    /** Data curta: "17 set". Segue o idioma do aparelho. */
    public static String shortDate(Context context, Date date) {
        if (date == null) {
            return "";
        }
        final String pattern = DateFormat.getBestDateTimePattern(
                Locale.getDefault(), "d MMM");
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    /** Data e hora: "17 set, 08:12". */
    public static String dateWithTime(Context context, Date date) {
        if (date == null) {
            return "";
        }
        final String datePart = shortDate(context, date);
        final String timePart = DateFormat.getTimeFormat(context).format(date);
        return datePart + ", " + timePart;
    }

    /** Só a hora, conforme o formato 12h/24h do aparelho. */
    public static String time(Context context, Date date) {
        if (date == null) {
            return "";
        }
        return DateFormat.getTimeFormat(context).format(date);
    }

    /** Cabeçalho de grupo na lista de medições: "Setembro 2026". */
    public static String monthHeader(Date date) {
        if (date == null) {
            return "";
        }
        final String pattern = DateFormat.getBestDateTimePattern(
                Locale.getDefault(), "LLLL yyyy");
        final String formatted =
                new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
        // Alguns idiomas devolvem o mês em minúscula; o design usa capitalizado.
        if (formatted.length() > 1) {
            return formatted.substring(0, 1).toUpperCase(Locale.getDefault())
                    + formatted.substring(1);
        }
        return formatted;
    }

    /** Dia do mês para a coluna esquerda da lista: "17 set". */
    public static String dayLabel(Context context, Date date) {
        return shortDate(context, date);
    }

    /** Mês e ano por extenso, para "Desde março de 2024" no Perfil. */
    public static String monthYear(Date date) {
        if (date == null) {
            return "";
        }
        final String pattern = DateFormat.getBestDateTimePattern(
                Locale.getDefault(), "LLLL yyyy");
        return new SimpleDateFormat(pattern, Locale.getDefault()).format(date);
    }

    private RedesignFormat() {
    }
}
