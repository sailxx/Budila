package com.budila.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class WakeTasksTest {
    private val alarm = Alarm(1, 7, 0)

    @Test fun problemsAreSolvableAndPositive() {
        val random = Random(1)
        repeat(1_000) {
            val p = mathProblem(random)
            val (a, op, b) = p.text.split(" ")
            val expected = when (op) {
                "+" -> a.toInt() + b.toInt()
                "−" -> a.toInt() - b.toInt()
                else -> a.toInt() * b.toInt()
            }
            assertEquals(expected, p.answer)
            assertTrue(p.answer in 10..999)
        }
    }

    @Test fun passwordIgnoresCaseAndSpaces() {
        assertTrue(passwordMatches(" Утро ", "утро"))
        assertFalse(passwordMatches("утр", "утро"))
        // Пустой пароль ничего не открывает
        assertFalse(passwordMatches("", ""))
    }

    @Test fun passwordWithoutPasswordFallsBackToMath() {
        val r = Ringing.of(alarm.copy(task = WakeTask.PASSWORD), AppSettings(password = ""))
        assertEquals(WakeTask.MATH, r.task)
        assertEquals(WakeTask.PASSWORD, Ringing.of(alarm.copy(task = WakeTask.PASSWORD), AppSettings(password = "x")).task)
    }

    @Test fun snoozeLimit() {
        assertNull(Ringing.of(alarm, AppSettings(maxSnoozes = -1), snoozes = 5).snoozesLeft)
        val last = Ringing.of(alarm, AppSettings(maxSnoozes = 2), snoozes = 1)
        assertEquals(1, last.snoozesLeft)
        assertTrue(last.canSnooze)
        assertFalse(Ringing.of(alarm, AppSettings(maxSnoozes = 2), snoozes = 2).canSnooze)
        assertFalse(Ringing.of(alarm, AppSettings(maxSnoozes = 0)).canSnooze)
    }

    @Test fun checkIsOneTapAndStrictRingNeedsTask() {
        val check = Ringing.of(alarm.copy(task = WakeTask.MATH), AppSettings(), check = true)
        assertEquals(WakeTask.NONE, check.task)
        assertFalse(check.canSnooze)
        // Не ответил на проверку: даже у будильника без задания — примеры и без «отложить»
        val strict = Ringing.of(alarm, AppSettings(), strict = true)
        assertEquals(WakeTask.MATH, strict.task)
        assertFalse(strict.canSnooze)
    }

    @Test fun mathNeedsAllProblemsAndWrongAnswerChangesProblem() {
        val state = WakeTaskState(WakeTask.MATH, "", repeats = 5, random = Random(7))
        repeat(4) {
            state.edit(state.problem.answer.toString())
            assertFalse(state.submit())
        }
        val before = state.problem
        state.edit((before.answer + 1).toString())
        assertFalse(state.submit())
        assertTrue(state.wrong)
        assertEquals(4, state.solved)
        state.edit(state.problem.answer.toString())
        assertTrue(state.submit())
    }

    @Test fun passwordRepeatsInARow() {
        val state = WakeTaskState(WakeTask.PASSWORD, "утро", repeats = 2)
        state.edit("утро")
        assertFalse(state.submit())
        assertEquals(1, state.solved)
        state.edit("ночь")
        assertFalse(state.submit())
        assertTrue(state.wrong)
        state.edit("Утро")
        assertTrue(state.submit())
        // Один повтор — хватает одного ввода
        assertTrue(WakeTaskState(WakeTask.PASSWORD, "утро", repeats = 1).apply { edit("утро") }.submit())
    }

    @Test fun weekStartsOnChosenDay() {
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), weekOrder(WeekStart.MONDAY))
        assertEquals(listOf(6, 0, 1, 2, 3, 4, 5), weekOrder(WeekStart.SUNDAY))
        assertEquals(listOf(5, 6, 0, 1, 2, 3, 4), weekOrder(WeekStart.SATURDAY))
    }

    @Test fun alarmTaskSurvivesJson() {
        val a = alarm.copy(task = WakeTask.PASSWORD)
        assertEquals(a, Alarm.fromJson(a.toJson()))
        // Старые будильники без поля task — без задания
        assertEquals(WakeTask.NONE, Alarm.fromJson(alarm.toJson().apply { remove("task") }).task)
    }
}
