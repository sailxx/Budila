package com.budila.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WakeStatsTest {
    private val d = LocalDate.of(2026, 10, 1)

    @Test fun streakCountsTrailingSuccessesAndKeepsBest() {
        val days = mapOf(d to true, d.plusDays(1) to true, d.plusDays(2) to true, d.plusDays(3) to false, d.plusDays(4) to true)
        val s = summarize(days)
        assertEquals(1, s.streak)
        assertEquals(3, s.best)
        assertEquals(4, s.woke)
        assertEquals(1, s.missed)
        assertEquals(WakeSummary(), summarize(emptyMap()))
    }

    @Test fun daysWithoutAlarmsDoNotBreakStreak() {
        assertEquals(2, summarize(mapOf(d to true, d.plusDays(5) to true)).streak)
    }

    @Test fun failureOutweighsSuccessWithinADay() {
        WakeStats.record(woke = false, today = d)
        WakeStats.record(woke = true, today = d)
        assertEquals(0, WakeStats.summary.value.streak)
        WakeStats.record(woke = true, today = d.plusDays(1))
        assertEquals(1, WakeStats.summary.value.streak)
    }

    @Test fun disciplineForcesTaskButNotOnCheck() {
        val alarm = Alarm(1, 7, 0)
        assertEquals(WakeTask.NONE, Ringing.of(alarm, AppSettings()).task)
        assertEquals(WakeTask.MATH, Ringing.of(alarm, AppSettings(discipline = true)).task)
        assertEquals(WakeTask.NONE, Ringing.of(alarm, AppSettings(discipline = true), check = true).task)
    }

    @Test fun averageTimeCountsOnlySuccessfulDays() {
        val days = mapOf(d to true, d.plusDays(1) to true, d.plusDays(2) to false)
        val times = mapOf(d to 400, d.plusDays(1) to 440, d.plusDays(2) to 900)
        assertEquals(420, summarize(days, times).avgMinute)
        assertEquals(null, summarize(emptyMap()).avgMinute)
    }

    @Test fun snoozeCostGrowsAndOnlyInDiscipline() {
        val alarm = Alarm(1, 7, 0)
        assertEquals(0, Ringing.of(alarm, AppSettings(), snoozes = 3).snoozeProblems)
        assertEquals(1, Ringing.of(alarm, AppSettings(discipline = true)).snoozeProblems)
        assertEquals(3, Ringing.of(alarm, AppSettings(discipline = true), snoozes = 2).snoozeProblems)
        assertEquals(MAX_SNOOZE_PROBLEMS, Ringing.of(alarm, AppSettings(discipline = true), snoozes = 40).snoozeProblems)
        assertEquals(0, Ringing.of(alarm, AppSettings(discipline = true), check = true).snoozeProblems)
    }

    @Test fun calendarStartsOnWeekStartFourWeeksBack() {
        val wed = LocalDate.of(2026, 10, 21)
        assertEquals(LocalDate.of(2026, 9, 21), calendarStart(wed, WeekStart.MONDAY))
        assertEquals(LocalDate.of(2026, 9, 20), calendarStart(wed, WeekStart.SUNDAY))
    }

    @Test fun alarmLocksShortlyBeforeRinging() {
        val now = ZonedDateTime.of(2026, 10, 1, 6, 45, 0, 0, ZoneId.of("UTC"))
        val on = AppSettings(discipline = true)
        val alarm = Alarm(1, 7, 0)
        assertTrue(alarm.isLocked(on, now))
        assertFalse(alarm.isLocked(AppSettings(), now))
        assertFalse(alarm.copy(enabled = false).isLocked(on, now))
        assertFalse(alarm.isLocked(on, now.minusHours(2)))
        assertTrue(disciplineLocked(listOf(alarm), on, now))
        assertFalse(disciplineLocked(emptyList(), on, now))
    }
}
