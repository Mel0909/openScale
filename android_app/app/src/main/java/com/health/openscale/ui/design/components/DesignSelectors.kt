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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.health.openscale.ui.design.DesignTokens

/**
 * A faixa de chips de métrica da tela Histórico.
 *
 * O protótipo desenha chips de canto totalmente arredondado, em rolagem
 * horizontal. O selecionado é preenchido com `primary` e texto `onPrimary`; os
 * demais são transparentes, com borda em `outline`.
 *
 * É um dos quatro lugares onde o rosa aparece: aqui ele marca *qual métrica o
 * gráfico está mostrando*.
 */
@Composable
fun <T> DesignChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items.size) { i ->
            val item = items[i]
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 42.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = RoundedCornerShape(percent = 50),
                    )
                    .clickable { onSelect(item) }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(item),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * O seletor de período segmentado: Semana · Mês · Ano.
 *
 * O protótipo desenha uma trilha em `surfaceVariant` com cantos totalmente
 * arredondados, e o segmento ativo em `surface` — ou seja, **o ativo não é
 * rosa**. Aqui o destaque vem da superfície mais clara, não da cor de ação:
 * é uma escolha de visualização, não uma ação.
 */
@Composable
fun <T> DesignSegmentedSelector(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = DesignTokens.Size.TouchTarget)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .clickable { onSelect(item) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(item),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * O cartão de estatística do rodapé do Histórico: Mínimo · Média · Máximo.
 *
 * Rótulo micro em cima, número em Bricolage 700 de 20 px embaixo.
 */
@Composable
fun DesignStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    DesignSurface(
        modifier = modifier,
        shape = RoundedCornerShape(DesignTokens.Radius.Row),
        contentPadding = 14.dp,
    ) {
        DesignMicroLabel(text = label)
        Box(modifier = Modifier.padding(top = 6.dp)) {
            DesignStatValue(value = value)
        }
    }
}
