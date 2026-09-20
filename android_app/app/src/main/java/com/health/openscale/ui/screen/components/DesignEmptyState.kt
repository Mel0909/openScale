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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * O estado vazio no tratamento do design: cartão sem sombra, título em
 * Bricolage, texto de apoio em Instrument e um botão rosa de ação.
 *
 * O protótipo não desenha estado vazio em nenhuma das cinco telas — ele assume
 * uma conta com histórico. Este componente aplica o vocabulário já
 * estabelecido (cartão, tipografia, botão) em vez de inventar um tratamento
 * novo: é a regra de estender, não criar.
 *
 * A única razão de existir separado dos cartões do upstream é a sombra: lá eles
 * usam `Card` com `elevation = 4.dp`, que contraria a regra de não usar sombra
 * em nenhum tema.
 *
 * @param actionLabel quando nulo, o cartão fica só informativo, sem botão.
 */
@Composable
fun DesignEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    actionEnabled: Boolean = true,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        DesignCard(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(DesignRadius.Large),
            contentPadding = 24.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (actionLabel != null && onAction != null) {
                    Button(
                        onClick = onAction,
                        enabled = actionEnabled,
                        shape = RoundedCornerShape(percent = 50),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            // O protótipo usa min-height 48px nos botões.
                            .defaultMinSize(minHeight = 48.dp),
                    ) {
                        if (actionIcon != null) {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize),
                            )
                            androidx.compose.foundation.layout.Spacer(
                                Modifier.size(ButtonDefaults.IconSpacing)
                            )
                        }
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}
