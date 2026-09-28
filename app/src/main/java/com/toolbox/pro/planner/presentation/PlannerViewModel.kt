package com.toolbox.pro.planner.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.pro.planner.data.EnergyLevel
import com.toolbox.pro.planner.data.GoalEntity
import com.toolbox.pro.planner.data.KpiEntity
import com.toolbox.pro.planner.data.MilestoneEntity
import com.toolbox.pro.planner.data.MilestoneStatus
import com.toolbox.pro.planner.data.ProgramEntity
import com.toolbox.pro.planner.data.ReviewType
import com.toolbox.pro.planner.data.TaskEntity
import com.toolbox.pro.planner.data.TaskStatus
import com.toolbox.pro.planner.data.isActive
import com.toolbox.pro.planner.domain.PlannerRepository
import com.toolbox.pro.planner.domain.TaskOccurrence
import com.toolbox.pro.planner.domain.TimeKit
import com.toolbox.pro.planner.notifications.TaskReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single ViewModel for the whole planner module: exposes the long-lived Room
 * flows the screens collect and funnels every mutation through the repository.
 */
@HiltViewModel
class PlannerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repo: PlannerRepository
) : ViewModel() {

    init {
        // Re-arm reminders for tasks that were scheduled on a previous launch.
        viewModelScope.launch {
            runCatching {
                repo.pendingReminders(System.currentTimeMillis()).first()
                    .forEach { TaskReminderScheduler.schedule(context, it) }
            }
        }
    }

    private val now = System.currentTimeMillis()
    private val dayEnd = TimeKit.endOfDay(now)
    private val opts = SharingStarted.WhileSubscribed(5_000)

    val programs: StateFlow<List<ProgramEntity>> =
        repo.programs().stateIn(viewModelScope, opts, emptyList())
    val inbox: StateFlow<List<TaskEntity>> =
        repo.inbox().stateIn(viewModelScope, opts, emptyList())
    val today: StateFlow<List<TaskEntity>> =
        repo.today(dayEnd).stateIn(viewModelScope, opts, emptyList())
    val overdue: StateFlow<List<TaskEntity>> =
        repo.overdue(now).stateIn(viewModelScope, opts, emptyList())
    val pinned: StateFlow<List<TaskEntity>> =
        repo.pinned().stateIn(viewModelScope, opts, emptyList())

    /** Everything the Smart Daily Planner engine may pick from. */
    val openPool: StateFlow<List<TaskEntity>> =
        combine(inbox, today, overdue) { a, b, c ->
            (a + b + c).distinctBy { it.id }.filter { it.status.isActive }
        }.stateIn(viewModelScope, opts, emptyList())

    val dailyReviews: StateFlow<List<com.toolbox.pro.planner.data.ReviewEntity>> =
        repo.reviews(ReviewType.DAILY).stateIn(viewModelScope, opts, emptyList())
    val weeklyReviews: StateFlow<List<com.toolbox.pro.planner.data.ReviewEntity>> =
        repo.reviews(ReviewType.WEEKLY).stateIn(viewModelScope, opts, emptyList())
    val monthlyReviews: StateFlow<List<com.toolbox.pro.planner.data.ReviewEntity>> =
        repo.reviews(ReviewType.MONTHLY).stateIn(viewModelScope, opts, emptyList())

    val countOpen: StateFlow<Int> = repo.countOpen().stateIn(viewModelScope, opts, 0)
    val countOverdue: StateFlow<Int> = repo.countOverdue(now).stateIn(viewModelScope, opts, 0)
    val completedWeek: StateFlow<Int> =
        repo.countCompletedSince(now - 7L * DAY).stateIn(viewModelScope, opts, 0)
    val progressDays: StateFlow<Int> =
        repo.progressDaysSince(now - 30L * DAY).stateIn(viewModelScope, opts, 0)
    val avgEnergy: StateFlow<Float?> =
        repo.avgEnergySince(now - 30L * DAY).stateIn(viewModelScope, opts, null)
    val minutesWeek: StateFlow<Int> =
        repo.sumMinutesSince(now - 7L * DAY).map { it ?: 0 }.stateIn(viewModelScope, opts, 0)
    val energyLogs: StateFlow<List<com.toolbox.pro.planner.data.EnergyLogEntity>> =
        repo.energyLogs(now - 30L * DAY).stateIn(viewModelScope, opts, emptyList())

    // ------------------------------------------------------------------ reads
    fun taskFlow(id: Long): Flow<TaskEntity?> = repo.task(id)
    fun tasksForProgram(programId: Long): Flow<List<TaskEntity>> = repo.tasksForProgram(programId)
    fun goalsForProgram(programId: Long?): Flow<List<GoalEntity>> = repo.goalsForProgram(programId)
    fun milestonesForGoal(goalId: Long): Flow<List<MilestoneEntity>> = repo.milestonesForGoal(goalId)
    fun kpisForProgram(programId: Long?): Flow<List<KpiEntity>> = repo.kpisForProgram(programId)
    fun occurrences(from: Long, to: Long): Flow<List<TaskOccurrence>> = repo.occurrencesBetween(from, to)

    fun reviewsFlow(type: ReviewType): Flow<List<com.toolbox.pro.planner.data.ReviewEntity>> = repo.reviews(type)

    fun decodeAnswers(raw: String): Map<String, String> = repo.decodeAnswers(raw)
    fun decodeSummary(raw: String): Map<String, String> = repo.decodeSummary(raw)

    fun notesForTask(taskId: Long): Flow<List<com.toolbox.pro.planner.data.NoteEntity>> =
        repo.notesForTask(taskId)

    fun minutesForTask(taskId: Long): Flow<Int?> = repo.minutesForTask(taskId)

    // ------------------------------------------------------------------ tasks
    fun quickAdd(title: String) = viewModelScope.launch {
        val t = title.trim()
        if (t.isNotEmpty()) repo.createTask(title = t, status = TaskStatus.INBOX)
    }

    fun addProgramTask(programId: Long, title: String) = viewModelScope.launch {
        val t = title.trim()
        if (t.isNotEmpty()) repo.createTask(title = t, programId = programId, status = TaskStatus.PLANNED)
    }

    fun saveTask(task: TaskEntity) = viewModelScope.launch {
        val id = repo.saveTask(task)
        syncReminder(id)
    }

    fun deleteTask(id: Long) = viewModelScope.launch {
        TaskReminderScheduler.cancel(context, id)
        repo.deleteTask(id)
    }

    private suspend fun syncReminder(taskId: Long) {
        val t = repo.task(taskId).first()
        if (t == null) TaskReminderScheduler.cancel(context, taskId)
        else TaskReminderScheduler.schedule(context, t)
    }

    fun complete(
        id: Long,
        actualMin: Int? = null,
        energy: EnergyLevel? = null,
        completionNote: String = "",
        learningNote: String = "",
        nextStep: String = ""
    ) = viewModelScope.launch {
        repo.complete(id, actualMin, energy, completionNote, learningNote, nextStep)
        syncReminder(id)
    }

    fun postpone(id: Long, newDueAt: Long) = viewModelScope.launch {
        repo.postpone(id, newDueAt)
        syncReminder(id)
    }

    fun snooze(id: Long) = viewModelScope.launch {
        repo.snoozeToTomorrow(id)
        syncReminder(id)
    }
    fun togglePin(id: Long) = viewModelScope.launch { repo.togglePin(id) }
    fun moveTask(id: Long, up: Boolean) = viewModelScope.launch { repo.moveTask(id, up) }
    fun setStatus(id: Long, status: TaskStatus) = viewModelScope.launch { repo.setStatus(id, status) }

    // ------------------------------------------------------------------ notes
    fun saveNote(taskId: Long, title: String, content: String) = viewModelScope.launch {
        val t = title.trim()
        if (t.isNotEmpty() || content.isNotBlank()) {
            repo.saveNote(
                com.toolbox.pro.planner.data.NoteEntity(taskId = taskId, title = t, content = content.trim())
            )
        }
    }

    fun deleteNote(id: Long) = viewModelScope.launch { repo.deleteNote(id) }

    // ---------------------------------------------------------- time tracking
    fun logTime(taskId: Long, startedAt: Long, endedAt: Long) = viewModelScope.launch {
        repo.logTime(taskId, startedAt, endedAt)
    }

    // -------------------------------------------------------- programs/goals
    fun createProgram(name: String, color: Long) = viewModelScope.launch {
        val n = name.trim()
        if (n.isNotEmpty()) repo.saveProgram(ProgramEntity(name = n, color = color))
    }

    fun saveProgram(program: ProgramEntity) = viewModelScope.launch { repo.saveProgram(program) }
    fun deleteProgram(id: Long) = viewModelScope.launch { repo.deleteProgram(id) }

    fun addGoal(programId: Long, title: String) = viewModelScope.launch {
        val t = title.trim()
        if (t.isNotEmpty()) repo.saveGoal(GoalEntity(programId = programId, title = t))
    }

    fun deleteGoal(id: Long) = viewModelScope.launch { repo.deleteGoal(id) }

    fun addMilestone(goalId: Long, title: String) = viewModelScope.launch {
        val t = title.trim()
        if (t.isNotEmpty()) repo.saveMilestone(MilestoneEntity(goalId = goalId, title = t))
    }

    fun deleteMilestone(id: Long) = viewModelScope.launch { repo.deleteMilestone(id) }

    fun toggleMilestone(m: MilestoneEntity) = viewModelScope.launch {
        repo.saveMilestone(
            m.copy(status = if (m.status == MilestoneStatus.DONE) MilestoneStatus.PENDING else MilestoneStatus.DONE)
        )
    }

    fun addKpi(programId: Long, name: String, unit: String, target: Float) = viewModelScope.launch {
        val n = name.trim()
        if (n.isNotEmpty()) repo.saveKpi(KpiEntity(programId = programId, name = n, unit = unit.trim(), target = target))
    }

    fun updateKpi(kpi: KpiEntity) = viewModelScope.launch { repo.saveKpi(kpi) }
    fun deleteKpi(id: Long) = viewModelScope.launch { repo.deleteKpi(id) }

    // --------------------------------------------------------------- reviews
    fun saveReview(type: ReviewType, answers: Map<String, String>) = viewModelScope.launch {
        repo.saveReview(type, System.currentTimeMillis(), answers)
    }

    // ------------------------------------------------------------------ logs
    fun logEnergy(value: Int, context: String = "") = viewModelScope.launch {
        repo.logEnergy(value, context)
    }

    private companion object {
        const val DAY = 24L * 60 * 60 * 1000
    }
}
