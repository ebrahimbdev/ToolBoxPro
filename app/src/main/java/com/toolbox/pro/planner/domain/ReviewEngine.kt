package com.toolbox.pro.planner.domain

import com.toolbox.pro.planner.data.ReviewType

/**
 * Review Engine (Phase 2): period boundaries + the question set per period
 * from the PRD. Answers are stored as a JSON object keyed by question key.
 */
object ReviewEngine {

    data class Period(val start: Long, val end: Long)

    fun periodFor(type: ReviewType, now: Long): Period = when (type) {
        ReviewType.DAILY -> Period(TimeKit.startOfDay(now), TimeKit.endOfDay(now))
        ReviewType.WEEKLY -> Period(TimeKit.startOfWeek(now), TimeKit.endOfDay(now))
        ReviewType.MONTHLY -> Period(TimeKit.startOfMonth(now), TimeKit.endOfMonth(now))
    }

    /**
     * Question keys per period — mapped to localized copy by the UI.
     * Daily: what did I do / what did I learn / what matters tomorrow.
     * Weekly: what got done / what didn't and why / worth building / energy drain.
     * Monthly: goals progress / KPI / time & energy / task trend.
     */
    fun questionKeys(type: ReviewType): List<String> = when (type) {
        ReviewType.DAILY -> listOf("did", "learned", "tomorrow")
        ReviewType.WEEKLY -> listOf("done", "not_done", "worth", "energy")
        ReviewType.MONTHLY -> listOf("goals", "kpi", "time_energy", "trend")
    }
}
