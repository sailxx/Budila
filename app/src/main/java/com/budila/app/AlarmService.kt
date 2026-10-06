package com.budila.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalTime

class AlarmService : Service() {

    companion object {
        const val SNOOZE_MINUTES = 5
        private const val ACTION_DISMISS = "com.budila.app.DISMISS"
        private const val ACTION_SNOOZE = "com.budila.app.SNOOZE"
        private const val CHANNEL_ID = "alarm_ringing"
        private const val NOTIFICATION_ID = 42
        private const val TIMEOUT_MS = 10 * 60_000L

        /** Будильник, который звонит прямо сейчас (null — тишина). */
        val ringing = MutableStateFlow<Alarm?>(null)

        fun start(context: Context, alarmId: Int) = ContextCompat.startForegroundService(
            context, Intent(context, AlarmService::class.java).putExtra(EXTRA_ALARM_ID, alarmId),
        )

        fun dismiss(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_DISMISS))
        }

        fun snooze(context: Context) {
            context.startService(Intent(context, AlarmService::class.java).setAction(ACTION_SNOOZE))
        }
    }

    private val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { stopRinging() }
    private var volume = 0.1f
    private val rampUp = object : Runnable {
        override fun run() {
            volume = (volume + 0.05f).coerceAtMost(1f)
            player?.setVolume(volume, volume)
            if (volume < 1f) handler.postDelayed(this, 1500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> stopRinging()
            ACTION_SNOOZE -> {
                ringing.value?.let {
                    AlarmScheduler.scheduleAt(
                        this, it.id, System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L, snooze = true,
                    )
                }
                stopRinging()
            }
            else -> {
                AlarmRepository.init(this)
                val id = intent?.getIntExtra(EXTRA_ALARM_ID, -1) ?: -1
                val now = LocalTime.now()
                val alarm = AlarmRepository.get(id) ?: Alarm(id, now.hour, now.minute)
                startRinging(alarm)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRinging(alarm: Alarm) {
        stopSound()
        ringing.value = alarm
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(alarm),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        if (wakeLock?.isHeld != true) {
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "budila:ringing")
                .apply { acquire(TIMEOUT_MS + 5_000) }
        }
        playSound()
        if (alarm.vibrate) vibrate()
        handler.removeCallbacks(timeout)
        handler.postDelayed(timeout, TIMEOUT_MS)
    }

    private fun playSound() {
        val uris = listOfNotNull(
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        volume = 0.1f
        for (uri in uris) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(audioAttrs)
                mp.setDataSource(this, uri)
                mp.isLooping = true
                mp.prepare()
                mp.setVolume(volume, volume)
                mp.start()
                player = mp
                handler.post(rampUp)
                return
            } catch (e: Exception) {
                mp.release()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate() {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = v
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), audioAttrs)
    }

    private fun stopSound() {
        handler.removeCallbacks(rampUp)
        player?.run { runCatching { stop() }; release() }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun stopRinging() {
        stopSound()
        ringing.value = null
        handler.removeCallbacksAndMessages(null)
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSound()
        ringing.value = null
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun buildNotification(alarm: Alarm): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Звонок будильника", NotificationManager.IMPORTANCE_HIGH).apply {
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
        )
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val fullScreen = PendingIntent.getActivity(
            this, 0,
            Intent(this, RingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION),
            flags,
        )
        val dismiss = PendingIntent.getService(
            this, 1, Intent(this, AlarmService::class.java).setAction(ACTION_DISMISS), flags,
        )
        val snooze = PendingIntent.getService(
            this, 2, Intent(this, AlarmService::class.java).setAction(ACTION_SNOOZE), flags,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(alarm.label.ifBlank { "Будильник" })
            .setContentText(formatTime(this, alarm.hour, alarm.minute))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(0, "Отложить на $SNOOZE_MINUTES мин", snooze)
            .addAction(0, "Выключить", dismiss)
            .build()
    }
}
