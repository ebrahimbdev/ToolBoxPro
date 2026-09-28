package com.toolbox.pro.planner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.pro.core.localization.Strings
import com.toolbox.pro.planner.data.EnergyLevel
import com.toolbox.pro.planner.data.Priority
import com.toolbox.pro.planner.data.TaskEntity
import com.toolbox.pro.planner.data.TaskStatus
import com.toolbox.pro.planner.domain.PlannerMode
import com.toolbox.pro.planner.domain.SuggestionReason
import com.toolbox.pro.planner.domain.TimeKit
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------- formatting
internal fun fmtTime(ms: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

internal fun fmtDate(ms: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))

internal fun fmtDateTime(ms: Long): String = "${fmtDate(ms)} · ${fmtTime(ms)}"

// ------------------------------------------------------------------- colors
internal fun priorityColor(p: Priority): Color = when (p) {
    Priority.LOW -> Color(0xFF9E9E9E)
    Priority.MEDIUM -> Color(0xFF42A5F5)
    Priority.HIGH -> Color(0xFFFF9800)
    Priority.CRITICAL -> Color(0xFFE53935)
}

internal fun energyColor(e: EnergyLevel): Color = when (e) {
    EnergyLevel.LOW -> Color(0xFF66BB6A)
    EnergyLevel.MEDIUM -> Color(0xFFFFB300)
    EnergyLevel.HIGH -> Color(0xFFEF5350)
}

// ------------------------------------------------------------------- labels
internal fun statusLabel(s: Strings, st: TaskStatus): String = when (st) {
    TaskStatus.INBOX -> s.statusInbox
    TaskStatus.PLANNED -> s.statusPlanned
    TaskStatus.TODAY -> s.statusToday
    TaskStatus.IN_PROGRESS -> s.statusInProgress
    TaskStatus.COMPLETED -> s.statusCompleted
    TaskStatus.SKIPPED -> s.statusSkipped
    TaskStatus.POSTPONED -> s.statusPostponed
    TaskStatus.CANCELLED -> s.statusCancelled
    TaskStatus.ARCHIVED -> s.statusArchived
}

internal fun priorityLabel(s: Strings, p: Priority): String = when (p) {
    Priority.LOW -> s.prioLow
    Priority.MEDIUM -> s.prioMedium
    Priority.HIGH -> s.prioHigh
    Priority.CRITICAL -> s.prioCritical
}

internal fun energyLabel(s: Strings, e: EnergyLevel): String = when (e) {
    EnergyLevel.LOW -> s.energyLow
    EnergyLevel.MEDIUM -> s.energyMedium
    EnergyLevel.HIGH -> s.energyHigh
}

internal fun reasonLabel(s: Strings, r: SuggestionReason): String = when (r) {
    SuggestionReason.OVERDUE -> s.reasonOverdue
    SuggestionReason.DUE_TODAY -> s.reasonDueToday
    SuggestionReason.HIGH_PRIORITY -> s.reasonHighPriority
    SuggestionReason.ENERGY_MATCH -> s.reasonEnergyMatch
    SuggestionReason.FITS_TIME -> s.reasonFitsTime
    SuggestionReason.QUICK_WIN -> s.reasonQuickWin
    SuggestionReason.PINNED -> s.reasonPinned
}

internal fun modeLabel(s: Strings, m: PlannerMode): String = when (m) {
    PlannerMode.FOCUS -> s.modeFocus
    PlannerMode.LIGHT -> s.modeLight
    PlannerMode.RECOVERY -> s.modeRecovery
}

// ------------------------------------------------------------------- pieces
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.padding(top = 12.dp, bottom = 6.dp)
    )
}

@Composable
internal fun InfoChip(text: String, dotColor: Color? = null, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier.size(7.dp).background(dotColor, CircleShape)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * One task row used across Today/Inbox/Program/Calendar lists: checkbox to
 * complete, title, meta chips (due time, duration, priority, energy, repeat).
 */
@Composable
internal fun TaskCard(
    task: TaskEntity,
    s: Strings,
    onClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    val done = task.status == TaskStatus.COMPLETED
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDone) {
                Icon(
                    imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                    contentDescription = s.markDone,
                    tint = if (done) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(5.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    val due = task.dueAt
                    if (due != null) {
                        val nowMs = System.currentTimeMillis()
                        val showTime = due > TimeKit.startOfDay(nowMs)
                        InfoChip(if (showTime) fmtTime(due) else fmtDate(due), dotColor = accent ?: MaterialTheme.colorScheme.primary)
                    }
                    InfoChip("${task.durationMin} ${s.minShort}")
                    InfoChip(priorityLabel(s, task.priority), dotColor = priorityColor(task.priority))
                    InfoChip(energyLabel(s, task.energy), dotColor = energyColor(task.energy))
                    if (task.repeatRule != "NONE") {
                        Icon(
                            imageVector = Icons.Filled.Repeat,
                            contentDescription = s.repeatLabel,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (task.pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = s.pinTask,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
