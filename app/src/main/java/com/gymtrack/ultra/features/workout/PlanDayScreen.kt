package com.gymtrack.ultra.features.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.PlanExercise
import com.gymtrack.ultra.ui.components.EmptyState
import com.gymtrack.ultra.ui.components.GhostButton
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.IconSquareButton
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.components.SkeletonBlock
import com.gymtrack.ultra.ui.theme.GtColors

@Composable
fun PlanDayScreen(
    onBack: () -> Unit,
    viewModel: PlanDayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Id ćwiczenia z otwartą kartą techniki — przetrwa obrót ekranu.
    var openExerciseId by rememberSaveable { mutableStateOf<String?>(null) }

    when (val current = state) {
        PlanDayUiState.Loading -> Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SkeletonBlock(height = 120.dp)
            SkeletonBlock(height = 300.dp)
        }

        PlanDayUiState.NotFound -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
            EmptyState("Nie ma takiego dnia", "Ten trening został usunięty z planu.")
            GhostButton("Wróć", onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        is PlanDayUiState.Ready -> {
            PlanDayContent(
                day = current.day,
                onBack = onBack,
                onOpenTechnique = { item ->
                    openExerciseId = item.exercise.id
                    viewModel.onTechniqueOpened(item.exercise.id)
                },
            )
            val open = current.day.exercises.firstOrNull { it.exercise.id == openExerciseId }
            if (open != null) {
                TechniqueSheet(item = open, onDismiss = { openExerciseId = null })
            }
        }
    }
}

@Composable
private fun PlanDayContent(
    day: PlanDay,
    onBack: () -> Unit,
    onOpenTechnique: (PlanExercise) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "top") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                IconSquareButton(icon = GtIcons.ChevronLeft, contentDescription = "Wróć", onClick = onBack)
                Column(Modifier.weight(1f)) {
                    SectionLabel(Formatters.capitalize(Formatters.dayName(day.dayOfWeek)), color = GtColors.Lime)
                    Text(
                        day.title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = GtColors.TextPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                }
            }
        }
        item(key = "summary") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(Formatters.countLabel(day.exercises.size, "ćwiczenie", "ćwiczenia", "ćwiczeń"))
                InfoChip(Formatters.countLabel(day.totalSets, "seria", "serie", "serii"))
                InfoChip("~${day.estimatedMinutes} min")
            }
        }
        item(key = "warmup") { WarmupCard() }
        itemsIndexed(day.exercises, key = { _, item -> item.id }) { index, item ->
            ExerciseRow(index = index, item = item, onClick = { onOpenTechnique(item) })
        }
    }
}

@Composable
private fun WarmupCard() {
    GtCard(verticalSpacing = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(GtIcons.Flame, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(18.dp))
            SectionLabel("Rozgrzewka · 5–8 min", color = GtColors.Lime)
        }
        Text(
            "Lekkie cardio (rower, szybki marsz), krążenia ramion i bioder, potem jedna lekka seria " +
                "pierwszego ćwiczenia z połową ciężaru.",
            style = MaterialTheme.typography.bodyMedium,
            color = GtColors.TextSoft,
        )
    }
}

@Composable
private fun ExerciseRow(index: Int, item: PlanExercise, onClick: () -> Unit) {
    GtCard(
        onClick = onClick,
        onClickLabel = "Pokaż technikę: ${item.exercise.namePl}",
        contentPadding = PaddingValues(16.dp),
        verticalSpacing = 0.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DayCodeBadge(code = "${index + 1}", highlighted = false)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.exercise.namePl, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
                Text(
                    "${item.targetLabel} · przerwa ${item.restSeconds} s",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
            Icon(GtIcons.Info, contentDescription = null, tint = GtColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
