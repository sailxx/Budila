package com.budila.app

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** За сколько минут до звонка «режим дисциплины» запрещает выключать, удалять и менять будильник. */
const val DISCIPLINE_LOCK_MINUTES = 30

/** Длина фразы «зачем встаёшь» на экране звонка. */
const val PROMISE_MAX = 80

/** Сколько дней истории хранить: для серии хватает, а хранилище не растёт бесконечно. */
private const val KEEP_DAYS = 400

/**
 * [streak] — сколько дней подряд встал вовремя; [best] — рекорд; [woke] и [missed] — всего дней
 * с подъёмом и со срывом; [avgMinute] — среднее время подъёма в минутах от полуночи;
 * [days] — итог каждого дня (true — встал вовремя).
 */
data class WakeSummary(
    val streak: Int = 0,
    val best: Int = 0,
    val woke: Int = 0,
    val missed: Int = 0,
    val avgMinute: Int? = null,
    val days: Map<LocalDate, Boolean> = emptyMap(),
)

/**
 * Серия подъёмов. День, в который встал вовремя (выключил без «отложить» и, если включена проверка,
 * ответил на неё), — успех; любой «отложить» или проспанный звонок за день — срыв, серия начинается заново.
 * Дни без будильников серию не рвут.
 */
fun summarize(days: Map<LocalDate, Boolean>, times: Map<LocalDate, Int> = emptyMap()): WakeSummary {
    var run = 0
    var best = 0
    for ((_, woke) in days.toSortedMap()) {
        run = if (woke) run + 1 else 0
        best = maxOf(best, run)
    }
    val minutes = times.filterKeys { days[it] == true }.values
    return WakeSummary(
        run, best, days.count { it.value }, days.count { !it.value },
        if (minutes.isEmpty()) null else minutes.average().toInt(), days.toMap(),
    )
}

object WakeStats {
    private lateinit var prefs: SharedPreferences
    private var days = sortedMapOf<LocalDate, Boolean>()
    /** Во сколько встал в успешные дни (минуты от полуночи) */
    private var times = sortedMapOf<LocalDate, Int>()
    private val _summary = MutableStateFlow(WakeSummary())
    val summary: StateFlow<WakeSummary> = _summary

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("wake_stats", Context.MODE_PRIVATE)
        val json = JSONObject(prefs.getString("days", "{}") ?: "{}")
        json.keys().forEach { key ->
            runCatching { LocalDate.parse(key) }.getOrNull()?.let { days[it] = json.optBoolean(key) }
        }
        val t = JSONObject(prefs.getString("times", "{}") ?: "{}")
        t.keys().forEach { key ->
            runCatching { LocalDate.parse(key) }.getOrNull()?.let { times[it] = t.optInt(key) }
        }
        _summary.value = summarize(days, times)
    }

    /** Записать итог сегодняшнего подъёма. Срыв сильнее успеха: после «отложить» день уже не засчитать. */
    @Synchronized
    fun record(
        woke: Boolean,
        today: LocalDate = LocalDate.now(),
        minuteOfDay: Int = LocalTime.now().let { it.hour * 60 + it.minute },
    ): WakeSummary {
        days[today] = woke && days[today] != false
        if (days[today] == true) times.putIfAbsent(today, minuteOfDay) else times.remove(today)
        while (days.size > KEEP_DAYS) days.remove(days.firstKey())
        times.keys.retainAll(days.keys)
        _summary.value = summarize(days, times)
        if (::prefs.isInitialized) {
            val json = JSONObject()
            days.forEach { (d, ok) -> json.put(d.toString(), ok) }
            val t = JSONObject()
            times.forEach { (d, m) -> t.put(d.toString(), m) }
            prefs.edit().putString("days", json.toString()).putString("times", t.toString()).apply()
        }
        return _summary.value
    }

    /** Записать итог и подбодрить (или честно сказать, что серия прервалась). */
    fun recordWithToast(context: Context, woke: Boolean) {
        val s = record(woke)
        val text = if (woke) {
            context.getString(R.string.streak_toast, context.getString(R.string.unit_days, s.streak))
        } else {
            context.getString(R.string.streak_broken)
        }
        Toast.makeText(context, text, Toast.LENGTH_LONG).show()
    }
}

/** Будильник заблокирован: режим дисциплины включён, а до звонка меньше [DISCIPLINE_LOCK_MINUTES] минут. */
fun Alarm.isLocked(settings: AppSettings = SettingsRepository.current, now: ZonedDateTime = ZonedDateTime.now()) =
    settings.discipline && enabled && Duration.between(now, nextTrigger(now)).toMinutes() < DISCIPLINE_LOCK_MINUTES

/** Пока какой-то будильник заблокирован, режим дисциплины нельзя выключить. */
fun disciplineLocked(
    alarms: List<Alarm> = AlarmRepository.alarms.value,
    settings: AppSettings = SettingsRepository.current,
    now: ZonedDateTime = ZonedDateTime.now(),
) = alarms.any { it.isLocked(settings, now) }

/** Переключатель режима дисциплины для настроек; false — выключить сейчас нельзя. */
fun setDiscipline(context: Context, on: Boolean): Boolean {
    if (!on && disciplineLocked()) {
        Toast.makeText(context, context.getString(R.string.msg_locked, DISCIPLINE_LOCK_MINUTES), Toast.LENGTH_SHORT).show()
        return false
    }
    SettingsRepository.update { it.copy(discipline = on) }
    return true
}
