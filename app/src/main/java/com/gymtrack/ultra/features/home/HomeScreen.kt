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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.SessionSummary
import com.gymtrack.ultra.domain.progression.AchievementProgress
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.ui.components.EmptyState
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.HexBadge
import com.gymtrack.ultra.ui.components.IconSquareButton
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.components.SkeletonBlock
import com.gymtrack.ultra.ui.components.achievementGlyph
import com.gymtrack.ultra.ui.components.badgeStyle
import com.gymtrack.ultra.ui.theme.GtColors
import java.time.ZoneId

@Composable
fun HomeScreen(
    onOpenPlanDay: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onOpenPlanDay = onOpenPlanDay,
        onOpenProfile = onOpenProfile,
        onOpenAchievements = onOpenAchievements,
        modifier = modifier,
    )
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    onOpenPlanDay: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Subtelna neonowa poświata w prawym górnym rogu.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GtColors.Lime.copy(alpha = 0.13f), Color.Transparent),
                        center = Offset(size.width * 0.95f, 0f),
                        radius = size.width * 0.75f,
                    ),
                    radius = size.width * 0.75f,
                    center = Offset(size.width * 0.95f, 0f),
                )
            },
    ) {
        if (state.isLoading) {
            HomeSkeleton()
        } else {
            HomeList(state, onOpenPlanDay, onOpenProfile, onOpenAchievements)
        }
    }
}

@Composable
private fun HomeList(
    state: HomeUiState,
    onOpenPlanDay: (Long) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAchievements: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            HomeHeader(state.greeting, state.headline, onOpenProfile)
        }
        item(key = "level") {
            LevelStrip(state.level, onClick = onOpenProfile)
        }
        item(key = "today") {
            val today = state.today
            if (today != null) {
                TodayWorkoutCard(today, state.showWarmupHint, onOpen = { onOpenPlanDay(today.dayId) })
            } else {
                GtCard { EmptyState("Brak planu", "Plan treningowy pojawi się tu, gdy go dodasz.") }
            }
        }
        item(key = "week") {
            WeeklyActivityCard(state.week, state.weeklyDone, state.weeklyTarget)
        }
        item(key = "recent") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LastSessionCard(state.lastSession, Modifier.weight(1f))
                LatestAchievementCard(state.latestAchievement, onOpenAchievements, Modifier.weight(1f))
            }
        }
        item(key = "strength") {
            StrengthPreviewCard(hasHistory = state.lastSession != null)
        }
    }
}

@Composable
private fun HomeHeader(greeting: String, headline: String, onOpenProfile: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(greeting, style = MaterialTheme.typography.bodyLarge, color = GtColors.TextSecondary)
            Text(
                headline,
                style = MaterialTheme.typography.headlineMedium,
                color = GtColors.TextPrimary,
                modifier = Modifier.semantics { heading() },
            )
        }
        IconSquareButton(icon = GtIcons.Profile, contentDescription = "Profil", onClick = onOpenProfile)
    }
}

@Composable
private fun LevelStrip(level: LevelProgress, onClick: () -> Unit) {
    val style = level.tier.badgeStyle()
    GtCard(
        onClick = onClick,
        onClickLabel = "Otwórz profil",
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HexBadge(fill = style.fill, stroke = style.stroke, glyph = style.glyph, size = 44.dp, contentDescription = "Ranga ${level.tier.title}")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "LVL ${level.level} · ${level.tier.title}",
                        style = MaterialTheme.typography.titleSmall,
                        color = GtColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (level.isMaxLevel) "MAX" else "${level.xpIntoLevel} / ${level.xpForNextLevel} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = GtColors.TextSecondary,
                    )
                }
                GradientProgressBar(progress = level.fraction)
            }
        }
    }
}

@Composable
private fun TodayWorkoutCard(card: TodayCardUi, showWarmupHint: Boolean, onOpen: () -> Unit) {
    GtCard(highlighted = card.isToday && !card.doneToday, contentPadding = PaddingValues(22.dp), verticalSpacing = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(card.eyebrow, color = if (card.isToday) GtColors.Lime else GtColors.TextSecondary, modifier = Modifier.weight(1f))
            Text(card.whenText, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(card.title, style = MaterialTheme.typography.displaySmall, color = GtColors.TextPrimary)
            Text(
                card.exercisePreview,
                style = MaterialTheme.typography.bodyMedium,
                color = GtColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(card.exerciseCount.toString(), Formatters.plural(card.exerciseCount, "ćwiczenie", "ćwiczenia", "ćwiczeń"), Modifier.weight(1f))
            MiniStat(card.setCount.toString(), Formatters.plural(card.setCount, "seria", "serie", "serii"), Modifier.weight(1f))
            MiniStat("~${card.minutes}", "minut", Modifier.weight(1f))
        }
        NeonButton(
            text = if (card.doneToday) "ZOBACZ PLAN DNIA" else "ZOBACZ TRENING",
            onClick = onOpen,
            leadingIcon = GtIcons.Play,
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showWarmupHint && !card.doneToday) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(GtIcons.Flame, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(16.dp))
                Text(
                    "Zacznij od 5–8 min rozgrzewki: lekkie cardio i jedna lekka seria.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(GtColors.Background.copy(alpha = 0.55f))
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
    }
}

@Composable
private fun WeeklyActivityCard(week: List<WeekDayUi>, done: Int, target: Int) {
    GtCard(verticalSpacing = 14.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            SectionLabel("Weekly activity", modifier = Modifier.weight(1f))
            Text(
                text = if (target > 0) "$done / $target ${Formatters.plural(target, "sesja", "sesje", "sesji")}" else "$done",
                style = MaterialTheme.typography.titleSmall,
                color = if (target > 0 && done >= target) GtColors.Lime else GtColors.TextSecondary,
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            week.forEach { day -> WeekDayCell(day) }
        }
        Text(
            "Wolne dni to regeneracja — mięśnie rosną właśnie wtedy.",
            style = MaterialTheme.typography.bodySmall,
            color = GtColors.TextSecondary,
        )
    }
}

@Composable
private fun WeekDayCell(day: WeekDayUi) {
    val shape = RoundedCornerShape(12.dp)
    val base = Modifier
        .size(38.dp)
        .clip(shape)
    val styled = when (day.state) {
        WeekDayState.DONE -> base.background(GtColors.Lime)
        WeekDayState.TODAY_PLANNED -> base.border(2.dp, GtColors.Lime, shape)
        WeekDayState.TODAY_REST -> base.border(2.dp, GtColors.OutlineStrong, shape).background(GtColors.Elevated)
        WeekDayState.PLANNED -> base.border(1.dp, GtColors.OutlineStrong, shape)
        WeekDayState.REST -> base.background(GtColors.Elevated)
    }
    val markColor = when (day.state) {
        WeekDayState.DONE -> GtColors.OnLime
        WeekDayState.TODAY_PLANNED -> GtColors.Lime
        WeekDayState.PLANNED -> GtColors.TextSecondary
        else -> GtColors.TextMuted
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = day.accessibilityText },
    ) {
        Text(day.label, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        Box(styled, contentAlignment = Alignment.Center) {
            Text(day.mark, style = MaterialTheme.typography.titleSmall, color = markColor)
        }
    }
}

@Composable
private fun LastSessionCard(session: SessionSummary?, modifier: Modifier = Modifier) {
    GtCard(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalSpacing = 6.dp) {
        SectionLabel("Ostatni")
        if (session == null) {
            Text("Jeszcze nic", style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
            Text("Pierwszy trening przed tobą", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        } else {
            val date = session.startedAt.atZone(ZoneId.systemDefault()).toLocalDate()
            val minutes = session.durationMillis / 60_000
            Text(session.title, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${Formatters.shortDay(date.dayOfWeek)} ${Formatters.dayMonth(date)} · ${Formatters.durationMinutes(minutes)}",
                style = MaterialTheme.typography.bodySmall,
                color = GtColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun LatestAchievementCard(achievement: AchievementProgress?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    GtCard(
        modifier = modifier,
        onClick = onOpen,
        onClickLabel = "Otwórz osiągnięcia",
        contentPadding = PaddingValues(16.dp),
        verticalSpacing = 6.dp,
    ) {
        SectionLabel("Osiągnięcie")
        if (achievement == null) {
            Text("FIRST REP", style = MaterialTheme.typography.titleMedium, color = GtColors.TextSoft)
            Text("Zalicz pierwszą serię", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HexBadge(
                    fill = GtColors.LimeDim,
                    stroke = GtColors.Lime,
                    glyph = achievementGlyph(achievement.definition.code),
                    size = 24.dp,
                )
                Text(
                    achievement.definition.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = GtColors.Lime,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(achievement.definition.description, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary, maxLines = 2)
        }
    }
}

@Composable
private fun StrengthPreviewCard(hasHistory: Boolean) {
    GtCard(verticalSpacing = 8.dp) {
        SectionLabel("Progres siłowy")
        EmptyState(
            title = if (hasHistory) "Wykres w drodze" else "Tu pojawi się twój progres",
            message = if (hasHistory) {
                "Po kolejnych treningach zobaczysz, jak rośnie ciężar w każdym ćwiczeniu."
            } else {
                "Zapisz ciężar i powtórzenia w pierwszym treningu — resztę policzymy za ciebie."
            },
            icon = GtIcons.Stats,
        )
    }
}

@Composable
private fun HomeSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SkeletonBlock(height = 64.dp)
        SkeletonBlock(height = 72.dp)
        SkeletonBlock(height = 300.dp)
        SkeletonBlock(height = 140.dp)
    }
}
