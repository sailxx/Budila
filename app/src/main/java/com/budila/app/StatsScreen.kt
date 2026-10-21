package com.budila.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

/** Сколько недель показывает календарь (включая текущую). */
private const val CALENDAR_WEEKS = 5

/** Первый день календаря: начало недели (по настройке) за [CALENDAR_WEEKS] − 1 недель до текущей. */
internal fun calendarStart(today: LocalDate, weekStart: WeekStart): LocalDate {
    val sinceWeekStart = (today.dayOfWeek.value - 1 - weekStart.offset + 7) % 7
    return today.minusDays(sinceWeekStart.toLong()).minusWeeks(CALENDAR_WEEKS - 1L)
}

/** Статистика подъёмов: серия, итоги, среднее время и календарь последних недель. Один вид для обоих дизайнов. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(onBack: () -> Unit, today: LocalDate = LocalDate.now()) {
    val context = LocalContext.current
    val stats by WakeStats.summary.collectAsStateWithLifecycle()
    val settings by SettingsRepository.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.streak_title), context.getString(R.string.unit_days, stats.streak), Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_best), context.getString(R.string.unit_days, stats.best), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stats_woke), "${stats.woke}", Modifier.weight(1f))
                StatTile(stringResource(R.string.stats_missed), "${stats.missed}", Modifier.weight(1f))
                StatTile(
                    stringResource(R.string.stats_avg),
                    stats.avgMinute?.let { formatTime(context, it / 60, it % 60) } ?: "—",
                    Modifier.weight(1f),
                )
            }
            Calendar(stats.days, today, settings.weekStart)
            Text(stringResource(R.string.stats_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 2)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun Calendar(days: Map<LocalDate, Boolean>, today: LocalDate, weekStart: WeekStart) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val start = calendarStart(today, weekStart)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            val names = dayShortNames(context)
            weekOrder(weekStart).forEach {
                Text(
                    names[it], Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant,
                )
            }
        }
        repeat(CALENDAR_WEEKS) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { d ->
                    val date = start.plusDays(week * 7L + d)
                    val woke = days[date]
                    val (bg, fg) = when {
                        woke == true -> colors.primary to colors.onPrimary
                        woke == false -> colors.errorContainer to colors.onErrorContainer
                        else -> colors.surfaceVariant.copy(alpha = 0.4f) to colors.onSurfaceVariant
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(if (date.isAfter(today)) colors.surface else bg, RoundedCornerShape(12.dp))
                            .then(if (date == today) Modifier.border(BorderStroke(2.dp, colors.tertiary), RoundedCornerShape(12.dp)) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!date.isAfter(today)) Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelLarge, color = fg)
                    }
                }
            }
        }
    }
}
