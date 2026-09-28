package com.toolbox.pro.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProgramStatus { ACTIVE, PAUSED, ARCHIVED }
enum class GoalStatus { ACTIVE, COMPLETED, CANCELLED }
enum class MilestoneStatus { PENDING, DONE, MISSED }
enum class TaskStatus { INBOX, PLANNED, TODAY, IN_PROGRESS, COMPLETED, SKIPPED, POSTPONED, CANCELLED, ARCHIVED }
enum class Priority { LOW, MEDIUM, HIGH, CRITICAL }
enum class EnergyLevel { LOW, MEDIUM, HIGH }
enum class ReviewType { DAILY, WEEKLY, MONTHLY }

/** Task statuses that still occupy the user's plan (not terminal). */
val TaskStatus.isActive: Boolean
    get() = this == TaskStatus.INBOX || this == TaskStatus.PLANNED || this == TaskStatus.TODAY ||
            this == TaskStatus.IN_PROGRESS || this == TaskStatus.POSTPONED

@Entity(
    tableName = "programs",
    indices = [Index("status")]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val color: Long = 0xFF6C63FF,
    val icon: String = "",
    val status: ProgramStatus = ProgramStatus.ACTIVE,
    val startAt: Long? = null,
    val endAt: Long? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "goals",
    indices = [Index("programId"), Index("status")]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long? = null,
    val title: String,
    val description: String = "",
    val deadline: Long? = null,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "milestones",
    indices = [Index("goalId")]
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val title: String,
    val dueAt: Long? = null,
    val status: MilestoneStatus = MilestoneStatus.PENDING,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tasks",
    indices = [Index("programId"), Index("dueAt"), Index("status"), Index("parentId")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long? = null,
    val parentId: Long? = null,
    val title: String,
    val description: String = "",
    /** Why / history / how-to / contacts — the context block from the PRD. */
    val caption: String = "",
    val status: TaskStatus = TaskStatus.INBOX,
    val priority: Priority = Priority.MEDIUM,
    val energy: EnergyLevel = EnergyLevel.MEDIUM,
    val durationMin: Int = 30,
    val dueAt: Long? = null,
    val position: Int = 0,
    val pinned: Boolean = false,
    /** NONE | DAILY | WEEKLY:1,3 | MONTHLY:5 | INTERVAL:7  (ISO days 1=Mon..7=Sun) */
    val repeatRule: String = "NONE",
    val reminderOffsetMin: Int? = null,
    val reminderEnabled: Boolean = false,
    val tags: String = "",
    val completionNote: String = "",
    val learningNote: String = "",
    val nextStep: String = "",
    val actualDurationMin: Int? = null,
    val completionEnergy: EnergyLevel? = null,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "task_instances",
    indices = [Index("taskId"), Index("occurrenceAt")]
)
data class TaskInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val occurrenceAt: Long,
    val status: TaskStatus = TaskStatus.PLANNED,
    val completedAt: Long? = null
)

@Entity(
    tableName = "planner_notes",
    indices = [Index("taskId"), Index("programId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val programId: Long? = null,
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reviews",
    indices = [Index("type")]
)
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ReviewType,
    val periodStart: Long,
    val periodEnd: Long,
    val answers: String = "{}",
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "energy_logs",
    indices = [Index("loggedAt")]
)
data class EnergyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** 1..5 */
    val value: Int,
    val loggedAt: Long = System.currentTimeMillis(),
    val context: String = ""
)

@Entity(
    tableName = "time_logs",
    indices = [Index("taskId")]
)
data class TimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val startedAt: Long,
    val endedAt: Long,
    val durationMin: Int
)

@Entity(
    tableName = "kpis",
    indices = [Index("programId")]
)
data class KpiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val programId: Long? = null,
    val name: String,
    val unit: String = "",
    val target: Float = 0f,
    val currentValue: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
