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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.input.KeyboardCapitalization
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
        setContent { BudilaTheme { AlarmListScreen() } }
    }
}

private data class EditorState(val alarm: Alarm, val isNew: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmListScreen() {
    val context = LocalContext.current
    val alarms by AlarmRepository.alarms.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var editor by remember { mutableStateOf<EditorState?>(null) }

    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(10_000)
        }
    }

    // --- разрешения ---
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
        if (alarm.enabled) showMessage("Будильник прозвенит " + untilText(alarm.nextTrigger()))
    }

    fun delete(alarm: Alarm) {
        AlarmScheduler.cancel(context, alarm.id)
        AlarmRepository.delete(alarm.id)
        showMessage("Будильник удалён", "Вернуть") {
            AlarmRepository.upsert(alarm)
            AlarmScheduler.schedule(context, alarm)
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(title = { Text("Budila") }, scrollBehavior = scrollBehavior)
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editor = EditorState(Alarm(AlarmRepository.newId(), 7, 0), isNew = true) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Добавить") },
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
                    "Уведомления выключены",
                    "Без них будильник не сможет показать экран звонка.",
                ) {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            if (!exactOk) item(key = "p_exact") {
                PermissionCard(
                    Icons.Rounded.Schedule,
                    "Нет доступа к точным будильникам",
                    "Без него будильник может опаздывать.",
                ) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                }
            }
            if (!fullScreenOk) item(key = "p_fsi") {
                PermissionCard(
                    Icons.Rounded.Fullscreen,
                    "Нет полноэкранных оповещений",
                    "Разрешите, чтобы будильник открывался поверх экрана блокировки.",
                ) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                }
            }
            if (alarms.isEmpty()) item(key = "empty") { EmptyState() }
            items(alarms, key = { it.id }) { alarm ->
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
                Text("Следующий будильник", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                if (next == null) {
                    Text("Не запланирован", style = MaterialTheme.typography.headlineSmall)
                } else {
                    val day = next.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("ru"))
                        .replaceFirstChar { it.uppercase() }
                    Text("$day, ${formatTime(context, next.hour, next.minute)}", style = MaterialTheme.typography.headlineMedium)
                    Text(untilText(next, now), style = MaterialTheme.typography.bodyMedium)
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
            TextButton(onClick = onFix) { Text("Разрешить") }
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
        Text("Пока нет будильников", style = MaterialTheme.typography.titleMedium)
        Text(
            "Нажмите «Добавить», чтобы создать первый",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AlarmCard(alarm: Alarm, onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (alarm.enabled) colors.surfaceContainerHigh else colors.surfaceContainerLow, label = "container",
    )
    val content = if (alarm.enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.6f)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 24.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatTime(context, alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.displayMedium,
                    color = content,
                )
                Text(
                    listOfNotNull(alarm.label.ifBlank { null }, daysText(alarm.days)).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
                thumbContent = if (alarm.enabled) {
                    { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
                } else null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmEditorSheet(
    state: EditorState,
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
            onCancel = { close(onDismiss) },
            onSave = { close { onSave(it) } },
            onDelete = { close(onDelete) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlarmEditorContent(
    alarm: Alarm,
    isNew: Boolean,
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

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (isNew) "Новый будильник" else "Изменить будильник",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { keyboardInput = !keyboardInput }) {
                    Icon(if (keyboardInput) Icons.Rounded.Schedule else Icons.Rounded.Keyboard, "Режим ввода")
                }
            }
            Spacer(Modifier.height(16.dp))

            if (keyboardInput) TimeInput(state = timeState) else TimePicker(state = timeState)

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Название") },
                leadingIcon = { Icon(Icons.Rounded.Label, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "Повтор: ${daysText(days).lowercase()}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DAY_SHORT.forEachIndexed { i, name ->
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

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Vibration, null)
                Spacer(Modifier.width(16.dp))
                Text("Вибрация", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (!isNew) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Rounded.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Удалить")
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onCancel) { Text("Отмена") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val result = initial.copy(
                        hour = timeState.hour,
                        minute = timeState.minute,
                        label = label.trim(),
                        days = days,
                        vibrate = vibrate,
                        enabled = true,
                    )
                    onSave(result)
                }) { Text("Сохранить") }
            }
        }
}
