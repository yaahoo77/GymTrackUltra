package com.gymtrack.ultra.features.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.LiveSession
import com.gymtrack.ultra.domain.model.RestTimer
import com.gymtrack.ultra.features.workout.TechniqueSheet
import com.gymtrack.ultra.ui.components.EmptyState
import com.gymtrack.ultra.ui.components.GhostButton
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.IconSquareButton
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.rememberGtHaptics
import com.gymtrack.ultra.ui.theme.GtColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveWorkoutScreen(
    onMinimize: () -> Unit,
    onFinished: (Long) -> Unit,
    onDiscarded: () -> Unit,
    viewModel: LiveWorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberGtHaptics()
    val context = LocalContext.current

    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var techniqueFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var noteFor by rememberSaveable { mutableStateOf<Long?>(null) }
    var restExpanded by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LiveEvent.Finished -> onFinished(event.sessionId)
                LiveEvent.Discarded -> onDiscarded()
                is LiveEvent.Message -> scope.launch { snackbar.showSnackbar(event.text) }
                is LiveEvent.SetCompleted -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(event.label, actionLabel = "COFNIJ", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.onUndo(event.setId)
                }
            }
        }
    }

    // Koniec przerwy: wibracja + wyczyszczenie timera (tylko świeżej przerwy, nie przeterminowanej sprzed godziny).
    val timer = state.timer
    LaunchedEffect(timer?.endsAtMillis) {
        if (timer != null) {
            val wait = timer.endsAtMillis - System.currentTimeMillis()
            if (wait > 0) {
                delay(wait)
                haptics.restDone()
            }
            viewModel.onRestFinished(timer.endsAtMillis)
        }
    }

    val session = state.session
    Box(Modifier.fillMaxSize().background(GtColors.Background)) {
        when {
            state.isLoading -> Unit
            session == null -> Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState("Brak aktywnego treningu", "Rozpocznij trening z ekranu Start albo z planu.")
                GhostButton("Wróć", onClick = onMinimize)
            }
            else -> LiveContent(
                session = session,
                state = state,
                viewModel = viewModel,
                onMinimize = onMinimize,
                onFinishClick = { showFinishDialog = true },
                onTechnique = { techniqueFor = it },
                onNote = { noteFor = it },
                onExpandRest = { restExpanded = true },
            )
        }

        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (timer != null) 92.dp else 16.dp),
        )

        AnimatedVisibility(
            visible = restExpanded && timer != null,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 },
        ) {
            if (timer != null) {
                RestOverlay(
                    timer = timer,
                    onAdjust = { haptics.tick(); viewModel.onRestAdjust(it) },
                    onSkip = { viewModel.onRestSkip(); restExpanded = false },
                    onCollapse = { restExpanded = false },
                )
            }
        }
    }

    if (showFinishDialog && session != null) {
        FinishDialog(
            session = session,
            onFinish = { showFinishDialog = false; viewModel.onFinish() },
            onDiscard = { showFinishDialog = false; viewModel.onDiscard() },
            onDismiss = { showFinishDialog = false },
        )
    }

    val techniqueExercise = session?.exercises?.firstOrNull { it.sessionExerciseId == techniqueFor }
    if (techniqueExercise != null) {
        TechniqueSheet(
            exercise = techniqueExercise.exercise,
            subtitle = "${techniqueExercise.targetLabel} · przerwa ${techniqueExercise.restSeconds} s",
            onDismiss = { techniqueFor = null },
        )
    }

    val noteExercise = session?.exercises?.firstOrNull { it.sessionExerciseId == noteFor }
    if (noteExercise != null) {
        NoteDialog(
            initial = noteExercise.note.orEmpty(),
            title = noteExercise.exercise.namePl,
            onSave = { viewModel.onNoteSaved(noteExercise, it); noteFor = null },
            onDismiss = { noteFor = null },
        )
    }
}

@Composable
private fun LiveContent(
    session: LiveSession,
    state: LiveUiState,
    viewModel: LiveWorkoutViewModel,
    onMinimize: () -> Unit,
    onFinishClick: () -> Unit,
    onTechnique: (Long) -> Unit,
    onNote: (Long) -> Unit,
    onExpandRest: () -> Unit,
) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(250)
        }
    }
    val elapsed = (now - session.startedAt.toEpochMilli()) / 1000

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconSquareButton(icon = GtIcons.ChevronLeft, contentDescription = "Zminimalizuj trening", onClick = onMinimize)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(session.title, style = MaterialTheme.typography.labelMedium, color = GtColors.TextSecondary)
                    Text(
                        Formatters.clock(elapsed),
                        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum"),
                        color = GtColors.TextPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                Text(
                    "Zakończ",
                    style = MaterialTheme.typography.titleSmall,
                    color = GtColors.OnLime,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (state.isFinishing) GtColors.Elevated else GtColors.Lime)
                        .clickable(enabled = !state.isFinishing, role = Role.Button, onClick = onFinishClick)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientProgressBar(progress = session.progress, modifier = Modifier.weight(1f))
                Text(
                    "${session.completedPlannedSets}/${session.plannedSets}",
                    style = MaterialTheme.typography.titleSmall,
                    color = GtColors.TextPrimary,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "warmup") {
                WarmupRow(done = session.warmupDone, onChange = viewModel::onWarmupChanged)
            }
            itemsIndexed(session.exercises, key = { _, ex -> ex.sessionExerciseId }) { index, exercise ->
                ExerciseCard(
                    index = index,
                    exercise = exercise,
                    inputRevision = state.inputRevision,
                    onValuesChanged = { set, w, v -> viewModel.onValuesChanged(exercise, set, w, v) },
                    onToggle = { set, w, v -> viewModel.onToggleSet(exercise, set, w, v) },
                    onAddSet = { viewModel.onAddSet(exercise) },
                    onLighter = { viewModel.onLighter(exercise) },
                    onNote = { onNote(exercise.sessionExerciseId) },
                    onTechnique = { onTechnique(exercise.sessionExerciseId) },
                )
            }
            item(key = "finish") {
                Spacer(Modifier.height(4.dp))
                NeonButton(
                    text = "ZAKOŃCZ TRENING",
                    onClick = onFinishClick,
                    enabled = !state.isFinishing,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        val timer = state.timer
        AnimatedVisibility(
            visible = timer != null && timer.endsAtMillis > now,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            if (timer != null) {
                RestBar(
                    timer = timer,
                    now = now,
                    onAdjust = viewModel::onRestAdjust,
                    onSkip = viewModel::onRestSkip,
                    onExpand = onExpandRest,
                )
            }
        }
    }
}

@Composable
private fun WarmupRow(done: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(GtColors.Surface)
            .border(1.dp, if (done) GtColors.Lime.copy(alpha = 0.5f) else GtColors.Divider, MaterialTheme.shapes.large)
            .clickable(role = Role.Checkbox) { onChange(!done) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(GtIcons.Flame, contentDescription = null, tint = if (done) GtColors.Lime else GtColors.TextSecondary, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text("Rozgrzewka zrobiona", style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
            Text("5–8 min cardio + lekka seria · +10 XP", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
        Checkbox(
            checked = done,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(
                checkedColor = GtColors.Lime,
                checkmarkColor = GtColors.OnLime,
                uncheckedColor = GtColors.OutlineStrong,
            ),
        )
    }
}

@Composable
private fun RestBar(
    timer: RestTimer,
    now: Long,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    onExpand: () -> Unit,
) {
    val remaining = ((timer.endsAtMillis - now).coerceAtLeast(0) + 999) / 1000
    val fraction = if (timer.totalSeconds > 0) (timer.endsAtMillis - now) / (timer.totalSeconds * 1000f) else 0f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GtColors.Surface)
            .clickable(onClickLabel = "Pokaż duży timer", onClick = onExpand)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RestRing(fraction = fraction)
        Column(Modifier.weight(1f)) {
            Text("Przerwa", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
            Text(Formatters.clock(remaining), style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)
        }
        SmallTimerButton("−15") { onAdjust(-15) }
        SmallTimerButton("+15") { onAdjust(15) }
        SmallTimerButton("Pomiń", onClick = onSkip)
    }
}

@Composable
private fun SmallTimerButton(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = GtColors.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GtColors.Elevated)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
private fun RestOverlay(
    timer: RestTimer,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    onCollapse: () -> Unit,
) {
    val now by produceState(System.currentTimeMillis(), timer.endsAtMillis) {
        while (true) {
            value = System.currentTimeMillis()
            delay(50)
        }
    }
    val remainingMs = (timer.endsAtMillis - now).coerceAtLeast(0)
    val fraction = if (timer.totalSeconds > 0) remainingMs / (timer.totalSeconds * 1000f) else 0f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GtColors.Background)
            .clickable(enabled = false) {}
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        Text("Przerwa", style = MaterialTheme.typography.titleMedium, color = GtColors.TextSecondary)
        Box(contentAlignment = Alignment.Center) {
            RestRing(fraction = fraction, size = 260.dp, stroke = 16.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    Formatters.clock((remainingMs + 999) / 1000),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                    color = GtColors.TextPrimary,
                )
                Text("z ${Formatters.clock(timer.totalSeconds.toLong())}", style = MaterialTheme.typography.bodyMedium, color = GtColors.TextSecondary)
            }
        }
        Text(
            "Następnie: ${timer.label}",
            style = MaterialTheme.typography.titleSmall,
            color = GtColors.TextSoft,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GhostButton("−15 s", onClick = { onAdjust(-15) }, modifier = Modifier.weight(1f), height = 56.dp)
            GhostButton("+15 s", onClick = { onAdjust(15) }, modifier = Modifier.weight(1f), height = 56.dp)
        }
        NeonButton("POMIŃ PRZERWĘ", onClick = onSkip, modifier = Modifier.fillMaxWidth())
        Text(
            "Zwiń",
            style = MaterialTheme.typography.titleSmall,
            color = GtColors.TextSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClick = onCollapse)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun FinishDialog(
    session: LiveSession,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    val anyDone = session.completedSetsTotal > 0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GtColors.Surface,
        title = { Text(if (anyDone) "Zakończyć trening?" else "Brak zaliczonych serii") },
        text = {
            Text(
                when {
                    !anyDone -> "Nic nie zostało zaliczone. Możesz wrócić do treningu albo go odrzucić."
                    session.completedPlannedSets < session.plannedSets ->
                        "Zaliczone serie: ${session.completedPlannedSets} z ${session.plannedSets}. " +
                            "Niezaliczone nie liczą się do XP — i to jest w porządku, jeśli tak podpowiada ciało."
                    else -> "Wszystkie serie zaliczone. Czas na podsumowanie!"
                },
                color = GtColors.TextSoft,
            )
        },
        confirmButton = {
            if (anyDone) {
                TextButton(onClick = onFinish) { Text("Zakończ", color = GtColors.Lime) }
            } else {
                TextButton(onClick = onDiscard) { Text("Odrzuć trening", color = GtColors.Error) }
            }
        },
        dismissButton = {
            Row {
                if (anyDone) TextButton(onClick = onDiscard) { Text("Odrzuć", color = GtColors.TextSecondary) }
                TextButton(onClick = onDismiss) { Text("Wróć", color = GtColors.TextPrimary) }
            }
        },
    )
}

@Composable
private fun NoteDialog(
    initial: String,
    title: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GtColors.Surface,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(280) },
                placeholder = { Text("Np. ustawienie siedziska 4, lekki ból barku") },
                minLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GtColors.Lime,
                    unfocusedBorderColor = GtColors.Outline,
                    cursorColor = GtColors.Lime,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Zapisz", color = GtColors.Lime) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj", color = GtColors.TextSecondary) } },
    )
}
