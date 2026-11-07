package com.gymtrack.ultra.features.complete

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.RankUp
import com.gymtrack.ultra.domain.model.WorkoutResult
import com.gymtrack.ultra.domain.progression.RecordType
import com.gymtrack.ultra.ui.components.ConfettiBurst
import com.gymtrack.ultra.ui.components.EmptyState
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.HexBadge
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.SectionTitle
import com.gymtrack.ultra.ui.components.StatTile
import com.gymtrack.ultra.ui.components.StreakFlame
import com.gymtrack.ultra.ui.components.achievementGlyph
import com.gymtrack.ultra.ui.components.rememberGtHaptics
import com.gymtrack.ultra.ui.rank.RankEmblem
import com.gymtrack.ultra.ui.rank.palette
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import kotlin.math.roundToInt

private enum class Step { SUMMARY, RANKS, STREAK }

@Composable
fun WorkoutCompleteScreen(
    onDone: () -> Unit,
    viewModel: WorkoutCompleteViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        CompleteUiState.Loading -> Box(Modifier.fillMaxSize())
        CompleteUiState.Missing -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            EmptyState("Nie znaleziono treningu", "Podsumowanie nie jest dostępne.")
            NeonButton("WRÓĆ", onClick = onDone, modifier = Modifier.fillMaxWidth())
        }
        is CompleteUiState.Ready -> CompleteFlow(s.result, onDone)
    }
}

@Composable
private fun CompleteFlow(result: WorkoutResult, onDone: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(Step.SUMMARY) }
    val haptics = rememberGtHaptics()
    LaunchedEffect(step) { if (step != Step.STREAK) haptics.celebrate() }

    AnimatedContent(
        targetState = step,
        transitionSpec = { (fadeIn(tween(260)) + slideInVertically { it / 12 }) togetherWith fadeOut(tween(160)) },
        label = "completeStep",
    ) { current ->
        when (current) {
            Step.SUMMARY -> SummaryStep(result) {
                step = if (result.rankUps.isNotEmpty()) Step.RANKS else Step.STREAK
            }
            Step.RANKS -> RankUpStep(result.rankUps) { step = Step.STREAK }
            Step.STREAK -> StreakStep(result, onDone)
        }
    }
}

// ── Krok 1: WORKOUT COMPLETE ────────────────────────────────────────────

@Composable
private fun SummaryStep(result: WorkoutResult, onNext: () -> Unit) {
    val reduced = LocalGtMotion.current.reduced
    val badge = remember { Animatable(if (reduced) 1f else 0.4f) }
    val xpShown = remember { Animatable(if (reduced) result.xpTotal.toFloat() else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            badge.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
        }
    }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(250)
            xpShown.animateTo(result.xpTotal.toFloat(), tween(900, easing = FastOutSlowInEasing))
        }
    }
    val levelUp = result.levelAfter.level > result.levelBefore.level

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Box(
                    Modifier
                        .size(96.dp)
                        .graphicsLayer {
                            scaleX = badge.value
                            scaleY = badge.value
                        }
                        .clip(CircleShape)
                        .background(GtColors.Lime),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(GtIcons.Check, contentDescription = null, tint = GtColors.OnLime, modifier = Modifier.size(52.dp))
                }
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "WORKOUT COMPLETE",
                        style = MaterialTheme.typography.headlineLarge,
                        color = GtColors.TextPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "${result.title} · ${Formatters.durationMinutes(result.durationMillis / 60_000)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GtColors.TextSecondary,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Formatters.clock(result.durationMillis / 1000), "Czas", Modifier.weight(1f))
                    StatTile("${result.completedExercises}/${result.totalExercises}", "Ćwiczenia", Modifier.weight(1f))
                    StatTile("${result.completedSets}/${result.plannedSets}", "Serie", Modifier.weight(1f))
                }
            }
            item {
                GtCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "+${xpShown.value.roundToInt()} XP",
                            style = MaterialTheme.typography.displaySmall,
                            color = GtColors.Lime,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (levelUp) "AWANS NA LVL ${result.levelAfter.level}!" else "LVL ${result.levelAfter.level}",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (levelUp) GtColors.Lime else GtColors.TextSecondary,
                        )
                    }
                    GradientProgressBar(progress = result.levelAfter.fraction)
                    if (result.xpLines.isEmpty()) {
                        Text(result.xpNote.orEmpty(), style = MaterialTheme.typography.bodySmall, color = GtColors.TextSoft)
                    }
                    result.xpLines.forEach { line ->
                        Row {
                            Text(line.label, style = MaterialTheme.typography.bodyMedium, color = GtColors.TextSoft, modifier = Modifier.weight(1f))
                            Text("+${line.amount}", style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
                        }
                    }
                }
            }
            if (result.records.isNotEmpty()) {
                item {
                    GtCard(modifier = Modifier.fillMaxWidth(), tint = GtColors.Fire) {
                        SectionTitle("Rekordy osobiste")
                        result.records.forEach { r ->
                            val value = when (r.record.type) {
                                RecordType.TIME -> "${r.record.durationSeconds} s"
                                else -> {
                                    val w = r.record.weightKg?.takeIf { it > 0.0 }?.let { Formatters.weightKg(it) + " × " }.orEmpty()
                                    "$w${r.record.reps}"
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(GtIcons.Trophy, contentDescription = null, tint = GtColors.Fire, modifier = Modifier.size(20.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.exerciseName, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
                                    Text(r.record.type.label, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                                }
                                Text(value, style = MaterialTheme.typography.titleSmall, color = GtColors.Fire)
                            }
                        }
                    }
                }
            }
            if (result.muscleGains.isNotEmpty()) {
                item {
                    GtCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("Punkty rang")
                        Text(
                            result.muscleGains.entries.sortedByDescending { it.value }
                                .joinToString("   ") { "${it.key.label} +${it.value}" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = GtColors.TextSoft,
                        )
                    }
                }
            }
            if (result.completedQuests.isNotEmpty() || result.newAchievements.isNotEmpty()) {
                item {
                    GtCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("Odblokowane")
                        result.completedQuests.forEach { q ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(GtIcons.Check, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(20.dp))
                                Text("Wyzwanie: ${q.title}", style = MaterialTheme.typography.bodyMedium, color = GtColors.TextPrimary, modifier = Modifier.weight(1f))
                                Text("+${q.xp} XP", style = MaterialTheme.typography.titleSmall, color = GtColors.Lime)
                            }
                        }
                        result.newAchievements.forEach { a ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HexBadge(fill = GtColors.LimeDim, stroke = GtColors.Lime, glyph = achievementGlyph(a.code), size = 28.dp)
                                Column {
                                    Text(a.title, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
                                    Text(a.description, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
            result.previousVolumeKg?.takeIf { it > 0.0 && result.volumeKg > 0.0 }?.let { previous ->
                item {
                    val change = ((result.volumeKg - previous) / previous * 100).roundToInt()
                    GtCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle("Vs. poprzedni ${result.title}")
                        Row {
                            Text("Objętość", style = MaterialTheme.typography.bodyMedium, color = GtColors.TextSoft, modifier = Modifier.weight(1f))
                            Text(
                                "${Formatters.weightKg(result.volumeKg)}  ${if (change >= 0) "+" else ""}$change%",
                                style = MaterialTheme.typography.titleSmall,
                                color = if (change >= 0) GtColors.Success else GtColors.TextSecondary,
                            )
                        }
                    }
                }
            }
            item {
                NeonButton("DALEJ", onClick = onNext, modifier = Modifier.fillMaxWidth())
            }
        }
        ConfettiBurst(Modifier.fillMaxSize())
    }
}

// ── Krok 2: AWANS RANGI ─────────────────────────────────────────────────

@Composable
private fun RankUpStep(rankUps: List<RankUp>, onNext: () -> Unit) {
    val main = rankUps.firstOrNull { it.muscle == null } ?: rankUps.maxBy { it.to }
    val others = rankUps - main
    val reduced = LocalGtMotion.current.reduced
    val flip = remember { Animatable(if (reduced) 0f else 180f) }
    val scale = remember { Animatable(if (reduced) 1f else 0.5f) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(150)
            flip.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessVeryLow))
        }
    }
    LaunchedEffect(Unit) {
        if (!reduced) scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
    }
    val color = main.to.tier.palette().text

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("AWANS!", style = MaterialTheme.typography.displaySmall, color = color)
            RankEmblem(
                rank = main.to,
                size = 200.dp,
                modifier = Modifier.graphicsLayer {
                    rotationY = flip.value
                    scaleX = scale.value
                    scaleY = scale.value
                    cameraDistance = 12f * density
                },
                contentDescription = "Nowa ranga ${main.to.label}",
            )
            Text(main.to.label, style = MaterialTheme.typography.headlineLarge, color = color)
            Text(
                (main.muscle?.label ?: "Ranga konta") + (main.from?.let { " · z ${it.label}" } ?: " · pierwsza ranga"),
                style = MaterialTheme.typography.bodyLarge,
                color = GtColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (others.isNotEmpty()) {
                GtCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = 8.dp) {
                    others.forEach { up ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RankEmblem(rank = up.to, size = 36.dp, showDivision = false)
                            Text(up.muscle?.label ?: "Konto", style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary, modifier = Modifier.weight(1f))
                            Text(up.to.label, style = MaterialTheme.typography.titleSmall, color = up.to.tier.palette().text)
                        }
                    }
                }
            }
            NeonButton("DALEJ", onClick = onNext, modifier = Modifier.fillMaxWidth())
        }
        ConfettiBurst(Modifier.fillMaxSize(), seed = 11)
    }
}

// ── Krok 3: SERIA (płomień) ─────────────────────────────────────────────

@Composable
private fun StreakStep(result: WorkoutResult, onDone: () -> Unit) {
    val reduced = LocalGtMotion.current.reduced
    val haptics = rememberGtHaptics()
    val grow = remember { Animatable(if (reduced) 1f else 0.2f) }
    var shown by remember { mutableIntStateOf(if (reduced) result.streakAfter.current else result.streakBefore) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            grow.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessLow))
            delay(150)
        }
        shown = result.streakAfter.current
        haptics.celebrate()
    }
    val streak = result.streakAfter
    val days = DayOfWeek.values().toList()

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
    ) {
        Box(contentAlignment = Alignment.Center) {
            StreakFlame(
                lit = true,
                size = 190.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = grow.value
                    scaleY = grow.value
                },
            )
        }
        AnimatedContent(
            targetState = shown,
            transitionSpec = {
                (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "streakNumber",
        ) { value ->
            Text(
                value.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
                color = GtColors.Fire,
            )
        }
        Text(
            if (streak.current == 1) "Seria rozpalona!" else "${Formatters.countLabel(streak.current, "trening", "treningi", "treningów")} w serii!",
            style = MaterialTheme.typography.headlineSmall,
            color = GtColors.TextPrimary,
            textAlign = TextAlign.Center,
        )

        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(GtColors.Surface)
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            days.forEachIndexed { i, day ->
                val trained = day in result.trainedThisWeek
                val pop = remember { Animatable(if (reduced || !trained) 1f else 0f) }
                LaunchedEffect(Unit) {
                    if (!reduced && trained) {
                        delay(500L + i * 90L)
                        pop.animateTo(1f, spring(dampingRatio = 0.45f))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(Formatters.shortDay(day), style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                    Box(
                        Modifier
                            .size(32.dp)
                            .graphicsLayer {
                                scaleX = pop.value
                                scaleY = pop.value
                            }
                            .clip(CircleShape)
                            .background(if (trained) GtColors.Fire else GtColors.Elevated)
                            .border(1.dp, if (trained) GtColors.Fire else GtColors.Outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (trained) Icon(GtIcons.Check, contentDescription = null, tint = GtColors.OnLime, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Text(
            when {
                streak.shieldUsedLastWeek -> "Tarcza ochroniła serię w zeszłym tygodniu. Dobrze, że wróciłeś!"
                streak.isSafeThisWeek -> "Ten tydzień zaliczony. Dni wolne są OK — ogień nie zgaśnie."
                else -> "Jeszcze ${streak.neededThisWeek} w tym tygodniu, żeby ogień płonął dalej. Dni wolne nie przerywają serii."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = GtColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        if (streak.shields > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(GtIcons.Shield, contentDescription = null, tint = GtColors.Shield, modifier = Modifier.size(18.dp))
                Text(
                    "Tarcze: ${streak.shields} — uratują serię w słabszym tygodniu",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.Shield,
                )
            }
        }
        NeonButton("GOTOWE", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }
}
