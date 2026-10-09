package com.budila.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.roundToInt

@Composable
internal fun InstrumentSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val s by SettingsRepository.settings.collectAsStateWithLifecycle()
    val pickRingtone = rememberRingtonePicker()
    val ringtoneTitle = remember(s.ringtone) { ringtoneTitle(context, s.ringtone) }
    val version = rememberAppVersion()
    val minutes = { v: Int -> context.getString(R.string.minutes_short, v) }
    val timeoutLabel = { v: Int -> if (v == 0) context.getString(R.string.never) else minutes(v) }
    val rampLabel = { v: Int ->
        when {
            v == 0 -> context.getString(R.string.off)
            v % 60 == 0 -> minutes(v / 60)
            else -> context.getString(R.string.seconds_short, v)
        }
    }
    val snoozeLimitLabel ={ v: Int -> if (v < 0) "∞" else "$v" }
    val checkLabel = { v: Int -> if (v == 0) context.getString(R.string.off) else minutes(v) }

    LazyColumn(
        Modifier.fillMaxSize().background(colors.bg),
        contentPadding = WindowInsets.systemBars.asPaddingValues().let {
            PaddingValues(start = 16.dp, end = 16.dp, top = it.calculateTopPadding() + 8.dp, bottom = it.calculateBottomPadding() + 32.dp)
        },
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconKey(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), Modifier.size(20.dp)) }
                Spacer(Modifier.weight(1f))
                Wordmark()
            }
            Text(
                stringResource(R.string.settings), style = IType.headline, color = colors.ink,
                modifier = Modifier.padding(top = 18.dp, bottom = 4.dp),
            )
        }

        // ---------- Оформление ----------
        item { Section(stringResource(R.string.section_appearance)) }
        item {
            CasingLegend(stringResource(R.string.design_title))
            Spacer(Modifier.height(8.dp))
            Strip(
                listOf(stringResource(R.string.design_instrument), stringResource(R.string.design_material)),
                if (s.design == Design.INSTRUMENT) 0 else 1,
                { i -> SettingsRepository.update { it.copy(design = if (i == 0) Design.INSTRUMENT else Design.MATERIAL) } },
            )
            Text(
                stringResource(R.string.design_desc), style = IType.small, color = colors.muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
            )
        }
        item {
            CasingLegend(stringResource(R.string.theme_title))
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(InstrumentTheme.entries) { theme ->
                    ThemeKey(theme, s, theme == s.instrumentTheme) {
                        SettingsRepository.update { it.copy(instrumentTheme = theme) }
                    }
                }
            }
        }
        if (s.instrumentTheme == InstrumentTheme.CLASSIC) {
            item {
                Spacer(Modifier.height(14.dp))
                CasingLegend(stringResource(R.string.mode_title))
                Spacer(Modifier.height(8.dp))
                Strip(
                    DarkMode.entries.map { stringResource(it.title) },
                    s.darkMode.ordinal,
                    { i -> SettingsRepository.update { it.copy(darkMode = DarkMode.entries[i]) } },
                )
            }
            item {
                Spacer(Modifier.height(6.dp))
                SwitchRow(Icons.Rounded.Contrast, stringResource(R.string.amoled_title), stringResource(R.string.amoled_desc), s.amoled) { v ->
                    SettingsRepository.update { it.copy(amoled = v) }
                }
            }
        }
        item {
            val (icon, selectIcon) = rememberLauncherIcon()
            Spacer(Modifier.height(14.dp))
            CasingLegend(stringResource(R.string.icon_title))
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
            ) {
                LauncherIcon.entries.forEach { option ->
                    val selected = option == icon
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                        Key({ selectIcon(option) }, Modifier.size(72.dp, 64.dp), latched = selected) {
                            AppIcon(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)), icon = option)
                            if (selected) Led(true, Modifier.align(Alignment.TopEnd).padding(5.dp), size = 5.dp)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(option.title),
                            style = IType.small.copy(fontSize = 12.5.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                            color = if (selected) colors.ink else colors.muted,
                            maxLines = 1,
                        )
                    }
                }
            }
            Text(stringResource(R.string.icon_desc), style = IType.small, color = colors.muted, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        if (Build.VERSION.SDK_INT >= 33) item {
            val locale = appLocale(context)
            Row_(Icons.Rounded.Language, stringResource(R.string.language), locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }, external = true) {
                context.startActivity(Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:${context.packageName}")))
            }
        }
        item {
            val days = remember { dayShortNames(context) }
            ChoiceStrip(Icons.Rounded.CalendarMonth, stringResource(R.string.week_start_title), WeekStart.entries.map { it.ordinal }, s.weekStart.ordinal, { days[WeekStart.entries[it].offset] }) { v ->
                SettingsRepository.update { it.copy(weekStart = WeekStart.entries[v]) }
            }
        }

        // ---------- Звонок ----------
        item { Section(stringResource(R.string.section_ringing)) }
        item { Row_(Icons.Rounded.MusicNote, stringResource(R.string.ringtone), ringtoneTitle, onClick = pickRingtone) }
        item { InstrumentVolumeRow() }
        item {
            ChoiceStrip(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.ramp_title), listOf(0, 15, 30, 60, 120), s.rampSeconds, rampLabel) { v ->
                SettingsRepository.update { it.copy(rampSeconds = v) }
            }
            Text(stringResource(R.string.ramp_desc), style = IType.small, color = colors.muted)
        }
        item {
            SwitchRow(Icons.Rounded.Spa, stringResource(R.string.gentle_wake), stringResource(R.string.gentle_default_desc), s.gentleDefault) { v ->
                SettingsRepository.update { it.copy(gentleDefault = v) }
            }
        }
        item {
            ChoiceStrip(Icons.Rounded.Snooze, stringResource(R.string.snooze_title), listOf(5, 10, 15, 20), s.snoozeMinutes, minutes) { v ->
                SettingsRepository.update { it.copy(snoozeMinutes = v) }
            }
        }
        item {
            ChoiceStrip(Icons.Rounded.TimerOff, stringResource(R.string.timeout_title), listOf(5, 10, 20, 30, 0), s.timeoutMinutes, timeoutLabel) { v ->
                SettingsRepository.update { it.copy(timeoutMinutes = v) }
            }
        }
        item {
            ChoiceStrip(Icons.Rounded.TouchApp, stringResource(R.string.dismiss_gesture_title), DismissGesture.entries.map { it.ordinal }, s.dismissGesture.ordinal, { context.getString(DismissGesture.entries[it].title) }) { v ->
                SettingsRepository.update { it.copy(dismissGesture = DismissGesture.entries[v]) }
            }
            Text(stringResource(R.string.dismiss_gesture_desc), style = IType.small, color = colors.muted)
        }
        item {
            ChoiceStrip(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.volume_keys_title), VolumeKeys.entries.map { it.ordinal }, s.volumeKeys.ordinal, { context.getString(VolumeKeys.entries[it].title) }) { v ->
                SettingsRepository.update { it.copy(volumeKeys = VolumeKeys.entries[v]) }
            }
        }
        item {
            SwitchRow(Icons.Rounded.Vibration, stringResource(R.string.vibrate_default), null, s.defaultVibrate) { v ->
                SettingsRepository.update { it.copy(defaultVibrate = v) }
            }
        }

        // ---------- Проверка пробуждения ----------
        item { Section(stringResource(R.string.section_wake)) }
        item {
            CasingLegend(stringResource(R.string.default_task_title))
            Spacer(Modifier.height(8.dp))
            Strip(
                WakeTask.entries.map { stringResource(it.title) }, s.defaultTask.ordinal,
                { i -> SettingsRepository.update { it.copy(defaultTask = WakeTask.entries[i]) } },
            )
            Text(
                taskHint(s.defaultTask, s.taskRepeats), style = IType.small, color = colors.muted,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
        item {
            ChoiceStrip(Icons.Rounded.Repeat, stringResource(R.string.task_repeats_title), TASK_REPEAT_OPTIONS, s.taskRepeats, { "×$it" }) { v ->
                SettingsRepository.update { it.copy(taskRepeats = v) }
            }
            Text(stringResource(R.string.task_repeats_desc), style = IType.small, color = colors.muted, modifier = Modifier.padding(bottom = 12.dp))
        }
        item {
            PasswordField(s.password, { v -> SettingsRepository.update { it.copy(password = v) } }, stringResource(R.string.password_title))
            Text(stringResource(R.string.password_desc), style = IType.small, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            ChoiceStrip(Icons.Rounded.Snooze, stringResource(R.string.max_snoozes_title), listOf(-1, 0, 1, 2, 3), s.maxSnoozes, snoozeLimitLabel) { v ->
                SettingsRepository.update { it.copy(maxSnoozes = v) }
            }
            Text(stringResource(R.string.max_snoozes_desc), style = IType.small, color = colors.muted)
        }
        item {
            ChoiceStrip(Icons.Rounded.Verified, stringResource(R.string.awake_check_title), listOf(0, 3, 5, 10), s.awakeCheckMinutes, checkLabel) { v ->
                SettingsRepository.update { it.copy(awakeCheckMinutes = v) }
            }
            Text(stringResource(R.string.awake_check_desc), style = IType.small, color = colors.muted)
        }

        // ---------- О приложении ----------
        item { Section(stringResource(R.string.section_about)) }
        item {
            Well(Modifier.fillMaxWidth()) {
                LegendRow(stringResource(R.string.legend_version)) {
                    Text(version, style = IType.label, color = colors.wellInk)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Budila", style = IType.title, color = colors.wellInk)
                        Text(stringResource(R.string.about_tagline), style = IType.small, color = colors.wellDim)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        item {
            Row_(null, stringResource(R.string.link_source), "github.com/sailxx/Budila", painter = R.drawable.ic_github, external = true) {
                openUrl(context, GITHUB_REPO)
            }
        }
        item {
            Row_(Icons.Rounded.Person, stringResource(R.string.link_author), "github.com/sailxx", external = true) {
                openUrl(context, GITHUB_USER)
            }
        }
        item {
            Row_(Icons.Rounded.SystemUpdate, stringResource(R.string.link_updates), stringResource(R.string.link_updates_desc), external = true) {
                openUrl(context, "$GITHUB_REPO/releases/latest")
            }
        }
        item {
            Row_(Icons.Rounded.BugReport, stringResource(R.string.link_issue), stringResource(R.string.link_issue_desc), external = true) {
                openUrl(context, "$GITHUB_REPO/issues/new")
            }
        }
        item {
            Row_(Icons.Rounded.StarOutline, stringResource(R.string.link_star), stringResource(R.string.link_star_desc), external = true) {
                openUrl(context, GITHUB_REPO)
            }
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(
        text, style = IType.title.copy(fontSize = 18.sp), color = LocalInstrument.current.ink,
        modifier = Modifier.padding(top = 28.dp, bottom = 12.dp),
    )
}

/** Тема — клавиша с миниатюрой: корпус, дисплей с «8» и главная клавиша. */
@Composable
private fun ThemeKey(theme: InstrumentTheme, s: AppSettings, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalInstrument.current
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val classicDark = s.darkMode == DarkMode.DARK || (s.darkMode == DarkMode.SYSTEM && systemDark)
    val p = theme.palette(classicDark, s.amoled)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Key(onClick, Modifier.size(72.dp, 64.dp), latched = selected) {
            Box(
                Modifier
                    .size(52.dp, 40.dp)
                    .background(p.bg, RoundedCornerShape(6.dp))
                    .padding(5.dp),
            ) {
                Box(
                    Modifier.fillMaxWidth().height(18.dp).wellFace(p, 3.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text("88", style = IType.readout(12.sp), color = p.wellInk, modifier = Modifier.padding(end = 4.dp))
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).size(16.dp, 9.dp).background(p.primary, RoundedCornerShape(2.dp)),
                )
                Box(
                    Modifier.align(Alignment.BottomStart).size(16.dp, 9.dp).background(p.key, RoundedCornerShape(2.dp)),
                )
            }
            if (selected) Led(true, Modifier.align(Alignment.TopEnd).padding(5.dp), size = 5.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(theme.title),
            style = IType.small.copy(fontSize = 12.5.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (selected) colors.ink else colors.muted,
            maxLines = 1,
        )
    }
}

/** Строка настроек: плоская клавиша на корпусе. */
@Composable
private fun Row_(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    painter: Int? = null,
    external: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalInstrument.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
    ) {
        when {
            painter != null -> Icon(painterResource(painter), null, tint = colors.muted, modifier = Modifier.size(22.dp))
            icon != null -> Icon(icon, null, tint = colors.muted, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = IType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
            if (subtitle != null) Text(subtitle, style = IType.small, color = colors.muted)
        }
        Icon(
            if (external) Icons.AutoMirrored.Rounded.OpenInNew else Icons.Rounded.ChevronRight, null,
            tint = colors.muted, modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ChoiceStrip(icon: ImageVector, title: String, options: List<Int>, selected: Int, label: (Int) -> String, onSelect: (Int) -> Unit) {
    val colors = LocalInstrument.current
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = colors.muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(title, style = IType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
        }
        Spacer(Modifier.height(10.dp))
        Strip(options.map(label), options.indexOf(selected).coerceAtLeast(0), { onSelect(options[it]) })
    }
}

/** Громкость будильника — ползунок системной громкости; отпустил — короткое прослушивание. */
@Composable
private fun InstrumentVolumeRow() {
    val colors = LocalInstrument.current
    val volume = rememberAlarmVolume()
    DisposableEffect(Unit) { onDispose { volume.stopPreview() } }
    Column(Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Alarm, null, tint = colors.muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.alarm_volume), style = IType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
        }
        Slider(
            value = volume.value.toFloat(),
            onValueChange = { volume.set(it.roundToInt()) },
            onValueChangeFinished = volume::preview,
            valueRange = volume.min.toFloat()..volume.max.toFloat().coerceAtLeast(volume.min + 1f),
            steps = (volume.max - volume.min - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = colors.ink,
                activeTrackColor = colors.ink,
                inactiveTrackColor = colors.line,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.padding(start = 36.dp),
        )
    }
}
