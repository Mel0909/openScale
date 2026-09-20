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
package com.health.openscale.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta do design próprio: rosa profundo como cor de dado e de ação, sobre
 * fundos quentes levemente rosados.
 *
 * Fonte: `design/Tokens.dc.html`. Cada par tem contraste WCAG AA verificado na
 * origem, e a razão medida está anotada ao lado.
 *
 * Fica separada de [Color.kt], que guarda a paleta azul gerada pelo upstream a
 * partir do seed #0099CC — assim dá para acompanhar o upstream sem conflito, e
 * a troca entre as duas é um `when` em [OpenScaleTheme].
 *
 * Regra do design: o rosa nunca preenche a tela. Ele aparece no número, na
 * linha do gráfico, no botão e na variação.
 */

// ── Tema claro ────────────────────────────────────────────────────────────────

val designPrimaryLight = Color(0xFFA81F52)            // 6,71 sobre surface
val designOnPrimaryLight = Color(0xFFFFFFFF)          // 7,04 sobre primary
val designPrimaryContainerLight = Color(0xFFFFD9E3)
val designOnPrimaryContainerLight = Color(0xFF3F0019) // 13,29 sobre container

val designSecondaryLight = Color(0xFF7A5A62)          // 5,77 sobre surface
val designOnSecondaryLight = Color(0xFFFFFFFF)        // 6,06 sobre secondary
val designSecondaryContainerLight = Color(0xFFFBE1E7)
val designOnSecondaryContainerLight = Color(0xFF2F1620) // 13,55 sobre container

val designTertiaryLight = Color(0xFF8A5A2B)           // 5,59 sobre surface
val designOnTertiaryLight = Color(0xFFFFFFFF)         // 5,87 sobre tertiary
val designTertiaryContainerLight = Color(0xFFFFDDB8)
val designOnTertiaryContainerLight = Color(0xFF2E1700) // 13,15 sobre container

val designBackgroundLight = Color(0xFFFBF3F1)
val designOnBackgroundLight = Color(0xFF2A2224)       // 14,19 sobre background
val designSurfaceLight = Color(0xFFFFF8F6)
val designOnSurfaceLight = Color(0xFF2A2224)          // 14,79 sobre surface
val designSurfaceVariantLight = Color(0xFFF4E4E6)
val designOnSurfaceVariantLight = Color(0xFF5D4B50)   // 7,73 sobre surface

/** Só borda e eixo: 4,25 fica abaixo de AA para texto. */
val designOutlineLight = Color(0xFF8A7176)
val designOutlineVariantLight = Color(0xFFE6D2D6)

val designErrorLight = Color(0xFFB3261E)              // 6,23 sobre surface
val designOnErrorLight = Color(0xFFFFFFFF)            // 6,54 sobre error
val designErrorContainerLight = Color(0xFFF9DEDC)
val designOnErrorContainerLight = Color(0xFF410E0B)   // 12,77 sobre container

val designScrimLight = Color(0xFF000000)
val designInverseSurfaceLight = Color(0xFF3B3032)
val designInverseOnSurfaceLight = Color(0xFFFCEDEF)
val designInversePrimaryLight = Color(0xFFFFB1C6)

// Escala de superfícies. O design trabalha com dois níveis (background e
// surface); os intermediários são interpolados para os componentes do Material
// que os exigem (menus, sheets, barra de navegação).
val designSurfaceDimLight = Color(0xFFE6D8D7)
val designSurfaceBrightLight = Color(0xFFFFF8F6)
val designSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val designSurfaceContainerLowLight = Color(0xFFFFF1F0)
val designSurfaceContainerLight = Color(0xFFFAEBEB)
val designSurfaceContainerHighLight = Color(0xFFF4E4E6)
val designSurfaceContainerHighestLight = Color(0xFFEEDEE0)

// ── Tema escuro ───────────────────────────────────────────────────────────────

val designPrimaryDark = Color(0xFFFFB1C6)             // 10,90 sobre surface
val designOnPrimaryDark = Color(0xFF5E0A2B)           // 7,90 sobre primary
val designPrimaryContainerDark = Color(0xFF7E1F45)
val designOnPrimaryContainerDark = Color(0xFFFFD9E3)  // 7,51 sobre container

val designSecondaryDark = Color(0xFFE3BDC7)           // 10,94 sobre surface
val designOnSecondaryDark = Color(0xFF432A33)         // 7,65 sobre secondary
val designSecondaryContainerDark = Color(0xFF5B3F48)
val designOnSecondaryContainerDark = Color(0xFFFFD9E3) // 7,24 sobre container

val designTertiaryDark = Color(0xFFEFBD8B)            // 10,90 sobre surface
val designOnTertiaryDark = Color(0xFF4A2800)          // 7,73 sobre tertiary
val designTertiaryContainerDark = Color(0xFF693C0D)
val designOnTertiaryContainerDark = Color(0xFFFFDDB8) // 7,40 sobre container

val designBackgroundDark = Color(0xFF141011)
val designOnBackgroundDark = Color(0xFFF0E0E3)        // 14,82 sobre background
val designSurfaceDark = Color(0xFF191113)
val designOnSurfaceDark = Color(0xFFF0E0E3)           // 14,60 sobre surface
val designSurfaceVariantDark = Color(0xFF3A2C30)
val designOnSurfaceVariantDark = Color(0xFFD6BFC5)    // 7,60 sobre surfaceVariant

/** No escuro é esta borda de 1px que dá hierarquia aos cartões: o design não usa sombra. */
val designOutlineDark = Color(0xFF9E848A)             // 5,41 sobre surface
val designOutlineVariantDark = Color(0xFF4A393E)

val designErrorDark = Color(0xFFF2B8B5)               // 10,88 sobre surface
val designOnErrorDark = Color(0xFF601410)             // 7,66 sobre error
val designErrorContainerDark = Color(0xFF8C1D18)
val designOnErrorContainerDark = Color(0xFFF9DEDC)    // 7,17 sobre container

val designScrimDark = Color(0xFF000000)
val designInverseSurfaceDark = Color(0xFFF0E0E3)
val designInverseOnSurfaceDark = Color(0xFF3B3032)
val designInversePrimaryDark = Color(0xFFA81F52)

val designSurfaceDimDark = Color(0xFF141011)
val designSurfaceBrightDark = Color(0xFF3B3032)
val designSurfaceContainerLowestDark = Color(0xFF0F0B0C)
val designSurfaceContainerLowDark = Color(0xFF191113)
val designSurfaceContainerDark = Color(0xFF1D1517)
val designSurfaceContainerHighDark = Color(0xFF281F21)
val designSurfaceContainerHighestDark = Color(0xFF332A2C)

// ── Gráficos ──────────────────────────────────────────────────────────────────

/**
 * Cores de gráfico do design. Uma série por vez é o padrão; a âmbar
 * ([designChartSecondSeries]) existe só para comparar duas métricas no mesmo
 * eixo.
 */
object DesignChartColors {
    val lineLight = Color(0xFFA81F52)
    val fillLight = Color(0xFFF7C9D8)
    val highlightLight = Color(0xFFC2185B)
    val secondSeriesLight = Color(0xFF8A5A2B)
    val gridLight = Color(0xFFE6D2D6)
    val axisLineLight = Color(0xFFD9C2C7)
    val axisTextLight = Color(0xFF7A6469)
    val limitLineLight = Color(0xFFF4E4E6)

    val lineDark = Color(0xFFFFB1C6)
    val fillDark = Color(0xFF5E2338)
    val highlightDark = Color(0xFFFFD9E3)
    val secondSeriesDark = Color(0xFFEFBD8B)
    val gridDark = Color(0xFF31252A)
    val axisLineDark = Color(0xFF4A393E)
    val axisTextDark = Color(0xFFB9A2A8)
    val limitLineDark = Color(0xFF2C2023)
}
