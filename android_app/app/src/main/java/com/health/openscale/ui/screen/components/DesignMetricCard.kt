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
package com.health.openscale.ui.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * O cartão de métrica da grade de composição, na tela Hoje.
 *
 * O protótipo desenha três linhas: rótulo micro, número com unidade e a
 * variação em rosa. Altura mínima de 112 dp, para os cartões da grade ficarem
 * alinhados mesmo quando um deles não tem variação a mostrar.
 *
 * @param delta a variação já formatada; quando nula, o espaço continua
 *   reservado, para a grade não ficar irregular.
 */
@Composable
fun DesignMetricCard(
    label: String,
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
    delta: String? = null,
    hasChange: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    DesignCard(
        modifier = modifier.defaultMinSize(minHeight = 112.dp),
        shape = RoundedCornerShape(DesignRadius.Medium),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            DesignMicroLabel(text = label)
            DesignValueWithUnit(
                value = value,
                unit = unit,
                style = MaterialTheme.typography.headlineMedium,
            )
            DesignDeltaText(
                // Espaço reservado mesmo sem variação: mantém a grade alinhada
                // quando só alguns cartões têm medição anterior para comparar.
                text = delta ?: "—",
                hasChange = hasChange && delta != null,
            )
        }
    }
}
