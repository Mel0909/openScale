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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.luminance

/**
 * O tratamento de superfície do design, num só lugar.
 *
 * O design não usa sombra em nenhum tema. A hierarquia vem de:
 *
 *  - **claro** — `surface` (#FFF8F6) mais claro que o `background` (#FBF3F1);
 *  - **escuro** — uma borda de 1 px em `outlineVariant`, já que ali as duas
 *    superfícies são quase idênticas (#191113 sobre #141011).
 *
 * Decisão explícita do `Tokens.dc.html`: evita blur e elevação custosos no
 * Android.
 *
 * Fica aqui, e não em [Theme.kt], porque `Theme.kt` é do upstream — manter o
 * que é nosso em arquivo próprio é o que mantém o merge barato.
 */

/**
 * Se a paleta em vigor é escura, deduzido da luminância do `surface`.
 *
 * Não usa `isSystemInDarkTheme()` de propósito: o app tem variantes (alto
 * contraste, pure black) em que o esquema efetivo pode divergir do que o
 * sistema pede. O que decide o tratamento de borda é a superfície que está
 * realmente pintada, não a preferência do sistema.
 */
@Composable
@ReadOnlyComposable
fun isDesignDarkPalette(): Boolean =
    MaterialTheme.colorScheme.surface.luminance() < 0.5f
