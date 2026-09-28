package com.toolbox.pro.planner.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.toolbox.pro.core.localization.LocalStrings
import com.toolbox.pro.planner.data.GoalEntity
import com.toolbox.pro.planner.data.KpiEntity
import com.toolbox.pro.planner.data.ProgramEntity
import com.toolbox.pro.planner.data.ProgramStatus
import com.toolbox.pro.planner.data.isActive
import com.toolbox.pro.ui.components.ToolHeader

private val PROGRAM_COLORS = listOf(
    0xFF6C63FF, 0xFF4ECDC4, 0xFFFF6B6B, 0xFFFFB75E, 0xFF43A047, 0xFFE91E63
)

// ====================================================================== list

@Composable
fun ProgramsScreen(
    vm: PlannerViewModel,
    onOpenProgram: (Long) -> Unit
) {
    val s = LocalStrings.current
    val programs by vm.programs.collectAsState()
    var showNew by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { ToolHeader(title = s.tabPrograms, icon = Icons.Filled.Folder) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNew = true }) {
                Icon(Icons.Filled.Add, contentDescription = s.newProgram)
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { inner ->
        if (programs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(s.emptyPrograms, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        s.newProgram,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(programs, key = { it.id }) { p ->
                    ProgramRow(program = p, onClick = { onOpenProgram(p.id) })
                }
            }
        }
    }

    if (showNew) {
        NewProgramDialog(
            onDismiss = { showNew = false },
            onCreate = { name, color ->
                vm.createProgram(name, color)
                showNew = false
            }
        )
    }
}

@Composable
private fun ProgramRow(program: ProgramEntity, onClick: () -> Unit) {
    val s = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color(program.color), RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    program.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (program.description.isNotBlank()) {
                    Text(
                        program.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val label = when (program.status) {
                ProgramStatus.ACTIVE -> s.statusActive
                ProgramStatus.PAUSED -> s.statusPaused
                ProgramStatus.ARCHIVED -> s.statusArchived
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun NewProgramDialog(onDismiss: () -> Unit, onCreate: (String, Long) -> Unit) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(PROGRAM_COLORS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(s.newProgram) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(s.programNameLabel) },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PROGRAM_COLORS.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .background(Color(c), RoundedCornerShape(8.dp))
                                .then(
                                    if (color == c) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .clickable { color = c }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, color) }, enabled = name.isNotBlank()) {
                Text(s.saveLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}

// ====================================================================== detail

@Composable
fun ProgramDetailScreen(
    vm: PlannerViewModel,
    programId: Long,
    onBack: () -> Unit,
    onOpenTask: (Long) -> Unit
) {
    val s = LocalStrings.current
    val programs by vm.programs.collectAsState()
    val current = programs.firstOrNull { it.id == programId }

    val goals by remember(programId) { vm.goalsForProgram(programId) }
        .collectAsState(initial = emptyList())
    val tasks by remember(programId) { vm.tasksForProgram(programId) }
        .collectAsState(initial = emptyList())
    val kpis by remember(programId) { vm.kpisForProgram(programId) }
        .collectAsState(initial = emptyList())

    var showDelete by remember { mutableStateOf(false) }
    var goalDialog by remember { mutableStateOf(false) }
    var kpiDialog by remember { mutableStateOf(false) }
    var taskDialog by remember { mutableStateOf(false) }
    var milestoneGoalId by remember { mutableStateOf<Long?>(null) }
    var kpiEdit by remember { mutableStateOf<KpiEntity?>(null) }

    Scaffold(
        topBar = {
            ToolHeader(
                title = current?.name ?: "…",
                subtitle = s.tabPrograms,
                onBack = onBack,
                actions = {
                    if (current != null) {
                        IconButton(onClick = { showDelete = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = s.deleteLabel,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { inner ->
        if (current == null) return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ---- status switcher
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProgramStatus.entries.forEach { st ->
                        androidx.compose.material3.FilterChip(
                            selected = current.status == st,
                            onClick = { vm.saveProgram(current.copy(status = st)) },
                            label = {
                                Text(
                                    when (st) {
                                        ProgramStatus.ACTIVE -> s.statusActive
                                        ProgramStatus.PAUSED -> s.statusPaused
                                        ProgramStatus.ARCHIVED -> s.statusArchived
                                    }
                                )
                            }
                        )
                    }
                }
            }
            if (current.description.isNotBlank()) {
                item {
                    Text(
                        current.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- goals
            item {
                SectionHeader(title = s.goalsLabel, actionLabel = s.newGoal) { goalDialog = true }
            }
            if (goals.isEmpty()) {
                item { Text(s.noResults, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(goals, key = { "g${it.id}" }) { goal ->
                GoalCard(goal = goal, vm = vm, onAddMilestone = { milestoneGoalId = goal.id })
            }

            // ---- KPIs
            item {
                SectionHeader(title = s.kpisLabel, actionLabel = s.newKpi) { kpiDialog = true }
            }
            if (kpis.isEmpty()) {
                item { Text(s.noResults, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(kpis, key = { "k${it.id}" }) { kpi ->
                KpiCard(kpi = kpi, onClick = { kpiEdit = kpi }, onDelete = { vm.deleteKpi(kpi.id) })
            }

            // ---- program tasks
            item {
                SectionHeader(title = s.tabToday, actionLabel = s.newTask) { taskDialog = true }
            }
            items(tasks.filter { it.status.isActive }, key = { "t${it.id}" }) { task ->
                TaskCard(
                    task = task,
                    s = s,
                    onClick = { onOpenTask(task.id) },
                    onDone = { vm.complete(task.id) }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    // ---- dialogs
    if (goalDialog) {
        TitleDialog(
            title = s.newGoal,
            label = s.goalTitleLabel,
            onDismiss = { goalDialog = false },
            onSave = { vm.addGoal(programId, it); goalDialog = false }
        )
    }
    if (kpiDialog) {
        KpiDialog(
            onDismiss = { kpiDialog = false },
            onSave = { name, unit, target, _ ->
                vm.addKpi(programId, name, unit, target)
                kpiDialog = false
            }
        )
    }
    if (taskDialog) {
        TitleDialog(
            title = s.newTask,
            label = s.taskTitle,
            onDismiss = { taskDialog = false },
            onSave = { vm.addProgramTask(programId, it); taskDialog = false }
        )
    }
    if (milestoneGoalId != null) {
        val gid = milestoneGoalId ?: 0L
        TitleDialog(
            title = s.newMilestone,
            label = s.milestoneTitleLabel,
            onDismiss = { milestoneGoalId = null },
            onSave = { vm.addMilestone(gid, it); milestoneGoalId = null }
        )
    }
    kpiEdit?.let { k ->
        KpiDialog(
            initial = k,
            onDismiss = { kpiEdit = null },
            onSave = { name, unit, target, current ->
                vm.updateKpi(k.copy(name = name, unit = unit, target = target, currentValue = current))
                kpiEdit = null
            }
        )
    }
    if (showDelete && current != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(s.deleteLabel) },
            text = { Text(current.name) },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.deleteProgram(current.id)
                    onBack()
                }) { Text(s.deleteLabel) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(s.cancel) } }
        )
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, vm: PlannerViewModel, onAddMilestone: () -> Unit) {
    val s = LocalStrings.current
    val milestones by remember(goal.id) { vm.milestonesForGoal(goal.id) }
        .collectAsState(initial = emptyList())

    Card(shape = RoundedCornerShape(14.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    goal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.deleteGoal(goal.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = s.deleteLabel, tint = MaterialTheme.colorScheme.error)
                }
            }
            goal.deadline?.let {
                InfoChip("${s.deadlineLabel}: ${fmtDate(it)}")
            }
            milestones.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = m.status == com.toolbox.pro.planner.data.MilestoneStatus.DONE,
                        onCheckedChange = { vm.toggleMilestone(m) }
                    )
                    Text(
                        m.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { vm.deleteMilestone(m.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = s.deleteLabel,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            TextButton(onClick = onAddMilestone) { Text("+ ${s.newMilestone}") }
        }
    }
}

@Composable
private fun KpiCard(kpi: KpiEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val s = LocalStrings.current
    val progress = if (kpi.target > 0f) (kpi.currentValue / kpi.target).coerceIn(0f, 1f) else 0f
    Card(
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    kpi.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${kpi.currentValue.takeIf { it % 1f != 0f } ?: kpi.currentValue.toInt()} / " +
                        "${kpi.target.takeIf { it % 1f != 0f } ?: kpi.target.toInt()} ${kpi.unit}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = s.deleteLabel, tint = MaterialTheme.colorScheme.error)
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            if (progress >= 1f) {
                Text(s.statusCompleted, style = MaterialTheme.typography.labelSmall, color = Color(0xFF43A047))
            }
        }
    }
}

@Composable
private fun TitleDialog(title: String, label: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val s = LocalStrings.current
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(s.saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}

@Composable
private fun KpiDialog(
    initial: KpiEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Float, Float) -> Unit
) {
    val s = LocalStrings.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var target by remember { mutableStateOf(initial?.target?.toString() ?: "") }
    var value by remember { mutableStateOf(initial?.currentValue?.toString() ?: "0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) s.newKpi else s.editLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(s.kpiNameLabel) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        unit, { unit = it },
                        label = { Text(s.unitLabel) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        target, { target = it },
                        label = { Text(s.targetLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (initial != null) {
                    OutlinedTextField(
                        value, { value = it },
                        label = { Text(s.currentLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        name,
                        unit,
                        target.toFloatOrNull() ?: 0f,
                        value.toFloatOrNull() ?: 0f
                    )
                },
                enabled = name.isNotBlank()
            ) { Text(s.saveLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}
