package com.budila.app

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.text.format.DateFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val EXTRA_ALARM_ID = "alarm_id"
const val EXTRA_SNOOZE = "snooze"
const val EXTRA_SNOOZE_COUNT = "snooze_count"
const val EXTRA_CHECK = "check"
const val ACTION_FIRE = "com.budila.app.FIRE"


data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    /** bit0 = понедельник … bit6 = воскресенье; 0 = однократный */
    val days: Int = 0,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
    val tags: List<String> = emptyList(),
    /** ARGB-цвет карточки; null — цвет темы */
    val color: Int? = null,
    /** Спокойное пробуждение: тихий старт, громкость растёт около минуты, вибрация позже */
    val gentle: Boolean = true,
    /** Что нужно сделать, чтобы выключить звонок (доказать, что проснулся) */
    val task: WakeTask = WakeTask.NONE,
) {
    val isRepeating get() = days != 0

    fun nextTrigger(now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        var t = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!t.isAfter(now)) t = t.plusDays(1)
        if (days != 0) {
            while (days and (1 shl (t.dayOfWeek.value - 1)) == 0) t = t.plusDays(1)
        }
        return t
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("hour", hour).put("minute", minute).put("label", label)
        .put("days", days).put("enabled", enabled).put("vibrate", vibrate)
        .put("tags", JSONArray(tags)).put("color", color ?: JSONObject.NULL).put("gentle", gentle)
        .put("task", task.name)

    companion object {
        fun fromJson(o: JSONObject) = Alarm(
            id = o.getInt("id"),
            hour = o.getInt("hour"),
            minute = o.getInt("minute"),
            label = o.optString("label"),
            days = o.optInt("days"),
            enabled = o.optBoolean("enabled", true),
            vibrate = o.optBoolean("vibrate", true),
            tags = o.optJSONArray("tags")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList(),
            color = if (o.isNull("color")) null else o.optInt("color"),
            gentle = o.optBoolean("gentle", true),
            task = WakeTask.entries.find { it.name == o.optString("task") } ?: WakeTask.NONE,
        )
    }
}

/** Палитра цветов для будильников */
val ALARM_COLORS = listOf(
    0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF00897B,
    0xFF039BE5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60, 0xFF6D4C41,
).map { it.toInt() }

fun dayShortNames(context: Context): List<String> = context.resources.getStringArray(R.array.days_short).toList()

fun defaultTags(context: Context): List<String> = context.resources.getStringArray(R.array.default_tags).toList()

fun formatTime(context: Context, hour: Int, minute: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(pattern, appLocale(context)))
}

fun appLocale(context: Context): Locale = context.resources.configuration.locales[0]

fun daysText(context: Context, days: Int): String = when (days) {
    0 -> context.getString(R.string.repeat_once)
    0b1111111 -> context.getString(R.string.repeat_every_day)
    0b0011111 -> context.getString(R.string.repeat_weekdays)
    0b1100000 -> context.getString(R.string.repeat_weekend)
    else -> dayShortNames(context).filterIndexed { i, _ -> days and (1 shl i) != 0 }.joinToString(", ")
}

fun untilText(context: Context, target: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): String {
    val totalMin = (Duration.between(now, target).toMillis() + 59_999) / 60_000
    if (totalMin <= 1) return context.getString(R.string.until_less_minute)
    val d = (totalMin / 1440).toInt()
    val h = ((totalMin % 1440) / 60).toInt()
    val m = (totalMin % 60).toInt()
    val parts = listOfNotNull(
        if (d > 0) context.getString(R.string.unit_days, d) else null,
        if (h > 0) context.getString(R.string.unit_hours, h) else null,
        if (m > 0) context.getString(R.string.unit_minutes, m) else null,
    ).joinToString(" ")
    return context.getString(R.string.until_in, parts)
}

class BudilaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmRepository.init(this)
        SettingsRepository.init(this)
        LauncherIcon.normalize(this)
    }
}

object AlarmRepository {
    private lateinit var prefs: SharedPreferences
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    val alarms: StateFlow<List<Alarm>> = _alarms

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("budila", Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("alarms", "[]"))
        _alarms.value = (0 until arr.length()).map { Alarm.fromJson(arr.getJSONObject(it)) }
    }

    fun get(id: Int) = _alarms.value.find { it.id == id }

    fun newId() = (_alarms.value.maxOfOrNull { it.id } ?: 0) + 1

    @Synchronized
    fun upsert(alarm: Alarm) = save(_alarms.value.filter { it.id != alarm.id } + alarm)

    @Synchronized
    fun delete(id: Int) = save(_alarms.value.filter { it.id != id })

    private fun save(list: List<Alarm>) {
        val sorted = list.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id }))
        _alarms.value = sorted
        prefs.edit().putString("alarms", JSONArray(sorted.map { it.toJson() }).toString()).apply()
    }
}

object AlarmScheduler {
    private const val CHECK_REQUEST_BASE = 1_000_000

    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) return cancel(context, alarm.id)
        scheduleAt(context, alarm.id, alarm.nextTrigger().toInstant().toEpochMilli(), snooze = false)
    }

    fun scheduleAt(context: Context, id: Int, millis: Long, snooze: Boolean, snoozeCount: Int = 0, check: Boolean = false) {
        val am = context.getSystemService(AlarmManager::class.java)
        val fire = firePendingIntent(context, id, snooze, snoozeCount, check)
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, fire)
            return
        }
        val show = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(millis, show), fire)
    }

    fun cancel(context: Context, id: Int) {
        context.getSystemService(AlarmManager::class.java).cancel(firePendingIntent(context, id, false))
    }

    fun rescheduleAll(context: Context) {
        AlarmRepository.alarms.value.filter { it.enabled }.forEach { schedule(context, it) }
    }

    /** Повторная проверка «точно встал?» через несколько минут после выключения. */
    fun scheduleCheck(context: Context, id: Int, minutes: Int) =
        scheduleAt(context, id, System.currentTimeMillis() + minutes * 60_000L, snooze = false, check = true)

    // У проверки свой requestCode, чтобы она не заменила следующий звонок этого будильника
    private fun firePendingIntent(
        context: Context, id: Int, snooze: Boolean, snoozeCount: Int = 0, check: Boolean = false,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context, if (check) CHECK_REQUEST_BASE + id else id,
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_FIRE)
                .putExtra(EXTRA_ALARM_ID, id)
                .putExtra(EXTRA_SNOOZE, snooze)
                .putExtra(EXTRA_SNOOZE_COUNT, snoozeCount)
                .putExtra(EXTRA_CHECK, check),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
