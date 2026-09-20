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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * O sparkline do cartão "Últimos 30 dias" da tela Hoje.
 *
 * Desenha exatamente o que o protótipo desenha: uma linha rosa, um
 * preenchimento translúcido abaixo dela e um ponto cheio no último valor.
 * Sem eixos, sem grade, sem rótulos — quem dá contexto é o cartão em volta.
 *
 * É um [Canvas] e não um gráfico da Vico de propósito: a Vico traz eixos,
 * marcadores e gestos que este cartão não usa, e o protótipo aqui é um SVG de
 * três elementos. O gráfico completo, com eixo e toque, é a tela Histórico.
 *
 * @param values os valores em ordem cronológica. Com menos de dois pontos não
 *   há linha a desenhar e o componente não desenha nada — quem chama decide o
 *   que mostrar no lugar.
 */
@Composable
fun DesignSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 104.dp,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    // O preenchimento do protótipo é o rosa claro em ~55% de opacidade. Derivar
    // de primary (em vez de uma cor fixa) faz o sparkline seguir o tema sem
    // precisar de um slot novo na paleta.
    val fillColor = lineColor.copy(alpha = 0.18f)

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        if (values.size < 2) return@Box

        Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
            val w = size.width
            val h = size.height
            // Margem vertical para o traço e o ponto não encostarem na borda.
            val pad = 10f

            val min = values.min()
            val max = values.max()
            // Série constante: uma faixa artificial evita divisão por zero e
            // desenha a linha no meio, que é o que faz sentido visualmente.
            val span = (max - min).takeIf { it > 0f } ?: 1f

            val points = values.mapIndexed { i, v ->
                val x = (i.toFloat() / (values.size - 1)) * w
                val y = pad + (1f - (v - min) / span) * (h - pad * 2)
                Offset(x, y)
            }

            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }

            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            drawPath(path = areaPath, color = fillColor)
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.4.dp.toPx()),
            )
            drawCircle(
                color = lineColor,
                radius = 4.5.dp.toPx(),
                center = points.last(),
            )
        }
    }
}
