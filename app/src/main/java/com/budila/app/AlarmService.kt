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
import android.net.Uri
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
        private const val ACTION_DISMISS = "com.budila.app.DISMISS"
        private const val ACTION_SNOOZE = "com.budila.app.SNOOZE"
        private const val CHANNEL_ID = "alarm_ringing"
        private const val NOTIFICATION_ID = 42
        private const val RAMP_TICK_MS = 500L
        private const val QUIET_START = 0.02f
        /** Повторная проверка всегда начинается тихо и дорастает за полминуты */
        private const val CHECK_RAMP_MS = 30_000L
        /** «Никогда не затихать» — но телефон не должен держать звонок бесконечно */
        private const val NEVER_TIMEOUT_MS = 3 * 60 * 60_000L

        /** Звонок, который идёт прямо сейчас (null — тишина). */
        val ringing = MutableStateFlow<Ringing?>(null)

        fun start(context: Context, alarmId: Int, snoozes: Int = 0, check: Boolean = false) =
            ContextCompat.startForegroundService(
                context,
                Intent(context, AlarmService::class.java)
                    .putExtra(EXTRA_ALARM_ID, alarmId)
                    .putExtra(EXTRA_SNOOZE_COUNT, snoozes)
                    .putExtra(EXTRA_CHECK, check),
            )

        /** Выключить звонок. Если у будильника есть задание, вызывать только после того, как оно выполнено. */
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
    private val timeout = Runnable { onTimeout() }
    private var volume = 0.1f
    /** На сколько поднимать громкость каждые [RAMP_TICK_MS], чтобы дойти до полной за заданное время */
    private var rampStep = 0.03f
    private val rampUp = object : Runnable {
        override fun run() {
            volume = (volume + rampStep).coerceAtMost(1f)
            player?.setVolume(volume, volume)
            if (volume < 1f) handler.postDelayed(this, RAMP_TICK_MS)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                val r = ringing.value
                // Выключил настоящий звонок — через несколько минут проверим, не уснул ли снова
                val checkIn = SettingsRepository.current.awakeCheckMinutes
                if (r != null && !r.check && checkIn > 0) AlarmScheduler.scheduleCheck(this, r.alarm.id, checkIn)
                stopRinging()
            }
            ACTION_SNOOZE -> {
                val r = ringing.value
                if (r != null && !r.canSnooze) return START_NOT_STICKY
                r?.let { snoozeFor(it, it.snoozes + 1) }
                stopRinging()
            }
            else -> {
                AlarmRepository.init(this)
                SettingsRepository.init(this)
                val id = intent?.getIntExtra(EXTRA_ALARM_ID, -1) ?: -1
                val now = LocalTime.now()
                val alarm = AlarmRepository.get(id) ?: Alarm(id, now.hour, now.minute)
                val r = Ringing.of(
                    alarm, SettingsRepository.current,
                    snoozes = intent?.getIntExtra(EXTRA_SNOOZE_COUNT, 0) ?: 0,
                    check = intent?.getBooleanExtra(EXTRA_CHECK, false) ?: false,
                )
                startRinging(r, gentle = r.check || alarm.gentle)
            }
        }
        return START_NOT_STICKY
    }

    private fun snoozeFor(r: Ringing, count: Int) = AlarmScheduler.scheduleAt(
        this, r.alarm.id, System.currentTimeMillis() + SettingsRepository.current.snoozeMinutes * 60_000L,
        snooze = true, snoozeCount = count,
    )

    private fun onTimeout() {
        val r = ringing.value
        when {
            r == null -> stopRinging()
            // Не ответил на проверку — значит, уснул: звоним по-настоящему, без «отложить»
            r.check -> startRinging(Ringing.of(r.alarm, SettingsRepository.current, strict = true), gentle = false)
            // С заданием звонок не затихает насовсем: замолкает и возвращается через «отложить»
            r.task != WakeTask.NONE -> {
                snoozeFor(r, r.snoozes)
                stopRinging()
            }
            else -> stopRinging()
        }
    }

    private fun startRinging(r: Ringing, gentle: Boolean) {
        stopSound()
        handler.removeCallbacksAndMessages(null)
        ringing.value = r
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(r),
            if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
        )
        val settings = SettingsRepository.current
        val timeoutMs = when {
            r.check -> CHECK_ANSWER_SECONDS * 1_000L
            settings.timeoutMinutes <= 0 -> NEVER_TIMEOUT_MS
            else -> settings.timeoutMinutes * 60_000L
        }
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "budila:ringing")
            .apply { acquire(timeoutMs + 5_000) }
        val rampMs = if (!gentle) 0L else if (r.check) CHECK_RAMP_MS else settings.rampSeconds * 1_000L
        playSound(rampMs)
        if (r.alarm.vibrate || r.check) {
            // При плавном старте вибрация включается на середине нарастания; у проверки — сразу
            if (rampMs > 0 && !r.check) handler.postDelayed({ vibrate() }, rampMs / 2) else vibrate()
        }
        if (timeoutMs != NEVER_TIMEOUT_MS) handler.postDelayed(timeout, timeoutMs)
    }

    /** [rampMs] — за сколько дорасти до полной громкости; 0 — сразу полная. */
    private fun playSound(rampMs: Long) {
        val uris = listOfNotNull(
            SettingsRepository.current.ringtone?.let(Uri::parse),
            RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
        )
        volume = if (rampMs > 0) QUIET_START else 1f
        rampStep = if (rampMs > 0) (1f - QUIET_START) / (rampMs / RAMP_TICK_MS).coerceAtLeast(1) else 1f
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

    private fun buildNotification(r: Ringing): Notification {
        val alarm = r.alarm
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_ringing), NotificationManager.IMPORTANCE_HIGH).apply {
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
            .setContentTitle(
                if (r.check) getString(R.string.check_title) else alarm.label.ifBlank { getString(R.string.alarm_default_label) },
            )
            .setContentText(formatTime(this, alarm.hour, alarm.minute))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .apply {
                if (r.canSnooze) addAction(0, getString(R.string.snooze_for, SettingsRepository.current.snoozeMinutes), snooze)
                // С заданием выключить можно только на экране звонка — кнопку в шторке не показываем
                when {
                    r.check -> addAction(0, getString(R.string.check_awake), dismiss)
                    r.task == WakeTask.NONE -> addAction(0, getString(R.string.dismiss), dismiss)
                }
            }
            .build()
    }
}
