package com.gymtrack.ultra.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.domain.model.PlanExercise
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.rememberGtHaptics
import com.gymtrack.ultra.ui.theme.GtColors

/** Edycja jednego ćwiczenia w planie: serie, zakres, przerwa + zamiana, kolejność, usunięcie. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlanExerciseSheet(
    item: PlanExercise,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onSave: (sets: Int, min: Int, max: Int, rest: Int) -> Unit,
    onSwap: () -> Unit,
    onMove: (delta: Int) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val timed = item.exercise.trackingType == TrackingType.TIME
    val unit = if (timed) "s" else "powt."
    val step = if (timed) 5 else 1
    val ceiling = if (timed) 300 else 30
    var sets by rememberSaveable(item.id) { mutableIntStateOf(item.targetSets) }
    var min by rememberSaveable(item.id) { mutableIntStateOf(item.targetMin) }
    var max by rememberSaveable(item.id) { mutableIntStateOf(item.targetMax) }
    var rest by rememberSaveable(item.id) { mutableIntStateOf(item.restSeconds) }
    val haptics = rememberGtHaptics()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = GtColors.Surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(item.exercise.namePl, style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)

            Stepper("Serie", "$sets", onMinus = { sets = (sets - 1).coerceAtLeast(1); haptics.tick() }, onPlus = { sets = (sets + 1).coerceAtMost(6); haptics.tick() })
            HorizontalDivider(color = GtColors.Divider)
            Stepper(
                if (timed) "Czas od" else "Powtórzenia od",
                "$min $unit",
                onMinus = { min = (min - step).coerceAtLeast(step); haptics.tick() },
                onPlus = {
                    min = (min + step).coerceAtMost(ceiling)
                    if (max < min) max = min
                    haptics.tick()
                },
            )
            Stepper(
                if (timed) "Czas do" else "Powtórzenia do",
                "$max $unit",
                onMinus = {
                    max = (max - step).coerceAtLeast(step)
                    if (min > max) min = max
                    haptics.tick()
                },
                onPlus = { max = (max + step).coerceAtMost(ceiling); haptics.tick() },
            )
            HorizontalDivider(color = GtColors.Divider)
            Stepper(
                "Przerwa",
                "$rest s",
                onMinus = { rest = (rest - 15).coerceAtLeast(30); haptics.tick() },
                onPlus = { rest = (rest + 15).coerceAtMost(240); haptics.tick() },
            )
            if (!timed && max > 15) {
                Text(
                    "Powyżej 15 powtórzeń to już bardziej wytrzymałość. Dla siły i mięśni najlepiej 6–15.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextMuted,
                )
            }

            NeonButton(
                "ZAPISZ",
                onClick = { onSave(sets, min, max, rest) },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetAction(GtIcons.Swap, "Zamień", Modifier.weight(1f), onClick = onSwap)
                SheetAction(GtIcons.ArrowUp, "Wyżej", Modifier.weight(1f), enabled = canMoveUp) { onMove(-1) }
                SheetAction(GtIcons.ArrowDown, "Niżej", Modifier.weight(1f), enabled = canMoveDown) { onMove(1) }
            }
            if (canRemove) {
                Text(
                    "Usuń z planu",
                    style = MaterialTheme.typography.labelLarge,
                    color = GtColors.Error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.Button, onClick = onRemove)
                        .padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = GtColors.TextSoft, modifier = Modifier.weight(1f))
        StepButton("−", "Mniej: $label", onMinus)
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = GtColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(92.dp),
        )
        StepButton("+", "Więcej: $label", onPlus)
    }
}

@Composable
private fun StepButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GtColors.Elevated)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)
    }
}

@Composable
private fun SheetAction(icon: ImageVector, text: String, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GtColors.Elevated)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val color = if (enabled) GtColors.TextPrimary else GtColors.TextMuted
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
