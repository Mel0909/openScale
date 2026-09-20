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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.health.openscale.ui.design.DesignTokens
import com.health.openscale.ui.theme.BricolageGrotesque
import com.health.openscale.ui.theme.InstrumentSans

/**
 * Os componentes do design, um por elemento que o protótipo desenha.
 *
 * Fonte de verdade: `design/App.dc.html`. Nada aqui existe sem um elemento
 * correspondente no protótipo.
 *
 * Esta pasta (`ui/design/`) é separada de `ui/screen/` de propósito: lá está o
 * código do upstream, que continua intocado e continua mesclando limpo. Aqui
 * está o design próprio, inteiro, sem reaproveitar componente do upstream.
 */

/**
 * Se a paleta em vigor é escura. Decide o tratamento de superfície: o design
 * não usa sombra em nenhum tema, e no escuro a hierarquia vem de uma borda de
 * 1 px.
 */
@Composable
private fun isDark(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Encolhe o conteúdo, proporcionalmente, só o bastante para caber na largura
 * disponível.
 *
 * Os números do design são grandes — 84 sp no peso-herói, 31 sp no cartão de
 * métrica — e foram medidos para os valores do protótipo ("72,4", "23,8").
 * Valores reais variam: "1110 kcal" tem o dobro de dígitos, e a pessoa pode
 * ter a fonte do sistema ampliada. Sem isto o número atravessa a tela e
 * empurra a unidade para fora, que foi exatamente o que aconteceu.
 *
 * Mede o texto no seu tamanho natural e aplica um fator de escala no desenho.
 * Como acontece na fase de layout, não há recomposição nem risco de oscilar
 * entre dois tamanhos — o que um laço de "diminui e tenta de novo" traria.
 */
private fun Modifier.designShrinkToFit(): Modifier = layout { measurable, constraints ->
    // Mede sem limite de largura: o tamanho que o texto quer ter.
    val placeable = measurable.measure(constraints.copy(maxWidth = Constraints.Infinity))
    val maxWidth = constraints.maxWidth
    val scale = if (placeable.width > maxWidth && placeable.width > 0) {
        maxWidth.toFloat() / placeable.width
    } else {
        1f
    }
    val width = (placeable.width * scale).toInt()
    val height = (placeable.height * scale).toInt()
    layout(width, height) {
        if (scale == 1f) {
            placeable.place(0, 0)
        } else {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                // Ancora no canto inferior esquerdo, para o número encolher
                // mantendo a linha de base alinhada com a unidade ao lado.
                transformOrigin = TransformOrigin(0f, 1f)
            }
        }
    }
}

// ── Cartão ────────────────────────────────────────────────────────────────────

/**
 * A superfície do design: `background:var(--sf)`, sem sombra.
 *
 * No claro a hierarquia vem de `surface` (#FFF8F6) ser mais claro que o
 * `background` (#FBF3F1); no escuro, da borda de 1 px, porque lá as duas
 * superfícies são quase idênticas (#191113 sobre #141011).
 */
@Composable
fun DesignSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(DesignTokens.Radius.MetricCard),
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isDark()) {
                    Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape,
                    )
                } else {
                    Modifier
                }
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

// ── Rótulo micro ──────────────────────────────────────────────────────────────

/**
 * `font:600 11px;letter-spacing:.09em;text-transform:uppercase` — o rótulo em
 * caixa alta que abre cada bloco: "PESO · 17 SET, 08:12", "COMPOSIÇÃO".
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

// ── Peso-herói ────────────────────────────────────────────────────────────────

/**
 * O número de abertura da tela Hoje: Bricolage 800 em 84 px, com a unidade em
 * Instrument 500 de 20 px ao lado, alinhada pela linha de base.
 *
 * Usa [DesignTokens.Type.HeroWeight] em vez do `displayLarge` porque a tela
 * desenha 84 px e o token da escala documenta 64 — aqui vale o que a tela
 * desenha.
 */
@Composable
fun DesignHeroValue(
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.ExtraBold,
                fontSize = DesignTokens.Type.HeroWeight,
                lineHeight = DesignTokens.Type.HeroWeightLineHeight,
                letterSpacing = (-0.04).em,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            // 84 sp é o tamanho do protótipo e vale enquanto couber. Quando
            // não cabe — número de muitos dígitos, fonte do sistema ampliada —
            // encolher é melhor que cortar ou empurrar a unidade para fora.
            modifier = Modifier.weight(1f, fill = false).designShrinkToFit(),
        )
        if (!unit.isNullOrBlank()) {
            Text(
                text = unit,
                style = TextStyle(
                    fontFamily = InstrumentSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = DesignTokens.Type.HeroUnit,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

// ── Chip de variação ──────────────────────────────────────────────────────────

/** A direção da variação. Decide só a seta. */
enum class DesignDelta { DOWN, UP, FLAT }

/**
 * `background:var(--prc);color:var(--onprc);border-radius:999px` — o chip
 * `↓ 0,6 kg` ao lado do peso-herói.
 *
 * O protótipo pinta de rosa tanto a queda de peso quanto a alta de músculo: o
 * rosa marca **a variação**, não um julgamento sobre ela. Por isso não há
 * parâmetro de "bom/ruim" — só a seta muda.
 */
@Composable
fun DesignDeltaChip(
    text: String,
    delta: DesignDelta,
    modifier: Modifier = Modifier,
) {
    val arrow = when (delta) {
        DesignDelta.DOWN -> "↓"
        DesignDelta.UP -> "↑"
        DesignDelta.FLAT -> "→"
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

// ── Cabeçalho de seção ────────────────────────────────────────────────────────

/**
 * O par "rótulo micro à esquerda, ação rosa à direita" que abre as seções:
 * "ÚLTIMOS 30 DIAS — Ver histórico", "COMPOSIÇÃO — Escolher métricas".
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
                    .clip(RoundedCornerShape(percent = 50))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

// ── Cartão de métrica ─────────────────────────────────────────────────────────

/**
 * O cartão da grade de composição: rótulo micro, número em Bricolage 700 de
 * 31 px com unidade, e a variação em rosa. `min-height:112px`.
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
    DesignSurface(
        modifier = modifier.defaultMinSize(minHeight = DesignTokens.Size.MetricCard),
        shape = RoundedCornerShape(DesignTokens.Radius.MetricCard),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            DesignMicroLabel(text = label)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = DesignTokens.Type.MetricValue,
                        lineHeight = DesignTokens.Type.MetricValue,
                        letterSpacing = (-0.02).em,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    // O cartão tem metade da largura da tela. Valores de quatro
                    // dígitos (1110 kcal) não cabem em 31 sp, e sem isto o
                    // número empurra a unidade para fora do cartão.
                    modifier = Modifier.weight(1f, fill = false).designShrinkToFit(),
                )
                if (!unit.isNullOrBlank()) {
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 1.dp),
                    )
                }
            }
            Text(
                // O protótipo sempre mostra esta linha; sem variação, um traço
                // mantém a altura dos cartões da grade igual.
                text = delta ?: "—",
                style = MaterialTheme.typography.labelLarge,
                color = if (hasChange && delta != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── Chip de usuário ───────────────────────────────────────────────────────────

/**
 * O botão de trocar de pessoa, no topo da tela Hoje: avatar rosa com a
 * inicial, o nome e um triângulo. Fundo `surfaceVariant`, cantos totalmente
 * arredondados, `min-height:44px`.
 */
@Composable
fun DesignUserChip(
    name: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = DesignTokens.Size.TouchTarget)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(DesignTokens.Size.AvatarSmall)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "▼",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── Título de tela ────────────────────────────────────────────────────────────

/**
 * `font-family:'Bricolage Grotesque';font-weight:700;font-size:28px` — o
 * título que abre Histórico, Medições, Perfil e Configurações.
 */
@Composable
fun DesignScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = TextStyle(
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = DesignTokens.Type.ScreenTitle,
            lineHeight = 31.sp,
            letterSpacing = (-0.02).em,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

// ── Números em outros tamanhos ────────────────────────────────────────────────

/**
 * O número do cartão de gráfico, no Histórico: Bricolage 700 em 32 px, com a
 * unidade em 13 px ao lado.
 */
@Composable
fun DesignChartValue(
    value: String,
    unit: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                fontSize = DesignTokens.Type.ChartValue,
                lineHeight = DesignTokens.Type.ChartValue,
                letterSpacing = (-0.02).em,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false).designShrinkToFit(),
        )
        if (!unit.isNullOrBlank()) {
            Text(
                text = unit,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 1.dp),
            )
        }
    }
}

/** O número do cartão de estatística: Bricolage 700 em 20 px, sem unidade. */
@Composable
fun DesignStatValue(
    value: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = value,
        style = TextStyle(
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = DesignTokens.Type.StatValue,
            lineHeight = DesignTokens.Type.StatValue,
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier.designShrinkToFit(),
    )
}
