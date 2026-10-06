package com.budila.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Иконка приложения на рабочем столе. Каждой соответствует свой ярлык (activity-alias в манифесте);
 * включён ровно один, поэтому выбор хранится в самом PackageManager, а не в настройках.
 */
enum class LauncherIcon(
    @StringRes val title: Int,
    private val alias: String,
    @DrawableRes val background: Int,
    @DrawableRes val foreground: Int,
) {
    KEY(R.string.icon_key, ".LauncherKey", R.drawable.ic_launcher_background_key, R.drawable.ic_launcher_foreground_key),
    KEY_LIGHT(R.string.icon_key_light, ".LauncherKeyLight", R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground_key_light),
    KEY_DARK(R.string.icon_key_dark, ".LauncherKeyDark", R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground_key_dark),
    ORANGE(R.string.icon_orange, ".LauncherOrange", R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground),
    BLACK(R.string.icon_black, ".LauncherBlack", R.drawable.ic_launcher_background_black, R.drawable.ic_launcher_foreground),
    ;

    private fun component(context: Context) = ComponentName(context.packageName, "com.budila.app$alias")

    private fun state(context: Context) = context.packageManager.getComponentEnabledSetting(component(context))

    companion object {
        /** Включена в манифесте; остальные ярлыки там выключены */
        private val default = KEY

        /** Выбранная пользователем (явно включённый ярлык), иначе иконка по умолчанию. */
        fun current(context: Context): LauncherIcon =
            entries.firstOrNull { it.state(context) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED } ?: default

        fun select(context: Context, icon: LauncherIcon) {
            val pm = context.packageManager
            // Сначала включаем новый ярлык, потом гасим остальные — чтобы приложение не пропало из лаунчера
            pm.setComponentEnabledSetting(
                icon.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP,
            )
            entries.filter { it != icon }.forEach {
                pm.setComponentEnabledSetting(
                    it.component(context), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP,
                )
            }
        }

        /**
         * После обновления может гореть два ярлыка: иконка по умолчанию сменилась, а старый выбор
         * пользователя остался явно включённым. Оставляем только выбранный.
         */
        fun normalize(context: Context) {
            val enabled = entries.filter {
                when (it.state(context)) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> it == default
                    else -> false
                }
            }
            if (enabled.size > 1) select(context, current(context))
        }
    }
}

/** Текущая иконка и функция её смены. */
@Composable
internal fun rememberLauncherIcon(): Pair<LauncherIcon, (LauncherIcon) -> Unit> {
    val context = LocalContext.current
    var icon by remember { mutableStateOf(LauncherIcon.current(context)) }
    return icon to { new ->
        if (new != icon) {
            LauncherIcon.select(context, new)
            icon = new
        }
    }
}
