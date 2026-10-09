package com.budila.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BudilaTheme {
                // Иконки статус-бара подстраиваем под тему приложения, а не системы
                val dark = isAppInDarkTheme()
                LaunchedEffect(dark) {
                    val style = if (dark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                }

                val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
                val instrument = settings.design == Design.INSTRUMENT
                var settingsOpen by rememberSaveable { mutableStateOf(false) }
                BackHandler(enabled = settingsOpen) { settingsOpen = false }
                Surface(color = MaterialTheme.colorScheme.background) {
                    AnimatedContent(
                        targetState = settingsOpen,
                        transitionSpec = {
                            val dir = if (targetState) 1 else -1
                            (slideInHorizontally { w -> dir * w / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { w -> -dir * w / 4 } + fadeOut())
                        },
                        label = "screen",
                    ) { open ->
                        when {
                            open && instrument -> InstrumentSettingsScreen(onBack = { settingsOpen = false })
                            open -> SettingsScreen(onBack = { settingsOpen = false })
                            instrument -> InstrumentListScreen(onOpenSettings = { settingsOpen = true })
                            else -> AlarmListScreen(onOpenSettings = { settingsOpen = true })
                        }
                    }
                }
            }
        }
    }
}

internal data class EditorState(val alarm: Alarm, val isNew: Boolean)

/** Текущее время, обновляется в начале каждой секунды. */
@Composable
internal fun rememberNow(): ZonedDateTime {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1_000 - System.currentTimeMillis() % 1_000)
        }
    }
    return now
}

/** Разрешения, без которых будильник звонит ненадёжно, и действия, чтобы их выдать. */
internal class Permissions(
    val notifOk: Boolean,
    val exactOk: Boolean,
    val fullScreenOk: Boolean,
    val fixNotif: () -> Unit,
    val fixExact: () -> Unit,
    val fixFullScreen: () -> Unit,
)

@Composable
internal fun rememberPermissions(): Permissions {
    val context = LocalContext.current
    var permTick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permTick++
        AlarmScheduler.rescheduleAll(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permTick++ }
    val notifOk = remember(permTick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val exactOk = remember(permTick) {
        Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
    val fullScreenOk = remember(permTick) {
        Build.VERSION.SDK_INT < 34 || context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
    }
    LaunchedEffect(Unit) {
        if (!notifOk && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    return Permissions(
        notifOk, exactOk, fullScreenOk,
        fixNotif = {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        },
        fixExact = {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
            )
        },
        fixFullScreen = { openFullScreenSettings(context) },
    )
}

/**
 * Экран разрешения на полноэкранные оповещения. На части прошивок (Xiaomi/HyperOS и др.)
 * стандартного экрана нет — тогда открываем первый, который найдётся: разрешения MIUI,
 * настройки уведомлений приложения, карточку приложения.
 */
private fun openFullScreenSettings(context: android.content.Context) {
    val pkg = context.packageName
    val pkgUri = Uri.parse("package:$pkg")
    val candidates = buildList {
        if (Build.VERSION.SDK_INT >= 34) add(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkgUri))
        add(
            Intent("miui.intent.action.APP_PERM_EDITOR")
                .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                .putExtra("extra_pkgname", pkg),
        )
        add(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, pkg))
        add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri))
    }
    for (intent in candidates) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) {
            // ActivityNotFoundException или SecurityException — пробуем следующий экран
        }
    }
}

/** Сохранение и удаление будильников с сообщением внизу экрана (и «Вернуть» после удаления). */
internal class AlarmActions(
    private val context: android.content.Context,
    private val scope: kotlinx.coroutines.CoroutineScope,
    val snackbar: SnackbarHostState,
) {
    fun showMessage(text: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(text, action, duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) {
                onAction()
            }
        }
    }

    fun save(alarm: Alarm) {
        AlarmRepository.upsert(alarm)
        AlarmScheduler.schedule(context, alarm)
        if (alarm.enabled) showMessage(context.getString(R.string.msg_will_ring, untilText(context, alarm.nextTrigger())))
    }

    fun delete(alarm: Alarm) {
        AlarmScheduler.cancel(context, alarm.id)
        AlarmRepository.delete(alarm.id)
        showMessage(context.getString(R.string.msg_deleted), context.getString(R.string.action_undo)) {
            AlarmRepository.upsert(alarm)
            AlarmScheduler.schedule(context, alarm)
        }
    }
}

@Composable
internal fun rememberAlarmActions(): AlarmActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember { AlarmActions(context, scope, SnackbarHostState()) }
}

/** Новый будильник на 7:00 с настройками по умолчанию (и тегом, если включён фильтр). */
internal fun newAlarm(tagFilter: String?): Alarm {
    val s = SettingsRepository.current
    return Alarm(
        AlarmRepository.newId(), 7, 0,
        vibrate = s.defaultVibrate,
        gentle = s.gentleDefault,
        tags = listOfNotNull(tagFilter),
        task = s.defaultTask,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmListScreen(onOpenSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val alarms by AlarmRepository.alarms.collectAsStateWithLifecycle()
    val actions = rememberAlarmActions()
    val snackbar = actions.snackbar
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var tagFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val allTags = remember(alarms) { alarms.flatMap { it.tags }.distinct().sorted() }
    if (tagFilter != null && tagFilter !in allTags) tagFilter = null
    val shown = if (tagFilter == null) alarms else alarms.filter { tagFilter in it.tags }

    val now = rememberNow()
    val perms = rememberPermissions()
    val notifOk = perms.notifOk
    val exactOk = perms.exactOk
    val fullScreenOk = perms.fullScreenOk

    fun save(alarm: Alarm) = actions.save(alarm)

    fun delete(alarm: Alarm) = actions.delete(alarm)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { ClockHeader(now) },
                actions = {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings)) }
                },
                expandedHeight = 96.dp,
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editor = EditorState(newAlarm(tagFilter), isNew = true) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text(stringResource(R.string.add)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 4.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "next") {
                val next = alarms.filter { it.enabled }.minOfOrNull { it.nextTrigger(now) }
                NextAlarmCard(next, now)
            }
            if (!notifOk) item(key = "p_notif") {
                PermissionCard(
                    Icons.Rounded.NotificationsOff,
                    stringResource(R.string.perm_notif_title),
                    stringResource(R.string.perm_notif_text),
                    perms.fixNotif,
                )
            }
            if (!exactOk) item(key = "p_exact") {
                PermissionCard(
                    Icons.Rounded.Schedule,
                    stringResource(R.string.perm_exact_title),
                    stringResource(R.string.perm_exact_text),
                    perms.fixExact,
                )
            }
            if (!fullScreenOk) item(key = "p_fsi") {
                PermissionCard(
                    Icons.Rounded.Fullscreen,
                    stringResource(R.string.perm_fsi_title),
                    stringResource(R.string.perm_fsi_text),
                    perms.fixFullScreen,
                )
            }
            if (allTags.isNotEmpty()) item(key = "tags") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = tagFilter == null,
                            onClick = { tagFilter = null },
                            label = { Text(stringResource(R.string.filter_all, alarms.size)) },
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tagFilter == tag,
                            onClick = { tagFilter = if (tagFilter == tag) null else tag },
                            label = { Text("$tag · ${alarms.count { tag in it.tags }}") },
                            leadingIcon = { Icon(Icons.Rounded.Sell, null, Modifier.size(FilterChipDefaults.IconSize)) },
                        )
                    }
                }
            }
            if (alarms.isEmpty()) item(key = "empty") { EmptyState() }
            items(shown, key = { it.id }) { alarm ->
                AlarmCard(
                    alarm = alarm,
                    onToggle = { save(alarm.copy(enabled = it)) },
                    onClick = { editor = EditorState(alarm, isNew = false) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    editor?.let { state ->
        AlarmEditorSheet(
            state = state,
            allTags = (allTags + defaultTags(context)).distinct(),
            onDismiss = { editor = null },
            onSave = { save(it); editor = null },
            onDelete = { delete(state.alarm); editor = null },
        )
    }
}

@Composable
private fun NextAlarmCard(next: ZonedDateTime?, now: ZonedDateTime) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.next_alarm), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                if (next == null) {
                    Text(stringResource(R.string.not_scheduled), style = MaterialTheme.typography.headlineSmall)
                } else {
                    val day = next.dayOfWeek.getDisplayName(TextStyle.SHORT, appLocale(context))
                        .replaceFirstChar { it.uppercase() }
                    Text("$day, ${formatTime(context, next.hour, next.minute)}", style = MaterialTheme.typography.headlineMedium)
                    Text(untilText(context, next, now), style = MaterialTheme.typography.bodyMedium)
                }
            }
            Icon(
                if (next == null) Icons.Rounded.AlarmOff else Icons.Rounded.AlarmOn,
                null,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, text: String, onFix: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onFix) { Text(stringResource(R.string.perm_allow)) }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.AlarmAdd, null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ClockHeader(now: ZonedDateTime) {
    val context = LocalContext.current
    val date = remember(now.toLocalDate()) {
        now.format(DateTimeFormatter.ofPattern(datePattern(appLocale(context)), appLocale(context))).replaceFirstChar { it.uppercase() }
    }
    Column {
        Text(
            formatTime(context, now.hour, now.minute),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            date,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmCard(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val dark = isAppInDarkTheme()
    val accent = alarm.color?.let { Color(it) }
    val base = if (alarm.enabled) colors.surfaceContainerHigh else colors.surfaceContainerLow
    val tint = when {
        !alarm.enabled -> 0.10f
        dark -> 0.32f
        else -> 0.22f
    }
    val container by animateColorAsState(accent?.let { lerp(base, it, tint) } ?: base, label = "container")
    val content = if (alarm.enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.6f)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatTime(context, alarm.hour, alarm.minute),
                        style = MaterialTheme.typography.displayMedium,
                        color = content,
                    )
                    if (alarm.task != WakeTask.NONE) {
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            taskIcon(alarm.task), stringResource(alarm.task.title),
                            tint = (accent ?: colors.primary).copy(alpha = if (alarm.enabled) 1f else 0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    if (alarm.gentle) {
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Rounded.Spa, stringResource(R.string.gentle_wake),
                            tint = (accent ?: colors.primary).copy(alpha = if (alarm.enabled) 1f else 0.5f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Text(
                    listOfNotNull(alarm.label.ifBlank { null }, daysText(context, alarm.days)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                if (alarm.tags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        alarm.tags.forEach { TagPill(it, accent) }
                    }
                }
            }
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                thumbContent = if (alarm.enabled) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
                colors = if (accent != null) {
                    SwitchDefaults.colors(
                        checkedTrackColor = accent,
                        checkedThumbColor = Color.White,
                        checkedIconColor = accent,
                    )
                } else {
                    SwitchDefaults.colors()
                },
            )
        }
    }
}

@Composable
private fun TagPill(tag: String, accent: Color?) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent?.copy(alpha = 0.25f) ?: MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(tag, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun ColorDot(color: Color?, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color ?: scheme.surfaceContainerHighest)
            .then(if (color == null) Modifier.border(1.5.dp, scheme.outlineVariant, CircleShape) else Modifier)
            .clickable(onClick = onClick),
    ) {
        when {
            selected -> Icon(
                Icons.Rounded.Check, stringResource(R.string.color_selected),
                tint = if (color == null) scheme.onSurface else Color.White,
                modifier = Modifier.size(20.dp),
            )
            color == null -> Icon(Icons.Rounded.FormatColorReset, stringResource(R.string.color_none), tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorSheet(
    state: EditorState,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        AlarmEditorContent(
            alarm = state.alarm,
            isNew = state.isNew,
            allTags = allTags,
            onCancel = { close(onDismiss) },
            onSave = { close { onSave(it) } },
            onDelete = { close(onDelete) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun AlarmEditorContent(
    alarm: Alarm,
    isNew: Boolean,
    allTags: List<String> = emptyList(),
    onCancel: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val initial = alarm
    val timeState = rememberTimePickerState(initial.hour, initial.minute, DateFormat.is24HourFormat(context))
    var keyboardInput by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf(initial.label) }
    var days by remember { mutableIntStateOf(initial.days) }
    var vibrate by remember { mutableStateOf(initial.vibrate) }
    var gentle by remember { mutableStateOf(initial.gentle) }
    var task by remember { mutableStateOf(initial.task) }
    var password by remember { mutableStateOf(SettingsRepository.current.password) }
    var tags by remember { mutableStateOf(initial.tags) }
    var color by remember { mutableStateOf(initial.color) }
    var addingTag by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }

    fun addTag() {
        val t = newTag.trim()
        if (t.isNotEmpty() && t !in tags) tags = tags + t
        newTag = ""
        addingTag = false
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(if (isNew) R.string.editor_new else R.string.editor_edit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { keyboardInput = !keyboardInput }) {
                Icon(if (keyboardInput) Icons.Rounded.Schedule else Icons.Rounded.Keyboard, stringResource(R.string.input_mode))
            }
        }
        Spacer(Modifier.height(16.dp))

        if (keyboardInput) TimeInput(state = timeState) else TimePicker(state = timeState)

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            label = { Text(stringResource(R.string.label_name)) },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Label, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        EditorSection(stringResource(R.string.repeat_label, daysText(context, days)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val names = dayShortNames(context)
            weekOrder().forEach { i ->
                val name = names[i]
                val bit = 1 shl i
                val selected = days and bit != 0
                Surface(
                    onClick = { days = days xor bit },
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(name, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        EditorSection(stringResource(R.string.tags))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            (allTags + tags).distinct().forEach { tag ->
                val selected = tag in tags
                FilterChip(
                    selected = selected,
                    onClick = { tags = if (selected) tags - tag else tags + tag },
                    label = { Text(tag) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else null,
                )
            }
            if (!addingTag) {
                AssistChip(
                    onClick = { addingTag = true },
                    label = { Text(stringResource(R.string.tag_custom)) },
                    leadingIcon = { Icon(Icons.Rounded.Add, null, Modifier.size(AssistChipDefaults.IconSize)) },
                )
            }
        }
        if (addingTag) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { if (it.length <= 20) newTag = it },
                label = { Text(stringResource(R.string.tag_new)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { addTag() }),
                trailingIcon = { IconButton(onClick = { addTag() }) { Icon(Icons.Rounded.Check, stringResource(R.string.tag_add)) } },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        EditorSection(stringResource(R.string.color))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ColorDot(null, color == null) { color = null }
            ALARM_COLORS.forEach { c -> ColorDot(Color(c), color == c) { color = c } }
        }

        EditorSection(stringResource(R.string.task_title))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            WakeTask.entries.forEachIndexed { i, t ->
                SegmentedButton(
                    selected = task == t,
                    onClick = { task = t },
                    shape = SegmentedButtonDefaults.itemShape(i, WakeTask.entries.size),
                    icon = { Icon(taskIcon(t), null, Modifier.size(SegmentedButtonDefaults.IconSize)) },
                ) { Text(stringResource(t.title), maxLines = 1) }
            }
        }
        Text(
            taskHint(task, SettingsRepository.current.taskRepeats),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        if (task == WakeTask.PASSWORD) {
            Spacer(Modifier.height(8.dp))
            MaterialPasswordField(password, { password = it }, stringResource(R.string.password_legend))
        }

        Spacer(Modifier.height(12.dp))
        EditorSwitch(
            Icons.Rounded.Spa, stringResource(R.string.gentle_wake),
            stringResource(R.string.gentle_wake_desc),
            gentle,
        ) { gentle = it }
        EditorSwitch(Icons.Rounded.Vibration, stringResource(R.string.vibration), null, vibrate) { vibrate = it }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (!isNew) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Rounded.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.delete))
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            Spacer(Modifier.width(8.dp))
            Button(enabled = task != WakeTask.PASSWORD || password.isNotBlank(), onClick = {
                if (addingTag) addTag()
                if (task == WakeTask.PASSWORD) SettingsRepository.update { it.copy(password = password.trim()) }
                onSave(
                    initial.copy(
                        hour = timeState.hour,
                        minute = timeState.minute,
                        label = label.trim(),
                        days = days,
                        vibrate = vibrate,
                        gentle = gentle,
                        tags = tags,
                        color = color,
                        enabled = true,
                        task = task,
                    ),
                )
            }) { Text(stringResource(R.string.save)) }
        }
    }
}

@Composable
private fun EditorSection(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
    )
}

/** Поле пароля со значком «показать» (редактор и настройки). */
@Composable
internal fun MaterialPasswordField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Rounded.Password, null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, stringResource(R.string.password_show))
            }
        },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun EditorSwitch(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 8.dp),
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** «Вторник, 6 октября» / «Tuesday, October 6» — порядок частей по правилам языка. */
internal fun datePattern(locale: java.util.Locale): String {
    val best = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return if (best.any { it == ' ' || it == ',' }) best else "EEEE, d MMMM"
}
