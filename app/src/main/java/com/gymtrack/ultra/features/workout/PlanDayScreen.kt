package com.gymtrack.ultra.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.ui.exercise.ExercisePickerSheet
import com.gymtrack.ultra.ui.components.shine
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.PlanExercise
import com.gymtrack.ultra.ui.components.EmptyState
import com.gymtrack.ultra.ui.components.GhostButton
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.IconSquareButton
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.components.SkeletonBlock
import com.gymtrack.ultra.ui.exercise.ExerciseFigure
import com.gymtrack.ultra.ui.theme.GtColors

@Composable
fun PlanDayScreen(
    onBack: () -> Unit,
    onStarted: () -> Unit,
    viewModel: PlanDayViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hasActive by viewModel.hasActiveSession.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.started.collect { onStarted() } }
    // Id ćwiczenia z otwartą kartą techniki — przetrwa obrót ekranu.
    var openExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pickerFor by rememberSaveable { mutableStateOf<Long?>(null) } // -1 = dodawanie nowego ćwiczenia
    val library by viewModel.library.collectAsStateWithLifecycle()

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
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    PlanDayContent(
                        day = current.day,
                        onBack = onBack,
                        onOpenTechnique = { item ->
                            openExerciseId = item.exercise.id
                            viewModel.onTechniqueOpened(item.exercise.id)
                        },
                        onEdit = { item -> editId = item.id },
                        onAdd = { pickerFor = ADD_NEW },
                    )
                }
                NeonButton(
                    text = if (hasActive) "WRÓĆ DO TRENINGU" else "START WORKOUT",
                    onClick = viewModel::startWorkout,
                    leadingIcon = GtIcons.Play,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .shine(enabled = !hasActive, delayMillis = 1600),
                )
            }
            val exercises = current.day.exercises
            val editing = exercises.firstOrNull { it.id == editId }
            if (editing != null) {
                val index = exercises.indexOf(editing)
                EditPlanExerciseSheet(
                    item = editing,
                    canMoveUp = index > 0,
                    canMoveDown = index < exercises.lastIndex,
                    canRemove = exercises.size > 1,
                    onSave = { sets, min, max, rest ->
                        viewModel.updateTargets(editing, sets, min, max, rest)
                        editId = null
                    },
                    onSwap = {
                        pickerFor = editing.id
                        editId = null
                    },
                    onMove = { delta -> viewModel.move(editing, delta) },
                    onRemove = {
                        viewModel.remove(editing)
                        editId = null
                    },
                    onDismiss = { editId = null },
                )
            }
            val picking = pickerFor
            if (picking != null) {
                val replacing = exercises.firstOrNull { it.id == picking }
                ExercisePickerSheet(
                    title = if (replacing != null) "Zamień ćwiczenie" else "Dodaj ćwiczenie",
                    library = library,
                    current = replacing?.exercise,
                    excludeIds = exercises.map { it.exercise.id }.toSet(),
                    onPick = { picked, _ ->
                        if (replacing != null) viewModel.replace(replacing, picked.id) else viewModel.add(picked.id)
                        pickerFor = null
                    },
                    onCreateCustom = if (replacing == null) {
                        { name, muscle, tracking ->
                            viewModel.createAndAdd(name, muscle, tracking)
                            pickerFor = null
                        }
                    } else {
                        null
                    },
                    onDismiss = { pickerFor = null },
                )
            }
            val open = current.day.exercises.firstOrNull { it.exercise.id == openExerciseId }
            if (open != null) {
                TechniqueSheet(
                    exercise = open.exercise,
                    subtitle = "${open.targetLabel} · przerwa ${open.restSeconds} s",
                    onDismiss = { openExerciseId = null },
                )
            }
        }
    }
}

@Composable
private fun PlanDayContent(
    day: PlanDay,
    onBack: () -> Unit,
    onOpenTechnique: (PlanExercise) -> Unit,
    onEdit: (PlanExercise) -> Unit,
    onAdd: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
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
            ExerciseRow(index = index, item = item, onClick = { onOpenTechnique(item) }, onEdit = { onEdit(item) })
        }
        item(key = "add") {
            GhostButton("+ Dodaj ćwiczenie", onClick = onAdd, modifier = Modifier.fillMaxWidth())
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
private fun ExerciseRow(index: Int, item: PlanExercise, onClick: () -> Unit, onEdit: () -> Unit) {
    GtCard(
        onClick = onClick,
        onClickLabel = "Pokaż technikę: ${item.exercise.namePl}",
        contentPadding = PaddingValues(12.dp),
        verticalSpacing = 0.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GtColors.Background),
            ) {
                ExerciseFigure(
                    exerciseId = item.exercise.id,
                    startDelayMillis = index * 380L,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(GtColors.Elevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = GtColors.TextPrimary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(item.exercise.namePl, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
                Text(
                    "${item.targetLabel} · przerwa ${item.restSeconds} s",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
            IconSquareButton(icon = GtIcons.Edit, contentDescription = "Edytuj: ${item.exercise.namePl}", onClick = onEdit, size = 44.dp)
        }
    }
}

private const val ADD_NEW = -1L
