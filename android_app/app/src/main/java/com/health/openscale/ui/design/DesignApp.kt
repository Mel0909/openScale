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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableRows
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.health.openscale.R
import com.health.openscale.ui.navigation.AppNavHost
import com.health.openscale.ui.navigation.Routes
import com.health.openscale.ui.screen.settings.BluetoothViewModel
import com.health.openscale.ui.screen.settings.SettingsViewModel
import com.health.openscale.ui.shared.SharedViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.merge
import kotlin.time.Duration.Companion.milliseconds

/**
 * A casca do app no design próprio.
 *
 * O protótipo (`design/App.dc.html`) desenha uma casca bem diferente da do
 * upstream, e é isso que faz o app "parecer" o design ou não:
 *
 * | | upstream (`ui/navigation/AppNavigation.kt`) | design |
 * |---|---|---|
 * | navegação | menu lateral (drawer) com 6 destinos | barra inferior, 4 abas |
 * | título | `TopAppBar` comum a todas as telas | cada tela traz o seu |
 * | ação principal | ícones na barra superior | botão "Pesar" flutuante |
 *
 * As quatro abas são as do protótipo: **Hoje · Histórico · Medições ·
 * Perfil**. Estatísticas e Insights não estão na barra porque o protótipo não
 * as desenha — continuam alcançáveis por rota, e ganham tratamento depois.
 *
 * O `AppNavigation.kt` do upstream fica intocado, para o merge continuar
 * limpo; quem escolhe qual casca usar é a `MainActivity`.
 */
@OptIn(FlowPreview::class)
@Composable
fun DesignApp(sharedViewModel: SharedViewModel) {
    val resources = LocalResources.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val bluetoothViewModel: BluetoothViewModel = hiltViewModel()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    // Mesma fusão de eventos do upstream: as três fontes de snackbar, sem
    // repetir a mesma mensagem em sequência.
    LaunchedEffect(Unit) {
        merge(
            sharedViewModel.snackbarEvents,
            settingsViewModel.snackbarEvents,
            bluetoothViewModel.snackbarEvents,
        )
            .distinctUntilChanged { a, b ->
                a.messageResId == b.messageResId &&
                    a.message == b.message &&
                    a.messageFormatArgs == b.messageFormatArgs
            }
            .debounce(150.milliseconds)
            .collect { evt ->
                val msg = evt.message ?: resources.getString(
                    requireNotNull(evt.messageResId),
                    *evt.messageFormatArgs.toTypedArray(),
                )
                val action = evt.actionLabel ?: evt.actionLabelResId?.let { resources.getString(it) }
                val res = snackbarHostState.run {
                    currentSnackbarData?.dismiss()
                    showSnackbar(message = msg, actionLabel = action, duration = evt.duration)
                }
                if (res == SnackbarResult.ActionPerformed) evt.onAction?.invoke()
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            DesignBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route -> navController.navigateToTab(route) },
            )
        },
        floatingActionButton = {
            // Só nas abas: numa tela de detalhe ou de ajustes o botão
            // "Pesar" não tem o que fazer.
            if (currentRoute in DesignTab.routes) {
                DesignWeighButton(
                    onClick = {
                        // A tela de nova medição é a do upstream por enquanto;
                        // o protótipo desenha uma folha própria, que ainda não
                        // foi implementada.
                        val userId = sharedViewModel.selectedUserId.value
                        if (userId != null) {
                            navController.navigate(
                                Routes.measurementDetail(measurementId = null, userId = userId)
                            )
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            innerPadding = innerPadding,
            sharedViewModel = sharedViewModel,
            settingsViewModel = settingsViewModel,
            bluetoothViewModel = bluetoothViewModel,
        )
    }
}

/**
 * Navega para uma aba com o comportamento que se espera de barra inferior:
 * volta à raiz do grafo sem empilhar, e não recria a tela se já se está nela.
 */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// ── Abas ──────────────────────────────────────────────────────────────────────

/**
 * As quatro abas do protótipo, na ordem em que ele as desenha.
 *
 * Os rótulos e ícones vêm do `rd_bottom_nav.xml` do redesign anterior, que já
 * tinha traduzido o protótipo para Android.
 */
private enum class DesignTab(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int,
) {
    HOJE(Routes.OVERVIEW, Icons.Filled.Home, R.string.design_nav_today),
    HISTORICO(Routes.GRAPH, Icons.AutoMirrored.Filled.ShowChart, R.string.design_nav_history),
    MEDICOES(Routes.TABLE, Icons.Filled.TableRows, R.string.design_nav_records),
    PERFIL(Routes.USER_SETTINGS, Icons.Filled.Person, R.string.design_nav_profile);

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()
    }
}

/**
 * A barra inferior do design: fundo `surface`, separador de 1 px no topo, sem
 * elevação — o design não usa sombra.
 *
 * A aba ativa ganha a pílula `primaryContainer` atrás do ícone, como no
 * protótipo, em vez do indicador padrão do Material.
 */
@Composable
private fun DesignBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                // Respeita a barra de gestos do sistema.
                .padding(WindowInsets.navigationBars.asPaddingValues()),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            DesignTab.entries.forEach { tab ->
                DesignTabItem(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    onClick = { onNavigate(tab.route) },
                )
            }
        }
    }
}

/** Uma aba: pílula rosa clara atrás do ícone quando ativa, rótulo embaixo. */
@Composable
private fun DesignTabItem(
    tab: DesignTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(DesignTokens.Radius.Compact))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    }
                )
                .padding(horizontal = 18.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(21.dp),
            )
        }
        Text(
            text = stringResource(tab.labelRes),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
        )
    }
}

// ── Botão Pesar ───────────────────────────────────────────────────────────────

/**
 * O botão "Pesar" do protótipo.
 *
 * Não é um FAB do Material: o design especifica um retângulo de cantos 20 dp
 * com ícone e rótulo, `height:60px`, não um círculo.
 */
@Composable
private fun DesignWeighButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(60.dp)
            .clip(RoundedCornerShape(DesignTokens.Radius.Row))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.MonitorWeight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.design_weigh_action),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
