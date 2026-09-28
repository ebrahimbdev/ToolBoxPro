package com.toolbox.pro.planner.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.WeekFields

/** Date helpers shared by the planner UI and engines (device timezone). */
object TimeKit {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun startOfDay(now: Long): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDay(now: Long): Long =
        Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun startOfWeek(now: Long): Long {
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val monday = date.with(WeekFields.ISO.dayOfWeek(), 1L)
        return monday.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun startOfMonth(now: Long): Long {
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return date.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun endOfMonth(now: Long): Long {
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return date.withDayOfMonth(date.lengthOfMonth())
            .plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    }

    fun toLocalDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun toMillis(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun millisOf(date: LocalDate, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(date, java.time.LocalTime.of(hour, minute))
            .atZone(zone).toInstant().toEpochMilli()

    fun startOfPreviousDay(now: Long): Long = startOfDay(now) - 1
}

sealed interface RepeatRule {
    data object None : RepeatRule
    data object Daily : RepeatRule
    /** ISO days: 1 = Monday .. 7 = Sunday. */
    data class Weekly(val days: Set<Int>) : RepeatRule
    data class Monthly(val dayOfMonth: Int) : RepeatRule
    /** Every [days] days counted from the anchor date. */
    data class Interval(val days: Int) : RepeatRule
}

object Recurrence {
    fun parse(raw: String?): RepeatRule {
        if (raw.isNullOrBlank() || raw == "NONE") return RepeatRule.None
        val parts = raw.split(":")
        return when (parts[0]) {
            "DAILY" -> RepeatRule.Daily
            "WEEKLY" -> {
                val days = parts.getOrNull(1)
                    ?.split(",")
                    ?.mapNotNull { it.toIntOrNull() }
                    ?.filter { it in 1..7 }
                    ?.toSet()
                    ?: emptySet()
                if (days.isEmpty()) RepeatRule.Daily else RepeatRule.Weekly(days)
            }

            "MONTHLY" -> RepeatRule.Monthly(parts.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 28) ?: 1)
            "INTERVAL" -> RepeatRule.Interval(parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 7)
            else -> RepeatRule.None
        }
    }

    fun format(rule: RepeatRule): String = when (rule) {
        RepeatRule.None -> "NONE"
        RepeatRule.Daily -> "DAILY"
        is RepeatRule.Weekly -> "WEEKLY:" + rule.days.sorted().joinToString(",")
        is RepeatRule.Monthly -> "MONTHLY:" + rule.dayOfMonth
        is RepeatRule.Interval -> "INTERVAL:" + rule.days
    }

    private fun dates(rule: RepeatRule, anchorDate: LocalDate): Sequence<LocalDate> =
        sequence {
            when (rule) {
                RepeatRule.None -> Unit
                RepeatRule.Daily -> {
                    var d = anchorDate
                    while (true) {
                        d = d.plusDays(1)
                        yield(d)
                    }
                }

                is RepeatRule.Interval -> {
                    var k = 1
                    while (true) {
                        yield(anchorDate.plusDays((k++ * rule.days).toLong()))
                    }
                }

                is RepeatRule.Weekly -> {
                    var d = anchorDate.plusDays(1)
                    while (true) {
                        if (d.dayOfWeek.value in rule.days) yield(d)
                        d = d.plusDays(1)
                    }
                }

                is RepeatRule.Monthly -> {
                    var m = anchorDate.plusMonths(1)
                    while (true) {
                        yield(m.withDayOfMonth(minOf(rule.dayOfMonth, m.lengthOfMonth())))
                        m = m.plusMonths(1)
                    }
                }
            }
        }

    /** First epoch occurrence strictly after [after], anchored on [anchor]'s date+time. */
    fun nextOccurrence(rule: RepeatRule, anchor: Long, after: Long): Long? {
        if (rule is RepeatRule.None) return null
        val zone = ZoneId.systemDefault()
        val anchorLdt = Instant.ofEpochMilli(anchor).atZone(zone)
        val anchorDate = anchorLdt.toLocalDate()
        val time = anchorLdt.toLocalTime()
        return dates(rule, anchorDate)
            .map { it.atTime(time).atZone(zone).toInstant().toEpochMilli() }
            .firstOrNull { it > after }
    }

    /** Occurrences of a recurring task inside [from, to) (one-shot rules return empty). */
    fun occurrencesBetween(rule: RepeatRule, anchor: Long, from: Long, to: Long): List<Long> {
        if (rule is RepeatRule.None) return emptyList()
        val zone = ZoneId.systemDefault()
        val anchorLdt = Instant.ofEpochMilli(anchor).atZone(zone)
        val anchorDate = anchorLdt.toLocalDate()
        val time = anchorLdt.toLocalTime()
        return dates(rule, anchorDate)
            .map { it.atTime(time).atZone(zone).toInstant().toEpochMilli() }
            .takeWhile { it < to }
            .filter { it >= from }
            .toList()
    }
}
