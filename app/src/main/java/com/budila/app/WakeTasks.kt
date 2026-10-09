package com.budila.app

import androidx.annotation.StringRes
import kotlin.random.Random

/** Задание, без которого звонок не выключить: доказательство, что ты проснулся. */
enum class WakeTask(@StringRes val title: Int) {
    NONE(R.string.task_none),
    MATH(R.string.task_math),
    PASSWORD(R.string.task_password),
}

/** Сколько раз повторить задание (решить пример / ввести пароль) — варианты в настройках. */
val TASK_REPEAT_OPTIONS = listOf(1, 2, 3, 5)

/** Сколько секунд даётся, чтобы ответить на повторную проверку, прежде чем будильник зазвонит снова. */
const val CHECK_ANSWER_SECONDS = 60

data class MathProblem(val text: String, val answer: Int)

/** Пример, который не решить во сне, но и не мучительный: двузначные числа, умножение на однозначное. */
fun mathProblem(random: Random = Random): MathProblem = when (random.nextInt(3)) {
    0 -> {
        val a = random.nextInt(12, 90)
        val b = random.nextInt(12, 90)
        MathProblem("$a + $b", a + b)
    }
    1 -> {
        val a = random.nextInt(40, 100)
        val b = random.nextInt(11, a - 10)
        MathProblem("$a − $b", a - b)
    }
    else -> {
        val a = random.nextInt(3, 10)
        val b = random.nextInt(12, 30)
        MathProblem("$a × $b", a * b)
    }
}

/** Пароль сравнивается без учёта регистра и пробелов по краям: спросонья легко нажать Shift. */
fun passwordMatches(input: String, password: String) =
    password.isNotBlank() && input.trim().equals(password.trim(), ignoreCase = true)

/**
 * Текущий звонок.
 * [check] — повторная проверка «точно встал?» после выключения: тихо, хватит одного нажатия.
 * [task] — что нужно сделать, чтобы выключить (уже с учётом настроек: без пароля → примеры).
 * [snoozesLeft] — сколько ещё раз можно отложить; null — без ограничений.
 */
data class Ringing(
    val alarm: Alarm,
    val task: WakeTask = WakeTask.NONE,
    val snoozes: Int = 0,
    val snoozesLeft: Int? = null,
    val check: Boolean = false,
) {
    val canSnooze get() = !check && (snoozesLeft == null || snoozesLeft > 0)

    companion object {
        /**
         * [strict] — будильник зазвонил снова, потому что ты не ответил на проверку:
         * откладывать нельзя, а выключить можно только заданием (если его нет — примеры).
         */
        fun of(alarm: Alarm, settings: AppSettings, snoozes: Int = 0, check: Boolean = false, strict: Boolean = false): Ringing {
            var task = if (check) WakeTask.NONE else alarm.task
            if (strict && task == WakeTask.NONE) task = WakeTask.MATH
            if (task == WakeTask.PASSWORD && settings.password.isBlank()) task = WakeTask.MATH
            val left = when {
                strict -> 0
                settings.maxSnoozes < 0 -> null
                else -> (settings.maxSnoozes - snoozes).coerceAtLeast(0)
            }
            return Ringing(alarm, task, snoozes, left, check)
        }
    }
}
