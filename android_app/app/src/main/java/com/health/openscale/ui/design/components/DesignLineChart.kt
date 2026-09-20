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
package com.health.openscale.ui.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * O gráfico da tela Histórico, como `design/App.dc.html` o desenha.
 *
 * O protótipo desenha um SVG com:
 *
 *  - três linhas de grade horizontais, em `--grid`;
 *  - a área sob a curva, em `--fill` a 50 % de opacidade;
 *  - a curva, em `--pr`, traço de 2,6 px;
 *  - círculos vazados nos pontos — preenchidos com `--sf` e contornados em
 *    `--pr`, de modo que a linha "passa por trás" deles;
 *  - rótulos do eixo Y à esquerda (alto, meio, baixo) e do eixo X embaixo.
 *
 * Não usa a Vico nem a MPAndroidChart: as duas trazem eixos, marcadores e
 * gestos com aparência própria, que teriam de ser desfeitos um a um para
 * chegar neste desenho. Um [Canvas] custa menos e é exatamente o protótipo.
 *
 * A curva é suavizada (Catmull-Rom), como o `CUBIC_BEZIER` que o redesign
 * anterior usava.
 *
 * @param values os valores em ordem cronológica.
 * @param xLabels os rótulos do eixo horizontal, distribuídos uniformemente.
 * @param yLabels exatamente três: topo, meio e base do eixo vertical.
 */
@Composable
fun DesignLineChart(
    values: List<Float>,
    xLabels: List<String>,
    yLabels: List<String>,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 156.dp,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = lineColor.copy(alpha = 0.20f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotFill = MaterialTheme.colorScheme.surface

    val axisStyle = MaterialTheme.typography.labelSmall.copy(
        // Os rótulos de eixo não são caixa alta nem espaçados: só herdam o
        // tamanho do rótulo micro.
        letterSpacing = TextUnit.Unspecified,
    )
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Eixo Y: três rótulos distribuídos na altura do gráfico.
        Column(
            modifier = Modifier.height(height),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End,
        ) {
            yLabels.forEach { label ->
                Text(text = label, style = axisStyle, color = axisColor, maxLines = 1)
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                val w = size.width
                val h = size.height
                // Margem vertical para os círculos dos extremos não serem
                // cortados pela borda do gráfico.
                val pad = 8f

                // As três linhas de grade: topo, meio e base.
                listOf(pad, h / 2f, h - pad).forEach { y ->
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }

                if (values.size < 2) return@Canvas

                val min = values.min()
                val max = values.max()
                val span = (max - min).takeIf { it > 0f } ?: 1f

                val points = values.mapIndexed { i, v ->
                    Offset(
                        x = (i.toFloat() / (values.size - 1)) * w,
                        y = pad + (1f - (v - min) / span) * (h - pad * 2),
                    )
                }

                val linePath = designSmoothPath(points)

                drawPath(
                    path = Path().apply {
                        addPath(linePath)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    },
                    color = fillColor,
                )
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 2.6.dp.toPx()),
                )

                // Círculos vazados: fundo na cor do cartão, contorno na cor da
                // linha — é o que faz a curva parecer passar por trás deles.
                points.forEach { p ->
                    drawCircle(color = dotFill, radius = 3.4.dp.toPx(), center = p)
                    drawCircle(
                        color = lineColor,
                        radius = 3.4.dp.toPx(),
                        center = p,
                        style = Stroke(width = 2.2.dp.toPx()),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                xLabels.forEachIndexed { i, label ->
                    Text(
                        text = label,
                        style = axisStyle,
                        color = axisColor,
                        maxLines = 1,
                        textAlign = when (i) {
                            0 -> TextAlign.Start
                            xLabels.lastIndex -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * Curva de Catmull-Rom convertida em Bézier cúbica.
 *
 * Compartilhada com o sparkline da tela Hoje: as duas curvas do design têm a
 * mesma suavização, e duplicar a matemática seria arriscar que divirjam.
 */
internal fun designSmoothPath(points: List<Offset>, smoothing: Float = 0.3f): Path =
    Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 0 until points.size - 1) {
            val p0 = points[if (i == 0) 0 else i - 1]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points[if (i + 2 < points.size) i + 2 else i + 1]
            cubicTo(
                p1.x + (p2.x - p0.x) * smoothing,
                p1.y + (p2.y - p0.y) * smoothing,
                p2.x - (p3.x - p1.x) * smoothing,
                p2.y - (p3.y - p1.y) * smoothing,
                p2.x,
                p2.y,
            )
        }
    }
