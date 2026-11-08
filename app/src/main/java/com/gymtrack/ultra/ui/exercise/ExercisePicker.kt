package com.gymtrack.ultra.ui.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.domain.model.Exercise
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.domain.plan.ExerciseAlternatives
import com.gymtrack.ultra.domain.progression.MuscleGroup
import com.gymtrack.ultra.ui.components.FieldKind
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.GtTextField
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.theme.GtColors

/**
 * Wybór ćwiczenia: przy zamianie najpierw "Podobne" (te same mięśnie), potem cała biblioteka po partiach.
 * [alsoInPlanDefault] != null pokazuje przełącznik "Zamień też w planie" (zamiana w trakcie treningu).
 * [onCreateCustom] != null pozwala dodać własne ćwiczenie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    title: String,
    library: List<Exercise>,
    onPick: (exercise: Exercise, alsoInPlan: Boolean) -> Unit,
    onDismiss: () -> Unit,
    current: Exercise? = null,
    excludeIds: Set<String> = emptySet(),
    alsoInPlanDefault: Boolean? = null,
    onCreateCustom: ((name: String, muscle: MuscleGroup, tracking: TrackingType) -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var alsoInPlan by rememberSaveable { mutableStateOf(alsoInPlanDefault ?: false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val available = library.filter { it.id !in excludeIds && it.id != current?.id }
    val similar = current?.let { ExerciseAlternatives.forExercise(it.id, available) }.orEmpty()
    val groups = ExerciseAlternatives.grouped(available)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GtColors.Surface,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)
                    current?.let {
                        Text("Zamiast: ${it.namePl}", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                    }
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClickLabel = "Zamknij", onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(GtIcons.Close, contentDescription = "Zamknij", tint = GtColors.TextSecondary)
                }
            }
            if (alsoInPlanDefault != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Zamień też w planie", style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
                        Text(
                            "Wyłączone = tylko w dzisiejszym treningu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GtColors.TextSecondary,
                        )
                    }
                    Switch(
                        checked = alsoInPlan,
                        onCheckedChange = { alsoInPlan = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GtColors.OnLime,
                            checkedTrackColor = GtColors.Lime,
                            uncheckedThumbColor = GtColors.TextSecondary,
                            uncheckedTrackColor = GtColors.Elevated,
                            uncheckedBorderColor = GtColors.OutlineStrong,
                        ),
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onCreateCustom != null) {
                    item(key = "create") {
                        if (creating) {
                            CustomExerciseForm(
                                onCreate = { name, muscle, tracking ->
                                    onCreateCustom(name, muscle, tracking)
                                    creating = false
                                },
                                onCancel = { creating = false },
                            )
                        } else {
                            PickerRow(
                                title = "Utwórz własne ćwiczenie",
                                subtitle = "Nazwa, partia, powtórzenia albo czas",
                                exerciseId = null,
                                onClick = { creating = true },
                            )
                        }
                    }
                }
                if (similar.isNotEmpty()) {
                    item(key = "similar-label") { SectionLabel("Podobne — te same mięśnie", color = GtColors.Lime) }
                    items(similar, key = { "s-" + it.id }) { exercise ->
                        PickerRow(exercise.namePl, exercise.muscleGroup, exercise.id, animate = true) { onPick(exercise, alsoInPlan) }
                    }
                }
                groups.forEach { (muscle, list) ->
                    item(key = "label-" + muscle.name) {
                        SectionLabel(muscle.label, modifier = Modifier.padding(top = 8.dp))
                    }
                    items(list, key = { "a-" + it.id }) { exercise ->
                        PickerRow(exercise.namePl, exercise.muscleGroup, exercise.id) { onPick(exercise, alsoInPlan) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    title: String,
    subtitle: String,
    exerciseId: String?,
    animate: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GtColors.Elevated)
            .clickable(role = Role.Button, onClickLabel = "Wybierz: $title", onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GtColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            if (exerciseId != null && ExerciseMotions.forId(exerciseId) != null) {
                ExerciseFigure(exerciseId, Modifier.fillMaxSize().padding(3.dp), playing = animate)
            } else {
                Icon(
                    if (exerciseId == null) GtIcons.Plus else GtIcons.Workout,
                    contentDescription = null,
                    tint = if (exerciseId == null) GtColors.Lime else GtColors.TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseForm(
    onCreate: (String, MuscleGroup, TrackingType) -> Unit,
    onCancel: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf(MuscleGroup.CHEST) }
    var tracking by rememberSaveable { mutableStateOf(TrackingType.REPS) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GtColors.Elevated)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Własne ćwiczenie", style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
        GtTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Nazwa, np. Wyciskanie na maszynie barkowej",
            kind = FieldKind.TEXT,
            modifier = Modifier.fillMaxWidth(),
        )
        SectionLabel("Główna partia")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MuscleGroup.entries.forEach { option ->
                Choice(option.label, selected = option == muscle) { muscle = option }
            }
        }
        SectionLabel("Liczysz")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Choice("Powtórzenia", selected = tracking == TrackingType.REPS) { tracking = TrackingType.REPS }
            Choice("Czas", selected = tracking == TrackingType.TIME) { tracking = TrackingType.TIME }
        }
        NeonButton(
            "DODAJ",
            enabled = name.isNotBlank(),
            onClick = { onCreate(name, muscle, tracking) },
            modifier = Modifier.fillMaxWidth(),
            height = 48.dp,
        )
        Text(
            "Anuluj",
            style = MaterialTheme.typography.labelLarge,
            color = GtColors.TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onCancel)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) GtColors.Lime else GtColors.TextSoft,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) GtColors.LimeDim else GtColors.Surface)
            .border(1.dp, if (selected) GtColors.Lime.copy(alpha = 0.6f) else GtColors.Outline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}
