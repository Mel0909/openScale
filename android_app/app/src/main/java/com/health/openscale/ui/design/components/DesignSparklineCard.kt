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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.health.openscale.ui.design.DesignTokens

/**
 * O cartão "Últimos 30 dias" da tela Hoje, inteiro.
 *
 * O protótipo desenha: cabeçalho com o rótulo micro e a ação "Ver histórico",
 * um SVG com área + linha + ponto final, e três rótulos de data embaixo. O
 * cartão todo é clicável e leva ao Histórico.
 *
 * O gráfico é um [Canvas] de três elementos, como no protótipo — sem eixo, sem
 * grade, sem marcador. Quem tem eixo e toque é a tela Histórico.
 *
 * @param values os valores em ordem cronológica, da janela de 30 dias de
 *   calendário. Com menos de dois pontos não há linha, e o cartão mostra o
 *   aviso em vez do gráfico.
 * @param axisLabels os três rótulos de data do rodapé: início, meio e fim.
 */
@Composable
fun DesignSparklineCard(
    label: String,
    actionLabel: String,
    values: List<Float>,
    axisLabels: List<String>,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    DesignSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.Radius.Card),
        onClick = onClick,
        contentPadding = 18.dp,
    ) {
        DesignSectionHeader(
            label = label,
            action = actionLabel,
            onActionClick = onClick,
        )

        Spacer(Modifier.height(8.dp))

        if (values.size < 2) {
            // O protótipo assume histórico. Sem dois pontos não há linha a
            // desenhar, e um texto curto é mais honesto que um gráfico vazio.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.Size.Sparkline),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            DesignSparkline(
                values = values,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                axisLabels.forEach { t ->
                    Text(
                        text = t,
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * A linha do sparkline: área translúcida, traço de 2,4 px e o ponto cheio no
 * último valor. É o SVG do protótipo, traduzido.
 */
@Composable
fun DesignSparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    // `fill:var(--fill)` com `opacity:0.55`. Derivar de primary faz a cor
    // seguir o tema sem precisar de um slot novo na paleta.
    val fillColor = lineColor.copy(alpha = 0.22f)

    Canvas(modifier = modifier.height(DesignTokens.Size.Sparkline)) {
        val w = size.width
        val h = size.height
        // Margem para o traço e o ponto final não encostarem na borda.
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
            style = Stroke(width = 2.4.dp.toPx()),
        )
        drawCircle(
            color = lineColor,
            radius = 4.5.dp.toPx(),
            center = points.last(),
        )
    }
}
