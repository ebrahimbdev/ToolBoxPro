package com.toolbox.pro.planner.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.toolbox.pro.core.localization.Strings
import com.toolbox.pro.planner.data.ReviewEntity
import com.toolbox.pro.planner.data.ReviewType
import com.toolbox.pro.ui.components.ToolHeader

/**
 * Stats tab with two segments: Reviews (question flows + past entries) and
 * Analytics (aggregate counters, progress days, energy).
 */
@Composable
fun StatsScreen(vm: PlannerViewModel) {
    val s = LocalStrings.current
    var segment by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            ToolHeader(
                title = s.tabReviews,
                icon = Icons.Filled.Assessment,
                tabs = listOf(s.tabReviews, s.tabAnalytics),
                selectedTab = segment,
                onTabSelected = { segment = it }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { inner ->
        if (segment == 0) ReviewsSegment(vm, s, Modifier.padding(inner))
        else AnalyticsSegment(vm, s, Modifier.padding(inner))
    }
}

private fun questionLabel(s: Strings, key: String): String = when (key) {
    "did" -> s.qDailyDid
    "learned" -> s.qDailyLearned
    "tomorrow" -> s.qDailyTomorrow
    "done" -> s.qWeeklyDone
    "not_done" -> s.qWeeklyNotDone
    "worth" -> s.qWeeklyWorth
    "energy" -> s.qWeeklyEnergy
    "goals" -> s.qMonthlyGoals
    "kpi" -> s.qMonthlyKpi
    "time_energy" -> s.qMonthlyTime
    "trend" -> s.qMonthlyTrend
    else -> key
}

@Composable
private fun ReviewsSegment(vm: PlannerViewModel, s: Strings, modifier: Modifier = Modifier) {
    var type by remember { mutableStateOf(ReviewType.DAILY) }
    val keys = remember(type) { com.toolbox.pro.planner.domain.ReviewEngine.questionKeys(type) }
    var answers by remember(type) { mutableStateOf(keys.associateWith { "" }.toMutableMap()) }
    var savedFlash by remember { mutableStateOf(false) }

    val reviews by remember(type) { vm.reviewsFlow(type) }.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = type == ReviewType.DAILY, onClick = { type = ReviewType.DAILY }, label = { Text(s.dailyReview) })
                FilterChip(selected = type == ReviewType.WEEKLY, onClick = { type = ReviewType.WEEKLY }, label = { Text(s.weeklyReview) })
                FilterChip(selected = type == ReviewType.MONTHLY, onClick = { type = ReviewType.MONTHLY }, label = { Text(s.monthlyReview) })
            }
        }

        // ---- question flow
        item {
            Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    keys.forEach { key ->
                        OutlinedTextField(
                            value = answers[key] ?: "",
                            onValueChange = { answers = answers.toMutableMap().apply { put(key, it) } },
                            label = { Text(questionLabel(s, key)) },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = {
                            vm.saveReview(type, answers.filterValues { it.isNotBlank() })
                            answers = keys.associateWith { "" }.toMutableMap()
                            savedFlash = true
                        },
                        enabled = answers.values.any { it.isNotBlank() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(s.saveReviewLabel) }
                    if (savedFlash) {
                        Text(
                            s.reviewSaved,
                            style = MaterialTheme.typography.labelMedium,
                            color = androidx.compose.ui.graphics.Color(0xFF43A047)
                        )
                    }
                }
            }
        }

        // ---- past reviews
        item { SectionTitle(s.tabReviews) }
        if (reviews.isEmpty()) {
            item {
                Text(s.noReviews, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(reviews, key = { it.id }) { r -> ReviewCard(r, vm, s) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ReviewCard(review: ReviewEntity, vm: PlannerViewModel, s: Strings) {
    val answers = remember(review.answers) { vm.decodeAnswers(review.answers) }
    val summary = remember(review.summary) { vm.decodeSummary(review.summary) }

    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(fmtDate(review.createdAt), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    summary["done"]?.let { InfoChip(s.statDone.format(it.toIntOrNull() ?: 0), dotColor = androidx.compose.ui.graphics.Color(0xFF43A047)) }
                    summary["slips"]?.let { InfoChip(s.statSlips.format(it.toIntOrNull() ?: 0), dotColor = androidx.compose.ui.graphics.Color(0xFFFF9800)) }
                }
            }
            answers.forEach { (k, v) ->
                Column {
                    Text(
                        questionLabel(s, k),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(v, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AnalyticsSegment(vm: PlannerViewModel, s: Strings, modifier: Modifier = Modifier) {
    val countOpen by vm.countOpen.collectAsState()
    val countOverdue by vm.countOverdue.collectAsState()
    val completedWeek by vm.completedWeek.collectAsState()
    val progressDays by vm.progressDays.collectAsState()
    val avgEnergy by vm.avgEnergy.collectAsState()
    val minutesWeek by vm.minutesWeek.collectAsState()
    val logs by vm.energyLogs.collectAsState()

    val stats = listOf(
        Triple(s.openLabel, countOpen.toString(), androidx.compose.ui.graphics.Color(0xFF6C63FF)),
        Triple(s.overdueLabel, countOverdue.toString(), androidx.compose.ui.graphics.Color(0xFFE53935)),
        Triple(s.completedLabel, completedWeek.toString(), androidx.compose.ui.graphics.Color(0xFF43A047)),
        Triple(s.progressDaysLabel, progressDays.toString(), androidx.compose.ui.graphics.Color(0xFF4ECDC4)),
        Triple(s.avgEnergyStat, (avgEnergy ?: 0f).let { String.format("%.1f", it) }, androidx.compose.ui.graphics.Color(0xFFFFB300)),
        Triple("${s.timeLoggedLabel} (${s.last7})", "$minutesWeek ${s.minShort}", androidx.compose.ui.graphics.Color(0xFF42A5F5))
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                s.last30,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(stats.chunked(2)) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (label, value, color) ->
                    StatCard(label = label, value = value, color = color, modifier = Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        item { SectionTitle(s.energyLabel) }
        if (logs.isEmpty()) {
            item { Text(s.logEnergyLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(logs.take(10), key = { it.id }) { log ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fmtDateTime(log.loggedAt), style = MaterialTheme.typography.bodySmall)
                    InfoChip("${log.value}/5", dotColor = energyColor(com.toolbox.pro.planner.data.EnergyLevel.entries[(log.value - 1).coerceIn(0, 2)]))
                }
            }
        }
        item {
            Button(onClick = { vm.logEnergy(3) }, modifier = Modifier.fillMaxWidth()) {
                Text(s.logEnergyLabel)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
