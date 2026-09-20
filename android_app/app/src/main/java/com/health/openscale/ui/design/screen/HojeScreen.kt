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
package com.health.openscale.ui.design.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.health.openscale.R
import com.health.openscale.core.data.InputFieldType
import com.health.openscale.core.data.MeasurementType
import com.health.openscale.core.data.Trend
import com.health.openscale.core.facade.SettingsPreferenceKeys
import com.health.openscale.core.model.AggregatedMeasurement
import com.health.openscale.core.utils.LocaleUtils
import com.health.openscale.ui.design.DesignTokens
import com.health.openscale.ui.design.components.DesignDelta
import com.health.openscale.ui.design.components.DesignDeltaChip
import com.health.openscale.ui.design.components.DesignHeroValue
import com.health.openscale.ui.design.components.DesignMetricCard
import com.health.openscale.ui.design.components.DesignMicroLabel
import com.health.openscale.ui.design.components.DesignSectionHeader
import com.health.openscale.ui.design.components.DesignSparklineCard
import com.health.openscale.ui.design.components.DesignUserChip
import com.health.openscale.ui.navigation.Routes
import com.health.openscale.ui.shared.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * A tela **Hoje**, exatamente como `design/App.dc.html` a desenha.
 *
 * O protótipo desenha, de cima para baixo:
 *
 * 1. cabeçalho — chip de usuário à esquerda, botão de ajustes à direita;
 * 2. peso-herói — rótulo micro com data, número de 84 px, chip de variação
 *    com "desde <data>" ao lado;
 * 3. cartão "Últimos 30 dias" — sparkline clicável que leva ao Histórico;
 * 4. grade de composição — duas colunas de cartões de métrica.
 *
 * **É só isso.** Nada de agregação, drill-down, metas, linhas expansíveis ou
 * avaliação por faixa: essas funcionalidades existem na
 * [com.health.openscale.ui.screen.overview.OverviewScreen] do upstream, que
 * segue no repositório, intocada. A decisão da dona do projeto foi entregar
 * primeiro o design exato e só depois decidir o que trazer de volta.
 *
 * Nenhum componente do upstream é reaproveitado aqui — tudo vem de
 * `ui/design/components/`.
 */
@Composable
fun HojeScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
) {
    val selectedUserId by sharedViewModel.selectedUserId.collectAsState()
    val currentUser by sharedViewModel.selectedUser.collectAsState()

    val state by sharedViewModel
        .screenFlow(SettingsPreferenceKeys.OVERVIEW_SCREEN_CONTEXT)
        .collectAsStateWithLifecycle(initialValue = SharedViewModel.UiState.Loading)

    val items: List<AggregatedMeasurement> = remember(state) {
        (state as? SharedViewModel.UiState.Success)?.data ?: emptyList()
    }

    // O protótipo desenha o próprio cabeçalho, dentro da área de rolagem. A
    // barra do app fica sem título nem ações para não duplicá-lo.
    DisposableEffect(selectedUserId) {
        sharedViewModel.setTopBarTitle("")
        sharedViewModel.setTopBarActions(emptyList())
        onDispose { }
    }

    when {
        state is SharedViewModel.UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = DesignTokens.Spacing.ScreenHorizontal,
                    end = DesignTokens.Spacing.ScreenHorizontal,
                    top = 4.dp,
                    // O protótipo reserva 130px no rodapé para o botão "Pesar"
                    // e a barra de navegação não cobrirem o conteúdo.
                    bottom = 130.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Section),
            ) {
                item(key = "header") {
                    HojeHeader(
                        userName = currentUser?.name.orEmpty(),
                        onUserClick = { navController.navigate(Routes.USER_SETTINGS) },
                        onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    )
                }

                item(key = "hero") {
                    HojeHero(items = items)
                }

                item(key = "spark") {
                    val window = remember(items) { last30CalendarDays(items) }
                    DesignSparklineCard(
                        label = stringResource(R.string.design_last_30_days),
                        actionLabel = stringResource(R.string.design_see_history),
                        values = window.values,
                        axisLabels = window.axisLabels,
                        emptyMessage = stringResource(R.string.design_not_enough_data),
                        onClick = { navController.navigate(Routes.GRAPH) },
                    )
                }

                item(key = "composition") {
                    HojeComposition(
                        items = items,
                        onMetricClick = { navController.navigate(Routes.GRAPH) },
                        onChooseMetrics = { navController.navigate(Routes.MEASUREMENT_TYPES) },
                    )
                }
            }
        }
    }
}

// ── Cabeçalho ─────────────────────────────────────────────────────────────────

/** Chip de usuário à esquerda, botão de ajustes à direita. */
@Composable
private fun HojeHeader(
    userName: String,
    onUserClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (userName.isNotBlank()) {
            DesignUserChip(name = userName, onClick = onUserClick)
        } else {
            Spacer(Modifier.width(1.dp))
        }
        Icon(
            imageVector = Icons.Filled.Tune,
            contentDescription = stringResource(R.string.route_title_settings),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(DesignTokens.Size.TouchTarget)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = onSettingsClick)
                .padding(11.dp),
        )
    }
}

// ── Peso-herói ────────────────────────────────────────────────────────────────

/** Rótulo micro com data, o número de 84 px e o chip de variação. */
@Composable
private fun HojeHero(items: List<AggregatedMeasurement>) {
    val context = LocalContext.current
    val latest = items.firstOrNull()
    val weight = latest?.enriched?.valuesWithTrend
        ?.find { it.currentValue.type.key == MeasurementType.WEIGHT }

    if (latest == null || weight == null) {
        Column {
            DesignMicroLabel(text = stringResource(R.string.design_no_measurement_yet))
            Spacer(Modifier.height(8.dp))
            DesignHeroValue(value = "- -,-", unit = null)
        }
        return
    }

    val type = weight.currentValue.type
    val raw = weight.currentValue.value.floatValue
    val ts = latest.enriched.measurementWithValues.measurement.timestamp

    // "Peso · 17 set, 08:12" — o formato do protótipo.
    val dateLabel = remember(ts) {
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(ts))
    }

    Column {
        DesignMicroLabel(
            text = "${type.getDisplayName(context)} · $dateLabel",
        )
        Spacer(Modifier.height(8.dp))
        DesignHeroValue(
            value = raw?.let { LocaleUtils.formatValueForDisplay(it.toString(), type.unit) } ?: "—",
            unit = type.unit.displayName,
        )

        val diff = weight.difference
        if (diff != null && weight.trend != Trend.NOT_APPLICABLE) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                DesignDeltaChip(
                    text = LocaleUtils.formatValueForDisplay(abs(diff).toString(), type.unit),
                    delta = when (weight.trend) {
                        Trend.DOWN -> DesignDelta.DOWN
                        Trend.UP -> DesignDelta.UP
                        else -> DesignDelta.FLAT
                    },
                )
                // "desde 12 set": a data da medição anterior, que é o segundo
                // item da lista — o ValueWithDifference traz o delta, não a data.
                val prevTs = items.getOrNull(1)
                    ?.enriched?.measurementWithValues?.measurement?.timestamp
                if (prevTs != null) {
                    Spacer(Modifier.width(9.dp))
                    val since = remember(prevTs) {
                        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(prevTs))
                    }
                    Text(
                        text = stringResource(R.string.design_since_date, since),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Grade de composição ───────────────────────────────────────────────────────

/** Duas colunas de cartões de métrica, sem o peso (que já é o herói). */
@Composable
private fun HojeComposition(
    items: List<AggregatedMeasurement>,
    onMetricClick: () -> Unit,
    onChooseMetrics: () -> Unit,
) {
    val context = LocalContext.current
    val latest = items.firstOrNull() ?: return

    val metrics = remember(latest) {
        latest.enriched.valuesWithTrend.filter { v ->
            val t = v.currentValue.type
            t.isEnabled &&
                t.key != MeasurementType.WEIGHT &&
                (t.inputType == InputFieldType.FLOAT || t.inputType == InputFieldType.INT)
        }
    }
    if (metrics.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Grid)) {
        DesignSectionHeader(
            label = stringResource(R.string.design_composition),
            action = stringResource(R.string.design_choose_metrics),
            onActionClick = onChooseMetrics,
        )

        metrics.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Grid)) {
                pair.forEach { v ->
                    val t = v.currentValue.type
                    val numeric = when (t.inputType) {
                        InputFieldType.INT -> v.currentValue.value.intValue?.toFloat()
                        else -> v.currentValue.value.floatValue
                    }
                    DesignMetricCard(
                        modifier = Modifier.weight(1f),
                        label = t.getDisplayName(context),
                        value = numeric
                            ?.let { LocaleUtils.formatValueForDisplay(it.toString(), t.unit) }
                            ?: "—",
                        unit = t.unit.displayName,
                        delta = v.difference
                            ?.takeIf { v.trend != Trend.NOT_APPLICABLE }
                            ?.let {
                                LocaleUtils.formatValueForDisplay(
                                    value = it.toString(),
                                    unit = t.unit,
                                    includeSign = true,
                                )
                            },
                        hasChange = v.trend == Trend.UP || v.trend == Trend.DOWN,
                        onClick = onMetricClick,
                    )
                }
                // Número ímpar: o vão da última linha precisa de peso
                // equivalente, senão o cartão solitário estica até a borda.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ── Janela do sparkline ───────────────────────────────────────────────────────

/** Os dados do sparkline: os valores e os três rótulos de data do rodapé. */
private data class SparkWindow(
    val values: List<Float>,
    val axisLabels: List<String>,
)

/**
 * Os pesos dos últimos **30 dias de calendário**, em ordem cronológica.
 *
 * Trinta dias corridos, não as últimas trinta medições — decisão da dona do
 * projeto. Quem pesa uma vez por semana vê ~4 pontos; quem pesa todo dia vê
 * ~30. Assim o rótulo "Últimos 30 dias" é literalmente verdadeiro e os
 * rótulos de data do rodapé fazem sentido.
 */
private fun last30CalendarDays(items: List<AggregatedMeasurement>): SparkWindow {
    val cutoff = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -30)
    }.timeInMillis

    // A lista chega da mais recente para a mais antiga; o gráfico desenha ao
    // contrário.
    val window = items
        .filter { it.enriched.measurementWithValues.measurement.timestamp >= cutoff }
        .reversed()

    val values = window.mapNotNull { item ->
        item.enriched.valuesWithTrend
            .find { it.currentValue.type.key == MeasurementType.WEIGHT }
            ?.currentValue?.value?.floatValue
    }

    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
    val timestamps = window.map { it.enriched.measurementWithValues.measurement.timestamp }
    val labels = when {
        timestamps.size < 2 -> emptyList()
        else -> listOf(
            fmt.format(Date(timestamps.first())),
            fmt.format(Date(timestamps[timestamps.size / 2])),
            fmt.format(Date(timestamps.last())),
        )
    }

    return SparkWindow(values = values, axisLabels = labels)
}
