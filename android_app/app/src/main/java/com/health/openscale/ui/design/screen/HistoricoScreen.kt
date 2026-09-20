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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.health.openscale.R
import com.health.openscale.core.data.InputFieldType
import com.health.openscale.core.data.MeasurementType
import com.health.openscale.core.facade.SettingsPreferenceKeys
import com.health.openscale.core.model.AggregatedMeasurement
import com.health.openscale.ui.design.DESIGN_METRIC_ORDER
import com.health.openscale.ui.design.DesignFormat
import com.health.openscale.ui.design.DesignTokens
import com.health.openscale.ui.design.components.DesignChartValue
import com.health.openscale.ui.design.components.DesignChipRow
import com.health.openscale.ui.design.components.DesignLineChart
import com.health.openscale.ui.design.components.DesignMicroLabel
import com.health.openscale.ui.design.components.DesignScreenTitle
import com.health.openscale.ui.design.components.DesignSegmentedSelector
import com.health.openscale.ui.design.components.DesignStatCard
import com.health.openscale.ui.design.components.DesignSurface
import com.health.openscale.ui.shared.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A tela **Histórico**, exatamente como `design/App.dc.html` a desenha.
 *
 * De cima para baixo:
 *
 * 1. título "Histórico" em Bricolage 28 px;
 * 2. chips de métrica, em rolagem horizontal — o selecionado em rosa;
 * 3. seletor de período segmentado: Semana · Mês · Ano;
 * 4. cartão do gráfico — título, valor atual, variação e a curva com eixos;
 * 5. três cartões de estatística: Mínimo · Média · Máximo;
 * 6. a nota explicando o que a agregação do período faz.
 *
 * O gráfico é um [DesignLineChart], desenhado num `Canvas`, e não a Vico do
 * upstream: ela traz eixos, marcadores e gestos com aparência própria que
 * teriam de ser desfeitos um a um.
 *
 * A `GraphScreen` do upstream segue no repositório, intocada.
 */
@Composable
fun HistoricoScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
) {
    val context = LocalContext.current
    val selectedUserId by sharedViewModel.selectedUserId.collectAsState()

    val state by sharedViewModel
        .screenFlow(SettingsPreferenceKeys.GRAPH_SCREEN_CONTEXT, useSmoothing = true)
        .collectAsStateWithLifecycle(initialValue = SharedViewModel.UiState.Loading)

    val items: List<AggregatedMeasurement> = remember(state) {
        (state as? SharedViewModel.UiState.Success)?.data ?: emptyList()
    }

    // O protótipo desenha o próprio título; a barra do app fica vazia.
    DisposableEffect(selectedUserId) {
        sharedViewModel.setTopBarTitle("")
        sharedViewModel.setTopBarActions(emptyList())
        onDispose { }
    }

    // Métrica e período são estado da tela, não do app: o protótipo os desenha
    // como seletores locais, e sair da tela e voltar recomeça do padrão.
    var selectedTypeId by rememberSaveable { mutableStateOf<Int?>(null) }
    var period by rememberSaveable { mutableStateOf(DesignPeriod.MES) }

    // As métricas que aparecem como chip: as habilitadas e numéricas da
    // medição mais recente, na ordem do design.
    val availableTypes: List<MeasurementType> = remember(items) {
        items.firstOrNull()?.enriched?.valuesWithTrend
            ?.map { it.currentValue.type }
            ?.filter { t ->
                t.isEnabled &&
                    (t.inputType == InputFieldType.FLOAT || t.inputType == InputFieldType.INT)
            }
            ?.sortedBy { t ->
                val idx = DESIGN_METRIC_ORDER.indexOf(t.key)
                if (idx >= 0) idx else DESIGN_METRIC_ORDER.size
            }
            ?: emptyList()
    }

    val selectedType = remember(availableTypes, selectedTypeId) {
        availableTypes.find { it.id == selectedTypeId } ?: availableTypes.firstOrNull()
    }

    // Fora do LazyColumn: o corpo de `item {}` não é escopo de composição, e
    // um `remember` ali dentro não sobreviveria à rolagem.
    val series = remember(items, selectedType, period) {
        selectedType?.let { buildSeries(items, it, period) }
            ?: DesignSeries(emptyList(), emptyList())
    }

    when {
        state is SharedViewModel.UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = DesignTokens.Spacing.ScreenHorizontal,
                end = DesignTokens.Spacing.ScreenHorizontal,
                top = 12.dp,
                bottom = 130.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "title") {
                DesignScreenTitle(text = stringResource(R.string.design_nav_history))
            }

            if (selectedType == null) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.design_not_enough_data),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                return@LazyColumn
            }

            item(key = "chips") {
                DesignChipRow(
                    items = availableTypes,
                    selected = selectedType,
                    label = { it.getDisplayName(context) },
                    onSelect = { selectedTypeId = it.id },
                )
            }

            item(key = "period") {
                val periodLabels = DesignPeriod.entries.associateWith {
                    stringResource(it.labelRes)
                }
                DesignSegmentedSelector(
                    items = DesignPeriod.entries,
                    selected = period,
                    label = { periodLabels[it].orEmpty() },
                    onSelect = { period = it },
                )
            }

            item(key = "chart") {
                HistoricoChartCard(
                    type = selectedType,
                    period = period,
                    series = series,
                )
            }

            if (series.values.size >= 2) {
                item(key = "stats") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesignStatCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.design_stat_min),
                            value = DesignFormat.number(series.values.min(), selectedType.unit),
                        )
                        DesignStatCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.design_stat_avg),
                            value = DesignFormat.number(
                                series.values.average().toFloat(),
                                selectedType.unit,
                            ),
                        )
                        DesignStatCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.design_stat_max),
                            value = DesignFormat.number(series.values.max(), selectedType.unit),
                        )
                    }
                }

                item(key = "note") {
                    Text(
                        text = stringResource(period.noteRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── Cartão do gráfico ─────────────────────────────────────────────────────────

/** Título, valor atual, variação no período e a curva. */
@Composable
private fun HistoricoChartCard(
    type: MeasurementType,
    period: DesignPeriod,
    series: DesignSeries,
) {
    val context = LocalContext.current

    DesignSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.Radius.Card),
        contentPadding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                DesignMicroLabel(
                    text = "${type.getDisplayName(context)} · ${stringResource(period.byLabelRes)}",
                )
                Spacer(Modifier.height(5.dp))
                DesignChartValue(
                    value = series.values.lastOrNull()
                        ?.let { DesignFormat.number(it, type.unit) }
                        ?: "—",
                    unit = DesignFormat.unitLabel(type.unit),
                )
            }

            // A variação entre o primeiro e o último ponto da janela — é o que
            // o protótipo mostra à direita ("−1,8 kg em 30 dias").
            if (series.values.size >= 2) {
                val delta = series.values.last() - series.values.first()
                Text(
                    text = buildString {
                        append(DesignFormat.number(delta, type.unit, includeSign = true))
                        DesignFormat.unitLabel(type.unit)?.let { append(" ").append(it) }
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (series.values.size < 2) {
            Box(
                modifier = Modifier.fillMaxWidth().height(156.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.design_not_enough_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            DesignLineChart(
                values = series.values,
                xLabels = series.xLabels,
                yLabels = series.yLabels(type),
            )
        }
    }
}

// ── Período ───────────────────────────────────────────────────────────────────

/**
 * Os três períodos do protótipo.
 *
 * Cada um define a janela de tempo e o tamanho do balde de agregação: por
 * semana, cada ponto é a média de uma semana, e assim por diante. É a mesma
 * ideia da agregação do upstream, mas com os três níveis que o protótipo
 * desenha, e local à tela.
 */
enum class DesignPeriod(
    val labelRes: Int,
    val byLabelRes: Int,
    val noteRes: Int,
    val windowDays: Int,
    val bucketDays: Int,
) {
    SEMANA(
        labelRes = R.string.design_period_week,
        byLabelRes = R.string.design_period_by_week,
        noteRes = R.string.design_period_note_week,
        windowDays = 7 * 12,
        bucketDays = 7,
    ),
    MES(
        labelRes = R.string.design_period_month,
        byLabelRes = R.string.design_period_by_month,
        noteRes = R.string.design_period_note_month,
        windowDays = 365,
        bucketDays = 30,
    ),
    ANO(
        labelRes = R.string.design_period_year,
        byLabelRes = R.string.design_period_by_year,
        noteRes = R.string.design_period_note_year,
        windowDays = 365 * 5,
        bucketDays = 365,
    ),
}

// ── Série ─────────────────────────────────────────────────────────────────────

/** Os dados prontos para o gráfico: valores e rótulos dos dois eixos. */
private data class DesignSeries(
    val values: List<Float>,
    val xLabels: List<String>,
) {
    /** Os três rótulos do eixo Y: máximo, meio e mínimo. */
    fun yLabels(type: MeasurementType): List<String> {
        if (values.isEmpty()) return emptyList()
        val min = values.min()
        val max = values.max()
        return listOf(max, (max + min) / 2f, min)
            .map { DesignFormat.number(it, type.unit) }
    }
}

/**
 * Agrega as medições em pontos, um por balde de período.
 *
 * Cada ponto é a **média** dos valores que caem no balde — o que a nota do
 * protótipo promete ("cada ponto é a média das medições daquela semana").
 * Baldes sem medição simplesmente não viram ponto: o protótipo diz
 * explicitamente que semanas sem medição ficam vazias, **sem interpolar**.
 */
private fun buildSeries(
    items: List<AggregatedMeasurement>,
    type: MeasurementType,
    period: DesignPeriod,
): DesignSeries {
    val cutoff = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -period.windowDays)
    }.timeInMillis

    val dayMillis = 24L * 60 * 60 * 1000
    val bucketMillis = period.bucketDays * dayMillis

    // (timestamp, valor) das medições da janela que têm esta métrica.
    val points = items
        .asSequence()
        .map { it.enriched }
        .mapNotNull { enriched ->
            val ts = enriched.measurementWithValues.measurement.timestamp
            if (ts < cutoff) return@mapNotNull null
            val v = enriched.valuesWithTrend
                .find { it.currentValue.type.id == type.id }
                ?.currentValue?.value
                ?: return@mapNotNull null
            val numeric = when (type.inputType) {
                InputFieldType.INT -> v.intValue?.toFloat()
                else -> v.floatValue
            } ?: return@mapNotNull null
            ts to numeric
        }
        .toList()
        .sortedBy { it.first }

    if (points.isEmpty()) return DesignSeries(emptyList(), emptyList())

    // Agrupa por balde, contado a partir da medição mais antiga da janela.
    val origin = points.first().first
    val buckets = points
        .groupBy { (ts, _) -> (ts - origin) / bucketMillis }
        .toSortedMap()

    val values = buckets.values.map { group -> group.map { it.second }.average().toFloat() }

    // Rótulos do eixo X: três datas — início, meio e fim — como no protótipo.
    val fmt = SimpleDateFormat(
        if (period == DesignPeriod.ANO) "yyyy" else "d MMM",
        Locale.getDefault(),
    )
    val timestamps = buckets.values.map { group -> group.first().first }
    val xLabels = when {
        timestamps.size < 2 -> timestamps.map { fmt.format(Date(it)) }
        else -> listOf(
            fmt.format(Date(timestamps.first())),
            fmt.format(Date(timestamps[timestamps.size / 2])),
            fmt.format(Date(timestamps.last())),
        )
    }

    return DesignSeries(values = values, xLabels = xLabels)
}
