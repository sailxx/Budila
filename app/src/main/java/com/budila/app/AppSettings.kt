package com.budila.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class AppTheme(@StringRes val title: Int, val seed: Color) {
    DYNAMIC(R.string.theme_material_you, Color(0xFF4F5B92)),
    INDIGO(R.string.theme_indigo, Color(0xFF4F5B92)),
    OCEAN(R.string.theme_ocean, Color(0xFF00838F)),
    FOREST(R.string.theme_forest, Color(0xFF2E7D32)),
    SUNSET(R.string.theme_sunset, Color(0xFFEF6C00)),
    SAKURA(R.string.theme_sakura, Color(0xFFD81B60)),
    GRAPHITE(R.string.theme_graphite, Color(0xFF5F6368)),
    CUSTOM(R.string.theme_custom, Color(0xFF4F5B92)),
    ;

    companion object {
        val dynamicAvailable get() = Build.VERSION.SDK_INT >= 31
        val available get() = entries.filter { it != DYNAMIC || dynamicAvailable }
    }
}

enum class DarkMode(@StringRes val title: Int) {
    SYSTEM(R.string.dark_system),
    LIGHT(R.string.dark_light),
    DARK(R.string.dark_dark),
}

/** Дизайн приложения: фирменный «Инструмент» (как в Okto) или Material 3. */
enum class Design { INSTRUMENT, MATERIAL }

/** Чем выключать звонок: чем труднее жест, тем меньше шанс выключить его во сне. */
enum class DismissGesture(@StringRes val title: Int) {
    TAP(R.string.gesture_tap),
    SLIDE(R.string.gesture_slide),
    HOLD(R.string.gesture_hold),
}

/** Что делают кнопки громкости, пока звонит будильник. */
enum class VolumeKeys(@StringRes val title: Int) {
    VOLUME(R.string.volume_keys_volume),
    SNOOZE(R.string.volume_keys_snooze),
    DISMISS(R.string.volume_keys_dismiss),
    NOTHING(R.string.volume_keys_nothing),
}

/** Первый день недели; [offset] — его номер, считая понедельник нулём. */
enum class WeekStart(val offset: Int) { MONDAY(0), SATURDAY(5), SUNDAY(6) }

data class AppSettings(
    val design: Design = Design.INSTRUMENT,
    /** Тема «Инструмента»; темы Material 3 — в [theme] */
    val instrumentTheme: InstrumentTheme = InstrumentTheme.CLASSIC,
    val theme: AppTheme = if (AppTheme.dynamicAvailable) AppTheme.DYNAMIC else AppTheme.INDIGO,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    /** Чистый чёрный фон в тёмной теме */
    val amoled: Boolean = false,
    /** null — системная мелодия будильника */
    val ringtone: String? = null,
    val gentleDefault: Boolean = true,
    val snoozeMinutes: Int = 5,
    /** Через сколько минут звонок затихнет сам; 0 — никогда */
    val timeoutMinutes: Int = 10,
    val defaultVibrate: Boolean = true,
    /** За сколько секунд громкость дорастает до полной у будильников со спокойным пробуждением; 0 — сразу */
    val rampSeconds: Int = 60,
    val dismissGesture: DismissGesture = DismissGesture.TAP,
    val volumeKeys: VolumeKeys = VolumeKeys.VOLUME,
    val weekStart: WeekStart = WeekStart.MONDAY,
    /** Пароль для будильников с заданием «Пароль» */
    val password: String = "",
    /** Задание для новых будильников */
    val defaultTask: WakeTask = WakeTask.NONE,
    /** Сколько раз подряд выполнить задание: решить примеров / ввести пароль */
    val taskRepeats: Int = 3,
    /** Сколько раз можно отложить один звонок; -1 — без ограничений */
    val maxSnoozes: Int = -1,
    /** Через сколько минут после выключения спросить «точно встал?»; 0 — не спрашивать */
    val awakeCheckMinutes: Int = 0,
    /** Своя тема: тон (0–360), насыщенность и стиль палитры */
    val customHue: Float = 265f,
    val customSaturation: Float = 0.6f,
    val customStyle: CustomStyle = CustomStyle.TonalSpot,
) {
    val customSeed get() = Color.hsv(customHue, customSaturation, 0.85f)
}

enum class CustomStyle(@StringRes val title: Int) {
    TonalSpot(R.string.style_tonal),
    Vibrant(R.string.style_vibrant),
    Expressive(R.string.style_expressive),
    Neutral(R.string.style_neutral),
    Fidelity(R.string.style_fidelity),
    Rainbow(R.string.style_rainbow),
    FruitSalad(R.string.style_fruit),
}

object SettingsRepository {
    private lateinit var prefs: SharedPreferences
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings
    val current get() = _settings.value

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val d = AppSettings()
        val theme = AppTheme.entries.find { it.name == prefs.getString("theme", null) }
            ?.takeIf { it != AppTheme.DYNAMIC || AppTheme.dynamicAvailable } ?: d.theme
        _settings.value = AppSettings(
            design = Design.entries.find { it.name == prefs.getString("design", null) } ?: d.design,
            instrumentTheme = InstrumentTheme.entries.find { it.name == prefs.getString("instrumentTheme", null) }
                ?: d.instrumentTheme,
            theme = theme,
            darkMode = DarkMode.entries.find { it.name == prefs.getString("darkMode", null) } ?: d.darkMode,
            amoled = prefs.getBoolean("amoled", d.amoled),
            ringtone = prefs.getString("ringtone", null),
            gentleDefault = prefs.getBoolean("gentleDefault", d.gentleDefault),
            snoozeMinutes = prefs.getInt("snoozeMinutes", d.snoozeMinutes),
            timeoutMinutes = prefs.getInt("timeoutMinutes", d.timeoutMinutes),
            defaultVibrate = prefs.getBoolean("defaultVibrate", d.defaultVibrate),
            rampSeconds = prefs.getInt("rampSeconds", d.rampSeconds),
            dismissGesture = DismissGesture.entries.find { it.name == prefs.getString("dismissGesture", null) } ?: d.dismissGesture,
            volumeKeys = VolumeKeys.entries.find { it.name == prefs.getString("volumeKeys", null) } ?: d.volumeKeys,
            weekStart = WeekStart.entries.find { it.name == prefs.getString("weekStart", null) } ?: d.weekStart,
            password = prefs.getString("password", null) ?: d.password,
            defaultTask = WakeTask.entries.find { it.name == prefs.getString("defaultTask", null) } ?: d.defaultTask,
            taskRepeats = prefs.getInt("taskRepeats", d.taskRepeats),
            maxSnoozes = prefs.getInt("maxSnoozes", d.maxSnoozes),
            awakeCheckMinutes = prefs.getInt("awakeCheckMinutes", d.awakeCheckMinutes),
            customHue = prefs.getFloat("customHue", d.customHue),
            customSaturation = prefs.getFloat("customSaturation", d.customSaturation),
            customStyle = CustomStyle.entries.find { it.name == prefs.getString("customStyle", null) } ?: d.customStyle,
        )
    }

    @Synchronized
    fun update(persist: Boolean = true, transform: (AppSettings) -> AppSettings) {
        val s = transform(_settings.value)
        _settings.value = s
        if (!persist || !::prefs.isInitialized) return
        prefs.edit()
            .putString("design", s.design.name)
            .putString("instrumentTheme", s.instrumentTheme.name)
            .putString("theme", s.theme.name)
            .putString("darkMode", s.darkMode.name)
            .putBoolean("amoled", s.amoled)
            .putString("ringtone", s.ringtone)
            .putBoolean("gentleDefault", s.gentleDefault)
            .putInt("snoozeMinutes", s.snoozeMinutes)
            .putInt("timeoutMinutes", s.timeoutMinutes)
            .putBoolean("defaultVibrate", s.defaultVibrate)
            .putInt("rampSeconds", s.rampSeconds)
            .putString("dismissGesture", s.dismissGesture.name)
            .putString("volumeKeys", s.volumeKeys.name)
            .putString("weekStart", s.weekStart.name)
            .putString("password", s.password)
            .putString("defaultTask", s.defaultTask.name)
            .putInt("taskRepeats", s.taskRepeats)
            .putInt("maxSnoozes", s.maxSnoozes)
            .putInt("awakeCheckMinutes", s.awakeCheckMinutes)
            .putFloat("customHue", s.customHue)
            .putFloat("customSaturation", s.customSaturation)
            .putString("customStyle", s.customStyle.name)
            .apply()
    }
}
