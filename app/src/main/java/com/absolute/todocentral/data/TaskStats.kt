package com.absolute.todocentral.data

import com.absolute.todocentral.data.models.Task
import java.util.Calendar

/**
 * Everything the bento home shows about the day, derived from the tasks
 * themselves so a tile can never disagree with the list beneath it.
 */
data class TaskStats(
        val dueToday: Int,
        val completedToday: Int,
        val overdue: Int,
        val outstanding: Int,
        /** Consecutive days, ending today or yesterday, with at least one completion. */
        val streakDays: Int,
        /** Completions per day for the last seven days, oldest first. */
        val week: IntArray,
        val nextTask: Task?
) {
    /** Share of today's work already done, 0f..1f. */
    val todayProgress: Float
        get() {
            val total = dueToday + completedToday
            return if (total <= 0) 0f else completedToday.toFloat() / total
        }

    val hasWorkToday: Boolean
        get() = dueToday + completedToday > 0

    override fun equals(other: Any?): Boolean =
            this === other || (other is TaskStats &&
                    dueToday == other.dueToday && completedToday == other.completedToday &&
                    overdue == other.overdue && outstanding == other.outstanding &&
                    streakDays == other.streakDays && week.contentEquals(other.week) &&
                    nextTask?.id == other.nextTask?.id)

    override fun hashCode(): Int {
        var result = dueToday
        result = 31 * result + completedToday
        result = 31 * result + overdue
        result = 31 * result + outstanding
        result = 31 * result + streakDays
        result = 31 * result + week.contentHashCode()
        result = 31 * result + (nextTask?.id?.hashCode() ?: 0)
        return result
    }

    companion object {
        const val WEEK_DAYS = 7

        fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

        fun startOfDay(atMillis: Long): Long = Calendar.getInstance().apply {
            timeInMillis = atMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        const val DAY_MS = 24L * 60 * 60 * 1000

        /**
         * Counts back from today while each day has a completion. Today not yet
         * having one does not break the streak — only a gap before yesterday
         * does — otherwise the number would read zero every morning.
         */
        fun streakFrom(completionTimes: List<Long>): Int {
            if (completionTimes.isEmpty()) return 0
            val days = completionTimes.map { startOfDay(it) }.toHashSet()
            val today = startOfToday()
            if (!days.contains(today) && !days.contains(today - DAY_MS)) return 0

            var streak = 0
            var cursor = if (days.contains(today)) today else today - DAY_MS
            while (days.contains(cursor)) {
                streak++
                cursor -= DAY_MS
            }
            return streak
        }

        /** Completions per day for the last [WEEK_DAYS] days, oldest first. */
        fun weekFrom(completionTimes: List<Long>): IntArray {
            val buckets = IntArray(WEEK_DAYS)
            val today = startOfToday()
            completionTimes.forEach { at ->
                val daysAgo = ((today - startOfDay(at)) / DAY_MS).toInt()
                if (daysAgo in 0 until WEEK_DAYS) {
                    buckets[WEEK_DAYS - 1 - daysAgo]++
                }
            }
            return buckets
        }
    }
}
