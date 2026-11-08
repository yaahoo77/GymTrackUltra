package com.gymtrack.ultra.features.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.ui.components.EmptyState
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.IconSquareButton
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.components.SkeletonBlock
import com.gymtrack.ultra.ui.components.StatTile
import com.gymtrack.ultra.ui.components.appear
import com.gymtrack.ultra.ui.components.rememberAppearRegistry
import com.gymtrack.ultra.ui.exercise.ExerciseFigure
import com.gymtrack.ultra.ui.exercise.ExerciseMotions
import com.gymtrack.ultra.ui.theme.GtColors
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToLong

private val GenitiveMonths = listOf(
    "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
    "lipca", "sierpnia", "września", "października", "listopada", "grudnia",
)

private fun fullDate(instant: Instant, zone: ZoneId): String {
    val date = instant.atZone(zone).toLocalDate()
    return "${Formatters.capitalize(Formatters.dayName(date.dayOfWeek))}, ${date.dayOfMonth} ${GenitiveMonths[date.monthValue - 1]} ${date.year}"
}

private fun volumeText(kg: Double): String =
    String.format(Locale.forLanguageTag("pl-PL"), "%,d kg", kg.roundToLong())

private fun minutesText(millis: Long): String = Formatters.durationMinutes(millis / 60_000)

@Composable
private fun BackHeader(title: String, subtitle: String?, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconSquareButton(icon = GtIcons.ChevronLeft, contentDescription = "Wróć", onClick = onBack)
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = GtColors.TextPrimary)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
    }
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appearRegistry = rememberAppearRegistry()
    Column(Modifier.fillMaxSize()) {
        BackHeader(
            "Historia",
            if (state.isLoading) null else Formatters.countLabel(state.total, "trening", "treningi", "treningów"),
            onBack,
        )
        when {
            state.isLoading -> Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(height = 88.dp)
                SkeletonBlock(height = 88.dp)
            }
            state.total == 0 -> Box(Modifier.padding(16.dp)) {
                GtCard {
                    EmptyState(
                        title = "Jeszcze pusto",
                        message = "Każdy ukończony trening pojawi się tutaj — z seriami, ciężarami i rekordami.",
                        icon = GtIcons.Clock,
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val indexOf = state.months.flatMap { it.items }.withIndex().associate { it.value.sessionId to it.index }
                state.months.forEach { month ->
                    item(key = "m-" + month.label) {
                        SectionLabel("${month.label} · ${month.items.size}", modifier = Modifier.padding(top = 6.dp))
                    }
                    items(month.items, key = { it.sessionId }) { item ->
                        Box(Modifier.appear(appearRegistry, item.sessionId, indexOf[item.sessionId] ?: 0)) {
                            HistoryCard(item, state.zone, onClick = { onOpen(item.sessionId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem, zone: ZoneId, onClick: () -> Unit) {
    val date = item.startedAt.atZone(zone).toLocalDate()
    GtCard(onClick = onClick, onClickLabel = "Szczegóły treningu", contentPadding = PaddingValues(14.dp), verticalSpacing = 0.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(
                Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GtColors.Elevated)
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)
                Text(Formatters.shortDay(date.dayOfWeek), style = MaterialTheme.typography.labelSmall, color = GtColors.TextSecondary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
                Text(
                    buildList {
                        if (item.durationMillis > 0) add(minutesText(item.durationMillis))
                        add(Formatters.countLabel(item.sets, "seria", "serie", "serii"))
                        if (item.volumeKg > 0) add(volumeText(item.volumeKg))
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
                if (item.exerciseNames.isNotEmpty()) {
                    Text(
                        item.exerciseNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = GtColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.xp > 0) Text("+${item.xp} XP", style = MaterialTheme.typography.labelLarge, color = GtColors.Lime)
                if (item.records > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Icon(GtIcons.Trophy, contentDescription = null, tint = GtColors.Fire, modifier = Modifier.size(14.dp))
                        Text("${item.records}", style = MaterialTheme.typography.labelLarge, color = GtColors.Fire)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryDetailScreen(
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        HistoryDetailUiState.Loading -> Column(Modifier.fillMaxSize()) { BackHeader("Trening", null, onBack) }
        HistoryDetailUiState.NotFound -> Column(Modifier.fillMaxSize()) {
            BackHeader("Trening", null, onBack)
            EmptyState("Nie ma takiego treningu", "Mógł zostać usunięty.")
        }
        is HistoryDetailUiState.Ready -> {
            val detail = current.detail
            val item = detail.item
            Column(Modifier.fillMaxSize()) {
                BackHeader(item.title, fullDate(item.startedAt, current.zone), onBack)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "tiles") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatTile(if (item.durationMillis > 0) minutesText(item.durationMillis) else "—", "Czas", Modifier.weight(1f))
                            StatTile("${item.sets}", "Serie", Modifier.weight(1f))
                            StatTile("+${item.xp}", "XP", Modifier.weight(1f), accent = true)
                        }
                    }
                    if (item.volumeKg > 0 || item.records > 0) {
                        item(key = "tiles2") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatTile(volumeText(item.volumeKg), "Objętość", Modifier.weight(1f))
                                StatTile("${item.records}", "Rekordy", Modifier.weight(1f))
                            }
                        }
                    }
                    items(detail.exercises, key = { it.exerciseId }) { exercise -> ExerciseHistoryCard(exercise) }
                }
            }
        }
    }
}

@Composable
private fun ExerciseHistoryCard(exercise: HistoryExercise) {
    GtCard(contentPadding = PaddingValues(14.dp), verticalSpacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GtColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                if (ExerciseMotions.forId(exercise.exerciseId) != null) {
                    ExerciseFigure(exercise.exerciseId, Modifier.fillMaxSize().padding(3.dp), playing = false)
                } else {
                    Icon(GtIcons.Workout, contentDescription = null, tint = GtColors.TextSecondary, modifier = Modifier.size(22.dp))
                }
            }
            Text(exercise.name, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary, modifier = Modifier.weight(1f))
            if (exercise.hasRecord) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GtColors.FireDim)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(GtIcons.Trophy, contentDescription = null, tint = GtColors.Fire, modifier = Modifier.size(12.dp))
                    Text("Rekord", style = MaterialTheme.typography.labelMedium, color = GtColors.Fire)
                }
            }
        }
        exercise.sets.forEachIndexed { index, set ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    color = GtColors.TextMuted,
                    modifier = Modifier.width(28.dp),
                )
                val weight = set.weightKg?.takeIf { it > 0.0 }
                val value = when (exercise.tracking) {
                    TrackingType.TIME -> "${set.durationSeconds ?: 0} s"
                    TrackingType.REPS -> if (weight != null) "${Formatters.weightKg(weight)} × ${set.reps ?: 0}" else "× ${set.reps ?: 0}"
                }
                Text(value, style = MaterialTheme.typography.bodyLarge, color = GtColors.TextPrimary)
            }
        }
    }
}
