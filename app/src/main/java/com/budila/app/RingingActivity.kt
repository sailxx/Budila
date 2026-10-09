package com.budila.app

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.random.Random

class RingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        // Звонок закончился (выключен из уведомления или по таймауту) — закрываем экран
        lifecycleScope.launch { AlarmService.ringing.collect { if (it == null) finish() } }

        setContent {
            BudilaTheme {
                val r = AlarmService.ringing.collectAsStateWithLifecycle().value ?: return@BudilaTheme
                val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
                val instrument = settings.design == Design.INSTRUMENT
                // Задание начинается заново, если звонок сменился (например, проверка перешла в настоящий звонок)
                LaunchedEffect(r) { solving = false }
                val label = r.alarm.label.ifBlank { null } ?: getString(R.string.alarm_default_label)
                val onSnooze = if (r.canSnooze) ({ AlarmService.snooze(this); finish() }) else null
                val dismiss = { AlarmService.dismiss(this); finish() }

                // «Назад» не выключает звонок; из задания возвращает к звонку
                BackHandler { solving = false }

                when {
                    r.check -> if (instrument) InstrumentCheckScreen(dismiss) else CheckScreen(dismiss)
                    solving -> {
                        val state = remember(r) { WakeTaskState(r.task, settings.password, settings.taskRepeats) }
                        if (instrument) {
                            InstrumentTaskScreen(state, onSolved = dismiss, onBack = { solving = false })
                        } else {
                            TaskScreen(state, onSolved = dismiss, onBack = { solving = false })
                        }
                    }
                    else -> {
                        val onDismiss = if (r.task == WakeTask.NONE) dismiss else ({ solving = true })
                        if (instrument) {
                            InstrumentRingingScreen(label, onSnooze, onDismiss, r.task, r.snoozesLeft)
                        } else {
                            RingingScreen(label, onSnooze, onDismiss, r.task, r.snoozesLeft)
                        }
                    }
                }
            }
        }
    }

    /** Открыт экран задания (а не сам звонок). Здесь, а не в Compose, — чтобы до него дотянулись кнопки громкости. */
    private var solving by mutableStateOf(false)

    /** Кнопки громкости во время звонка — как выбрано в настройках. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val volumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        val r = AlarmService.ringing.value
        val mode = SettingsRepository.current.volumeKeys
        if (!volumeKey || r == null || mode == VolumeKeys.VOLUME) return super.onKeyDown(keyCode, event)
        if (event.repeatCount > 0) return true
        when (mode) {
            VolumeKeys.SNOOZE -> if (r.canSnooze) {
                AlarmService.snooze(this)
                finish()
            }
            // С заданием кнопка не выключает, а открывает задание
            VolumeKeys.DISMISS -> if (r.check || r.task == WakeTask.NONE) {
                AlarmService.dismiss(this)
                finish()
            } else {
                solving = true
            }
            else -> Unit
        }
        return true
    }
}

/**
 * Ход задания: какой пример, что введено, была ли ошибка. Общий для обоих дизайнов.
 * [repeats] — сколько раз подряд выполнить задание (решить примеров / ввести пароль).
 */
@Stable
internal class WakeTaskState(
    val task: WakeTask,
    private val password: String,
    repeats: Int = 3,
    private val random: Random = Random,
) {
    val repeats = repeats.coerceAtLeast(1)
    var problem by mutableStateOf(mathProblem(random))
        private set
    var solved by mutableIntStateOf(0)
        private set
    var input by mutableStateOf("")
    var wrong by mutableStateOf(false)
        private set

    fun type(digit: Char) {
        wrong = false
        if (input.length < 4) input += digit
    }

    fun erase() {
        wrong = false
        input = input.dropLast(1)
    }

    fun edit(text: String) {
        wrong = false
        input = text
    }

    /** Проверить ответ; true — задание выполнено, можно выключать. */
    fun submit(): Boolean {
        val right = if (task == WakeTask.PASSWORD) passwordMatches(input, password) else input.toIntOrNull() == problem.answer
        if (right) {
            solved++
            input = ""
            if (solved >= repeats) return true
            if (task != WakeTask.PASSWORD) problem = mathProblem(random)
            return false
        }
        // Ошибся — новый пример, чтобы ответ нельзя было подобрать
        wrong = true
        input = ""
        if (task != WakeTask.PASSWORD) problem = mathProblem(random)
        return false
    }
}

fun taskActionText(task: WakeTask) = when (task) {
    WakeTask.MATH -> R.string.task_action_math
    WakeTask.PASSWORD -> R.string.task_action_password
    WakeTask.NONE -> R.string.dismiss
}

fun taskIcon(task: WakeTask): ImageVector = when (task) {
    WakeTask.MATH -> Icons.Rounded.Calculate
    WakeTask.PASSWORD -> Icons.Rounded.Password
    WakeTask.NONE -> Icons.Rounded.AlarmOff
}

@Composable
fun taskHint(task: WakeTask, repeats: Int): String = when (task) {
    WakeTask.NONE -> stringResource(R.string.task_hint_none)
    WakeTask.MATH -> stringResource(R.string.task_hint_math, repeats)
    WakeTask.PASSWORD -> stringResource(R.string.task_hint_password, repeats)
}

/** Надпись над полем пароля: при нескольких повторах — «Ввод 2 из 3». */
@Composable
internal fun passwordLegend(state: WakeTaskState): String =
    if (state.repeats > 1) stringResource(R.string.task_entry, state.solved + 1, state.repeats)
    else stringResource(R.string.password_title)

@Composable
fun snoozeText(snoozesLeft: Int?): String {
    val base = stringResource(R.string.snooze_for, SettingsRepository.current.snoozeMinutes)
    return if (snoozesLeft == null) base else stringResource(R.string.snooze_left, base, snoozesLeft)
}

@Composable
internal fun RingingScreen(
    label: String,
    onSnooze: (() -> Unit)?,
    onDismiss: () -> Unit,
    task: WakeTask = WakeTask.NONE,
    snoozesLeft: Int? = null,
    gesture: DismissGesture = SettingsRepository.current.dismissGesture,
) {
    val context = LocalContext.current
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000)
        }
    }
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val colors = MaterialTheme.colorScheme

    Surface(color = colors.primaryContainer, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Text(
                    formatTime(context, now.hour, now.minute),
                    fontSize = 88.sp,
                    fontWeight = FontWeight.Light,
                    color = colors.onPrimaryContainer,
                )
                Text(label, style = MaterialTheme.typography.headlineSmall, color = colors.onPrimaryContainer)
            }

            Box(
                modifier = Modifier
                    .size(168.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(colors.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Alarm, null, tint = colors.onPrimary, modifier = Modifier.size(84.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                val action = stringResource(taskActionText(task))
                DismissControl(
                    gesture, action, taskIcon(task), onDismiss,
                    DismissStyle(CircleShape, colors.surface, colors.primary, colors.primary, colors.onPrimary, MaterialTheme.typography.titleLarge),
                    Modifier.fillMaxWidth().height(72.dp),
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                        Icon(taskIcon(task), null)
                        Spacer(Modifier.width(12.dp))
                        Text(action, style = MaterialTheme.typography.titleLarge)
                    }
                }
                if (onSnooze != null) {
                    FilledTonalButton(
                        onClick = onSnooze,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = colors.surface,
                            contentColor = colors.primary,
                        ),
                    ) {
                        Icon(Icons.Rounded.Snooze, null)
                        Spacer(Modifier.width(12.dp))
                        Text(snoozeText(snoozesLeft), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TaskScreen(state: WakeTaskState, onSolved: () -> Unit, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val submit = { if (state.submit()) onSolved() }

    Surface(color = colors.surface, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.task_prove), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))

            if (state.task == WakeTask.PASSWORD) {
                var visible by remember { mutableStateOf(false) }
                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                OutlinedTextField(
                    value = state.input,
                    onValueChange = state::edit,
                    label = { Text(passwordLegend(state)) },
                    singleLine = true,
                    isError = state.wrong,
                    supportingText = if (state.wrong) ({ Text(stringResource(R.string.password_wrong)) }) else null,
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, stringResource(R.string.password_show))
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            } else {
                Text(
                    stringResource(R.string.task_problem, state.solved + 1, state.repeats),
                    style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant,
                )
                Text("${state.problem.text} =", style = MaterialTheme.typography.displayMedium)
                Text(
                    state.input.ifEmpty { "?" },
                    style = MaterialTheme.typography.displayLarge,
                    color = if (state.input.isEmpty()) colors.onSurfaceVariant else colors.primary,
                )
                Text(
                    if (state.wrong) stringResource(R.string.task_wrong) else "",
                    style = MaterialTheme.typography.bodyLarge, color = colors.error,
                )
                Spacer(Modifier.height(16.dp))
                NumberPad(state::type, state::erase, onClear = { state.edit("") })
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = submit, enabled = state.input.isNotBlank(), modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text(stringResource(R.string.task_submit), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable
private fun NumberPad(onDigit: (Char) -> Unit, onErase: () -> Unit, onClear: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("123", "456", "789", "C0<").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { k ->
                    FilledTonalButton(
                        onClick = {
                            when (k) {
                                'C' -> onClear()
                                '<' -> onErase()
                                else -> onDigit(k)
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                    ) {
                        if (k == '<') {
                            Icon(Icons.AutoMirrored.Rounded.Backspace, null)
                        } else {
                            Text("$k", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

/** Секунды, оставшиеся на ответ повторной проверке. */
@Composable
internal fun rememberCheckSecondsLeft(): Int {
    val startedAt = remember { System.currentTimeMillis() }
    var left by remember { mutableIntStateOf(CHECK_ANSWER_SECONDS) }
    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(250)
            left = (CHECK_ANSWER_SECONDS - (System.currentTimeMillis() - startedAt) / 1000).toInt().coerceAtLeast(0)
        }
    }
    return left
}

@Composable
internal fun CheckScreen(onAwake: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val left = rememberCheckSecondsLeft()
    Surface(color = colors.tertiaryContainer, modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Icon(Icons.Rounded.WbSunny, null, tint = colors.onTertiaryContainer, modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.check_title), style = MaterialTheme.typography.displaySmall, color = colors.onTertiaryContainer)
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.check_hint), style = MaterialTheme.typography.bodyLarge,
                    color = colors.onTertiaryContainer, textAlign = TextAlign.Center,
                )
            }
            Text("%d:%02d".format(left / 60, left % 60),fontSize = 88.sp, fontWeight = FontWeight.Light, color = colors.onTertiaryContainer)
            Button(onClick = onAwake, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                Text(stringResource(R.string.check_awake), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
