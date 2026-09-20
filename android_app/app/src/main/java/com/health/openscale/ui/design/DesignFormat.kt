/*
 * openScale
 * Copyright (C) 2026 openScale contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.health.openscale.ui.design

import com.health.openscale.core.data.UnitType
import com.health.openscale.core.utils.ConverterUtils
import com.health.openscale.core.utils.LocaleUtils
import java.util.Locale
import kotlin.math.abs

/**
 * Formatação de número para o design.
 *
 * O design separa o número da unidade: o número vai em Bricolage, grande, e a
 * unidade em Instrument, pequena, ao lado — `72,4` + `kg`, não `72,4 kg` num
 * texto só.
 *
 * O [LocaleUtils.formatValueForDisplay] do upstream **já concatena a
 * unidade**, então usá-lo para o número e pôr a unidade ao lado produz
 * "72,4 kg kg". Daí este formatador existir.
 *
 * As casas decimais por unidade são as mesmas do upstream, de propósito: o
 * mesmo valor não deve aparecer com precisão diferente em duas telas.
 */
object DesignFormat {

    /**
     * Só o número, sem unidade, com as casas decimais que a unidade pede.
     *
     * @param includeSign prefixa `+` ou `−` — para variações, onde o sinal é
     *   a informação principal.
     */
    fun number(
        value: Float,
        unit: UnitType,
        includeSign: Boolean = false,
    ): String {
        // effectiveLocale() e privado no upstream; getDefault() e o mesmo
        // valor salvo quando ha override de idioma no app.
        val locale = Locale.getDefault()
        val n = value.toDouble()
        val sign = when {
            !includeSign -> ""
            n > 0 -> "+"
            n < 0 -> "−"
            else -> ""
        }
        val absVal = abs(n)

        // Stone é o caso especial: não é um número decimal, e sim dois
        // valores ("11 st 6 lb"). Não há como separar número de unidade, então
        // o texto inteiro vira o "número" e a unidade ao lado fica vazia.
        if (unit == UnitType.ST) {
            val (st, lb) = ConverterUtils.decimalStToStLb(absVal)
            return "$sign$st st $lb lb"
        }

        return sign + LocaleUtils.formatNumber(absVal, unit.maxFractionDigits(), locale)
    }

    /**
     * A unidade a mostrar ao lado do número, ou `null` quando não há.
     *
     * `NONE` não tem unidade (IMC, por exemplo), e `ST` já vem embutida no
     * número.
     */
    fun unitLabel(unit: UnitType): String? = when (unit) {
        UnitType.NONE, UnitType.ST -> null
        else -> unit.displayName
    }

    /** As casas decimais de cada unidade, iguais às do upstream. */
    private fun UnitType.maxFractionDigits(): Int = when (this) {
        UnitType.KG, UnitType.INCH -> 2
        UnitType.LB, UnitType.PERCENT, UnitType.CM, UnitType.OHM, UnitType.NONE -> 1
        UnitType.KCAL, UnitType.BPM -> 0
        UnitType.ST -> 0 // tratado acima
    }
}
