package com.budila.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        AlarmRepository.init(context)
        val alarm = AlarmRepository.get(intent.getIntExtra(EXTRA_ALARM_ID, -1)) ?: return
        // Повторная проверка «точно встал?» — не трогаем расписание самого будильника
        if (intent.getBooleanExtra(EXTRA_CHECK, false)) {
            AlarmService.start(context, alarm.id, check = true)
            return
        }
        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        if (!alarm.enabled && !snooze) return

        if (alarm.isRepeating) {
            AlarmScheduler.schedule(context, alarm)
        } else if (alarm.enabled) {
            AlarmRepository.upsert(alarm.copy(enabled = false))
        }
        AlarmService.start(context, alarm.id, snoozes = if (snooze) intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0) else 0)
    }
}

/** Перезагрузка, смена времени/часового пояса, обновление приложения — заново ставим будильники. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmRepository.init(context)
        AlarmScheduler.rescheduleAll(context)
    }
}
