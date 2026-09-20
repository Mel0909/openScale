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
package com.health.openscale.ui.screen.overview

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.health.openscale.R
import com.health.openscale.core.data.AggregationLevel
import com.health.openscale.core.data.InputFieldType
import com.health.openscale.core.data.MeasurementType
import com.health.openscale.core.data.Trend
import com.health.openscale.core.data.UserGoals
import com.health.openscale.core.facade.SettingsPreferenceKeys
import com.health.openscale.core.model.AggregatedMeasurement
import com.health.openscale.core.usecase.GoalProgress
import com.health.openscale.core.utils.LocaleUtils
import com.health.openscale.ui.navigation.Routes
import com.health.openscale.ui.screen.components.DesignCard
import com.health.openscale.ui.screen.components.DesignDeltaChip
import com.health.openscale.ui.screen.components.DesignDeltaDirection
import com.health.openscale.ui.screen.components.DesignEmptyState
import com.health.openscale.ui.screen.components.DesignMetricCard
import com.health.openscale.ui.screen.components.DesignMicroLabel
import com.health.openscale.ui.screen.components.DesignRadius
import com.health.openscale.ui.screen.components.DesignSectionHeader
import com.health.openscale.ui.screen.components.DesignValueWithUnit
import com.health.openscale.ui.screen.components.MeasurementChart
import com.health.openscale.ui.screen.components.UserGoalChip
import com.health.openscale.ui.screen.components.provideFilterTopBarAction
import com.health.openscale.ui.screen.components.rememberAddMeasurementActionButton
import com.health.openscale.ui.screen.components.rememberBluetoothActionButton
import com.health.openscale.ui.screen.components.rememberResolvedAggregationLevel
import com.health.openscale.ui.screen.dialog.DeleteConfirmationDialog
import com.health.openscale.ui.screen.dialog.UserGoalDialog
import com.health.openscale.ui.screen.settings.BluetoothViewModel
import com.health.openscale.ui.shared.SharedViewModel
import com.health.openscale.ui.shared.TopBarAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * A tela Hoje, na forma do protótipo: peso-herói, chip de variação, cartão de
 * sparkline e grade de métricas.
 *
 * Fonte de verdade do visual: `design/App.dc.html`, tela "Hoje".
 *
 * **O que esta tela preserva da [OverviewScreen] do upstream**, por decisão da
 * dona do projeto — a forma muda, as funcionalidades não:
 *
 *  - **agregação por período** — o seletor mora no cartão do gráfico, e não
 *    mais numa barra própria;
 *  - **drill-down** — tocar num período agregado continua abrindo as medições
 *    que o compõem;
 *  - **avaliação por faixa e banner de erro** — seguem nas linhas de medição,
 *    via [MeasurementRowExpandable];
 *  - **linhas de medição expansíveis** — idem;
 *  - **metas** — a faixa de chips continua onde estava, abaixo do gráfico.
 *
 * **O que saiu:** o splitter arrastável. Ver [DesignOverviewChartCard] para o
 * porquê e para o que entrou no lugar.
 *
 * O arquivo original fica intocado, e a rota é que aponta para cá — assim o
 * upstream continua mesclando limpo e dá para comparar as duas formas.
 */
@Composable
fun DesignOverviewScreen(
    navController: NavController,
    sharedViewModel: SharedViewModel,
    bluetoothViewModel: BluetoothViewModel,
    drillDownStartMillis: Long? = null,
    drillDownEndMillis: Long? = null,
) {
    val isDrillDown = drillDownStartMillis != null && drillDownEndMillis != null

    val selectedUserId by sharedViewModel.selectedUserId.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // ── Agregação ─────────────────────────────────────────────────────────────
    val activeAggregationLevel by rememberResolvedAggregationLevel(
        screenContextName = SettingsPreferenceKeys.OVERVIEW_SCREEN_CONTEXT,
        sharedViewModel = sharedViewModel,
    )
    val isAggregated = !isDrillDown && activeAggregationLevel != AggregationLevel.NONE
    val weekFields = remember { LocaleUtils.systemWeekFields() }

    // ── Dados ─────────────────────────────────────────────────────────────────
    val overviewState by if (isDrillDown) {
        sharedViewModel.drillDownFlow(drillDownStartMillis, drillDownEndMillis)
            .collectAsStateWithLifecycle(initialValue = SharedViewModel.UiState.Loading)
    } else {
        sharedViewModel.screenFlow(SettingsPreferenceKeys.OVERVIEW_SCREEN_CONTEXT)
            .collectAsStateWithLifecycle(initialValue = SharedViewModel.UiState.Loading)
    }

    val aggregatedItems: List<AggregatedMeasurement> = remember(overviewState) {
        when (val s = overviewState) {
            is SharedViewModel.UiState.Success -> s.data
            else -> emptyList()
        }
    }

    // ── Ações da barra superior ───────────────────────────────────────────────
    val bluetoothAction = rememberBluetoothActionButton(bluetoothViewModel, sharedViewModel, navController)
    val addMeasurementAction = rememberAddMeasurementActionButton(sharedViewModel, navController)
    val timeFilterAction = if (isDrillDown) null else provideFilterTopBarAction(
        sharedViewModel = sharedViewModel,
        screenContextName = SettingsPreferenceKeys.OVERVIEW_SCREEN_CONTEXT,
    )

    // ── Metas ─────────────────────────────────────────────────────────────────
    val allMeasurementTypes by sharedViewModel.measurementTypes.collectAsState()
    val goalDialogContextData by sharedViewModel.userGoalDialogContext.collectAsState()
    val userGoals by if (selectedUserId != null && selectedUserId != 0) {
        sharedViewModel.getAllGoalsForUser(selectedUserId!!).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<UserGoals>()) }
    }
    val goalProgressState by sharedViewModel.goalProgressFlow.collectAsStateWithLifecycle()
    val progressByTypeId: Map<Int, GoalProgress> = remember(goalProgressState) {
        (goalProgressState as? SharedViewModel.UiState.Success)?.data
            ?.associateBy { it.type.id } ?: emptyMap()
    }
    val isGoalsSectionExpanded by sharedViewModel.myGoalsExpandedOverview.collectAsState(initial = true)
    val userEvalContext by sharedViewModel.userEvaluationContext.collectAsState()
    val currentSelectedUser by sharedViewModel.selectedUser.collectAsState()
    val typeById: Map<Int, MeasurementType> = remember(allMeasurementTypes) {
        allMeasurementTypes.associateBy { it.id }
    }

    var currentSelectedAggregatedTs by rememberSaveable { mutableStateOf<Long?>(null) }

    // ── Diálogo de exclusão ───────────────────────────────────────────────────
    var measurementToDelete by remember { mutableStateOf<AggregatedMeasurement?>(null) }
    measurementToDelete?.let { aggItem ->
        val enrichedItem = aggItem.enriched
        val weightValue = enrichedItem.valuesWithTrend
            .find { it.currentValue.type.key == MeasurementType.WEIGHT }
        val weightString = weightValue?.currentValue?.let {
            LocaleUtils.formatValueForDisplay(it.value.floatValue.toString(), it.type.unit)
        } ?: ""
        val formattedDate = remember(enrichedItem.measurementWithValues.measurement.timestamp) {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                .format(Date(enrichedItem.measurementWithValues.measurement.timestamp))
        }
        DeleteConfirmationDialog(
            onDismissRequest = { measurementToDelete = null },
            onConfirm = {
                sharedViewModel.deleteMeasurement(enrichedItem.measurementWithValues.measurement)
                measurementToDelete = null
            },
            title = stringResource(R.string.dialog_title_delete_item),
            text = stringResource(R.string.dialog_message_delete_item, formattedDate, weightString),
        )
    }

    // ── Barra superior ────────────────────────────────────────────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, selectedUserId, bluetoothAction, timeFilterAction, isDrillDown, aggregatedItems) {
        fun updateTopBar() {
            if (isDrillDown) {
                val fmt = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                val title = "${fmt.format(Date(drillDownStartMillis))} – ${fmt.format(Date(drillDownEndMillis - 1))}"
                sharedViewModel.setTopBarTitle(title)
                sharedViewModel.setTopBarActions(emptyList())
            } else {
                sharedViewModel.setTopBarTitle(resources.getString(R.string.route_title_overview))
                val actions = mutableListOf<TopBarAction>()
                actions.add(bluetoothAction)
                actions.add(addMeasurementAction)
                timeFilterAction?.let { actions.add(it) }
                sharedViewModel.setTopBarActions(actions)
            }
        }
        updateTopBar()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                updateTopBar()
                val target = sharedViewModel.lastDrillDownPeriodStart.value ?: return@LifecycleEventObserver
                val idx = aggregatedItems.indexOfFirst { it.periodStartMillis == target }
                if (idx >= 0) {
                    scope.launch {
                        listState.scrollToItem(idx)
                        sharedViewModel.setLastDrillDownPeriodStart(null)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Corpo ─────────────────────────────────────────────────────────────────
    when {
        overviewState is SharedViewModel.UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        selectedUserId == null -> {
            DesignEmptyState(
                icon = Icons.Filled.PersonSearch,
                title = stringResource(R.string.no_user_selected_title),
                message = stringResource(R.string.no_user_selected_message),
                actionLabel = stringResource(R.string.action_add_user),
                actionIcon = Icons.Filled.PersonAdd,
                onAction = { navController.navigate(Routes.userDetail(-1)) },
            )
        }

        else -> when (val state = overviewState) {
            is SharedViewModel.UiState.Success -> {
                if (aggregatedItems.isEmpty()) {
                    DesignEmptyState(
                        icon = Icons.Filled.Assessment,
                        title = stringResource(R.string.no_measurements_title),
                        message = stringResource(R.string.no_measurements_message),
                        actionLabel = stringResource(R.string.action_add_measurement),
                        actionIcon = Icons.Filled.Add,
                        actionEnabled = selectedUserId != null,
                        onAction = {
                            selectedUserId?.let { userId ->
                                navController.navigate(
                                    Routes.measurementDetail(measurementId = null, userId = userId)
                                )
                            }
                        },
                    )
                } else {
                    val topId = aggregatedItems.firstOrNull()
                        ?.enriched?.measurementWithValues?.measurement?.id
                    LaunchedEffect(topId, aggregatedItems.size) {
                        if (topId != null && !listState.isScrollInProgress) {
                            delay(60.milliseconds)
                            listState.smartScrollTo(0)
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        // ── Peso-herói + chip de variação ─────────────────────
                        // Só na visão normal: no drill-down o herói seria o
                        // primeiro item da janela, não "o peso de hoje", e a
                        // barra superior já diz de que período se trata.
                        if (!isDrillDown) {
                            item(key = "hero") {
                                DesignOverviewHero(
                                    aggregatedItems = aggregatedItems,
                                    isAggregated = isAggregated,
                                )
                            }

                            // ── Cartão do gráfico ─────────────────────────────
                            item(key = "chart") {
                                DesignOverviewChartCard(
                                    sharedViewModel = sharedViewModel,
                                    aggregatedItems = aggregatedItems,
                                    isAggregated = isAggregated,
                                    onNavigateToHistory = { navController.navigate(Routes.GRAPH) },
                                    onPointSelected = { selectedTs ->
                                        onChartPointSelected(
                                            selectedTs = selectedTs,
                                            isAggregated = isAggregated,
                                            aggregatedItems = aggregatedItems,
                                            sharedViewModel = sharedViewModel,
                                            listState = listState,
                                            scope = scope,
                                            onAggregatedTsChange = { currentSelectedAggregatedTs = it },
                                        )
                                    },
                                )
                            }

                            // ── Grade de composição ───────────────────────────
                            item(key = "metrics") {
                                DesignCompositionGrid(
                                    aggregatedItems = aggregatedItems,
                                    onMetricClick = { navController.navigate(Routes.GRAPH) },
                                    onChooseMetrics = {
                                        navController.navigate(Routes.MEASUREMENT_TYPES)
                                    },
                                )
                            }

                            // ── Metas ─────────────────────────────────────────
                            if (userGoals.isNotEmpty()) {
                                item(key = "goals") {
                                    DesignGoalsSection(
                                        userGoals = userGoals,
                                        typeById = typeById,
                                        progressByTypeId = progressByTypeId,
                                        currentUserId = currentSelectedUser?.id,
                                        isExpanded = isGoalsSectionExpanded,
                                        onToggleExpanded = {
                                            scope.launch {
                                                sharedViewModel.setMyGoalsExpandedOverview(
                                                    !isGoalsSectionExpanded
                                                )
                                            }
                                        },
                                        onGoalClick = { type, goal ->
                                            sharedViewModel.showUserGoalDialogWithContext(
                                                type = type,
                                                existingGoal = goal,
                                            )
                                        },
                                    )
                                }
                            }

                            item(key = "recent-header") {
                                DesignMicroLabel(
                                    text = stringResource(R.string.route_title_table),
                                )
                            }
                        }

                        // ── Lista de medições ─────────────────────────────────
                        itemsIndexed(
                            items = aggregatedItems,
                            key = { _, item ->
                                val m = item.enriched.measurementWithValues.measurement
                                "${m.id}_${m.timestamp}"
                            },
                        ) { _, aggItem ->
                            val enrichedItem = aggItem.enriched
                            val ts = enrichedItem.measurementWithValues.measurement.timestamp

                            if (isAggregated) {
                                val periodStart = aggItem.periodStartMillis
                                val periodEnd = aggItem.periodEndMillis
                                DesignMeasurementCard(
                                    sharedViewModel = sharedViewModel,
                                    aggregatedItem = aggItem,
                                    userEvaluationContext = userEvalContext,
                                    isAggregated = true,
                                    aggregatedPeriodLabel = activeAggregationLevel.periodLabel(
                                        timestamp = ts,
                                        calendarWeekAbbrev = stringResource(R.string.calendar_week_abbrev),
                                        weekFields = weekFields,
                                    ),
                                    // Drill-down preservado: tocar no período
                                    // agregado abre as medições que o compõem.
                                    onClick = {
                                        currentSelectedAggregatedTs = ts
                                        sharedViewModel.setLastDrillDownPeriodStart(periodStart)
                                        navController.navigate(
                                            Routes.overviewDrillDown(periodStart, periodEnd)
                                        )
                                    },
                                    onDelete = null,
                                )
                            } else {
                                DesignMeasurementCard(
                                    sharedViewModel = sharedViewModel,
                                    aggregatedItem = aggItem,
                                    userEvaluationContext = userEvalContext,
                                    isAggregated = false,
                                    onClick = {
                                        selectedUserId?.let { userId ->
                                            navController.navigate(
                                                Routes.measurementDetail(
                                                    enrichedItem.measurementWithValues.measurement.id,
                                                    userId,
                                                )
                                            )
                                        }
                                    },
                                    onDelete = { measurementToDelete = aggItem },
                                )
                            }
                        }
                    }
                }
            }

            is SharedViewModel.UiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message ?: stringResource(R.string.error_loading_data))
                }
            }

            SharedViewModel.UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // ── Diálogo de meta ───────────────────────────────────────────────────────
    if (goalDialogContextData.showDialog) {
        val dialogContext = goalDialogContextData
        val userIdForDialog = currentSelectedUser?.id
        if (dialogContext.typeForDialog == null || userIdForDialog == null || userIdForDialog == 0) {
            LaunchedEffect(goalDialogContextData.showDialog) {
                sharedViewModel.dismissUserGoalDialogWithContext()
            }
        } else {
            UserGoalDialog(
                navController = navController,
                existingUserGoal = dialogContext.existingGoalForDialog,
                allMeasurementTypes = allMeasurementTypes,
                allGoalsOfCurrentUser = userGoals,
                onDismiss = { sharedViewModel.dismissUserGoalDialogWithContext() },
                onConfirm = { measurementTypeId, goalValue, goalTargetDate, startDate ->
                    val goalToProcess = UserGoals(
                        userId = userIdForDialog,
                        measurementTypeId = measurementTypeId,
                        goalValue = goalValue,
                        goalTargetDate = goalTargetDate,
                        startDate = startDate,
                    )
                    if (dialogContext.existingGoalForDialog != null) {
                        sharedViewModel.updateUserGoal(goalToProcess)
                    } else {
                        sharedViewModel.insertUserGoal(goalToProcess)
                    }
                },
                onDelete = { _, measurementTypeIdToDelete ->
                    sharedViewModel.deleteUserGoal(userIdForDialog, measurementTypeIdToDelete)
                    android.widget.Toast
                        .makeText(context, R.string.toast_goal_deleted, android.widget.Toast.LENGTH_SHORT)
                        .show()
                    sharedViewModel.dismissUserGoalDialogWithContext()
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Peso-herói
// ---------------------------------------------------------------------------

/**
 * O bloco de abertura do protótipo: rótulo micro com data, peso em 64 sp e o
 * chip de variação em relação à medição anterior.
 *
 * Usa o peso da medição mais recente. Em modo agregado esse valor é a média do
 * período — o rótulo diz isso, para o número grande nunca ficar ambíguo.
 */
@Composable
private fun DesignOverviewHero(
    aggregatedItems: List<AggregatedMeasurement>,
    isAggregated: Boolean,
) {
    val latest = aggregatedItems.firstOrNull() ?: return
    val weightValue = latest.enriched.valuesWithTrend
        .find { it.currentValue.type.key == MeasurementType.WEIGHT }
        ?: return

    val type = weightValue.currentValue.type
    val raw = weightValue.currentValue.value.floatValue ?: return
    val formatted = LocaleUtils.formatValueForDisplay(raw.toString(), type.unit)

    val timestamp = latest.enriched.measurementWithValues.measurement.timestamp
    val dateLabel = remember(timestamp) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
            .format(Date(timestamp))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DesignMicroLabel(
            text = buildString {
                append(type.getDisplayName(LocalContext.current))
                append(" · ")
                append(dateLabel)
                // Em agregado o herói é a média do período, não uma pesagem.
                // Dizer isso evita que o número grande seja lido como "o peso
                // de hoje" quando não é.
                if (isAggregated && latest.aggregatedFromCount > 1) {
                    append(" · ⌀ ${latest.aggregatedFromCount}")
                }
            },
        )

        DesignValueWithUnit(
            value = formatted,
            unit = type.unit.displayName,
            style = MaterialTheme.typography.displayLarge,
            unitStyle = MaterialTheme.typography.titleMedium,
        )

        val difference = weightValue.difference
        if (difference != null && weightValue.trend != Trend.NOT_APPLICABLE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DesignDeltaChip(
                    text = LocaleUtils.formatValueForDisplay(
                        value = kotlin.math.abs(difference).toString(),
                        unit = type.unit,
                    ),
                    direction = when (weightValue.trend) {
                        Trend.DOWN -> DesignDeltaDirection.DOWN
                        Trend.UP -> DesignDeltaDirection.UP
                        else -> DesignDeltaDirection.FLAT
                    },
                )
                // O protótipo põe "desde 12 set" ao lado do chip. A data da
                // medição anterior não vem no ValueWithDifference, então o
                // segundo item da lista é a melhor referência disponível.
                val previousTs = aggregatedItems.getOrNull(1)
                    ?.enriched?.measurementWithValues?.measurement?.timestamp
                if (previousTs != null) {
                    Spacer(Modifier.width(9.dp))
                    val sinceLabel = remember(previousTs) {
                        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                            .format(Date(previousTs))
                    }
                    Text(
                        text = stringResource(R.string.design_since_date, sinceLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Cartão do gráfico
// ---------------------------------------------------------------------------

/**
 * O cartão de sparkline do protótipo, com o gráfico completo do upstream
 * dentro.
 *
 * **Sobre o splitter arrastável:** ele saiu. O protótipo desenha o gráfico
 * como um cartão de altura fixa dentro de uma página que rola, e as duas
 * coisas são incompatíveis — um splitter divide uma tela em dois painéis de
 * altura variável, o que exige que a tela *não* role. O que ele resolvia
 * (gráfico ocupando espaço demais) o cartão já resolve: ele tem altura fixa e
 * sai de vista ao rolar. A preferência `OVERVIEW_SCREEN_CONTEXT` do splitter
 * continua gravada, intocada, caso a decisão volte atrás.
 *
 * O seletor de período (dia/semana/mês/ano) continua acessível pela ação de
 * filtro na barra superior, que é de onde ele já vinha — [MeasurementChart]
 * recebe `showFilterControls = true` e desenha os próprios controles.
 */
@Composable
private fun DesignOverviewChartCard(
    sharedViewModel: SharedViewModel,
    aggregatedItems: List<AggregatedMeasurement>,
    isAggregated: Boolean,
    onNavigateToHistory: () -> Unit,
    onPointSelected: (Long) -> Unit,
) {
    DesignCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignRadius.Large),
        contentPadding = 18.dp,
    ) {
        DesignSectionHeader(
            label = stringResource(R.string.design_last_30_days),
            action = stringResource(R.string.design_see_history),
            onActionClick = onNavigateToHistory,
        )

        Spacer(Modifier.height(8.dp))

        // O gráfico do upstream, com toque, marcador e seleção de ponto
        // preservados. O sparkline do protótipo é o que ele vira quando há
        // poucos pontos para valer um gráfico com eixo.
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            MeasurementChart(
                sharedViewModel = sharedViewModel,
                screenContextName = SettingsPreferenceKeys.OVERVIEW_SCREEN_CONTEXT,
                showFilterControls = true,
                modifier = Modifier.fillMaxWidth(),
                showYAxis = false,
                onPointSelected = onPointSelected,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Grade de composição
// ---------------------------------------------------------------------------

/**
 * A grade de duas colunas do protótipo.
 *
 * Mostra as métricas da medição mais recente, menos o peso — que já é o herói
 * logo acima e ficaria repetido. Respeita `isEnabled`, que é o que a tela de
 * tipos de medição controla, então "Escolher métricas" leva exatamente ao
 * lugar que muda esta grade.
 *
 * Não é uma `LazyVerticalGrid` de propósito: ela dentro de uma `LazyColumn`
 * exige altura fixa, e o número de métricas é pequeno e conhecido. Duas
 * colunas montadas à mão evitam isso.
 */
@Composable
private fun DesignCompositionGrid(
    aggregatedItems: List<AggregatedMeasurement>,
    onMetricClick: () -> Unit,
    onChooseMetrics: () -> Unit,
) {
    val latest = aggregatedItems.firstOrNull() ?: return
    val context = LocalContext.current

    val metrics = remember(latest) {
        latest.enriched.valuesWithTrend.filter { v ->
            val t = v.currentValue.type
            t.isEnabled &&
                t.key != MeasurementType.WEIGHT &&
                (t.inputType == InputFieldType.FLOAT || t.inputType == InputFieldType.INT)
        }
    }

    if (metrics.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DesignSectionHeader(
            label = stringResource(R.string.design_composition),
            action = stringResource(R.string.design_choose_metrics),
            onActionClick = onChooseMetrics,
        )

        metrics.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { valueWithTrend ->
                    val type = valueWithTrend.currentValue.type
                    val numeric = when (type.inputType) {
                        InputFieldType.INT -> valueWithTrend.currentValue.value.intValue?.toFloat()
                        else -> valueWithTrend.currentValue.value.floatValue
                    }
                    DesignMetricCard(
                        modifier = Modifier.weight(1f),
                        label = type.getDisplayName(context),
                        value = numeric
                            ?.let { LocaleUtils.formatValueForDisplay(it.toString(), type.unit) }
                            ?: "—",
                        unit = type.unit.displayName,
                        delta = valueWithTrend.difference
                            ?.takeIf { valueWithTrend.trend != Trend.NOT_APPLICABLE }
                            ?.let {
                                LocaleUtils.formatValueForDisplay(
                                    value = it.toString(),
                                    unit = type.unit,
                                    includeSign = true,
                                )
                            },
                        hasChange = valueWithTrend.trend == Trend.UP ||
                            valueWithTrend.trend == Trend.DOWN,
                        onClick = onMetricClick,
                    )
                }
                // Número ímpar de métricas: o vão da última linha precisa de um
                // peso equivalente, senão o cartão solitário estica até a borda.
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Metas
// ---------------------------------------------------------------------------

/** A faixa de chips de meta, preservada do upstream com o cabeçalho do design. */
@Composable
private fun DesignGoalsSection(
    userGoals: List<UserGoals>,
    typeById: Map<Int, MeasurementType>,
    progressByTypeId: Map<Int, GoalProgress>,
    currentUserId: Int?,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onGoalClick: (MeasurementType, UserGoals) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DesignMicroLabel(
                text = stringResource(R.string.my_goals_label) +
                    if (!isExpanded) " (${userGoals.size})" else "",
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) R.string.action_show_less_desc else R.string.action_show_more_desc
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.designIconClickable(onToggleExpanded),
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    userGoals,
                    key = { goal -> "${goal.userId}_${goal.measurementTypeId}" },
                ) { goal ->
                    if (currentUserId != null && goal.userId == currentUserId) {
                        val measurementType = typeById[goal.measurementTypeId]
                        if (measurementType != null) {
                            UserGoalChip(
                                userGoal = goal,
                                measurementType = measurementType,
                                progress = progressByTypeId[goal.measurementTypeId],
                                onClick = {
                                    if (currentUserId != 0) {
                                        onGoalClick(measurementType, goal)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Área de toque para os ícones de expandir/recolher.
 *
 * O ícone desenhado tem 24 dp, metade do mínimo de 48 dp que o Material pede
 * para alvo de toque. O `size(48.dp)` aqui amplia só a área sensível; o
 * `padding(12.dp)` devolve o ícone ao tamanho visual de 24 dp. O `clip` vem
 * antes do `clickable` para a ondulação sair circular, e não quadrada.
 */
@Composable
private fun Modifier.designIconClickable(onClick: () -> Unit): Modifier =
    this
        .size(48.dp)
        .clip(RoundedCornerShape(percent = 50))
        .clickable(onClick = onClick)
        .padding(12.dp)

// ---------------------------------------------------------------------------
// Cartão de medição
// ---------------------------------------------------------------------------

/**
 * A linha de registro do protótipo — data à esquerda, peso ao centro, variação
 * à direita — com as linhas expansíveis do upstream preservadas dentro.
 *
 * As avaliações por faixa e os banners de erro continuam vindo de
 * [MeasurementRowExpandable], que é reusada tal como está.
 */
@Composable
private fun DesignMeasurementCard(
    sharedViewModel: SharedViewModel,
    aggregatedItem: AggregatedMeasurement,
    userEvaluationContext: com.health.openscale.core.model.UserEvaluationContext?,
    isAggregated: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    aggregatedPeriodLabel: String = "",
) {
    val enriched = aggregatedItem.enriched
    val measurement = enriched.measurementWithValues.measurement
    val context = LocalContext.current
    var isExpanded by rememberSaveable(measurement.id, measurement.timestamp) {
        mutableStateOf(false)
    }

    val weightValue = enriched.valuesWithTrend
        .find { it.currentValue.type.key == MeasurementType.WEIGHT }

    val dayLabel = remember(measurement.timestamp, isAggregated, aggregatedPeriodLabel) {
        if (isAggregated) {
            aggregatedPeriodLabel
        } else {
            DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                .format(Date(measurement.timestamp))
        }
    }
    val timeLabel = remember(measurement.timestamp) {
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
            .format(Date(measurement.timestamp))
    }

    DesignCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignRadius.Small),
        onClick = onClick,
        contentPadding = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.width(68.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                Text(
                    text = if (isAggregated) {
                        stringResource(
                            R.string.design_aggregated_count,
                            aggregatedItem.aggregatedFromCount,
                        )
                    } else {
                        timeLabel
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (weightValue != null) {
                    val type = weightValue.currentValue.type
                    val raw = weightValue.currentValue.value.floatValue
                    DesignValueWithUnit(
                        value = raw
                            ?.let { LocaleUtils.formatValueForDisplay(it.toString(), type.unit) }
                            ?: "—",
                        unit = type.unit.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
                val summary = remember(enriched) {
                    enriched.valuesWithTrend
                        .filter {
                            it.currentValue.type.isEnabled &&
                                it.currentValue.type.key != MeasurementType.WEIGHT
                        }
                        .take(2)
                        .mapNotNull { v ->
                            val t = v.currentValue.type
                            val n = when (t.inputType) {
                                InputFieldType.INT -> v.currentValue.value.intValue?.toFloat()
                                else -> v.currentValue.value.floatValue
                            } ?: return@mapNotNull null
                            "${LocaleUtils.formatValueForDisplay(n.toString(), t.unit)} " +
                                t.getDisplayName(context)
                        }
                        .joinToString(" · ")
                }
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }

            // Expandir: é o que revela as linhas de medição com avaliação por
            // faixa e os banners de erro — a funcionalidade que o protótipo não
            // desenhou mas que a dona do projeto pediu para preservar.
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(
                    if (isExpanded) R.string.action_show_less_desc else R.string.action_show_more_desc
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.designIconClickable { isExpanded = !isExpanded },
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                val expandedTypeIds = remember { androidx.compose.runtime.mutableStateMapOf<Int, Boolean>() }
                enriched.valuesWithTrend
                    .filter { it.currentValue.type.isEnabled }
                    .forEach { valueWithTrend ->
                        MeasurementRowExpandable(
                            sharedViewModel = sharedViewModel,
                            valueWithTrend = valueWithTrend,
                            userEvaluationContext = userEvaluationContext,
                            measuredAtMillis = measurement.timestamp,
                            expandedTypeIds = expandedTypeIds,
                            valuePrefix = if (isAggregated && aggregatedItem.aggregatedFromCount > 1) "⌀ " else "",
                        )
                    }

                if (onDelete != null) {
                    Text(
                        text = stringResource(R.string.delete_button_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .designIconClickable(onDelete),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Seleção de ponto no gráfico
// ---------------------------------------------------------------------------

/**
 * O que acontece ao tocar num ponto do gráfico: rolar a lista até a medição
 * correspondente. Extraído para o corpo da tela não crescer demais; a lógica é
 * a mesma da [OverviewScreen].
 */
private fun onChartPointSelected(
    selectedTs: Long,
    isAggregated: Boolean,
    aggregatedItems: List<AggregatedMeasurement>,
    sharedViewModel: SharedViewModel,
    listState: androidx.compose.foundation.lazy.LazyListState,
    scope: kotlinx.coroutines.CoroutineScope,
    onAggregatedTsChange: (Long) -> Unit,
) {
    if (isAggregated) {
        val idx = aggregatedItems.indexOfFirst { item ->
            selectedTs in item.periodStartMillis until item.periodEndMillis
        }
        if (idx >= 0) {
            val itemTs = aggregatedItems[idx].enriched.measurementWithValues.measurement.timestamp
            scope.launch {
                // +1: os cartões de herói, gráfico e grade ocupam os primeiros
                // índices da LazyColumn, então a medição n está em n + 1.
                listState.smartScrollTo(idx + 1)
                onAggregatedTsChange(itemTs)
            }
        }
    } else {
        val listForFind = aggregatedItems.map { it.enriched.measurementWithValues }
        sharedViewModel.findClosestMeasurement(selectedTs, listForFind)
            ?.let { (targetIndex, _) ->
                scope.launch { listState.smartScrollTo(targetIndex + 1) }
            }
    }
}
