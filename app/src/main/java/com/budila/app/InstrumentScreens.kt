package com.budila.app

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/** Размер цифр, чтобы строка из [chars] знаков заняла ширину [width] (не больше [max]). */
private fun readoutSize(width: Dp, chars: Int, max: Float) = minOf(width.value / (chars * 0.6f), max).sp

/** Время без AM/PM — для крупных цифр; AM/PM выводится отдельной надписью. */
private fun clockDigits(context: android.content.Context, hour: Int, minute: Int): String =
    if (DateFormat.is24HourFormat(context)) "%02d:%02d".format(hour, minute)
    else "%2d:%02d".format(if (hour % 12 == 0) 12 else hour % 12, minute)

private fun amPm(context: android.content.Context, hour: Int): String? =
    if (DateFormat.is24HourFormat(context)) null else if (hour < 12) "AM" else "PM"

// ======================================================================
// Главный экран
// ======================================================================

@Composable
internal fun InstrumentListScreen(onOpenSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val alarms by AlarmRepository.alarms.collectAsStateWithLifecycle()
    val actions = rememberAlarmActions()
    val now = rememberNow()
    val perms = rememberPermissions()
    var editor by remember { mutableStateOf<EditorState?>(null) }
    var tagFilter by rememberSaveable { mutableStateOf<String?>(null) }
    val allTags = remember(alarms) { alarms.flatMap { it.tags }.distinct().sorted() }
    if (tagFilter != null && tagFilter !in allTags) tagFilter = null
    val shown = if (tagFilter == null) alarms else alarms.filter { tagFilter in it.tags }

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = WindowInsets.systemBars.asPaddingValues().let {
                PaddingValues(start = 16.dp, end = 16.dp, top = it.calculateTopPadding() + 8.dp, bottom = it.calculateBottomPadding() + 104.dp)
            },
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "top") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Wordmark()
                    Spacer(Modifier.weight(1f))
                    IconKey(onOpenSettings) { Icon(Icons.Rounded.Tune, stringResource(R.string.settings), Modifier.size(20.dp)) }
                }
            }
            item(key = "next") { NextWell(alarms, now) }
            if (!perms.notifOk) item(key = "p_notif") {
                PermissionWell(R.string.perm_notif_title, R.string.perm_notif_text, perms.fixNotif)
            }
            if (!perms.exactOk) item(key = "p_exact") {
                PermissionWell(R.string.perm_exact_title, R.string.perm_exact_text, perms.fixExact)
            }
            if (!perms.fullScreenOk) item(key = "p_fsi") {
                PermissionWell(R.string.perm_fsi_title, R.string.perm_fsi_text, perms.fixFullScreen)
            }
            if (allTags.isNotEmpty()) item(key = "tags") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    item {
                        Chip(stringResource(R.string.filter_all_short), tagFilter == null, { tagFilter = null }, count = alarms.size)
                    }
                    items(allTags) { tag ->
                        Chip(
                            tag, tagFilter == tag, { tagFilter = if (tagFilter == tag) null else tag },
                            count = alarms.count { tag in it.tags },
                        )
                    }
                }
            }
            if (alarms.isEmpty()) item(key = "empty") { EmptyWell() }
            itemsIndexed(shown, key = { _, a -> a.id }) { i, alarm ->
                AlarmWell(
                    alarm = alarm,
                    order = i + 1,
                    onToggle = { actions.save(alarm.copy(enabled = it)) },
                    onClick = { editor = EditorState(alarm, isNew = false) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        // Плавающая клавиша — единственная инвертированная на экране
        Key(
            onClick = { editor = EditorState(newAlarm(tagFilter), isNew = true) },
            radius = 14.dp,
            face = colors.primary,
            contentColor = colors.onPrimary,
            drop = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 20.dp)
                .size(60.dp),
        ) { Icon(Icons.Rounded.Add, stringResource(R.string.add), Modifier.size(28.dp)) }

        SnackbarHost(
            actions.snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp),
        ) { Toast(it) }
    }

    editor?.let { state ->
        InstrumentEditorSheet(
            state = state,
            allTags = (allTags + defaultTags(context)).distinct(),
            onDismiss = { editor = null },
            onSave = { actions.save(it); editor = null },
            onDelete = { actions.delete(state.alarm); editor = null },
        )
    }
}

/** Главный дисплей: следующий будильник крупно, сколько осталось и шкала суток. */
@Composable
private fun NextWell(alarms: List<Alarm>, now: ZonedDateTime) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val enabled = alarms.filter { it.enabled }
    val next = enabled.minByOrNull { it.nextTrigger(now) }
    val at = next?.nextTrigger(now)
    Well(Modifier.fillMaxWidth()) {
        LegendRow(stringResource(R.string.legend_next)) {
            if (at != null) {
                val day = when (at.toLocalDate()) {
                    now.toLocalDate() -> stringResource(R.string.today)
                    now.toLocalDate().plusDays(1) -> stringResource(R.string.tomorrow)
                    else -> at.dayOfWeek.getDisplayName(TextStyle.SHORT, appLocale(context))
                }
                Text(day.uppercase(), style = IType.label, color = colors.wellInk)
            }
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val size = readoutSize(maxWidth, 5, 104f)
            Row(verticalAlignment = Alignment.Bottom) {
                Digits(
                    if (at != null) clockDigits(context, at.hour, at.minute) else "--:--",
                    IType.readout(size),
                    color = if (at != null) colors.wellInk else colors.wellDim,
                )
                if (at != null) amPm(context, at.hour)?.let {
                    Spacer(Modifier.width(8.dp))
                    Text(it, style = IType.label, color = colors.wellDim, modifier = Modifier.padding(bottom = 10.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (at != null) untilText(context, at, now) else stringResource(R.string.not_scheduled),
            style = IType.reading,
            color = colors.wellDim,
        )
        Spacer(Modifier.height(14.dp))
        DayStrip(enabled, now)
    }
}

/** Шкала суток 00–24: отметка на каждом включённом будильнике, красная черта — сейчас. */
@Composable
private fun DayStrip(alarms: List<Alarm>, now: ZonedDateTime) {
    val colors = LocalInstrument.current
    val today = 1 shl (now.dayOfWeek.value - 1)
    val tomorrow = 1 shl (now.dayOfWeek.value % 7)
    Column {
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val w = size.width
            val h = size.height
            val px = 1.dp.toPx()
            drawRect(colors.wellGhost, topLeft = Offset(0f, h - px), size = Size(w, px))
            for (hr in 0..24 step 3) {
                val x = (w - px) * hr / 24f
                drawRect(colors.wellGhost, topLeft = Offset(x, h - 6 * px), size = Size(px, 5 * px))
            }
            val bar = 3 * px
            alarms.forEach { a ->
                val minutes = a.hour * 60 + a.minute
                val x = (w - bar) * minutes / 1440f
                // Ярко — то, что прозвенит в ближайшие сутки; остальное приглушено
                val passed = minutes <= now.hour * 60 + now.minute
                val soon = if (passed) a.days == 0 || a.days and tomorrow != 0 else a.days == 0 || a.days and today != 0
                drawRoundRect(
                    if (soon) colors.wellInk else colors.wellInk.copy(alpha = 0.3f),
                    topLeft = Offset(x, h - 16 * px), size = Size(bar, 14 * px), cornerRadius = CornerRadius(px),
                )
            }
            val nowX = (w - 2 * px) * (now.hour * 60 + now.minute) / 1440f
            drawRect(colors.red, topLeft = Offset(nowX, 0f), size = Size(2 * px, h))
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach {
                Text(it, style = IType.label.copy(fontSize = 10.sp, letterSpacing = 0.sp), color = colors.wellDim)
            }
        }
    }
}

/** Будильник — дисплей: дни недели, время, подпись и теги; выключенный гаснет. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlarmWell(alarm: Alarm, order: Int, onToggle: (Boolean) -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val on = alarm.enabled
    Well(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (alarm.color != null) {
                Box(Modifier.size(8.dp).background(Color(alarm.color).copy(alpha = if (on) 1f else 0.5f), RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
            }
            if (alarm.days == 0) {
                Text(stringResource(R.string.repeat_once).uppercase(), style = IType.label, color = colors.wellDim)
            } else {
                DayLetters(alarm.days, on)
            }
            Spacer(Modifier.weight(1f))
            if (alarm.task != WakeTask.NONE) {
                Icon(taskIcon(alarm.task), stringResource(alarm.task.title), tint = colors.wellDim, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
            }
            if (alarm.gentle) {
                Icon(Icons.Rounded.Spa, stringResource(R.string.gentle_wake), tint = colors.wellDim, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                Digits(
                    clockDigits(context, alarm.hour, alarm.minute),
                    IType.readout(52.sp),
                    color = if (on) colors.wellInk else colors.wellDim.copy(alpha = 0.6f),
                    order = order,
                )
                amPm(context, alarm.hour)?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = IType.label, color = colors.wellDim, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            InstrumentSwitch(on, onToggle, onWell = true)
        }
        val caption = alarm.label.ifBlank { null }
        if (caption != null || alarm.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (caption != null) {
                    Text(
                        caption, style = IType.bodyStrong,
                        color = if (on) colors.wellInk else colors.wellDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                alarm.tags.forEach { tag ->
                    Text(
                        tag,
                        style = IType.small.copy(fontSize = 12.sp),
                        color = colors.wellDim,
                        modifier = Modifier
                            .border(1.dp, colors.wellEdge, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

/** Семь букв дней недели: дни звонка горят, остальные — как незажжённые сегменты. */
@Composable
private fun DayLetters(days: Int, on: Boolean) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val names = remember { dayShortNames(context) }
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        weekOrder().forEach { i ->
            val name = names[i]
            val lit = days and (1 shl i) != 0
            Text(
                name.take(2).uppercase(),
                style = IType.label.copy(letterSpacing = 0.04.sp),
                color = when {
                    !lit -> colors.wellDim.copy(alpha = 0.35f)
                    on -> colors.wellInk
                    else -> colors.wellDim
                },
            )
        }
    }
}

@Composable
private fun PermissionWell(title: Int, text: Int, onFix: () -> Unit) {
    val colors = LocalInstrument.current
    Well(Modifier.fillMaxWidth()) {
        LegendRow(stringResource(R.string.legend_attention)) {
            Text("!", style = IType.label, color = colors.red)
        }
        Spacer(Modifier.height(8.dp))
        Text(stringResource(title), style = IType.bodyStrong, color = colors.red)
        Text(stringResource(text), style = IType.small, color = colors.wellDim)
        Spacer(Modifier.height(12.dp))
        TextKey(stringResource(R.string.perm_allow), onFix, Modifier.height(42.dp))
    }
}

@Composable
private fun EmptyWell() {
    val colors = LocalInstrument.current
    Well(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        LegendRow(stringResource(R.string.legend_alarms)) { Text("0", style = IType.label, color = colors.wellDim) }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.empty_title), style = IType.bodyStrong, color = colors.wellInk)
        Text(stringResource(R.string.empty_text_key), style = IType.small, color = colors.wellDim)
    }
}

// ======================================================================
// Редактор: время набирается на клавишах, как на калькуляторе
// ======================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstrumentEditorSheet(
    state: EditorState,
    allTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalInstrument.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close(then: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { then() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(36.dp, 4.dp).background(colors.line, RoundedCornerShape(2.dp)))
        },
    ) {
        InstrumentEditorContent(
            alarm = state.alarm,
            isNew = state.isNew,
            allTags = allTags,
            onCancel = { close(onDismiss) },
            onSave = { close { onSave(it) } },
            onDelete = { close(onDelete) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun InstrumentEditorContent(
    alarm: Alarm,
    isNew: Boolean,
    allTags: List<String> = emptyList(),
    onCancel: () -> Unit,
    onSave: (Alarm) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val is24 = DateFormat.is24HourFormat(context)

    // Набор времени: null — показано текущее значение; иначе введённые цифры (до 4), как в микроволновке
    var entry by remember { mutableStateOf<String?>(null) }
    var pm by remember { mutableStateOf(alarm.hour >= 12) }
    var label by remember { mutableStateOf(alarm.label) }
    var days by remember { mutableIntStateOf(alarm.days) }
    var vibrate by remember { mutableStateOf(alarm.vibrate) }
    var gentle by remember { mutableStateOf(alarm.gentle) }
    var task by remember { mutableStateOf(alarm.task) }
    var password by remember { mutableStateOf(SettingsRepository.current.password) }
    var tags by remember { mutableStateOf(alarm.tags) }
    var color by remember { mutableStateOf(alarm.color) }
    var addingTag by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf("") }

    val shownHour = if (is24) alarm.hour else (if (alarm.hour % 12 == 0) 12 else alarm.hour % 12)
    val digits = entry?.padStart(4, '0') ?: "%02d%02d".format(shownHour, alarm.minute)
    val h = digits.take(2).toInt()
    val m = digits.takeLast(2).toInt()
    val valid = m <= 59 && if (is24) h <= 23 else h in 1..12
    val hour24 = if (is24) h else (h % 12) + if (pm) 12 else 0

    fun type(d: Char) { entry = ((entry ?: "") + d).trimStart('0').takeLast(4) }

    fun addTag() {
        val t = newTag.trim()
        if (t.isNotEmpty() && t !in tags) tags = tags + t
        newTag = ""
        addingTag = false
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
    ) {
        Text(
            stringResource(if (isNew) R.string.editor_new else R.string.editor_edit),
            style = IType.title, color = colors.ink,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
        Spacer(Modifier.height(16.dp))

        // --- дисплей времени ---
        Well(Modifier.fillMaxWidth(), padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
            LegendRow(stringResource(if (entry != null) R.string.legend_input else R.string.legend_time)) {
                if (valid) {
                    val preview = alarm.copy(hour = hour24, minute = m, days = days).nextTrigger()
                    Text(untilText(context, preview), style = IType.reading.copy(fontSize = 12.sp), color = colors.wellDim, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                val size = readoutSize(maxWidth, 5, 96f)
                Row(verticalAlignment = Alignment.Bottom) {
                    if (!is24) {
                        Text(if (pm) "PM" else "AM", style = IType.label, color = colors.wellDim, modifier = Modifier.padding(bottom = 12.dp, end = 10.dp))
                    }
                    Digits(
                        "${digits.take(2)}:${digits.takeLast(2)}",
                        IType.readout(size),
                        color = if (valid) colors.wellInk else colors.red,
                    )
                }
            }
        }
        if (!is24) {
            Spacer(Modifier.height(10.dp))
            Strip(listOf("AM", "PM"), if (pm) 1 else 0, { pm = it == 1 })
        }
        Spacer(Modifier.height(12.dp))

        // --- клавиатура ---
        val rows = listOf("123", "456", "789")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { d -> PadKey(Modifier.weight(1f), { type(d) }) { Text("$d", style = padStyle) } }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PadKey(Modifier.weight(1f), { entry = "" }) { Text("C", style = padStyle) }
                PadKey(Modifier.weight(1f), { type('0') }) { Text("0", style = padStyle) }
                PadKey(Modifier.weight(1f), { entry = (entry ?: digits.trimStart('0')).dropLast(1) }) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.erase), Modifier.size(20.dp))
                }
            }
        }

        // --- название ---
        Spacer(Modifier.height(22.dp))
        Field(
            label, { label = it }, stringResource(R.string.label_name),
            placeholder = stringResource(R.string.alarm_default_label),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        // --- повтор ---
        Spacer(Modifier.height(20.dp))
        CasingLegend(stringResource(R.string.repeat_label, daysText(context, days)))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val names = dayShortNames(context)
            weekOrder().forEach { i ->
                val name = names[i]
                val bit = 1 shl i
                val sel = days and bit != 0
                Key({ days = days xor bit }, Modifier.weight(1f).height(44.dp), latched = sel, contentColor = if (sel) colors.ink else colors.muted) {
                    Text(name, style = IType.legend.copy(fontSize = 12.sp, letterSpacing = 0.02.sp))
                    if (sel) Led(true, Modifier.align(Alignment.TopEnd).padding(5.dp), size = 4.dp)
                }
            }
        }

        // --- теги ---
        Spacer(Modifier.height(20.dp))
        CasingLegend(stringResource(R.string.tags))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (allTags + tags).distinct().forEach { tag ->
                val sel = tag in tags
                Chip(tag, sel, { tags = if (sel) tags - tag else tags + tag })
            }
            if (!addingTag) {
                Row(
                    Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { addingTag = true }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Add, null, tint = colors.muted, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.tag_custom).uppercase(), style = IType.legend.copy(fontSize = 11.sp), color = colors.muted)
                }
            }
        }
        if (addingTag) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Field(
                    newTag, { if (it.length <= 20) newTag = it }, stringResource(R.string.tag_new),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addTag() }),
                )
                Spacer(Modifier.width(8.dp))
                IconKey({ addTag() }, size = 48.dp) { Icon(Icons.Rounded.Check, stringResource(R.string.tag_add)) }
            }
        }

        // --- цвет: только маленькие метки, как цвета списков в Okto ---
        Spacer(Modifier.height(20.dp))
        CasingLegend(stringResource(R.string.color))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<Int?>(null) + ALARM_COLORS).forEach { col ->
                val sel = color == col
                Key({ color = col }, Modifier.size(40.dp), latched = sel) {
                    if (col == null) {
                        Box(Modifier.size(12.dp, 2.dp).background(colors.muted))
                    } else {
                        Box(Modifier.size(12.dp).background(Color(col), RoundedCornerShape(3.dp)))
                    }
                    if (sel) Led(true, Modifier.align(Alignment.TopEnd).padding(4.dp), size = 4.dp)
                }
            }
        }

        // --- как выключить: проверка, что проснулся ---
        Spacer(Modifier.height(20.dp))
        CasingLegend(stringResource(R.string.task_title))
        Spacer(Modifier.height(8.dp))
        Strip(WakeTask.entries.map { stringResource(it.title) }, task.ordinal, { task = WakeTask.entries[it] })
        Text(taskHint(task, SettingsRepository.current.taskRepeats), style = IType.small, color = colors.muted, modifier = Modifier.padding(top = 8.dp))
        if (task == WakeTask.PASSWORD) {
            Spacer(Modifier.height(12.dp))
            PasswordField(password, { password = it }, stringResource(R.string.password_legend))
        }

        // --- переключатели ---
        Spacer(Modifier.height(14.dp))
        SwitchRow(Icons.Rounded.Spa, stringResource(R.string.gentle_wake), stringResource(R.string.gentle_wake_desc), gentle) { gentle = it }
        SwitchRow(Icons.Rounded.Vibration, stringResource(R.string.vibration), null, vibrate) { vibrate = it }

        // --- клавиши ---
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!isNew) {
                TextKey(stringResource(R.string.delete), onDelete, Modifier.weight(1f), danger = true)
            }
            TextKey(stringResource(R.string.cancel), onCancel, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        PrimaryKey(
            stringResource(R.string.save),
            onClick = {
                if (addingTag) addTag()
                if (task == WakeTask.PASSWORD) SettingsRepository.update { it.copy(password = password.trim()) }
                onSave(
                    alarm.copy(
                        hour = hour24, minute = m,
                        label = label.trim(), days = days,
                        vibrate = vibrate, gentle = gentle,
                        tags = tags, color = color, enabled = true,
                        task = task,
                    ),
                )
            },
            enabled = valid && (task != WakeTask.PASSWORD || password.isNotBlank()),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private val padStyle = IType.readout(20.sp).copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)

@Composable
private fun PadKey(modifier: Modifier, onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Key(onClick, modifier.height(52.dp), content = content)
}

/** Поле пароля с клавишей «показать» (редактор и настройки). */
@Composable
internal fun PasswordField(value: String, onChange: (String) -> Unit, legend: String) {
    var visible by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.Bottom) {
        Field(
            value, onChange, legend,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        )
        Spacer(Modifier.width(8.dp))
        IconKey({ visible = !visible }, size = 48.dp) {
            Icon(
                if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                stringResource(R.string.password_show), Modifier.size(20.dp),
            )
        }
    }
}

/** Строка с переключателем на корпусе (редактор и настройки). */
@Composable
internal fun SwitchRow(icon: ImageVector?, title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalInstrument.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onChange(!checked) }
            .padding(vertical = 10.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, tint = colors.muted, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = IType.body.copy(fontWeight = FontWeight.Medium), color = colors.ink)
            if (subtitle != null) Text(subtitle, style = IType.small, color = colors.muted)
        }
        Spacer(Modifier.width(12.dp))
        InstrumentSwitch(checked, onChange)
    }
}

// ======================================================================
// Звонок: красный циферблат, как у будильника на тумбочке
// ======================================================================

@Composable
internal fun InstrumentRingingScreen(
    label: String,
    onSnooze: (() -> Unit)?,
    onDismiss: () -> Unit,
    task: WakeTask = WakeTask.NONE,
    snoozesLeft: Int? = null,
    gesture: DismissGesture = SettingsRepository.current.dismissGesture,
) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val timeout = remember { SettingsRepository.current.timeoutMinutes * 60 }
    // 0 — звонок не затихает сам: отсчёта нет, все сегменты горят
    val never = timeout <= 0
    val startedAt = remember { System.currentTimeMillis() }
    val now = rememberNow()
    val elapsed = ((now.toInstant().toEpochMilli() - startedAt) / 1000).toInt().coerceIn(0, timeout)
    val left = timeout - elapsed
    val time = LocalTime.of(now.hour, now.minute)
    val digits = clockDigits(context, time.hour, time.minute)
    // Двоеточие мигает раз в секунду
    val shown = if (now.second % 2 == 0) digits else digits.replace(':', ' ')

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Wordmark()
            Spacer(Modifier.weight(1f))
            val locale = appLocale(context)
            Text(
                now.format(DateTimeFormatter.ofPattern("EEE d MMM", locale)).uppercase(locale),
                style = IType.label, color = colors.muted,
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(label, style = IType.headline.copy(fontSize = 34.sp, lineHeight = 38.sp), color = colors.ink)
        Spacer(Modifier.weight(1f))

        Well(Modifier.fillMaxWidth(), padding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp)) {
            LegendRow(stringResource(R.string.legend_ringing)) {
                if (!never) {
                    Text(
                        stringResource(R.string.legend_silence, "%d:%02d".format(left / 60, left % 60)).uppercase(),
                        style = IType.label, color = colors.wellDim,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                val size = readoutSize(maxWidth, 5, 150f)
                Row(verticalAlignment = Alignment.Bottom) {
                    amPm(context, time.hour)?.let {
                        Text(it, style = IType.label, color = colors.red, modifier = Modifier.padding(bottom = 14.dp, end = 10.dp))
                    }
                    Digits(shown, IType.readout(size), color = colors.red)
                }
            }
            Spacer(Modifier.height(14.dp))
            Segments(24, if (never) 24 else (24 * left + timeout - 1) / timeout.coerceAtLeast(1), color = colors.red)
        }

        Spacer(Modifier.weight(1f))
        val action = stringResource(taskActionText(task))
        DismissControl(
            gesture, action, taskIcon(task), onDismiss,
            DismissStyle(RoundedCornerShape(8.dp), colors.key, colors.keyInk, colors.primary, colors.onPrimary, IType.keyText),
            Modifier.fillMaxWidth().height(72.dp),
        ) {
            PrimaryKey(
                action, onDismiss,
                Modifier.fillMaxWidth().height(72.dp),
                icon = { Icon(taskIcon(task), null) },
            )
        }
        if (onSnooze != null) {
            Spacer(Modifier.height(12.dp))
            TextKey(
                snoozeText(snoozesLeft), onSnooze,
                Modifier.fillMaxWidth().height(60.dp),
                icon = { Icon(Icons.Rounded.Snooze, null) },
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ======================================================================
// Задание: доказать, что проснулся
// ======================================================================

@Composable
internal fun InstrumentTaskScreen(state: WakeTaskState, onSolved: () -> Unit, onBack: () -> Unit) {
    val colors = LocalInstrument.current
    val submit = { if (state.submit()) onSolved() }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconKey(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), Modifier.size(20.dp)) }
            Spacer(Modifier.weight(1f))
            Wordmark()
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.task_prove), style = IType.headline.copy(fontSize = 30.sp, lineHeight = 34.sp), color = colors.ink)
        Spacer(Modifier.weight(1f))

        if (state.task == WakeTask.PASSWORD) {
            var visible by remember { mutableStateOf(false) }
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            Row(verticalAlignment = Alignment.Bottom) {
                Field(
                    state.input, state::edit, passwordLegend(state),
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    inputModifier = Modifier.focusRequester(focus),
                    error = state.wrong,
                )
                Spacer(Modifier.width(8.dp))
                IconKey({ visible = !visible }, size = 48.dp) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        stringResource(R.string.password_show), Modifier.size(20.dp),
                    )
                }
            }
            Text(
                if (state.wrong) stringResource(R.string.password_wrong).uppercase() else "",
                style = IType.label, color = colors.red, modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Well(Modifier.fillMaxWidth(), padding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
                LegendRow(stringResource(R.string.task_problem, state.solved + 1, state.repeats)) {
                    if (state.wrong) Text(stringResource(R.string.task_wrong).uppercase(), style = IType.label, color = colors.red)
                }
                Spacer(Modifier.height(10.dp))
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    val text = "${state.problem.text} = ${state.input.ifEmpty { "?" }}"
                    Text(
                        text, style = IType.readout(readoutSize(maxWidth, text.length.coerceAtLeast(9), 54f)),
                        color = colors.wellInk, maxLines = 1,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Segments(state.repeats, state.solved)
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("123", "456", "789").forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { d -> PadKey(Modifier.weight(1f), { state.type(d) }) { Text("$d", style = padStyle) } }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PadKey(Modifier.weight(1f), { state.edit("") }) { Text("C", style = padStyle) }
                    PadKey(Modifier.weight(1f), { state.type('0') }) { Text("0", style = padStyle) }
                    PadKey(Modifier.weight(1f), state::erase) {
                        Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.erase), Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryKey(
            stringResource(R.string.task_submit), submit,
            Modifier.fillMaxWidth().height(64.dp),
            enabled = state.input.isNotBlank(),
        )
        Spacer(Modifier.height(8.dp))
    }
}

// ======================================================================
// Повторная проверка: «Точно встал?»
// ======================================================================

@Composable
internal fun InstrumentCheckScreen(onAwake: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalInstrument.current
    val left = rememberCheckSecondsLeft()
    val now = rememberNow()

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Wordmark()
            Spacer(Modifier.weight(1f))
            Text(clockDigits(context, now.hour, now.minute).trim(), style = IType.label, color = colors.muted)
        }
        Spacer(Modifier.height(28.dp))
        Text(stringResource(R.string.check_title), style = IType.headline.copy(fontSize = 34.sp, lineHeight = 38.sp), color = colors.ink)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.check_hint), style = IType.body, color = colors.muted)
        Spacer(Modifier.weight(1f))

        Well(Modifier.fillMaxWidth(), padding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp)) {
            LegendRow(stringResource(R.string.legend_check))
            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Digits("%d:%02d".format(left / 60, left % 60), IType.readout(readoutSize(maxWidth, 5, 150f)), color = colors.wellInk)
            }
            Spacer(Modifier.height(14.dp))
            Segments(24, (24 * left + CHECK_ANSWER_SECONDS - 1) / CHECK_ANSWER_SECONDS)
        }

        Spacer(Modifier.weight(1f))
        PrimaryKey(
            stringResource(R.string.check_awake), onAwake,
            Modifier.fillMaxWidth().height(72.dp),
            icon = { Icon(Icons.Rounded.WbSunny, null) },
        )
        Spacer(Modifier.height(8.dp))
    }
}
