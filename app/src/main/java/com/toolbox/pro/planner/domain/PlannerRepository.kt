package com.toolbox.pro.planner.domain

import com.toolbox.pro.core.api.AppJson
import com.toolbox.pro.planner.data.EnergyLogEntity
import com.toolbox.pro.planner.data.EnergyLevel
import com.toolbox.pro.planner.data.GoalEntity
import com.toolbox.pro.planner.data.KpiEntity
import com.toolbox.pro.planner.data.MilestoneEntity
import com.toolbox.pro.planner.data.NoteEntity
import com.toolbox.pro.planner.data.PlannerDao
import com.toolbox.pro.planner.data.Priority
import com.toolbox.pro.planner.data.ProgramEntity
import com.toolbox.pro.planner.data.ReviewEntity
import com.toolbox.pro.planner.data.ReviewType
import com.toolbox.pro.planner.data.TaskEntity
import com.toolbox.pro.planner.data.TaskInstanceEntity
import com.toolbox.pro.planner.data.TaskStatus
import com.toolbox.pro.planner.data.TimeLogEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** One task appearance on the calendar (one-off or a recurring occurrence). */
data class TaskOccurrence(
    val taskId: Long,
    val instanceId: Long?,
    val title: String,
    val at: Long,
    val durationMin: Int,
    val status: TaskStatus,
    val programId: Long?,
    val pinned: Boolean
)

@Singleton
class PlannerRepository @Inject constructor(
    private val dao: PlannerDao
) {
    private val json = AppJson.json
    private val mapSerializer = kotlinx.serialization.builtins.MapSerializer(
        kotlinx.serialization.serializer<String>(),
        kotlinx.serialization.serializer<String>()
    )

    // ------------------------------------------------------------------ flows
    fun programs(): Flow<List<ProgramEntity>> = dao.observePrograms()
    fun program(id: Long): Flow<ProgramEntity?> = dao.observeProgram(id)
    fun today(dayEnd: Long): Flow<List<TaskEntity>> = dao.observeToday(dayEnd)
    fun inbox(): Flow<List<TaskEntity>> = dao.observeInbox()
    fun overdue(now: Long): Flow<List<TaskEntity>> = dao.observeOverdue(now)
    fun pinned(): Flow<List<TaskEntity>> = dao.observePinned()
    fun pendingReminders(now: Long): Flow<List<TaskEntity>> = dao.observePendingReminders(now)
    fun tasksForProgram(programId: Long): Flow<List<TaskEntity>> = dao.observeTasksForProgram(programId)
    fun task(id: Long): Flow<TaskEntity?> = dao.observeTask(id)
    fun search(query: String): Flow<List<TaskEntity>> = dao.searchTasks(query)
    fun notesForTask(taskId: Long): Flow<List<NoteEntity>> = dao.observeNotesForTask(taskId)
    fun goalsForProgram(programId: Long?): Flow<List<GoalEntity>> = dao.observeGoalsForProgram(programId)
    fun milestonesForGoal(goalId: Long): Flow<List<MilestoneEntity>> = dao.observeMilestones(goalId)
    fun kpisForProgram(programId: Long?): Flow<List<KpiEntity>> = dao.observeKpisForProgram(programId)
    fun reviews(type: ReviewType): Flow<List<ReviewEntity>> = dao.observeReviews(type)
    fun energyLogs(from: Long): Flow<List<EnergyLogEntity>> = dao.observeEnergyLogs(from)
    fun timeLogs(from: Long): Flow<List<TimeLogEntity>> = dao.observeTimeLogs(from)
    fun minutesForTask(taskId: Long): Flow<Int?> = dao.minutesForTask(taskId)

    fun countTasks(): Flow<Int> = dao.countTasks()
    fun countOpen(): Flow<Int> = dao.countOpen()
    fun countOverdue(now: Long): Flow<Int> = dao.countOverdue(now)
    fun countCompletedSince(from: Long): Flow<Int> = dao.countCompletedSince(from)
    fun progressDaysSince(from: Long): Flow<Int> = dao.countProgressDaysSince(from)
    fun avgEnergySince(from: Long): Flow<Float?> = dao.avgEnergySince(from)
    fun sumMinutesSince(from: Long): Flow<Int?> = dao.sumMinutesSince(from)

    /** Calendar feed: masters expanded by recurrence, merged with instance states. */
    fun occurrencesBetween(from: Long, to: Long): Flow<List<TaskOccurrence>> =
        kotlinx.coroutines.flow.combine(
            dao.observeTasksDueBefore(to),
            dao.observeInstances(from, to)
        ) { tasks, instances ->
            val instanceByKey = instances.associateBy { it.taskId to it.occurrenceAt }
            val out = mutableListOf<TaskOccurrence>()
            for (t in tasks) {
                val due = t.dueAt ?: continue
                val rule = Recurrence.parse(t.repeatRule)
                val times = if (rule is RepeatRule.None) {
                    if (due >= from && due < to) listOf(due) else emptyList()
                } else {
                    Recurrence.occurrencesBetween(rule, due, from, to)
                        .ifEmpty { if (due >= from && due < to) listOf(due) else emptyList() }
                }
                for (at in times) {
                    val inst = instanceByKey[t.id to at]
                    val status = inst?.status ?: t.status
                    if (status == TaskStatus.COMPLETED || status == TaskStatus.CANCELLED) {
                        if (inst != null) {
                            out += TaskOccurrence(t.id, inst.id, t.title, at, t.durationMin, status, t.programId, t.pinned)
                        }
                        continue
                    }
                    out += TaskOccurrence(t.id, inst?.id, t.title, at, t.durationMin, status, t.programId, t.pinned)
                }
            }
            out.sortedBy { it.at }
        }

    // ------------------------------------------------------------------ tasks
    suspend fun createTask(
        title: String,
        programId: Long? = null,
        status: TaskStatus = TaskStatus.INBOX,
        dueAt: Long? = null,
        durationMin: Int = 30,
        energy: EnergyLevel = EnergyLevel.MEDIUM,
        priority: Priority = Priority.MEDIUM,
        caption: String = "",
        description: String = "",
        repeatRule: String = "NONE",
        reminderEnabled: Boolean = false,
        reminderOffsetMin: Int? = null
    ): Long {
        val position = dao.nextPosition(programId)
        return dao.insertTask(
            TaskEntity(
                title = title.trim(),
                programId = programId,
                status = if (dueAt != null && status == TaskStatus.INBOX) TaskStatus.PLANNED else status,
                dueAt = dueAt,
                durationMin = durationMin,
                energy = energy,
                priority = priority,
                caption = caption,
                description = description,
                repeatRule = repeatRule,
                reminderEnabled = reminderEnabled,
                reminderOffsetMin = reminderOffsetMin,
                position = position
            )
        )
    }

    suspend fun saveTask(task: TaskEntity): Long = if (task.id == 0L) {
        dao.insertTask(task.copy(position = dao.nextPosition(task.programId)))
    } else {
        dao.updateTask(task)
        task.id
    }

    suspend fun deleteTask(id: Long) {
        dao.deleteInstancesForTask(id)
        dao.deleteTask(id)
    }

    suspend fun togglePin(id: Long) {
        val t = dao.getTask(id) ?: return
        dao.setPinned(id, !t.pinned)
    }

    suspend fun moveTask(id: Long, up: Boolean) {
        val t = dao.getTask(id) ?: return
        val siblings = dao.tasksForProgramOnce(t.programId).filter { it.id != id }
        val ordered = if (up) siblings.sortedBy { it.position } else siblings.sortedByDescending { it.position }
        val neighbor = ordered.firstOrNull { (if (up) it.position < t.position else it.position > t.position) } ?: return
        val oldPos = t.position
        dao.setPosition(t.id, neighbor.position)
        dao.setPosition(neighbor.id, oldPos)
    }

    suspend fun setStatus(id: Long, status: TaskStatus) = dao.setStatus(id, status)

    /**
     * Completes a task; recurring tasks keep the master alive and instead get
     * a completed TaskInstance for this occurrence plus a moved due date.
     */
    suspend fun complete(
        taskId: Long,
        actualMin: Int? = null,
        energy: EnergyLevel? = null,
        completionNote: String = "",
        learningNote: String = "",
        nextStep: String = ""
    ) {
        val t = dao.getTask(taskId) ?: return
        val now = System.currentTimeMillis()
        val rule = Recurrence.parse(t.repeatRule)
        if (rule !is RepeatRule.None) {
            val anchor = t.dueAt ?: now
            dao.insertInstance(
                TaskInstanceEntity(taskId = t.id, occurrenceAt = anchor, status = TaskStatus.COMPLETED, completedAt = now)
            )
            val next = Recurrence.nextOccurrence(rule, anchor, now)
            if (next != null) {
                val todayEnd = TimeKit.endOfDay(now)
                dao.updateTask(
                    t.copy(
                        dueAt = next,
                        status = if (next <= todayEnd) TaskStatus.TODAY else TaskStatus.PLANNED,
                        actualDurationMin = actualMin,
                        completionEnergy = energy,
                        completionNote = completionNote,
                        learningNote = learningNote,
                        nextStep = nextStep,
                        updatedAt = now
                    )
                )
            } else {
                dao.completeTask(t.id, now, actualMin, energy, completionNote, learningNote, nextStep)
            }
        } else {
            dao.completeTask(taskId, now, actualMin, energy, completionNote, learningNote, nextStep)
        }
    }

    suspend fun postpone(taskId: Long, newDueAt: Long) = dao.postponeTask(taskId, newDueAt)

    suspend fun snoozeToTomorrow(taskId: Long) {
        val now = System.currentTimeMillis()
        postpone(taskId, TimeKit.startOfDay(now) + 24L * 60 * 60 * 1000)
    }

    // ----------------------------------------------------- programs/goals/kpi
    suspend fun saveProgram(program: ProgramEntity): Long = if (program.id == 0L) {
        dao.insertProgram(program.copy(position = dao.countPrograms()))
    } else {
        dao.updateProgram(program)
        program.id
    }

    suspend fun deleteProgram(id: Long) = dao.deleteProgram(id)
    suspend fun saveGoal(goal: GoalEntity): Long = dao.insertGoal(goal)
    suspend fun deleteGoal(id: Long) = dao.deleteGoal(id)
    suspend fun saveMilestone(milestone: MilestoneEntity): Long = dao.insertMilestone(milestone)
    suspend fun deleteMilestone(id: Long) = dao.deleteMilestone(id)
    suspend fun saveKpi(kpi: KpiEntity): Long = dao.insertKpi(kpi)
    suspend fun deleteKpi(id: Long) = dao.deleteKpi(id)
    suspend fun saveNote(note: NoteEntity): Long = dao.insertNote(note)
    suspend fun deleteNote(id: Long) = dao.deleteNote(id)

    // ---------------------------------------------------------------- reviews
    /** Saves a review with its answers and a metric summary for the period. */
    suspend fun saveReview(type: ReviewType, now: Long, answers: Map<String, String>): Long {
        val period = ReviewEngine.periodFor(type, now)
        val done = dao.countCompletedBetween(period.start, period.end)
        val slips = dao.countSlipsBetween(period.start, period.end)
        val summary = json.encodeToString(
            mapSerializer,
            mapOf(
                "done" to done.toString(),
                "slips" to slips.toString()
            )
        )
        return dao.insertReview(
            ReviewEntity(
                type = type,
                periodStart = period.start,
                periodEnd = period.end,
                answers = json.encodeToString(mapSerializer, answers),
                summary = summary
            )
        )
    }

    fun decodeAnswers(raw: String): Map<String, String> = runCatching {
        json.decodeFromString(mapSerializer, raw)
    }.getOrDefault(emptyMap())

    fun decodeSummary(raw: String): Map<String, String> = runCatching {
        json.decodeFromString(mapSerializer, raw)
    }.getOrDefault(emptyMap())

    // ------------------------------------------------------------------- logs
    suspend fun logEnergy(value: Int, context: String = "") =
        dao.insertEnergyLog(EnergyLogEntity(value = value.coerceIn(1, 5), context = context))

    suspend fun logTime(taskId: Long, startedAt: Long, endedAt: Long) =
        dao.insertTimeLog(
            TimeLogEntity(
                taskId = taskId,
                startedAt = startedAt,
                endedAt = endedAt,
                durationMin = ((endedAt - startedAt) / 60000L).toInt().coerceAtLeast(1)
            )
        )
}
