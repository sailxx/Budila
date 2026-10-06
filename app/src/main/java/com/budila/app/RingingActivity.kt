package com.budila.app

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

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
                val alarm by AlarmService.ringing.collectAsStateWithLifecycle()
                val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
                val label = alarm?.label?.ifBlank { null } ?: getString(R.string.alarm_default_label)
                val onSnooze = { AlarmService.snooze(this); finish() }
                val onDismiss = { AlarmService.dismiss(this); finish() }
                if (settings.design == Design.INSTRUMENT) {
                    InstrumentRingingScreen(label, onSnooze, onDismiss)
                } else {
                    RingingScreen(label, onSnooze, onDismiss)
                }
            }
        }
    }
}

@Composable
internal fun RingingScreen(label: String, onSnooze: () -> Unit, onDismiss: () -> Unit) {
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
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    Icon(Icons.Rounded.AlarmOff, null)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.dismiss), style = MaterialTheme.typography.titleLarge)
                }
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
                    Text(stringResource(R.string.snooze_for, SettingsRepository.current.snoozeMinutes), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
