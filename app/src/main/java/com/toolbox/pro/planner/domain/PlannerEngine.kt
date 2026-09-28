package com.toolbox.pro.planner.domain

import com.toolbox.pro.planner.data.EnergyLevel
import com.toolbox.pro.planner.data.Priority
import com.toolbox.pro.planner.data.TaskEntity
import com.toolbox.pro.planner.data.TaskStatus

enum class PlannerMode { FOCUS, LIGHT, RECOVERY }

enum class SuggestionReason { OVERDUE, DUE_TODAY, HIGH_PRIORITY, ENERGY_MATCH, FITS_TIME, QUICK_WIN, PINNED }

data class PlannerInput(
    val openTasks: List<TaskEntity>,
    val freeMinutes: Int,
    val energy: EnergyLevel,
    val now: Long
)

data class PlannerSuggestion(
    val task: TaskEntity,
    val mode: PlannerMode,
    val reason: SuggestionReason,
    val score: Int
)

data class PlannerPlan(
    val focus: List<PlannerSuggestion>,
    val light: List<PlannerSuggestion>,
    val recovery: List<PlannerSuggestion>
) {
    val isEmpty: Boolean get() = focus.isEmpty() && light.isEmpty() && recovery.isEmpty()
}

/**
 * Smart Daily Planner (rules engine — Phase 2, no AI): ranks open tasks by
 * overdue/deadline/priority/energy/fit and returns three selectable buckets.
 * The user always picks; nothing is imposed.
 */
object PlannerEngine {

    fun plan(input: PlannerInput): PlannerPlan {
        val dayStart = TimeKit.startOfDay(input.now)
        val dayEnd = TimeKit.endOfDay(input.now)
        val now = input.now

        val dated = input.openTasks.filter {
            it.dueAt != null && it.dueAt <= dayEnd &&
                it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED &&
                it.status != TaskStatus.ARCHIVED && it.status != TaskStatus.SKIPPED
        }
        val inbox = input.openTasks.filter { it.dueAt == null && it.status == TaskStatus.INBOX }

        fun score(t: TaskEntity): Int {
            var s = 0
            val due = t.dueAt
            if (due != null && due < dayStart) s += 100
            else if (due != null) s += 60
            s += when (t.priority) {
                Priority.CRITICAL -> 40
                Priority.HIGH -> 25
                Priority.MEDIUM -> 10
                Priority.LOW -> 0
            }
            if (t.pinned) s += 30
            if (t.energy == input.energy) s += 15
            s += if (t.durationMin <= input.freeMinutes) 10 else -10
            return s
        }

        fun reason(t: TaskEntity): SuggestionReason {
            val due = t.dueAt
            return when {
                due != null && due < dayStart -> SuggestionReason.OVERDUE
                t.pinned -> SuggestionReason.PINNED
                due != null -> SuggestionReason.DUE_TODAY
                t.priority >= Priority.HIGH -> SuggestionReason.HIGH_PRIORITY
                t.energy == input.energy -> SuggestionReason.ENERGY_MATCH
                else -> SuggestionReason.FITS_TIME
            }
        }

        val ranked = dated.sortedByDescending(::score)

        // Focus: important / long-but-worthwhile work that fits the free window.
        val focusPool = ranked.filter {
            it.durationMin >= 25 || it.priority >= Priority.HIGH || it.pinned
        }
        // Light: everything else with a deadline, capped to short-ish chunks.
        val lightPool = ranked.filter {
            it !in focusPool && it.durationMin <= 45 && it.energy != EnergyLevel.HIGH
        }
        // Recovery: quick inbox wins and low-energy items.
        val recoveryPool = (inbox.filter { it.durationMin <= 20 } +
            ranked.filter { it.energy == EnergyLevel.LOW && it.durationMin <= 20 })
            .distinctBy { it.id }

        fun fill(pool: List<TaskEntity>, mode: PlannerMode, reasonOf: (TaskEntity) -> SuggestionReason): List<PlannerSuggestion> {
            val out = mutableListOf<PlannerSuggestion>()
            var budget = input.freeMinutes
            for (t in pool) {
                if (budget <= 0 && out.isNotEmpty()) break
                val s = PlannerSuggestion(t, mode, reasonOf(t), score(t))
                out += s
                budget -= t.durationMin
            }
            return out
        }

        return PlannerPlan(
            focus = fill(focusPool, PlannerMode.FOCUS, ::reason),
            light = fill(lightPool, PlannerMode.LIGHT, ::reason),
            recovery = fill(recoveryPool, PlannerMode.RECOVERY) {
                if (it.status == TaskStatus.INBOX) SuggestionReason.QUICK_WIN
                else SuggestionReason.ENERGY_MATCH
            }
        )
    }
}
