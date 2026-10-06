package com.budila.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        AlarmRepository.init(context)
        val alarm = AlarmRepository.get(intent.getIntExtra(EXTRA_ALARM_ID, -1)) ?: return
        val snooze = intent.getBooleanExtra(EXTRA_SNOOZE, false)
        if (!alarm.enabled && !snooze) return

        if (alarm.isRepeating) {
            AlarmScheduler.schedule(context, alarm)
        } else if (alarm.enabled) {
            AlarmRepository.upsert(alarm.copy(enabled = false))
        }
        AlarmService.start(context, alarm.id)
    }
}

/** Перезагрузка, смена времени/часового пояса, обновление приложения — заново ставим будильники. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmRepository.init(context)
        AlarmScheduler.rescheduleAll(context)
    }
}
