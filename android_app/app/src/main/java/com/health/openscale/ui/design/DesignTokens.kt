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

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * As medidas do design, lidas direto de `design/App.dc.html`.
 *
 * Cada valor aqui corresponde a um número que está escrito no protótipo. O
 * protótipo é px em CSS; aqui vira dp, que é a tradução direta (o protótipo foi
 * desenhado numa moldura de 412 px de largura, que é a largura de um telefone
 * Android típico em dp).
 *
 * **Estes valores não se inventam.** Se um número não está no protótipo, ele
 * não entra aqui — a regra declarada no design cobre o resto: *"o rosa nunca
 * preenche a tela — ele aparece no número, na linha do gráfico, no botão e na
 * variação"*.
 */
object DesignTokens {

    /** Raios de canto, por papel. Do `border-radius` de cada elemento. */
    object Radius {
        /** Cartão de sparkline e de gráfico: `border-radius:26px`. */
        val Card: Dp = 26.dp

        /** Cartão de métrica e campo de perfil: `border-radius:22px`. */
        val MetricCard: Dp = 22.dp

        /** Linha de registro: `border-radius:20px`. */
        val Row: Dp = 20.dp

        /** Bloco compacto: `border-radius:18px`. */
        val Compact: Dp = 18.dp

        /** Folha inferior: `border-radius:30px 30px 0 0`. */
        val Sheet: Dp = 30.dp
    }

    /** Espaçamentos verticais entre blocos, do `gap` de cada coluna. */
    object Spacing {
        /** Entre os blocos da tela: `gap:20px`. */
        val Section: Dp = 20.dp

        /** Entre cartões da grade e itens de lista: `gap:12px`. */
        val Grid: Dp = 12.dp

        /** Dentro de um cartão, entre rótulo e valor: `gap:7px`/`gap:8px`. */
        val Inner: Dp = 8.dp

        /** Margem lateral da tela: `padding:4px 20px`. */
        val ScreenHorizontal: Dp = 20.dp

        /** Preenchimento interno do cartao grande: `padding:18px`/20dp. */
        val CardLarge: Dp = 20.dp
    }

    /** Alturas mínimas. O protótipo usa `min-height` para alvo de toque. */
    object Size {
        /** Cartão de métrica: `min-height:112px`. */
        val MetricCard: Dp = 112.dp

        /** Botão e chip de usuário: `min-height:44px`. */
        val TouchTarget: Dp = 44.dp

        /** Botão principal e de folha: `min-height:48px`/`52px`. */
        val Button: Dp = 48.dp

        /** Linha de registro: `min-height:66px`. */
        val RecordRow: Dp = 66.dp

        /** Altura do sparkline: `height:104`. */
        val Sparkline: Dp = 104.dp

        /** Avatar do chip de usuário: `width:32px;height:32px`. */
        val AvatarSmall: Dp = 32.dp

        /** Avatar da tela de perfil: `width:60px;height:60px`. */
        val AvatarLarge: Dp = 60.dp
    }

    /**
     * Tamanhos de texto que o protótipo usa mas a escala do
     * [com.health.openscale.ui.theme.DesignTypography] não cobre.
     *
     * O peso-herói da tela Hoje é 84 px, maior que o `displayLarge` de 64 sp
     * definido no `Tokens.dc.html` — o token documenta 64, mas a tela desenha
     * 84. Aqui vale o que a tela desenha.
     */
    object Type {
        /** Peso-herói da tela Hoje: `font-size:84px`. */
        val HeroWeight = 84.sp

        /** Entrelinha do herói: `line-height:.88`. */
        val HeroWeightLineHeight = 74.sp

        /** Unidade ao lado do herói: `font:500 20px`. */
        val HeroUnit = 20.sp

        /** Número do cartão de métrica: `font-size:31px`. */
        val MetricValue = 31.sp

        /** Título de tela: `font-size:28px`. */
        val ScreenTitle = 28.sp

        /** Número do cartão de gráfico, no Histórico: `font-size:32px`. */
        val ChartValue = 32.sp

        /** Número do cartão de estatística: `font-size:20px`. */
        val StatValue = 20.sp
    }
}
