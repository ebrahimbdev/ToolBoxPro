package com.toolbox.pro.planner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.planner.domain.TimeKit
import com.toolbox.pro.ui.components.ToolHeader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar with three views (PRD IA):
 *  - Day:   timeline of the selected day
 *  - Week:  Monday-first seven-day list for the selected day's week
 *  - Month: navigable grid; tapping a day jumps to the Day view
 * One occurrence feed covers the visible month ± a week.
 */
@Composable
fun CalendarScreen(
    vm: PlannerViewModel,
    onOpenTask: (Long) -> Unit
) {
    val s = LocalStrings.current
    var selected by remember { mutableStateOf(TimeKit.toLocalDate(System.currentTimeMillis())) }
    var monthAnchor by remember { mutableStateOf(selected.withDayOfMonth(1)) }
    var view by remember { mutableIntStateOf(2) } // start on Month

    val from = remember(monthAnchor) { TimeKit.toMillis(monthAnchor.minusDays(7)) }
    val to = remember(monthAnchor) { TimeKit.toMillis(monthAnchor.plusMonths(1).plusDays(7)) }
    val occurrences by remember(from, to) { vm.occurrences(from, to) }
        .collectAsState(initial = emptyList())

    fun itemsOn(date: LocalDate): List<com.toolbox.pro.planner.domain.TaskOccurrence> {
        val start = TimeKit.toMillis(date)
        val end = start + 24L * 60 * 60 * 1000
        return occurrences.filter { it.at in start until end }
    }

    val weekStartMillis = remember(selected) { TimeKit.startOfWeek(TimeKit.toMillis(selected)) }
    val weekDays = remember(weekStartMillis) {
        (0 until 7).map {
            TimeKit.toLocalDate(weekStartMillis + it * 24L * 60 * 60 * 1000)
        }
    }

    Scaffold(
        topBar = {
            ToolHeader(
                title = s.tabCalendar,
                icon = Icons.Filled.CalendarMonth,
                tabs = listOf(s.dayLabel, s.weekLabel, s.monthLabel),
                selectedTab = view,
                onTabSelected = { view = it }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (view) {
                // -------------------------------------------------------- Day
                0 -> {
                    item {
                        SectionTitle(fmtDate(TimeKit.toMillis(selected)))
                    }
                    val dayItems = itemsOn(selected)
                    if (dayItems.isEmpty()) {
                        item {
                            Text(
                                s.noEvents,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(dayItems.size) { i ->
                            val occ = dayItems[i]
                            Column {
                                Text(
                                    fmtTime(occ.at),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OccurrenceCard(occ = occ, s = s, onClick = { onOpenTask(occ.taskId) })
                            }
                        }
                    }
                }

                // ------------------------------------------------------- Week
                1 -> {
                    val lastDay = weekDays.last()
                    item {
                        SectionTitle(
                            "${fmtDate(TimeKit.toMillis(weekDays.first()))} — ${fmtDate(TimeKit.toMillis(lastDay))}"
                        )
                    }
                    weekDays.forEach { date ->
                        val dayItems = itemsOn(date)
                        item(key = "h${date}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    date.format(
                                        DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (date == TimeKit.toLocalDate(System.currentTimeMillis()))
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (date != selected) {
                                    Text(
                                        "${dayItems.size} ▸",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable {
                                            selected = date
                                            monthAnchor = date.withDayOfMonth(1)
                                            view = 0
                                        }
                                    )
                                }
                            }
                        }
                        if (dayItems.isEmpty()) {
                            item(key = "e${date}") {
                                Text(
                                    s.noEvents,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        } else {
                            items(dayItems.size, key = { "${date}-$it" }) { i ->
                                val occ = dayItems[i]
                                OccurrenceCard(occ = occ, s = s, onClick = { onOpenTask(occ.taskId) })
                            }
                        }
                    }
                }

                // ------------------------------------------------------ Month
                else -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { monthAnchor = monthAnchor.minusMonths(1) }) {
                                Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                            }
                            Text(
                                monthAnchor.format(
                                    DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { monthAnchor = monthAnchor.plusMonths(1) }) {
                                Icon(Icons.Filled.ChevronRight, contentDescription = null)
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val locale = Locale.getDefault()
                            for (i in 1..7) {
                                val dow = java.time.DayOfWeek.of(i)
                                Text(
                                    dow.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        val first = monthAnchor
                        val lead = first.dayOfWeek.value - 1 // Monday-first offset
                        val cells = (0 until 42).map { first.minusDays(lead.toLong()).plusDays(it.toLong()) }
                        Column {
                            cells.chunked(7).forEach { week ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    week.forEach { date ->
                                        DayCell(
                                            date = date,
                                            inMonth = date.month == monthAnchor.month,
                                            isSelected = date == selected,
                                            isToday = date == TimeKit.toLocalDate(System.currentTimeMillis()),
                                            hasItems = itemsOn(date).isNotEmpty(),
                                            onClick = {
                                                selected = date
                                                view = 0
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OccurrenceCard(
    occ: com.toolbox.pro.planner.domain.TaskOccurrence,
    s: com.toolbox.pro.core.localization.Strings,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    occ.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoChip("${occ.durationMin} ${s.minShort}")
                    if (occ.status == com.toolbox.pro.planner.data.TaskStatus.COMPLETED) {
                        InfoChip(s.statusCompleted, dotColor = Color(0xFF43A047))
                    }
                    if (occ.pinned) {
                        InfoChip(s.pinTask, dotColor = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    hasItems: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else if (hasItems) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        CircleShape
                    )
            )
        }
    }
}
