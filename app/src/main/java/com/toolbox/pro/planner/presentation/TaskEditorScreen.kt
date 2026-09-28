package com.toolbox.pro.planner.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.planner.data.EnergyLevel
import com.toolbox.pro.planner.data.Priority
import com.toolbox.pro.planner.data.TaskEntity
import com.toolbox.pro.planner.data.TaskStatus
import com.toolbox.pro.planner.domain.RepeatRule
import com.toolbox.pro.planner.domain.Recurrence
import com.toolbox.pro.planner.domain.TimeKit
import com.toolbox.pro.ui.components.ToolHeader
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val ACTIVE_STATUSES = listOf(
    TaskStatus.INBOX, TaskStatus.PLANNED, TaskStatus.TODAY, TaskStatus.IN_PROGRESS
)

/**
 * Full task editor: all PRD fields (title, context/caption, description,
 * program, status, date/time, duration, energy, priority, repeat, reminder,
 * tags) plus delete-with-confirmation for important tasks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    vm: PlannerViewModel,
    taskId: Long,
    onBack: () -> Unit
) {
    val s = LocalStrings.current
    var draft by remember { mutableStateOf<TaskEntity?>(null) }
    var loadedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(taskId) {
        if (taskId == 0L) {
            draft = TaskEntity(title = "")
            loadedOnce = true
        } else {
            val existing = vm.taskFlow(taskId).first()
            draft = existing ?: TaskEntity(title = "")
            loadedOnce = true
        }
    }

    val task = draft
    if (!loadedOnce || task == null) {
        Scaffold(contentWindowInsets = WindowInsets(0, 0, 0, 0)) { inner ->
            Column(modifier = Modifier.fillMaxSize().padding(inner).padding(16.dp)) {
                Text(s.checking, style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    val programs by vm.programs.collectAsState()
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }

    // POST_NOTIFICATIONS must be granted on API 33+ before a reminder can show.
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val notifPermission = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }
    fun requestNotifIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                appContext, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun commit(new: TaskEntity) { draft = new }
    fun save() {
        vm.saveTask(task)
        onBack()
    }

    Scaffold(
        topBar = {
            ToolHeader(
                title = if (taskId == 0L) s.newTask else s.editLabel,
                onBack = onBack,
                actions = {
                    if (taskId != 0L) {
                        IconButton(onClick = { vm.moveTask(taskId, up = true) }) {
                            Icon(Icons.Filled.ArrowUpward, contentDescription = s.moveUp)
                        }
                        IconButton(onClick = { vm.moveTask(taskId, up = false) }) {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = s.moveDown)
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = s.deleteLabel, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    IconButton(onClick = { save() }, enabled = task.title.isNotBlank()) {
                        Icon(Icons.Filled.Save, contentDescription = s.saveLabel)
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = task.title,
                onValueChange = { commit(task.copy(title = it)) },
                label = { Text(s.taskTitle) },
                placeholder = { Text(s.taskTitleHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = task.caption,
                onValueChange = { commit(task.copy(caption = it)) },
                label = { Text(s.caption) },
                placeholder = { Text(s.captionHint) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = task.description,
                onValueChange = { commit(task.copy(description = it)) },
                label = { Text(s.descriptionLabel) },
                placeholder = { Text(s.descriptionHint) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // ---- program
            FieldLabel(s.tabPrograms)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = task.programId == null,
                    onClick = { commit(task.copy(programId = null)) },
                    label = { Text("—") }
                )
                programs.filter { it.status == com.toolbox.pro.planner.data.ProgramStatus.ACTIVE }
                    .forEach { p ->
                        FilterChip(
                            selected = task.programId == p.id,
                            onClick = { commit(task.copy(programId = p.id)) },
                            label = { Text(p.name) }
                        )
                    }
            }

            // ---- status
            FieldLabel(s.statusLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ACTIVE_STATUSES.forEach { st ->
                    FilterChip(
                        selected = task.status == st,
                        onClick = { commit(task.copy(status = st)) },
                        label = { Text(statusLabel(s, st)) }
                    )
                }
            }

            // ---- due date + time
            FieldLabel(s.dueDate)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { showDate = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(task.dueAt?.let { fmtDate(it) } ?: s.noDate)
                }
                OutlinedButton(onClick = { if (task.dueAt != null) showTime = true }, enabled = task.dueAt != null) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(task.dueAt?.let { fmtTime(it) } ?: "--:--")
                }
                if (task.dueAt != null) {
                    TextButton(onClick = { commit(task.copy(dueAt = null)) }) { Text(s.cancel) }
                }
            }

            // ---- duration
            FieldLabel(s.durationLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(10, 15, 25, 30, 45, 60, 90, 120).forEach { m ->
                    FilterChip(
                        selected = task.durationMin == m,
                        onClick = { commit(task.copy(durationMin = m)) },
                        label = { Text("$m${s.minShort}") }
                    )
                }
            }

            // ---- priority + energy
            FieldLabel(s.priorityLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = task.priority == p,
                        onClick = { commit(task.copy(priority = p)) },
                        label = { Text(priorityLabel(s, p)) }
                    )
                }
            }
            FieldLabel(s.energyLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EnergyLevel.entries.forEach { e ->
                    FilterChip(
                        selected = task.energy == e,
                        onClick = { commit(task.copy(energy = e)) },
                        label = { Text(energyLabel(s, e)) }
                    )
                }
            }

            // ---- repeat
            FieldLabel(s.repeatLabel)
            val currentRule = Recurrence.parse(task.repeatRule)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = currentRule is RepeatRule.None,
                        onClick = { commit(task.copy(repeatRule = "NONE")) },
                        label = { Text(s.repeatNone) }
                    )
                    FilterChip(
                        selected = currentRule is RepeatRule.Daily,
                        onClick = { commit(task.copy(repeatRule = "DAILY")) },
                        label = { Text(s.repeatDaily) }
                    )
                    FilterChip(
                        selected = currentRule is RepeatRule.Weekly,
                        onClick = { commit(task.copy(repeatRule = "WEEKLY:1")) },
                        label = { Text(s.repeatWeekly) }
                    )
                    FilterChip(
                        selected = currentRule is RepeatRule.Monthly,
                        onClick = { commit(task.copy(repeatRule = "MONTHLY:1")) },
                        label = { Text(s.repeatMonthly) }
                    )
                    FilterChip(
                        selected = currentRule is RepeatRule.Interval,
                        onClick = { commit(task.copy(repeatRule = "INTERVAL:7")) },
                        label = { Text(s.repeatInterval) }
                    )
                }
                if (currentRule is RepeatRule.Weekly) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..7).forEach { iso ->
                            FilterChip(
                                selected = currentRule.days.contains(iso),
                                onClick = {
                                    val days = if (currentRule.days.contains(iso)) {
                                        (currentRule.days - iso).ifEmpty { setOf(1) }
                                    } else currentRule.days + iso
                                    commit(task.copy(repeatRule = "WEEKLY:" + days.sorted().joinToString(",")))
                                },
                                label = { Text(dayInitial(iso)) }
                            )
                        }
                    }
                }
            }

            // ---- reminder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldLabel(s.reminderLabel)
                Switch(
                    checked = task.reminderEnabled,
                    onCheckedChange = {
                        if (it) requestNotifIfNeeded()
                        commit(task.copy(reminderEnabled = it, reminderOffsetMin = if (it) (task.reminderOffsetMin ?: 10) else null))
                    }
                )
            }
            if (task.reminderEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { m ->
                        FilterChip(
                            selected = task.reminderOffsetMin == m,
                            onClick = { commit(task.copy(reminderOffsetMin = m)) },
                            label = { Text(if (m >= 60) "${m / 60}h" else "$m${s.minShort}") }
                        )
                    }
                }
            }

            // ---- tags
            OutlinedTextField(
                value = task.tags,
                onValueChange = { commit(task.copy(tags = it)) },
                label = { Text(s.tagsLabel) },
                placeholder = { Text(s.tagsHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (taskId != 0L) {
                // ---- time tracker (start/stop → time_logs)
                val totalMinutes by remember(taskId) { vm.minutesForTask(taskId) }
                    .collectAsState(initial = 0)
                var timerStart by remember { mutableStateOf<Long?>(null) }
                var tick by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(timerStart) {
                    if (timerStart != null) {
                        while (true) {
                            kotlinx.coroutines.delay(1000L)
                            tick = System.currentTimeMillis()
                        }
                    }
                }
                FieldLabel("${s.timeLoggedLabel}: ${totalMinutes ?: 0} ${s.minShort}")
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = {
                        val start = timerStart
                        if (start == null) {
                            timerStart = System.currentTimeMillis()
                        } else {
                            vm.logTime(taskId, start, System.currentTimeMillis())
                            timerStart = null
                        }
                    }) {
                        Icon(
                            if (timerStart == null) Icons.Filled.PlayArrow else Icons.Filled.Stop,
                            contentDescription = null,
                            modifier = Modifier.width(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (timerStart == null) s.timerStartBtn else s.timerStopBtn)
                    }
                    timerStart?.let { start ->
                        val elapsed = ((tick - start) / 1000L).coerceAtLeast(0L)
                        Text(
                            "%02d:%02d:%02d".format(elapsed / 3600, (elapsed % 3600) / 60, elapsed % 60),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // ---- notes
                val notes by remember(taskId) { vm.notesForTask(taskId) }
                    .collectAsState(initial = emptyList())
                FieldLabel(s.notesLabel)
                notes.forEach { note ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (note.title.isNotBlank()) {
                                    Text(
                                        note.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (note.content.isNotBlank()) {
                                    Text(
                                        note.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { vm.deleteNote(note.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = s.deleteLabel,
                                    modifier = Modifier.width(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showNoteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.width(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.newNote)
                }
            }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { save() },
                enabled = task.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(s.saveLabel) }
            Spacer(Modifier.height(24.dp))
        }
    }

    // ---- date picker (UTC-midnight → local midnight, keeps time of day)
    if (showDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = task.dueAt ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val sel = pickerState.selectedDateMillis
                    if (sel != null) {
                        val key = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date(sel))
                        val local = SimpleDateFormat("yyyyMMdd", Locale.US).apply {
                            timeZone = TimeZone.getDefault()
                        }.parse(key)?.time
                        if (local != null) {
                            val old = task.dueAt
                            val timePart = if (old != null) old - TimeKit.startOfDay(old) else 0L
                            commit(task.copy(dueAt = local + timePart))
                        }
                    }
                    showDate = false
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(s.cancel) } }
        ) { DatePicker(state = pickerState) }
    }

    // ---- time picker
    if (showTime) {
        val due = task.dueAt ?: System.currentTimeMillis()
        val dueCal = java.util.Calendar.getInstance().apply { timeInMillis = due }
        val timeState = rememberTimePickerState(
            initialHour = dueCal.get(java.util.Calendar.HOUR_OF_DAY),
            initialMinute = dueCal.get(java.util.Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val base = TimeKit.startOfDay(task.dueAt ?: System.currentTimeMillis())
                    commit(task.copy(dueAt = base + timeState.hour * 3600000L + timeState.minute * 60000L))
                    showTime = false
                }) { Text(s.saveLabel) }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text(s.cancel) } },
            text = { TimePicker(state = timeState) }
        )
    }

    // ---- note editor
    if (showNoteDialog && taskId != 0L) {
        NoteDialog(
            onDismiss = { showNoteDialog = false },
            onSave = { title, content ->
                vm.saveNote(taskId, title, content)
                showNoteDialog = false
            }
        )
    }

    // ---- delete confirmation (PRD: important-task deletion needs confirm)
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(s.deleteLabel) },
            text = { Text(task.title) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteTask(task.id)
                    onBack()
                }) { Text(s.deleteLabel) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(s.cancel) } }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun NoteDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val s = LocalStrings.current
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.newNote) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s.noteTitle) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(s.noteContent) },
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                enabled = title.isNotBlank() || content.isNotBlank()
            ) { Text(s.saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}

private fun dayInitial(iso: Int): String {
    // ISO: 1=Mon … 7=Sun
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    return letters[(iso - 1).coerceIn(0, 6)]
}
