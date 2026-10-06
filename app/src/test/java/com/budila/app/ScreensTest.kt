package com.budila.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Рендерит экраны в PNG (папка screenshots/). Запуск: ./gradlew testDebugUnitTest */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [30], qualifiers = "ru-w400dp-h860dp-xhdpi")
class ScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val sample = listOf(
        Alarm(1, 6, 45, "Пробежка", days = 0b0010101, enabled = true, tags = listOf("Спорт"), color = ALARM_COLORS[3]),
        Alarm(2, 7, 30, "Работа", days = 0b0011111, enabled = true, tags = listOf("Работа", "Важное"), color = ALARM_COLORS[6]),
        Alarm(3, 9, 0, "", days = 0b1100000, enabled = false),
        Alarm(4, 13, 15, "Таблетки", days = 0, enabled = false, tags = listOf("Лекарства"), color = ALARM_COLORS[0], gentle = false),
        Alarm(5, 22, 30, "Спать", days = 0b1111111, enabled = true, color = ALARM_COLORS[7]),
    )

    private fun seed(alarms: List<Alarm>) {
        AlarmRepository.init(ApplicationProvider.getApplicationContext())
        AlarmRepository.alarms.value.forEach { AlarmRepository.delete(it.id) }
        alarms.forEach { AlarmRepository.upsert(it) }
    }

    private fun shot(
        name: String,
        dark: Boolean = false,
        theme: AppTheme = AppTheme.INDIGO,
        custom: AppSettings? = null,
        content: @Composable () -> Unit,
    ) {
        if (dark) RuntimeEnvironment.setQualifiers("+night")
        SettingsRepository.update { custom ?: AppSettings(design = Design.MATERIAL, theme = theme) }
        compose.mainClock.autoAdvance = false
        compose.setContent { BudilaTheme { content() } }
        compose.mainClock.advanceTimeBy(1_500)
        compose.onRoot().captureRoboImage("../screenshots/$name.png")
    }

    @Test fun list() { seed(sample); shot("1_list") { AlarmListScreen() } }

    @Test fun listDark() { seed(sample); shot("2_list_dark", dark = true) { AlarmListScreen() } }

    @Test fun empty() { seed(emptyList()); shot("3_empty") { AlarmListScreen() } }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun editor() = shot("4_editor") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column {
                BottomSheetDefaults.DragHandle(modifier = Modifier.align(Alignment.CenterHorizontally))
                AlarmEditorContent(sample[1], isNew = false, allTags = listOf("Работа", "Спорт", "Важное", "Учёба", "Лекарства"), onCancel = {}, onSave = {}, onDelete = {})
            }
        }
    }

    @Test fun ringing() = shot("5_ringing") { RingingScreen("Работа", onSnooze = {}, onDismiss = {}) }

    @Test fun ringingDark() = shot("6_ringing_dark", dark = true) { RingingScreen("Работа", onSnooze = {}, onDismiss = {}) }

    @Test fun settings() {
        RuntimeEnvironment.setQualifiers("+h1960dp")
        shot("7_settings") { SettingsScreen(onBack = {}) }
    }

    @Test fun settingsDark() = shot("8_settings_dark", dark = true, theme = AppTheme.OCEAN) { SettingsScreen(onBack = {}) }

    @Test fun listForest() { seed(sample); shot("9_list_forest", theme = AppTheme.FOREST) { AlarmListScreen() } }

    @Test fun listSunsetDark() { seed(sample); shot("10_list_sunset_dark", dark = true, theme = AppTheme.SUNSET) { AlarmListScreen() } }

    @Test fun editorTall() {
        RuntimeEnvironment.setQualifiers("+h1500dp")
        shot("11_editor_full") {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                AlarmEditorContent(
                    sample[1], isNew = false, allTags = listOf("Работа", "Спорт", "Важное", "Учёба", "Лекарства"),
                    onCancel = {}, onSave = {}, onDelete = {},
                )
            }
        }
    }

    @Test fun customTheme() {
        RuntimeEnvironment.setQualifiers("+h1000dp")
        val custom = AppSettings(design = Design.MATERIAL, theme = AppTheme.CUSTOM, customHue = 170f, customSaturation = 0.7f, customStyle = CustomStyle.Vibrant)
        shot("12_custom_theme", custom = custom) { SettingsScreen(onBack = {}) }
    }

    @Test fun listCustom() {
        seed(sample)
        val custom = AppSettings(
            design = Design.MATERIAL, theme = AppTheme.CUSTOM, darkMode = DarkMode.DARK,
            customHue = 20f, customSaturation = 0.8f, customStyle = CustomStyle.Expressive,
        )
        shot("13_list_custom_dark", custom = custom) { AlarmListScreen() }
    }

    @Test fun listEnglish() {
        RuntimeEnvironment.setQualifiers("en-rUS-w400dp-h860dp-xhdpi")
        seed(sample)
        shot("14_list_en") { AlarmListScreen() }
    }

    @Test fun settingsGerman() {
        RuntimeEnvironment.setQualifiers("de-w400dp-h860dp-xhdpi")
        shot("15_settings_de", theme = AppTheme.OCEAN) { SettingsScreen(onBack = {}) }
    }

    @Test fun editorSpanish() {
        RuntimeEnvironment.setQualifiers("es-w400dp-h1500dp-xhdpi")
        shot("16_editor_es", theme = AppTheme.SAKURA) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                AlarmEditorContent(
                    sample[1], isNew = true, allTags = listOf("Trabajo", "Deporte", "Estudios"),
                    onCancel = {}, onSave = {}, onDelete = {},
                )
            }
        }
    }

    /** Иконка в разных масках лаунчера (видимая часть — 72 из 108 dp каждого слоя). */
    @Test fun icon() = shot("0_icon") {
        Row(
            modifier = Modifier.background(Color(0xFFE9E4F0)).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Все иконки на выбор: в квадратной и круглой маске
            LauncherIcon.entries.forEach { icon ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)), icon = icon)
                    AppIcon(Modifier.size(56.dp).clip(CircleShape), icon = icon)
                }
            }
        }
    }

    // ---------- Дизайн «Инструмент» ----------

    private fun instrument(theme: InstrumentTheme = InstrumentTheme.CLASSIC, dark: DarkMode = DarkMode.LIGHT, amoled: Boolean = false) =
        AppSettings(design = Design.INSTRUMENT, instrumentTheme = theme, darkMode = dark, amoled = amoled)

    @Test fun iList() { seed(sample); shot("i1_list", custom = instrument()) { InstrumentListScreen() } }

    @Test fun iListDark() { seed(sample); shot("i2_list_dark", custom = instrument(dark = DarkMode.DARK)) { InstrumentListScreen() } }

    @Test fun iListPaper() { seed(sample); shot("i3_list_paper", custom = instrument(InstrumentTheme.PAPER)) { InstrumentListScreen() } }

    @Test fun iEditor() {
        RuntimeEnvironment.setQualifiers("+h1700dp")
        shot("i4_editor", custom = instrument()) {
            Surface(color = LocalInstrument.current.bg) {
                InstrumentEditorContent(
                    sample[1], isNew = false, allTags = listOf("Работа", "Спорт", "Важное", "Учёба", "Лекарства"),
                    onCancel = {}, onSave = {}, onDelete = {},
                )
            }
        }
    }

    @Test fun iRinging() = shot("i5_ringing", custom = instrument()) { InstrumentRingingScreen("Работа", onSnooze = {}, onDismiss = {}) }

    @Test fun iRingingOled() =
        shot("i6_ringing_oled", custom = instrument(dark = DarkMode.DARK, amoled = true)) { InstrumentRingingScreen("Работа", onSnooze = {}, onDismiss = {}) }

    @Test fun iSettings() {
        RuntimeEnvironment.setQualifiers("+h1900dp")
        shot("i7_settings", custom = instrument()) { InstrumentSettingsScreen(onBack = {}) }
    }

    @Test fun iListMidnight() { seed(sample); shot("i8_list_midnight", custom = instrument(InstrumentTheme.MIDNIGHT)) { InstrumentListScreen() } }

    @Test fun iEmpty() { seed(emptyList()); shot("i9_empty", custom = instrument()) { InstrumentListScreen() } }
}
