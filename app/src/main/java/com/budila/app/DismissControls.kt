package com.budila.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Сколько держать палец при жесте «Удержание». */
private const val HOLD_MS = 1_500

/** Цвета и форма для жестов «Свайп» и «Удержание» — свои у «Инструмента» и у Material. */
data class DismissStyle(
    val shape: Shape,
    /** Фон дорожки */
    val track: Color,
    /** Цвет текста на дорожке */
    val onTrack: Color,
    /** Бегунок свайпа и заливка удержания */
    val accent: Color,
    /** Значок на бегунке */
    val onAccent: Color,
    val text: TextStyle,
)

/**
 * Клавиша выключения с жестом из настроек: нажатие ([tap] — обычная клавиша дизайна),
 * свайп до конца дорожки или удержание полторы секунды.
 */
@Composable
fun DismissControl(
    gesture: DismissGesture,
    text: String,
    icon: ImageVector,
    onDismiss: () -> Unit,
    style: DismissStyle,
    modifier: Modifier = Modifier,
    tap: @Composable () -> Unit,
) {
    when (gesture) {
        DismissGesture.TAP -> tap()
        DismissGesture.SLIDE -> SlideToDismiss(text, onDismiss, style, modifier)
        DismissGesture.HOLD -> HoldToDismiss(text, icon, onDismiss, style, modifier)
    }
}

@Composable
private fun SlideToDismiss(text: String, onDone: () -> Unit, style: DismissStyle, modifier: Modifier) {
    val done by rememberUpdatedState(onDone)
    BoxWithConstraints(
        modifier.clip(style.shape).background(style.track).padding(6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val knob = maxHeight
        val maxPx = with(LocalDensity.current) { (maxWidth - knob).toPx() }.coerceAtLeast(1f)
        var offset by remember { mutableFloatStateOf(0f) }
        var dragging by remember { mutableStateOf(false) }
        // Отпустил раньше конца — бегунок плавно возвращается
        val shown by animateFloatAsState(offset, if (dragging) tween(0) else tween(250), label = "knob")
        val progress = shown / maxPx

        Text(
            text, style = style.text, color = style.onTrack,
            modifier = Modifier.align(Alignment.Center).padding(start = knob).alpha(1f - progress),
        )
        Box(
            Modifier
                .offset { IntOffset(shown.roundToInt(), 0) }
                .size(knob)
                .clip(style.shape)
                .background(style.accent)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { d -> offset = (offset + d).coerceIn(0f, maxPx) },
                    onDragStarted = { dragging = true },
                    onDragStopped = {
                        dragging = false
                        if (offset >= maxPx * 0.9f) {
                            offset = maxPx
                            done()
                        } else {
                            offset = 0f
                        }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = style.onAccent)
        }
    }
}

@Composable
private fun HoldToDismiss(text: String, icon: ImageVector, onDone: () -> Unit, style: DismissStyle, modifier: Modifier) {
    val done by rememberUpdatedState(onDone)
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .clip(style.shape)
            .background(style.track)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val fill = scope.launch {
                        progress.animateTo(1f, tween(((1f - progress.value) * HOLD_MS).roundToInt(), easing = LinearEasing))
                        done()
                    }
                    tryAwaitRelease()
                    if (progress.value < 1f) {
                        fill.cancel()
                        scope.launch { progress.animateTo(0f, tween(250)) }
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(progress.value).background(style.accent))
        // Текст меняет цвет вместе с заливкой: после середины он уже на заливке
        val ink = if (progress.value > 0.5f) style.onAccent else style.onTrack
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = ink)
                Spacer(Modifier.width(10.dp))
                Text(text, style = style.text, color = ink)
            }
            Text(stringResource(R.string.gesture_hold_hint), style = style.text.copy(fontSize = style.text.fontSize * 0.75f), color = ink.copy(alpha = 0.7f))
        }
    }
}
