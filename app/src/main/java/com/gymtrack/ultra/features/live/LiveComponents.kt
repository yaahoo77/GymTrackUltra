package com.gymtrack.ultra.features.live

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.ultra.domain.progression.LoadAdvice
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.LiveExercise
import com.gymtrack.ultra.domain.model.LiveSet
import com.gymtrack.ultra.domain.model.PreviousSet
import com.gymtrack.ultra.domain.model.TrackingType
import com.gymtrack.ultra.domain.progression.MuscleMap
import com.gymtrack.ultra.domain.progression.MuscleScoring
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.rememberGtHaptics
import com.gymtrack.ultra.ui.exercise.ExerciseFigure
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlin.math.cos
import kotlin.math.sin

/** Karta ćwiczenia w trakcie treningu: animacja ruchu, nagłówek, "ostatnio", serie, akcje. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExerciseCard(
    index: Int,
    exercise: LiveExercise,
    isCurrent: Boolean,
    inputRevision: Int,
    onValuesChanged: (LiveSet, String, String) -> Unit,
    onToggle: (LiveSet, String, String) -> Unit,
    onAddSet: () -> Unit,
    onLighter: () -> Unit,
    onNote: () -> Unit,
    onSwap: () -> Unit,
    onTechnique: () -> Unit,
) {
    val done = exercise.isDone
    val shape = MaterialTheme.shapes.large
    val borderColor by animateColorAsState(
        when {
            done -> GtColors.Lime.copy(alpha = 0.55f)
            isCurrent -> GtColors.Lime.copy(alpha = 0.32f)
            else -> GtColors.Divider
        },
        tween(220),
        label = "cardBorder",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(GtColors.Surface)
            .border(1.dp, borderColor, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GtColors.Background)
                    .clickable(onClickLabel = "Pokaż ruch: ${exercise.exercise.namePl}", role = Role.Button, onClick = onTechnique),
            ) {
                ExerciseFigure(
                    exerciseId = exercise.exercise.id,
                    playing = isCurrent && !done,
                    dimmed = done,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                )
                Box(
                    Modifier
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (done) GtColors.Lime else GtColors.Elevated),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(GtIcons.Check, contentDescription = null, tint = GtColors.OnLime, modifier = Modifier.size(13.dp))
                    } else {
                        Text(
                            "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) GtColors.Lime else GtColors.TextPrimary,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (isCurrent && !done) {
                    Text("TERAZ", style = MaterialTheme.typography.labelMedium, color = GtColors.Lime)
                }
                Text(exercise.exercise.namePl, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
                Text(
                    "${exercise.targetLabel} · przerwa ${exercise.restSeconds} s",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
                previousLine(exercise)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = GtColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val advice = exercise.advice
                if (advice is LoadAdvice.Increase && !done) {
                    Row(
                        Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GtColors.LimeDim)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(GtIcons.Sparkle, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(12.dp))
                        Text(
                            "Dziś +${Formatters.weightKg(advice.stepKg)} → ${Formatters.weightKg(advice.toKg)} (ostatnio ${advice.sets}×${advice.reps})",
                            style = MaterialTheme.typography.labelMedium,
                            color = GtColors.Lime,
                        )
                    }
                }
            }
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GtColors.Elevated)
                    .clickable(onClickLabel = "Technika: ${exercise.exercise.namePl}", role = Role.Button, onClick = onTechnique),
                contentAlignment = Alignment.Center,
            ) {
                Icon(GtIcons.Info, contentDescription = "Technika i bezpieczeństwo", tint = GtColors.TextSecondary, modifier = Modifier.size(20.dp))
            }
        }

        val timed = exercise.exercise.trackingType == TrackingType.TIME
        Row(Modifier.padding(horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#", style = MaterialTheme.typography.labelMedium, color = GtColors.TextMuted, modifier = Modifier.width(24.dp))
            Text("KG", style = MaterialTheme.typography.labelMedium, color = GtColors.TextMuted, modifier = Modifier.weight(1f))
            Text(if (timed) "SEKUNDY" else "POWT.", style = MaterialTheme.typography.labelMedium, color = GtColors.TextMuted, modifier = Modifier.weight(1f))
            Box(Modifier.width(52.dp))
        }

        exercise.sets.forEach { set ->
            val extra = set.index >= exercise.targetSets
            SetRow(
                exercise = exercise,
                set = set,
                isExtra = extra,
                inputRevision = inputRevision,
                onValuesChanged = { w, v -> onValuesChanged(set, w, v) },
                onToggle = { w, v -> onToggle(set, w, v) },
            )
        }

        AnimatedVisibility(visible = done, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            ExerciseDoneBanner(exercise)
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionChip("+ Seria", onAddSet)
            ActionChip("Lżej −10%", onLighter)
            ActionChip(if (exercise.note.isNullOrBlank()) "Notatka" else "Notatka ✓", onNote)
            // Zamiana tylko przed pierwszą zaliczoną serią — maszyna zajęta albo jej brak.
            if (exercise.sets.none { it.completed }) ActionChip("Zamień", onSwap)
        }
        exercise.note?.takeIf { it.isNotBlank() }?.let {
            Text("„$it”", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSoft)
        }
    }
}

private fun previousLine(exercise: LiveExercise): String? {
    if (exercise.previous.isEmpty()) return null
    val timed = exercise.exercise.trackingType == TrackingType.TIME
    val weight = exercise.previous.firstNotNullOfOrNull { it.weightKg }
    val values = exercise.previous.joinToString(" · ") { prev: PreviousSet ->
        if (timed) "${prev.durationSeconds ?: 0} s" else "${prev.reps ?: 0}"
    }
    return if (weight != null && weight > 0.0) "Ostatnio: ${Formatters.weightKg(weight)} × $values" else "Ostatnio: $values"
}

@Composable
private fun ExerciseDoneBanner(exercise: LiveExercise) {
    val targets = MuscleMap.targetsOf(exercise.exercise.id)
    val counted = minOf(exercise.completedSets, exercise.targetSets)
    val gains = targets.primary.map { "${it.label} +${counted * MuscleScoring.PRIMARY_SET}" } +
        targets.secondary.map { "${it.label} +${counted * MuscleScoring.SECONDARY_SET}" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GtColors.LimeDim)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(GtIcons.Check, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(18.dp))
        Column(Modifier.weight(1f)) {
            Text("ĆWICZENIE UKOŃCZONE", style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = GtColors.Lime)
            if (gains.isNotEmpty()) {
                Text(gains.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = GtColors.TextSoft)
            }
        }
    }
}

@Composable
private fun ActionChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
        color = GtColors.TextSoft,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GtColors.Elevated)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

@Composable
private fun SetRow(
    exercise: LiveExercise,
    set: LiveSet,
    isExtra: Boolean,
    inputRevision: Int,
    onValuesChanged: (String, String) -> Unit,
    onToggle: (String, String) -> Unit,
) {
    val timed = exercise.exercise.trackingType == TrackingType.TIME
    var weightText by remember(set.id, inputRevision) { mutableStateOf(set.weightKg?.let(Formatters::weightNumber).orEmpty()) }
    var valueText by remember(set.id, inputRevision) {
        mutableStateOf((if (timed) set.durationSeconds else set.reps)?.toString().orEmpty())
    }
    val previous = exercise.previous.getOrNull(set.index) ?: exercise.previous.lastOrNull()
    val weightHint = previous?.weightKg?.let(Formatters::weightNumber) ?: "—"
    val valueHint = when {
        timed -> (previous?.durationSeconds ?: exercise.targetMin).toString()
        else -> (previous?.reps ?: exercise.targetMax).toString()
    }
    val rowBg by animateColorAsState(
        if (set.completed) GtColors.Lime.copy(alpha = 0.08f) else Color.Transparent,
        tween(200),
        label = "rowBg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(rowBg)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            if (isExtra) "+" else "${set.index + 1}",
            style = MaterialTheme.typography.titleSmall,
            color = if (set.completed) GtColors.Lime else GtColors.TextSecondary,
            modifier = Modifier.width(24.dp),
        )
        NumberField(
            value = weightText,
            placeholder = weightHint,
            decimal = true,
            enabled = !set.completed,
            description = "Ciężar, seria ${set.index + 1}",
            onValueChange = {
                weightText = it
                onValuesChanged(weightText, valueText)
            },
            modifier = Modifier.weight(1f),
        )
        NumberField(
            value = valueText,
            placeholder = valueHint,
            decimal = false,
            enabled = !set.completed,
            description = (if (timed) "Sekundy" else "Powtórzenia") + ", seria ${set.index + 1}",
            onValueChange = {
                valueText = it
                onValuesChanged(weightText, valueText)
            },
            modifier = Modifier.weight(1f),
        )
        SetCheckButton(
            completed = set.completed,
            label = "Seria ${set.index + 1}",
            onClick = { onToggle(weightText, valueText) },
        )
    }
}

@Composable
private fun NumberField(
    value: String,
    placeholder: String,
    decimal: Boolean,
    enabled: Boolean,
    description: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = TextStyle(
        color = if (enabled) GtColors.TextPrimary else GtColors.TextSoft,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
    )
    BasicTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || (decimal && (it == ',' || it == '.')) }.take(6)
            onValueChange(filtered)
        },
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(GtColors.Lime),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier
            .height(48.dp)
            .semantics { contentDescription = description },
        decorationBox = { inner ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) GtColors.Elevated else GtColors.Surface)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = textStyle.copy(color = GtColors.TextMuted))
                }
                inner()
            }
        },
    )
}

/** Duży przycisk zaliczenia serii: wypełnienie lime, rysowany ptaszek, wibracja i krótki wybuch drobinek. */
@Composable
private fun SetCheckButton(completed: Boolean, label: String, onClick: () -> Unit) {
    val motion = LocalGtMotion.current
    val haptics = rememberGtHaptics()
    val bg by animateColorAsState(if (completed) GtColors.Lime else Color.Transparent, tween(160), label = "checkBg")
    val scale = remember { Animatable(1f) }
    val burst = remember { Animatable(1f) }
    var lastCompleted by remember { mutableStateOf(completed) }

    LaunchedEffect(completed) {
        if (completed && !lastCompleted && !motion.reduced) {
            scale.snapTo(0.82f)
            scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
        }
        if (completed && !lastCompleted && motion.showCelebrations) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
        }
        lastCompleted = completed
    }

    Box(
        modifier = Modifier
            .size(52.dp)
            .semantics {
                role = Role.Checkbox
                stateDescription = if (completed) "zaliczona" else "niezaliczona"
                contentDescription = label
            },
        contentAlignment = Alignment.Center,
    ) {
        if (burst.value < 1f) {
            Canvas(Modifier.size(52.dp)) {
                val t = burst.value
                val c = Offset(size.width / 2f, size.height / 2f)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0 - 90.0)
                    val r = 18f + t * 34f
                    drawCircle(
                        color = if (i % 2 == 0) GtColors.Lime else GtColors.Success,
                        radius = 4f * (1f - t) + 1f,
                        center = Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat()),
                        alpha = 1f - t,
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .size(52.dp * scale.value)
                .clip(RoundedCornerShape(12.dp))
                .background(bg)
                .border(2.dp, if (completed) GtColors.Lime else GtColors.OutlineStrong, RoundedCornerShape(12.dp))
                .clickable {
                    if (!completed) haptics.success() else haptics.tick()
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(
                visible = completed,
                enter = scaleIn(spring(dampingRatio = 0.5f)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(GtIcons.Check, contentDescription = null, tint = GtColors.OnLime, modifier = Modifier.size(26.dp))
            }
        }
    }
}

/** Pierścień odliczania przerwy. */
@Composable
internal fun RestRing(
    fraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    stroke: Dp = 5.dp,
) {
    Canvas(modifier.size(size)) {
        val w = stroke.toPx()
        val inset = w / 2f
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - w, this.size.height - w)
        drawArc(GtColors.Elevated, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(w))
        drawArc(
            color = GtColors.Lime,
            startAngle = -90f,
            sweepAngle = 360f * fraction.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(w, cap = StrokeCap.Round),
        )
    }
}
