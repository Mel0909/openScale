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

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.health.openscale.R

/**
 * Tipografia do design.
 *
 * Duas famílias, ambas OFL e empacotadas no APK (não usam Downloadable Fonts,
 * que dependem do Play Services e falhariam no F-Droid):
 *
 *  - **Bricolage Grotesque** (800/700) — só números e títulos curtos. É ela que
 *    cria o contraste de peso do design.
 *  - **Instrument Sans** (400/500/600) — todo o resto.
 *
 * Os dois arquivos são fontes variáveis, então os pesos vêm de eixos em vez de
 * arquivos separados — uma única .ttf cobre toda a escala.
 *
 * Fonte da escala: `design/Tokens.dc.html`, seção "Escala tipográfica".
 */

val BricolageGrotesque = FontFamily(
    Font(R.font.bricolage_grotesque, weight = FontWeight.Normal),
    Font(R.font.bricolage_grotesque, weight = FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque, weight = FontWeight.Bold),
    Font(R.font.bricolage_grotesque, weight = FontWeight.ExtraBold),
)

val InstrumentSans = FontFamily(
    Font(R.font.instrument_sans, weight = FontWeight.Normal),
    Font(R.font.instrument_sans, weight = FontWeight.Medium),
    Font(R.font.instrument_sans, weight = FontWeight.SemiBold),
    Font(R.font.instrument_sans, weight = FontWeight.Bold),
)

/**
 * Escala do design mapeada nos slots do Material 3.
 *
 * Só os slots que o design define recebem tratamento próprio; os demais herdam
 * do padrão com a família trocada, para nada ficar em Roboto por descuido.
 */
val DesignTypography: Typography = Typography().let { default ->
    Typography(
        // Número herói — o peso na tela inicial.
        displayLarge = TextStyle(
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 64.sp,
            lineHeight = 59.sp,
            letterSpacing = (-0.035).em,
        ),
        displayMedium = default.displayMedium.copy(fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold),
        displaySmall = default.displaySmall.copy(fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold),

        // Número secundário — valores nos cartões de métrica.
        headlineMedium = TextStyle(
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).em,
        ),
        headlineLarge = default.headlineLarge.copy(fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold),

        // Título de tela — "Histórico", "Medições", "Perfil".
        headlineSmall = TextStyle(
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.015).em,
        ),

        // Título de seção.
        titleMedium = TextStyle(
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 23.sp,
        ),
        titleLarge = default.titleLarge.copy(fontFamily = InstrumentSans,
            fontWeight = FontWeight.SemiBold),
        titleSmall = default.titleSmall.copy(fontFamily = InstrumentSans,
            fontWeight = FontWeight.SemiBold),

        // Corpo.
        bodyLarge = TextStyle(
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        ),
        bodyMedium = default.bodyMedium.copy(fontFamily = InstrumentSans),
        bodySmall = default.bodySmall.copy(fontFamily = InstrumentSans),

        // Rótulo.
        labelLarge = TextStyle(
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 16.sp,
        ),
        labelMedium = default.labelMedium.copy(fontFamily = InstrumentSans,
            fontWeight = FontWeight.Medium),

        // Rótulo micro — caixa alta e muito espaçado. É a assinatura do design.
        // O textAllCaps fica com quem usa: aqui só o tracking e o tamanho.
        labelSmall = TextStyle(
            fontFamily = InstrumentSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.09.em,
        ),
    )
}
