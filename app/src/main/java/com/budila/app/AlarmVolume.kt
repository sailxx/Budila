package com.budila.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Системная громкость будильника (поток ALARM) — та же, что в настройках телефона. */
internal class AlarmVolume(private val context: Context) {
    private val am = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var preview: Ringtone? = null

    val min = if (Build.VERSION.SDK_INT >= 28) am.getStreamMinVolume(AudioManager.STREAM_ALARM) else 0
    val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
    var value by mutableIntStateOf(am.getStreamVolume(AudioManager.STREAM_ALARM))
        private set

    fun set(v: Int) {
        value = v.coerceIn(min, max)
        // В режиме «Не беспокоить» система может запретить менять громкость
        runCatching { am.setStreamVolume(AudioManager.STREAM_ALARM, value, 0) }
    }

    /** Полторы секунды мелодии будильника на выбранной громкости — чтобы услышать, как будет звучать. */
    fun preview() {
        stopPreview()
        val uri = SettingsRepository.current.ringtone?.let(Uri::parse)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: return
        preview = RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
            runCatching { play() }
        }
        handler.postDelayed({ stopPreview() }, 1_500)
    }

    fun stopPreview() {
        handler.removeCallbacksAndMessages(null)
        preview?.stop()
        preview = null
    }
}

@Composable
internal fun rememberAlarmVolume(): AlarmVolume {
    val context = LocalContext.current
    return remember { AlarmVolume(context.applicationContext) }
}
