package com.gymtrack.ultra.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.domain.progression.MuscleRank
import com.gymtrack.ultra.domain.progression.QuestProgress
import com.gymtrack.ultra.domain.progression.RecordType
import com.gymtrack.ultra.domain.progression.StreakState
import com.gymtrack.ultra.ui.components.GhostButton
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.GtTopBar
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.SectionTitle
import com.gymtrack.ultra.ui.components.SkeletonBlock
import com.gymtrack.ultra.ui.components.StreakFlame
import com.gymtrack.ultra.ui.rank.MuscleRankRow
import com.gymtrack.ultra.ui.rank.RankEmblem
import com.gymtrack.ultra.ui.rank.palette
import com.gymtrack.ultra.ui.theme.GtColors

@Composable
fun HomeScreen(
    onOpenLiveWorkout: () -> Unit,
    onOpenPlanDay: (Long) -> Unit,
    onOpenRanks: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showStreak by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.openLive.collect { onOpenLiveWorkout() } }

    Column(Modifier.fillMaxSize()) {
        GtTopBar(streak = state.streak, onStreakClick = { showStreak = true })
        if (state.isLoading) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SkeletonBlock(height = 88.dp)
                SkeletonBlock(height = 200.dp)
                SkeletonBlock(height = 140.dp)
            }
        } else {
            HomeList(
                state = state,
                onStart = viewModel::startWorkout,
                onResume = onOpenLiveWorkout,
                onOpenPlanDay = onOpenPlanDay,
                onOpenRanks = onOpenRanks,
                onOpenStreak = { showStreak = true },
            )
        }
    }

    if (showStreak) {
        StreakSheet(streak = state.streak, week = state.week, onDismiss = { showStreak = false })
    }
}

@Composable
private fun HomeList(
    state: HomeUiState,
    onStart: (Long) -> Unit,
    onResume: () -> Unit,
    onOpenPlanDay: (Long) -> Unit,
    onOpenRanks: () -> Unit,
    onOpenStreak: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.activeSession?.let { active ->
            item(key = "active") { ActiveSessionCard(active, onResume) }
        }
        item(key = "rank") { AccountRankCard(state.level, onOpenRanks) }
        state.today?.let { today ->
            item(key = "today") {
                TodayCard(
                    card = today,
                    hasActive = state.activeSession != null,
                    onStart = { onStart(today.dayId) },
                    onResume = onResume,
                    onPreview = { onOpenPlanDay(today.dayId) },
                )
            }
        }
        item(key = "streak") { StreakCard(state.streak, state.week, onOpenStreak) }
        if (state.quests.isNotEmpty()) {
            item(key = "quests") { QuestsCard(state.quests) }
        }
        item(key = "muscles") { MusclesPreview(state.topMuscles, onOpenRanks) }
        if (state.recentRecords.isNotEmpty()) {
            item(key = "records") { RecordsCard(state) }
        }
    }
}

@Composable
private fun ActiveSessionCard(active: ActiveSessionUi, onResume: () -> Unit) {
    GtCard(highlighted = true, onClick = onResume, onClickLabel = "Wróć do treningu") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(GtColors.Lime),
            )
            Column(Modifier.weight(1f)) {
                Text("Trening w toku", style = MaterialTheme.typography.bodySmall, color = GtColors.Lime)
                Text(active.title, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
            }
            Text("${active.done}/${active.planned}", style = MaterialTheme.typography.titleSmall, color = GtColors.TextSecondary)
            Icon(GtIcons.ChevronRight, contentDescription = null, tint = GtColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun AccountRankCard(level: LevelProgress, onClick: () -> Unit) {
    val palette = level.rank.tier.palette()
    GtCard(tint = palette.text, onClick = onClick, onClickLabel = "Otwórz rangi", contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RankEmblem(rank = level.rank, size = 64.dp, contentDescription = "Ranga konta ${level.rank.label}")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(level.rank.label, style = MaterialTheme.typography.titleMedium, color = palette.text, modifier = Modifier.weight(1f))
                    Text("LVL ${level.level}", style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
                }
                GradientProgressBar(progress = level.fraction, height = 6.dp, color = palette.text)
                Text(
                    if (level.isMaxLevel) "Maksymalny poziom" else "${level.xpIntoLevel} / ${level.xpForNextLevel} XP do LVL ${level.level + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun TodayCard(
    card: TodayCardUi,
    hasActive: Boolean,
    onStart: () -> Unit,
    onResume: () -> Unit,
    onPreview: () -> Unit,
) {
    GtCard(onClick = onPreview, onClickLabel = "Podgląd ${card.title}", verticalSpacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    card.isToday && card.doneToday -> "Dziś zrobione"
                    card.isToday -> "Dzisiejszy trening"
                    else -> "Dziś regeneracja · następny: ${card.whenText.lowercase()}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (card.isToday && !card.doneToday) GtColors.Lime else GtColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Icon(GtIcons.ChevronRight, contentDescription = null, tint = GtColors.TextMuted, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                card.title,
                style = MaterialTheme.typography.headlineMedium,
                color = GtColors.TextPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "${Formatters.countLabel(card.exerciseCount, "ćwiczenie", "ćwiczenia", "ćwiczeń")} · " +
                    "${Formatters.countLabel(card.setCount, "seria", "serie", "serii")} · ~${card.minutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = GtColors.TextSecondary,
            )
            Text(
                card.exercisePreview,
                style = MaterialTheme.typography.bodySmall,
                color = GtColors.TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when {
            hasActive -> NeonButton("WRÓĆ DO TRENINGU", onClick = onResume, leadingIcon = GtIcons.Play, modifier = Modifier.fillMaxWidth())
            card.isToday && !card.doneToday ->
                NeonButton("START WORKOUT", onClick = onStart, leadingIcon = GtIcons.Play, modifier = Modifier.fillMaxWidth())
            else -> GhostButton(
                if (card.doneToday) "Trenuj jeszcze raz" else "Zrób dziś mimo to",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StreakCard(streak: StreakState, week: List<WeekDayUi>, onClick: () -> Unit) {
    GtCard(onClick = onClick, onClickLabel = "Szczegóły serii", verticalSpacing = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StreakFlame(lit = streak.isLit, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (streak.isLit) "Seria: ${Formatters.countLabel(streak.current, "trening", "treningi", "treningów")}" else "Rozpal serię",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (streak.isLit) GtColors.Fire else GtColors.TextPrimary,
                )
                Text(
                    if (streak.isSafeThisWeek) {
                        "Tydzień zaliczony · dni wolne są OK"
                    } else {
                        "W tym tygodniu ${streak.thisWeekCount}/${streak.weeklyMinimum} · dni wolne są OK"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
        }
        WeekRow(week)
    }
}

@Composable
internal fun WeekRow(week: List<WeekDayUi>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        week.forEach { day ->
            val (bg, border, fg) = when (day.state) {
                WeekDayState.DONE -> Triple(GtColors.Fire, GtColors.Fire, GtColors.OnLime)
                WeekDayState.TODAY_PLANNED -> Triple(GtColors.Surface, GtColors.Lime, GtColors.Lime)
                WeekDayState.TODAY_REST -> Triple(GtColors.Elevated, GtColors.OutlineStrong, GtColors.TextSecondary)
                WeekDayState.PLANNED -> Triple(GtColors.Surface, GtColors.OutlineStrong, GtColors.TextSecondary)
                WeekDayState.REST -> Triple(GtColors.Elevated, GtColors.Elevated, GtColors.TextMuted)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clearAndSetSemantics { contentDescription = day.accessibilityText },
            ) {
                Text(day.label, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(bg)
                        .border(1.5.dp, border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (day.state == WeekDayState.DONE) {
                        Icon(GtIcons.Check, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                    } else {
                        Text(day.mark, style = MaterialTheme.typography.titleSmall, color = fg)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestsCard(quests: List<QuestProgress>) {
    GtCard(verticalSpacing = 12.dp) {
        SectionTitle("Wyzwania tygodnia")
        quests.forEach { q ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (q.isDone) GtColors.Lime else GtColors.Elevated),
                    contentAlignment = Alignment.Center,
                ) {
                    if (q.isDone) Icon(GtIcons.Check, contentDescription = null, tint = GtColors.OnLime, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(q.quest.title, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary, modifier = Modifier.weight(1f))
                        Text("${q.current}/${q.quest.target}", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                    }
                    GradientProgressBar(progress = q.fraction, height = 5.dp)
                }
                Text(
                    "+${q.quest.xp} XP",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (q.isDone) GtColors.Lime else GtColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun MusclesPreview(muscles: List<MuscleRank>, onOpenRanks: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Rangi mięśni", action = "Wszystkie", onAction = onOpenRanks)
        muscles.forEach { MuscleRankRow(it, onClick = onOpenRanks) }
    }
}

@Composable
private fun RecordsCard(state: HomeUiState) {
    GtCard(verticalSpacing = 10.dp) {
        SectionTitle("Ostatnie rekordy")
        state.recentRecords.forEach { r ->
            val value = when (r.record.type) {
                RecordType.TIME -> "${r.record.durationSeconds} s"
                else -> (r.record.weightKg?.takeIf { it > 0.0 }?.let { Formatters.weightKg(it) + " × " }.orEmpty()) + "${r.record.reps}"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(GtIcons.Trophy, contentDescription = null, tint = GtColors.Fire, modifier = Modifier.size(18.dp))
                Text(
                    r.exerciseName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GtColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(value, style = MaterialTheme.typography.titleSmall, color = GtColors.Fire)
            }
        }
    }
}
