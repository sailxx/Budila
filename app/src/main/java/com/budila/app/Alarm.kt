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

const val EXTRA_ALARM_ID = "alarm_id"
const val EXTRA_SNOOZE = "snooze"
const val ACTION_FIRE = "com.budila.app.FIRE"

val DAY_SHORT = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    /** bit0 = понедельник … bit6 = воскресенье; 0 = однократный */
    val days: Int = 0,
    val enabled: Boolean = true,
    val vibrate: Boolean = true,
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

    companion object {
        fun fromJson(o: JSONObject) = Alarm(
            id = o.getInt("id"),
            hour = o.getInt("hour"),
            minute = o.getInt("minute"),
            label = o.optString("label"),
            days = o.optInt("days"),
            enabled = o.optBoolean("enabled", true),
            vibrate = o.optBoolean("vibrate", true),
        )
    }
}

fun formatTime(context: Context, hour: Int, minute: Int): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(hour, minute).format(DateTimeFormatter.ofPattern(pattern))
}

fun daysText(days: Int): String = when (days) {
    0 -> "Однократно"
    0b1111111 -> "Каждый день"
    0b0011111 -> "Будни"
    0b1100000 -> "Выходные"
    else -> DAY_SHORT.filterIndexed { i, _ -> days and (1 shl i) != 0 }.joinToString(", ")
}

fun untilText(target: ZonedDateTime, now: ZonedDateTime = ZonedDateTime.now()): String {
    val totalMin = (Duration.between(now, target).toMillis() + 59_999) / 60_000
    if (totalMin <= 1) return "меньше чем через минуту"
    val d = totalMin / 1440
    val h = (totalMin % 1440) / 60
    val m = totalMin % 60
    return "через " + listOfNotNull(
        if (d > 0) "$d д" else null,
        if (h > 0) "$h ч" else null,
        if (m > 0) "$m мин" else null,
    ).joinToString(" ")
}

class BudilaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlarmRepository.init(this)
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
    fun schedule(context: Context, alarm: Alarm) {
        if (!alarm.enabled) return cancel(context, alarm.id)
        scheduleAt(context, alarm.id, alarm.nextTrigger().toInstant().toEpochMilli(), snooze = false)
    }

    fun scheduleAt(context: Context, id: Int, millis: Long, snooze: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java)
        val fire = firePendingIntent(context, id, snooze)
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

    private fun firePendingIntent(context: Context, id: Int, snooze: Boolean): PendingIntent =
        PendingIntent.getBroadcast(
            context, id,
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_FIRE)
                .putExtra(EXTRA_ALARM_ID, id)
                .putExtra(EXTRA_SNOOZE, snooze),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
