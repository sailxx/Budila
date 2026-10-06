package com.budila.app

import androidx.compose.foundation.layout.Column
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
        Alarm(1, 6, 45, "Пробежка", days = 0b0010101, enabled = true),
        Alarm(2, 7, 30, "Работа", days = 0b0011111, enabled = true),
        Alarm(3, 9, 0, "", days = 0b1100000, enabled = false),
        Alarm(4, 13, 15, "Таблетки", days = 0, enabled = false),
        Alarm(5, 22, 30, "Спать", days = 0b1111111, enabled = true),
    )

    private fun seed(alarms: List<Alarm>) {
        AlarmRepository.init(ApplicationProvider.getApplicationContext())
        AlarmRepository.alarms.value.forEach { AlarmRepository.delete(it.id) }
        alarms.forEach { AlarmRepository.upsert(it) }
    }

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        if (dark) RuntimeEnvironment.setQualifiers("+night")
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
                BottomSheetDefaults.DragHandle(modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
                AlarmEditorContent(sample[1], isNew = false, onCancel = {}, onSave = {}, onDelete = {})
            }
        }
    }

    @Test fun ringing() = shot("5_ringing") { RingingScreen("Работа", onSnooze = {}, onDismiss = {}) }

    @Test fun ringingDark() = shot("6_ringing_dark", dark = true) { RingingScreen("Работа", onSnooze = {}, onDismiss = {}) }
}
