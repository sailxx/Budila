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
        assertEquals(WakeSummary(streak = 1, best = 3), summarize(days))
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
