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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.health.openscale.ui.theme.isDesignDarkPalette

/**
 * Primitivas visuais do design, compartilhadas por todas as telas.
 *
 * Fonte de verdade: `design/App.dc.html` e `design/Tokens.dc.html`.
 *
 * A regra estrutural que estas primitivas implementam, declarada no design:
 * **sem sombra em nenhum tema**. No claro a hierarquia vem de `surface` mais
 * claro que `background`; no escuro, de uma borda de 1 px em `outlineVariant`.
 * Isso evita blur e elevação custosos no Android, e é o motivo de
 * [DesignCard] não usar o `Card` do Material, que traz elevação por padrão.
 *
 * A outra regra: *o rosa nunca preenche a tela — ele aparece no número, na
 * linha do gráfico, no botão e na variação*. Por isso nenhuma primitiva aqui
 * pinta fundo com `primary`, com a única exceção do chip de variação, que usa
 * `primaryContainer` (rosa claro), não `primary`.
 */

// ── Raios ─────────────────────────────────────────────────────────────────────

/**
 * Raios de canto medidos no protótipo. O design usa cantos generosos e
 * consistentes por papel: quanto maior o elemento, maior o raio.
 */
object DesignRadius {
    /** Cartão de destaque: sparkline, gráfico, blocos grandes. */
    val Large: Dp = 26.dp

    /** Cartão de métrica, campo de perfil, bloco de dados. */
    val Medium: Dp = 22.dp

    /** Linha de registro, bloco pequeno, campo de entrada. */
    val Small: Dp = 20.dp

    /** Bloco compacto: estatística de rodapé, par calculado. */
    val ExtraSmall: Dp = 18.dp
}

// ── Cartão ────────────────────────────────────────────────────────────────────

/**
 * O cartão do design: fundo `surface`, sem sombra, com borda de 1 px só no
 * tema escuro.
 *
 * Substitui o `Card` do Material nas telas redesenhadas. O `Card` aplica
 * `elevation` por padrão, que no claro vira sombra e no escuro vira um tom de
 * superfície mais alto — os dois contrariam o design.
 *
 * @param onClick quando não nulo, o cartão inteiro vira área de toque.
 */
@Composable
fun DesignCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DesignRadius.Medium),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border: BorderStroke? = if (isDesignDarkPalette()) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    } else {
        null
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

// ── Rótulo micro ──────────────────────────────────────────────────────────────

/**
 * O rótulo em caixa alta muito espaçado que é a assinatura do design:
 * "PESO · 17 SET, 08:12", "COMPOSIÇÃO", "ÚLTIMOS 30 DIAS".
 *
 * O `labelSmall` já traz peso, tamanho e tracking; o `uppercase` fica aqui
 * porque o Compose não tem `textAllCaps` no estilo.
 */
@Composable
fun DesignMicroLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// ── Valor com unidade ─────────────────────────────────────────────────────────

/**
 * Um número na tipografia de dado, com a unidade menor ao lado, alinhada pela
 * linha de base — o par que aparece em toda tela do protótipo.
 *
 * O número usa Bricolage (via os slots `display*`/`headline*`) e a unidade usa
 * Instrument, que é a hierarquia declarada no design.
 *
 * @param style o slot do número. `displayLarge` para o peso-herói,
 *   `headlineMedium` para cartão de métrica, `headlineSmall` para linha de
 *   registro.
 */
@Composable
fun DesignValueWithUnit(
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    unitStyle: TextStyle = MaterialTheme.typography.labelLarge,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    unitColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = value,
            style = style,
            color = valueColor,
            maxLines = 1,
        )
        if (!unit.isNullOrBlank()) {
            Text(
                text = unit,
                style = unitStyle,
                color = unitColor,
                maxLines = 1,
                // Compensa a diferença de linha de base entre o número grande e
                // a unidade pequena, para as duas assentarem na mesma linha.
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}

// ── Chip de variação ──────────────────────────────────────────────────────────

/**
 * A direção de uma variação. Determina a seta e — junto com
 * [DesignDeltaChip] — o tratamento de cor.
 */
enum class DesignDeltaDirection { DOWN, UP, FLAT }

/**
 * O chip de variação: `↓ 0,6 kg` sobre `primaryContainer`.
 *
 * É um dos quatro lugares onde o design deixa o rosa aparecer. O protótipo usa
 * o container rosa (claro) como fundo e `onPrimaryContainer` como texto — par
 * com contraste verificado (13,29 no claro, 7,51 no escuro).
 *
 * **Sobre a cor não codificar julgamento:** o protótipo pinta de rosa tanto a
 * queda de peso quanto a alta de músculo — ou seja, o rosa marca *a variação em
 * si*, não "bom" ou "ruim". Perder peso não é universalmente bom, e o app não
 * decide isso pela pessoa. Por isso [DesignDeltaChip] não recebe nenhum
 * parâmetro de "positivo/negativo": todas as variações têm o mesmo tratamento,
 * e só a seta muda.
 */
@Composable
fun DesignDeltaChip(
    text: String,
    direction: DesignDeltaDirection,
    modifier: Modifier = Modifier,
) {
    val arrow = when (direction) {
        DesignDeltaDirection.DOWN -> "↓"
        DesignDeltaDirection.UP -> "↑"
        DesignDeltaDirection.FLAT -> "→"
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = "$arrow $text",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
        )
    }
}

/**
 * A mesma variação em forma de texto, sem fundo — usada dentro dos cartões de
 * métrica e nas linhas de registro, onde um chip cheio pesaria demais.
 *
 * Segue a mesma regra de não julgar: rosa quando há variação, neutro quando
 * não há.
 */
@Composable
fun DesignDeltaText(
    text: String,
    hasChange: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (hasChange) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// ── Cabeçalho de seção ────────────────────────────────────────────────────────

/**
 * O par "rótulo micro à esquerda, ação em rosa à direita" que abre cada seção
 * do protótipo: "COMPOSIÇÃO — Escolher métricas", "ÚLTIMOS 30 DIAS — Ver
 * histórico".
 *
 * @param action texto da ação à direita; quando nulo, fica só o rótulo.
 */
@Composable
fun DesignSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DesignMicroLabel(text = label, modifier = Modifier.weight(1f, fill = false))
        if (action != null && onActionClick != null) {
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(DesignRadius.ExtraSmall))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

// ── Linha de métrica ──────────────────────────────────────────────────────────

/**
 * A linha "rótulo à esquerda, valor à direita" dos blocos de detalhe:
 * "Gordura corporal — 23,8 %".
 *
 * Separador de 1 px no topo, como no protótipo, para as linhas empilharem sem
 * precisar de divisor externo.
 *
 * @param showDivider falso na primeira linha de um bloco, para não desenhar
 *   um separador colado na borda do cartão.
 */
@Composable
fun DesignMetricRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            trailing?.invoke(this)
        }
    }
}
