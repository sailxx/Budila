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
enum class LauncherIcon(@StringRes val title: Int, private val alias: String, @DrawableRes val background: Int) {
    ORANGE(R.string.icon_orange, ".LauncherOrange", R.drawable.ic_launcher_background),
    BLACK(R.string.icon_black, ".LauncherBlack", R.drawable.ic_launcher_background_black),
    ;

    private fun component(context: Context) = ComponentName(context.packageName, "com.budila.app$alias")

    companion object {
        private val default = ORANGE

        fun current(context: Context): LauncherIcon = entries.firstOrNull {
            when (context.packageManager.getComponentEnabledSetting(it.component(context))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> it == default
                else -> false
            }
        } ?: default

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
