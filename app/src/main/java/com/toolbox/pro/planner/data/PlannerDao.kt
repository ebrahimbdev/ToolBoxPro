package com.toolbox.pro.planner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {

    // ---------------------------------------------------------------- programs
    @Query("SELECT * FROM programs ORDER BY position ASC, id ASC")
    fun observePrograms(): Flow<List<ProgramEntity>>

    @Query("SELECT * FROM programs WHERE id = :id")
    fun observeProgram(id: Long): Flow<ProgramEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity): Long

    @Update
    suspend fun updateProgram(program: ProgramEntity)

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun deleteProgram(id: Long)

    @Query("SELECT COUNT(*) FROM programs")
    suspend fun countPrograms(): Int

    // ------------------------------------------------------------------- goals
    @Query("SELECT * FROM goals ORDER BY position ASC, id ASC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE programId IS :programId ORDER BY position ASC, id ASC")
    fun observeGoalsForProgram(programId: Long?): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    // -------------------------------------------------------------- milestones
    @Query("SELECT * FROM milestones WHERE goalId = :goalId ORDER BY position ASC, id ASC")
    fun observeMilestones(goalId: Long): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones WHERE goalId IN (SELECT id FROM goals WHERE programId IS :programId)")
    fun observeMilestonesForProgram(programId: Long?): Flow<List<MilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: MilestoneEntity): Long

    @Update
    suspend fun updateMilestone(milestone: MilestoneEntity)

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun deleteMilestone(id: Long)

    // ------------------------------------------------------------------- tasks
    @Query("SELECT * FROM tasks WHERE status = 'INBOX' ORDER BY position ASC, id ASC")
    fun observeInbox(): Flow<List<TaskEntity>>

    /** Today view: everything still open whose due date reached today (includes overdue). */
    @Query(
        """
        SELECT * FROM tasks
        WHERE status IN ('PLANNED', 'TODAY', 'IN_PROGRESS', 'POSTPONED')
          AND dueAt IS NOT NULL AND dueAt < :dayEnd
        ORDER BY pinned DESC, dueAt ASC, position ASC, id ASC
        """
    )
    fun observeToday(dayEnd: Long): Flow<List<TaskEntity>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE status IN ('PLANNED', 'TODAY', 'IN_PROGRESS', 'POSTPONED')
          AND dueAt IS NOT NULL AND dueAt < :now
        ORDER BY dueAt ASC
        """
    )
    fun observeOverdue(now: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE pinned = 1 ORDER BY dueAt ASC, id ASC")
    fun observePinned(): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE reminderEnabled = 1 AND dueAt IS NOT NULL AND dueAt > :now " +
            "AND status IN ('INBOX','PLANNED','TODAY','IN_PROGRESS','POSTPONED') ORDER BY dueAt ASC"
    )
    fun observePendingReminders(now: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueAt IS NOT NULL AND dueAt < :to ORDER BY dueAt ASC")
    fun observeTasksDueBefore(to: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status IN ('INBOX', 'PLANNED', 'TODAY', 'IN_PROGRESS', 'POSTPONED')")
    suspend fun openTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE programId IS :programId ORDER BY position ASC, id ASC")
    suspend fun tasksForProgramOnce(programId: Long?): List<TaskEntity>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM tasks WHERE programId IS :programId")
    suspend fun nextPosition(programId: Long?): Int

    @Query("SELECT * FROM tasks WHERE programId = :programId ORDER BY position ASC, id ASC")
    fun observeTasksForProgram(programId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskEntity?

    @Query(
        """
        SELECT * FROM tasks
        WHERE title LIKE '%' || :query || '%'
           OR caption LIKE '%' || :query || '%'
           OR description LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY pinned DESC, dueAt ASC
        """
    )
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("UPDATE tasks SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = :status, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: TaskStatus, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET position = :position, updatedAt = :now WHERE id = :id")
    suspend fun setPosition(id: Long, position: Int, now: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE tasks SET status = 'COMPLETED', completedAt = :now, updatedAt = :now,
            actualDurationMin = :actualMin, completionEnergy = :energy,
            completionNote = :completionNote, learningNote = :learningNote, nextStep = :nextStep
        WHERE id = :id
        """
    )
    suspend fun completeTask(
        id: Long,
        now: Long,
        actualMin: Int? = null,
        energy: EnergyLevel? = null,
        completionNote: String = "",
        learningNote: String = "",
        nextStep: String = ""
    )

    @Query("UPDATE tasks SET status = 'POSTPONED', dueAt = :newDueAt, updatedAt = :now WHERE id = :id")
    suspend fun postponeTask(id: Long, newDueAt: Long, now: Long = System.currentTimeMillis())

    /** Keeps positions dense after reorder/remove. */
    @Query("UPDATE tasks SET position = position + 1 WHERE programId IS :programId AND position >= :fromPosition")
    suspend fun shiftPositions(programId: Long?, fromPosition: Int)

    // --------------------------------------------------------------- instances
    @Query("SELECT * FROM task_instances WHERE occurrenceAt >= :from AND occurrenceAt < :to ORDER BY occurrenceAt ASC")
    fun observeInstances(from: Long, to: Long): Flow<List<TaskInstanceEntity>>

    @Query("SELECT * FROM task_instances WHERE taskId = :taskId ORDER BY occurrenceAt ASC")
    fun observeInstancesForTask(taskId: Long): Flow<List<TaskInstanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstance(instance: TaskInstanceEntity): Long

    @Query("UPDATE task_instances SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun setInstanceStatus(id: Long, status: TaskStatus, completedAt: Long?)

    @Query("DELETE FROM task_instances WHERE taskId = :taskId")
    suspend fun deleteInstancesForTask(taskId: Long)

    @Query("DELETE FROM task_instances WHERE occurrenceAt < :before")
    suspend fun pruneInstancesBefore(before: Long)

    // ------------------------------------------------------------------- notes
    @Query("SELECT * FROM planner_notes ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM planner_notes WHERE taskId = :taskId ORDER BY updatedAt DESC")
    fun observeNotesForTask(taskId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM planner_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    // ----------------------------------------------------------------- reviews
    @Query("SELECT * FROM reviews WHERE type = :type ORDER BY createdAt DESC")
    fun observeReviews(type: ReviewType): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun observeAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity): Long

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReview(id: Long)

    // ------------------------------------------------------------- energy/time
    @Query("SELECT * FROM energy_logs WHERE loggedAt >= :from ORDER BY loggedAt DESC")
    fun observeEnergyLogs(from: Long): Flow<List<EnergyLogEntity>>

    @Insert
    suspend fun insertEnergyLog(log: EnergyLogEntity): Long

    @Query("SELECT * FROM time_logs WHERE startedAt >= :from ORDER BY startedAt DESC")
    fun observeTimeLogs(from: Long): Flow<List<TimeLogEntity>>

    @Insert
    suspend fun insertTimeLog(log: TimeLogEntity): Long

    // -------------------------------------------------------------------- kpis
    @Query("SELECT * FROM kpis ORDER BY id ASC")
    fun observeKpis(): Flow<List<KpiEntity>>

    @Query("SELECT * FROM kpis WHERE programId IS :programId ORDER BY id ASC")
    fun observeKpisForProgram(programId: Long?): Flow<List<KpiEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKpi(kpi: KpiEntity): Long

    @Update
    suspend fun updateKpi(kpi: KpiEntity)

    @Query("DELETE FROM kpis WHERE id = :id")
    suspend fun deleteKpi(id: Long)

    // -------------------------------------------------------------- analytics
    @Query("SELECT COUNT(*) FROM tasks")
    fun countTasks(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status IN ('INBOX', 'PLANNED', 'TODAY', 'IN_PROGRESS', 'POSTPONED')")
    fun countOpen(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status IN ('INBOX', 'PLANNED', 'TODAY', 'IN_PROGRESS', 'POSTPONED') AND dueAt IS NOT NULL AND dueAt < :now")
    fun countOverdue(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND completedAt >= :from")
    fun countCompletedSince(from: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND completedAt >= :from AND completedAt < :to")
    suspend fun countCompletedBetween(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE status IN ('SKIPPED', 'POSTPONED') AND updatedAt >= :from AND updatedAt < :to")
    suspend fun countSlipsBetween(from: Long, to: Long): Int

    /** North Star: days with at least one completed task in the window. */
    @Query("SELECT COUNT(DISTINCT date(completedAt / 1000, 'unixepoch', 'localtime')) FROM tasks WHERE status = 'COMPLETED' AND completedAt >= :from")
    fun countProgressDaysSince(from: Long): Flow<Int>

    @Query("SELECT AVG(value) FROM energy_logs WHERE loggedAt >= :from")
    fun avgEnergySince(from: Long): Flow<Float?>

    @Query("SELECT SUM(durationMin) FROM time_logs WHERE startedAt >= :from")
    fun sumMinutesSince(from: Long): Flow<Int?>

    @Query("SELECT SUM(durationMin) FROM time_logs WHERE taskId = :taskId")
    fun minutesForTask(taskId: Long): Flow<Int?>

    @Query(
        """
        SELECT * FROM tasks WHERE status = 'COMPLETED' AND completedAt >= :from AND completedAt < :to
        ORDER BY completedAt DESC
        """
    )
    suspend fun completedTasksBetween(from: Long, to: Long): List<TaskEntity>

    @Query(
        """
        SELECT COUNT(*) FROM tasks WHERE status = 'COMPLETED' AND programId = :programId
        AND completedAt >= :from
        """
    )
    suspend fun countProgramCompleted(programId: Long, from: Long): Int
}
