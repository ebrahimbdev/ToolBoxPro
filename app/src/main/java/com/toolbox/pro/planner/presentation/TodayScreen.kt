package com.toolbox.pro.planner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.planner.data.EnergyLevel
import com.toolbox.pro.planner.data.TaskEntity
import com.toolbox.pro.planner.domain.PlannerEngine
import com.toolbox.pro.planner.domain.PlannerInput
import com.toolbox.pro.ui.components.ToolHeader
import java.text.DateFormat
import java.util.Date

/**
 * Today: date header, free-time + energy pickers feeding the Smart Daily
 * Planner engine, its three suggestion buckets, then overdue / pinned /
 * planned-for-today lists.
 */
@Composable
fun TodayScreen(
    vm: PlannerViewModel,
    onOpenTask: (Long) -> Unit
) {
    val s = LocalStrings.current
    var freeMinutes by remember { mutableIntStateOf(45) }
    var energy by remember { mutableStateOf(EnergyLevel.MEDIUM) }

    val pool by vm.openPool.collectAsState()
    val todayTasks by vm.today.collectAsState()
    val overdue by vm.overdue.collectAsState()
    val pinned by vm.pinned.collectAsState()

    val plan = remember(pool, freeMinutes, energy) {
        PlannerEngine.plan(PlannerInput(pool, freeMinutes, energy, System.currentTimeMillis()))
    }
    val overdueIds = remember(overdue) { overdue.map { it.id }.toSet() }
    val todayRest = remember(todayTasks, overdueIds) { todayTasks.filter { it.id !in overdueIds } }

    Scaffold(
        topBar = {
            ToolHeader(
                title = s.tabToday,
                subtitle = DateFormat.getDateInstance(DateFormat.FULL).format(Date()),
                icon = Icons.Filled.Today
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ---- pickers feeding the rules engine
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    s.freeTimeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(15, 30, 45, 60, 90).forEach { m ->
                    FilterChip(
                        selected = freeMinutes == m,
                        onClick = { freeMinutes = m },
                        label = { Text("$m ${s.minShort}") }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    s.myEnergyLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(EnergyLevel.LOW, EnergyLevel.MEDIUM, EnergyLevel.HIGH).forEach { e ->
                    FilterChip(
                        selected = energy == e,
                        onClick = { energy = e },
                        label = { Text(energyLabel(s, e)) }
                    )
                }
            }

            // ---- engine suggestions (user always picks; nothing imposed)
            SectionTitle(s.suggestionsTitle)
            if (plan.isEmpty) {
                Text(
                    s.planEmpty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                listOf(
                    com.toolbox.pro.planner.domain.PlannerMode.FOCUS to plan.focus,
                    com.toolbox.pro.planner.domain.PlannerMode.LIGHT to plan.light,
                    com.toolbox.pro.planner.domain.PlannerMode.RECOVERY to plan.recovery
                ).forEach { (mode, items) ->
                    if (items.isEmpty()) return@forEach
                    Text(
                        modeLabel(s, mode),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (mode) {
                            com.toolbox.pro.planner.domain.PlannerMode.FOCUS -> androidx.compose.ui.graphics.Color(0xFF6C63FF)
                            com.toolbox.pro.planner.domain.PlannerMode.LIGHT -> androidx.compose.ui.graphics.Color(0xFF4ECDC4)
                            else -> androidx.compose.ui.graphics.Color(0xFF66BB6A)
                        },
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    items.forEach { sug ->
                        Column {
                            TaskCard(
                                task = sug.task,
                                s = s,
                                onClick = { onOpenTask(sug.task.id) },
                                onDone = { vm.complete(sug.task.id) }
                            )
                            Text(
                                "• ${reasonLabel(s, sug.reason)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            // ---- overdue
            if (overdue.isNotEmpty()) {
                SectionTitle(s.overdueLabel, color = androidx.compose.ui.graphics.Color(0xFFE53935))
                overdue.forEach { t -> TodayRow(t, s, vm, onOpenTask) }
            }

            // ---- pinned
            if (pinned.isNotEmpty()) {
                SectionTitle(s.pinTask)
                pinned.forEach { t -> TodayRow(t, s, vm, onOpenTask) }
            }

            // ---- rest of today
            SectionTitle(s.tabToday)
            if (todayRest.isEmpty() && overdue.isEmpty() && pinned.isEmpty()) {
                Text(
                    s.emptyToday,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                todayRest.forEach { t -> TodayRow(t, s, vm, onOpenTask) }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TodayRow(
    task: TaskEntity,
    s: com.toolbox.pro.core.localization.Strings,
    vm: PlannerViewModel,
    onOpenTask: (Long) -> Unit
) {
    TaskCard(
        task = task,
        s = s,
        onClick = { onOpenTask(task.id) },
        onDone = { vm.complete(task.id) }
    )
}
