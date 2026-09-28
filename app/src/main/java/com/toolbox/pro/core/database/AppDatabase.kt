package com.toolbox.pro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.toolbox.pro.planner.data.PlannerDao
import com.toolbox.pro.planner.data.PlannerTypeConverters
import com.toolbox.pro.planner.data.EnergyLogEntity
import com.toolbox.pro.planner.data.GoalEntity
import com.toolbox.pro.planner.data.KpiEntity
import com.toolbox.pro.planner.data.MilestoneEntity
import com.toolbox.pro.planner.data.NoteEntity
import com.toolbox.pro.planner.data.ProgramEntity
import com.toolbox.pro.planner.data.ReviewEntity
import com.toolbox.pro.planner.data.TaskEntity
import com.toolbox.pro.planner.data.TaskInstanceEntity
import com.toolbox.pro.planner.data.TimeLogEntity
import com.toolbox.pro.qr.data.QrHistoryDao
import com.toolbox.pro.qr.data.QrHistoryEntity

@Database(
    entities = [
        QrHistoryEntity::class,
        ProgramEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        TaskEntity::class,
        TaskInstanceEntity::class,
        NoteEntity::class,
        ReviewEntity::class,
        EnergyLogEntity::class,
        TimeLogEntity::class,
        KpiEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(PlannerTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qrHistoryDao(): QrHistoryDao
    abstract fun plannerDao(): PlannerDao

    companion object {
        /** v1 (QR history only) -> v2 (+ planner tables). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `programs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                        "`color` INTEGER NOT NULL, `icon` TEXT NOT NULL, " +
                        "`status` TEXT NOT NULL, `startAt` INTEGER, `endAt` INTEGER, " +
                        "`position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_programs_status` ON `programs` (`status`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`programId` INTEGER, `title` TEXT NOT NULL, " +
                        "`description` TEXT NOT NULL, `deadline` INTEGER, " +
                        "`status` TEXT NOT NULL, `position` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_programId` ON `goals` (`programId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goals_status` ON `goals` (`status`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `milestones` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`goalId` INTEGER NOT NULL, `title` TEXT NOT NULL, " +
                        "`dueAt` INTEGER, `status` TEXT NOT NULL, " +
                        "`position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_milestones_goalId` ON `milestones` (`goalId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tasks` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`programId` INTEGER, `parentId` INTEGER, " +
                        "`title` TEXT NOT NULL, `description` TEXT NOT NULL, " +
                        "`caption` TEXT NOT NULL, `status` TEXT NOT NULL, " +
                        "`priority` TEXT NOT NULL, `energy` TEXT NOT NULL, " +
                        "`durationMin` INTEGER NOT NULL, `dueAt` INTEGER, " +
                        "`position` INTEGER NOT NULL, `pinned` INTEGER NOT NULL, " +
                        "`repeatRule` TEXT NOT NULL, `reminderOffsetMin` INTEGER, " +
                        "`reminderEnabled` INTEGER NOT NULL, `tags` TEXT NOT NULL, " +
                        "`completionNote` TEXT NOT NULL, `learningNote` TEXT NOT NULL, " +
                        "`nextStep` TEXT NOT NULL, `actualDurationMin` INTEGER, " +
                        "`completionEnergy` TEXT, `completedAt` INTEGER, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_programId` ON `tasks` (`programId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_dueAt` ON `tasks` (`dueAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_status` ON `tasks` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_parentId` ON `tasks` (`parentId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `task_instances` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, `occurrenceAt` INTEGER NOT NULL, " +
                        "`status` TEXT NOT NULL, `completedAt` INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_instances_taskId` ON `task_instances` (`taskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_instances_occurrenceAt` ON `task_instances` (`occurrenceAt`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `planner_notes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER, `programId` INTEGER, " +
                        "`title` TEXT NOT NULL, `content` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_planner_notes_taskId` ON `planner_notes` (`taskId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_planner_notes_programId` ON `planner_notes` (`programId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reviews` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, `periodStart` INTEGER NOT NULL, " +
                        "`periodEnd` INTEGER NOT NULL, `answers` TEXT NOT NULL, " +
                        "`summary` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reviews_type` ON `reviews` (`type`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `energy_logs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`value` INTEGER NOT NULL, `loggedAt` INTEGER NOT NULL, " +
                        "`context` TEXT NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_energy_logs_loggedAt` ON `energy_logs` (`loggedAt`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `time_logs` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`taskId` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, " +
                        "`endedAt` INTEGER NOT NULL, `durationMin` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_time_logs_taskId` ON `time_logs` (`taskId`)")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kpis` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`programId` INTEGER, `name` TEXT NOT NULL, " +
                        "`unit` TEXT NOT NULL, `target` REAL NOT NULL, " +
                        "`currentValue` REAL NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_kpis_programId` ON `kpis` (`programId`)")
            }
        }
    }
}
