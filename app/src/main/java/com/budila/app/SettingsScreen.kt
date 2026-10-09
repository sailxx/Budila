package com.budila.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal const val GITHUB_USER = "https://github.com/sailxx"
internal const val GITHUB_REPO = "https://github.com/sailxx/Budila"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s by SettingsRepository.settings.collectAsStateWithLifecycle()
    val dark = isAppInDarkTheme()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val pickRingtone = rememberRingtonePicker()
    val ringtoneTitle = remember(s.ringtone) { ringtoneTitle(context, s.ringtone) }
    val version = rememberAppVersion()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item { SectionHeader(stringResource(R.string.section_appearance)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.design_title)) },
                    leadingContent = { Icon(Icons.Rounded.Calculate, null) },
                    supportingContent = {
                        Column {
                            Text(stringResource(R.string.design_desc))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                listOf(Design.INSTRUMENT to R.string.design_instrument, Design.MATERIAL to R.string.design_material)
                                    .forEachIndexed { i, (design, title) ->
                                        SegmentedButton(
                                            selected = s.design == design,
                                            onClick = { SettingsRepository.update { it.copy(design = design) } },
                                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                                        ) { Text(stringResource(title), maxLines = 1) }
                                    }
                            }
                        }
                    },
                )
            }
            item {
                ThemePicker(settings = s, dark = dark) { t ->
                    SettingsRepository.update { it.copy(theme = t) }
                }
            }
            item {
                AnimatedVisibility(
                    visible = s.theme == AppTheme.CUSTOM,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    CustomThemeEditor(s)
                }
            }
            item {
                SingleChoiceSegmentedButtonRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    DarkMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = s.darkMode == mode,
                            onClick = { SettingsRepository.update { it.copy(darkMode = mode) } },
                            shape = SegmentedButtonDefaults.itemShape(i, DarkMode.entries.size),
                            icon = {
                                SegmentedButtonDefaults.Icon(active = s.darkMode == mode) {
                                    Icon(
                                        when (mode) {
                                            DarkMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                            DarkMode.LIGHT -> Icons.Rounded.LightMode
                                            DarkMode.DARK -> Icons.Rounded.DarkMode
                                        },
                                        null,
                                        Modifier.size(SegmentedButtonDefaults.IconSize),
                                    )
                                }
                            },
                        ) { Text(stringResource(mode.title), maxLines = 1) }
                    }
                }
            }
            item {
                SwitchItem(
                    Icons.Rounded.Contrast, stringResource(R.string.amoled_title),
                    stringResource(R.string.amoled_desc),
                    s.amoled,
                ) { v -> SettingsRepository.update { it.copy(amoled = v) } }
            }

            item {
                val (icon, selectIcon) = rememberLauncherIcon()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.icon_title)) },
                    leadingContent = { Icon(Icons.Rounded.Apps, null) },
                    supportingContent = {
                        Column {
                        Text(stringResource(R.string.icon_desc))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()),
                        ) {
                            LauncherIcon.entries.forEach { option ->
                                val selected = option == icon
                                AppIcon(
                                    Modifier
                                        .size(44.dp)
                                        .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                                        .padding(if (selected) 5.dp else 0.dp)
                                        .clip(CircleShape)
                                        .clickable { selectIcon(option) },
                                    icon = option,
                                )
                            }
                        }
                        }
                    },
                )
            }
            if (Build.VERSION.SDK_INT >= 33) item {
                val locale = appLocale(context)
                ListItem(
                    headlineContent = { Text(stringResource(R.string.language)) },
                    supportingContent = { Text(locale.getDisplayName(locale).replaceFirstChar { it.uppercase() }) },
                    leadingContent = { Icon(Icons.Rounded.Language, null) },
                    trailingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(20.dp)) },
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.parse("package:${context.packageName}")),
                        )
                    },
                )
            }
            item { SectionHeader(stringResource(R.string.section_ringing)) }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.ringtone)) },
                    supportingContent = { Text(ringtoneTitle) },
                    leadingContent = { Icon(Icons.Rounded.MusicNote, null) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable(onClick = pickRingtone),
                )
            }
            item {
                SwitchItem(
                    Icons.Rounded.Spa, stringResource(R.string.gentle_wake),
                    stringResource(R.string.gentle_default_desc),
                    s.gentleDefault,
                ) { v -> SettingsRepository.update { it.copy(gentleDefault = v) } }
            }
            item {
                ChoiceItem(Icons.Rounded.Snooze, stringResource(R.string.snooze_title), listOf(5, 10, 15, 20), s.snoozeMinutes) { v ->
                    SettingsRepository.update { it.copy(snoozeMinutes = v) }
                }
            }
            item {
                ChoiceItem(Icons.Rounded.TimerOff, stringResource(R.string.timeout_title), listOf(5, 10, 20, 30), s.timeoutMinutes) { v ->
                    SettingsRepository.update { it.copy(timeoutMinutes = v) }
                }
            }
            item {
                SwitchItem(
                    Icons.Rounded.Vibration, stringResource(R.string.vibrate_default), null, s.defaultVibrate,
                ) { v -> SettingsRepository.update { it.copy(defaultVibrate = v) } }
            }

            item { SectionHeader(stringResource(R.string.section_wake)) }
            item {
                ChoiceItem(
                    taskIcon(s.defaultTask), stringResource(R.string.default_task_title),
                    WakeTask.entries.map { it.ordinal }, s.defaultTask.ordinal,
                    subtitle = taskHint(s.defaultTask, s.taskRepeats),
                    label = { stringResource(WakeTask.entries[it].title) },
                ) { v -> SettingsRepository.update { it.copy(defaultTask = WakeTask.entries[v]) } }
            }
            item {
                ChoiceItem(
                    Icons.Rounded.Repeat, stringResource(R.string.task_repeats_title), TASK_REPEAT_OPTIONS, s.taskRepeats,
                    subtitle = stringResource(R.string.task_repeats_desc),
                    label = { "×$it" },
                ) { v -> SettingsRepository.update { it.copy(taskRepeats = v) } }
            }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    MaterialPasswordField(
                        s.password, { v -> SettingsRepository.update { it.copy(password = v) } },
                        stringResource(R.string.password_title),
                    )
                    Text(
                        stringResource(R.string.password_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                }
            }
            item {
                ChoiceItem(
                    Icons.Rounded.Snooze, stringResource(R.string.max_snoozes_title), listOf(-1, 0, 1, 2, 3), s.maxSnoozes,
                    subtitle = stringResource(R.string.max_snoozes_desc),
                    label = { if (it < 0) "∞" else "$it" },
                ) { v -> SettingsRepository.update { it.copy(maxSnoozes = v) } }
            }
            item {
                ChoiceItem(
                    Icons.Rounded.Verified, stringResource(R.string.awake_check_title), listOf(0, 3, 5, 10), s.awakeCheckMinutes,
                    subtitle = stringResource(R.string.awake_check_desc),
                    label = { if (it == 0) context.getString(R.string.off) else context.getString(R.string.minutes_short, it) },
                ) { v -> SettingsRepository.update { it.copy(awakeCheckMinutes = v) } }
            }

            item { SectionHeader(stringResource(R.string.section_about)) }
            item { AboutCard(version) }
            item {
                LinkItem(painterIcon = R.drawable.ic_github, title = stringResource(R.string.link_source), subtitle = "github.com/sailxx/Budila") {
                    openUrl(context, GITHUB_REPO)
                }
            }
            item {
                LinkItem(icon = Icons.Rounded.Person, title = stringResource(R.string.link_author), subtitle = "github.com/sailxx") {
                    openUrl(context, GITHUB_USER)
                }
            }
            item {
                LinkItem(icon = Icons.Rounded.SystemUpdate, title = stringResource(R.string.link_updates), subtitle = stringResource(R.string.link_updates_desc)) {
                    openUrl(context, "$GITHUB_REPO/releases/latest")
                }
            }
            item {
                LinkItem(icon = Icons.Rounded.BugReport, title = stringResource(R.string.link_issue), subtitle = stringResource(R.string.link_issue_desc)) {
                    openUrl(context, "$GITHUB_REPO/issues/new")
                }
            }
            item {
                LinkItem(icon = Icons.Rounded.StarOutline, title = stringResource(R.string.link_star), subtitle = stringResource(R.string.link_star_desc)) {
                    openUrl(context, GITHUB_REPO)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun ThemePicker(settings: AppSettings, dark: Boolean, onSelect: (AppTheme) -> Unit) {
    val context = LocalContext.current
    val selected = settings.theme
    val amoled = settings.amoled
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(AppTheme.available) { theme ->
            val customKey = if (theme == AppTheme.CUSTOM) Triple(settings.customHue, settings.customSaturation, settings.customStyle) else null
            val scheme = remember(theme, dark, amoled, customKey) { colorSchemeFor(context, theme, dark, amoled, settings) }
            val isSelected = theme == selected
            val ring by animateDpAsState(if (isSelected) 3.dp else 0.dp, label = "ring")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelect(theme) }
                    .padding(6.dp)
                    .width(68.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .then(if (ring > 0.dp) Modifier.border(ring, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                        .padding(5.dp)
                        .clip(CircleShape),
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawRect(scheme.surfaceContainerHighest)
                        drawRect(scheme.primary, size = Size(w, h / 2))
                        drawRect(scheme.secondaryContainer, topLeft = Offset(0f, h / 2), size = Size(w / 2, h / 2))
                        drawRect(scheme.tertiary, topLeft = Offset(w / 2, h / 2), size = Size(w / 2, h / 2))
                    }
                    if (isSelected) {
                        Surface(shape = CircleShape, color = scheme.primaryContainer, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Check, null, tint = scheme.onPrimaryContainer, modifier = Modifier.padding(4.dp))
                        }
                    } else if (theme == AppTheme.DYNAMIC) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
                    } else if (theme == AppTheme.CUSTOM) {
                        Icon(Icons.Rounded.Palette, null, tint = scheme.onPrimary, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(theme.title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SwitchItem(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceItem(
    icon: ImageVector,
    title: String,
    options: List<Int>,
    selected: Int,
    subtitle: String? = null,
    label: @Composable (Int) -> String = { stringResource(R.string.minutes_short, it) },
    onSelect: (Int) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null) },
        supportingContent = {
            Column {
                if (subtitle != null) Text(subtitle)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    options.forEachIndexed { i, o ->
                        SegmentedButton(
                            selected = o == selected,
                            onClick = { onSelect(o) },
                            shape = SegmentedButtonDefaults.itemShape(i, options.size),
                            icon = {},
                        ) { Text(label(o), maxLines = 1) }
                    }
                }
            }
        },
    )
}

@Composable
private fun LinkItem(
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    painterIcon: Int? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            if (painterIcon != null) Icon(painterResource(painterIcon), null, Modifier.size(24.dp))
            else if (icon != null) Icon(icon, null)
        },
        trailingContent = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, Modifier.size(20.dp)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun AboutCard(version: String) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Budila", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.version, version), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/** Иконка приложения из слоёв адаптивной иконки (видимая зона — центральные 72 из 108 dp). */
@Composable
internal fun AppIcon(modifier: Modifier = Modifier, icon: LauncherIcon? = null) {
    val context = LocalContext.current
    val shown = icon ?: LauncherIcon.current(context)
    Box(modifier, contentAlignment = Alignment.Center) {
        Image(
            painterResource(shown.background), null,
            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        Image(
            painterResource(shown.foreground), null,
            modifier = Modifier.fillMaxSize().scale(1.5f),
        )
    }
}

/** Системный выбор мелодии будильника; выбранная сохраняется в настройках. */
@Composable
internal fun rememberRingtonePicker(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = r.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        SettingsRepository.update {
            it.copy(ringtone = uri?.takeIf { u -> u != Settings.System.DEFAULT_ALARM_ALERT_URI }?.toString())
        }
    }
    return {
        val current = SettingsRepository.current.ringtone?.let(Uri::parse) ?: Settings.System.DEFAULT_ALARM_ALERT_URI
        launcher.launch(
            Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.ringtone_picker_title))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current),
        )
    }
}

@Composable
internal fun rememberAppVersion(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""
    }
}

internal fun ringtoneTitle(context: Context, uri: String?): String {
    if (uri == null) return context.getString(R.string.ringtone_default)
    return runCatching { RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context) }
        .getOrNull() ?: context.getString(R.string.ringtone_custom)
}

internal fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
    }
}

/** Своя тема: тон по кругу цветов, насыщенность и «гамма» — стиль палитры Material 3. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomThemeEditor(s: AppSettings) {
    var hue by remember { mutableFloatStateOf(s.customHue) }
    var saturation by remember { mutableFloatStateOf(s.customSaturation) }
    val seed = Color.hsv(hue, saturation, 0.85f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(seed))
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(stringResource(R.string.custom_theme), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.custom_summary, hue.roundToInt(), (saturation * 100).roundToInt(), stringResource(s.customStyle.title)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(stringResource(R.string.hue), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
            GradientSlider(
                value = hue,
                range = 0f..360f,
                brush = Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.85f, 0.9f) }),
                thumbColor = Color.hsv(hue, 0.85f, 0.9f),
                onChange = { v -> hue = v; SettingsRepository.update(persist = false) { it.copy(customHue = v) } },
                onFinished = { SettingsRepository.update { it.copy(customHue = hue) } },
            )

            Text(stringResource(R.string.saturation), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
            GradientSlider(
                value = saturation,
                range = 0.1f..1f,
                brush = Brush.horizontalGradient(listOf(Color.hsv(hue, 0.1f, 0.85f), Color.hsv(hue, 1f, 0.85f))),
                thumbColor = seed,
                onChange = { v -> saturation = v; SettingsRepository.update(persist = false) { it.copy(customSaturation = v) } },
                onFinished = { SettingsRepository.update { it.copy(customSaturation = saturation) } },
            )

            Text(stringResource(R.string.palette), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomStyle.entries.forEach { style ->
                    FilterChip(
                        selected = s.customStyle == style,
                        onClick = { SettingsRepository.update { it.copy(customStyle = style) } },
                        label = { Text(stringResource(style.title)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GradientSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    brush: Brush,
    thumbColor: Color,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Slider(
        value = value,
        onValueChange = onChange,
        onValueChangeFinished = onFinished,
        valueRange = range,
        thumb = {
            Box(
                Modifier
                    .size(26.dp)
                    .shadow(3.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
                    .background(thumbColor, CircleShape),
            )
        },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(brush),
            )
        },
    )
}
