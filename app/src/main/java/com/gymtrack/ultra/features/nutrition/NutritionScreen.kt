package com.gymtrack.ultra.features.nutrition

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.nutrition.FoodEntry
import com.gymtrack.ultra.domain.nutrition.FoodPresets
import com.gymtrack.ultra.domain.nutrition.ProteinCalculator
import com.gymtrack.ultra.domain.nutrition.ProteinDay
import com.gymtrack.ultra.domain.nutrition.ProteinGoal
import com.gymtrack.ultra.ui.components.FieldKind
import com.gymtrack.ultra.ui.components.GhostButton
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.GtTextField
import com.gymtrack.ultra.ui.components.IconSquareButton
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.components.appear
import com.gymtrack.ultra.ui.components.rememberAppearRegistry
import com.gymtrack.ultra.ui.components.rememberCountUp
import com.gymtrack.ultra.ui.components.rememberGtHaptics
import com.gymtrack.ultra.ui.components.rememberPressScale
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NutritionScreen(
    onBack: () -> Unit,
    viewModel: NutritionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = rememberGtHaptics()
    var editing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is NutritionEvent.Added -> scope.launch {
                    snackbar.currentSnackbarData?.dismiss()
                    val result = snackbar.showSnackbar(event.text, actionLabel = "COFNIJ", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undo(event.entryId)
                }
                is NutritionEvent.Message -> scope.launch { snackbar.showSnackbar(event.text) }
            }
        }
    }

    // Wibracja, gdy dzienny cel właśnie został osiągnięty (nie przy wejściu na ekran).
    var wasReached by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(state.reachedToday, state.isLoading) {
        if (state.isLoading) return@LaunchedEffect
        if (wasReached == false && state.reachedToday) haptics.success()
        wasReached = state.reachedToday
    }

    Box(
        Modifier
            .fillMaxSize()
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                IconSquareButton(icon = GtIcons.ChevronLeft, contentDescription = "Wróć", onClick = onBack)
                Column {
                    Text("Białko", style = MaterialTheme.typography.headlineSmall, color = GtColors.TextPrimary)
                    Text("Bez liczenia kalorii — tylko to, co buduje mięśnie.", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                }
            }
            when {
                state.isLoading -> Unit
                !state.enabled -> DisabledState(onEnable = { viewModel.setEnabled(true) })
                state.target == null || editing -> SetupList(
                    state = state,
                    viewModel = viewModel,
                    onDone = { editing = false },
                    onCancel = if (state.target != null) ({ editing = false }) else null,
                )
                else -> ProteinList(
                    state = state,
                    viewModel = viewModel,
                    onTick = { haptics.tick() },
                    onEditGoal = { editing = true },
                )
            }
        }
        SnackbarHost(
            snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}

@Composable
private fun DisabledState(onEnable: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        GtCard {
            Text("Śledzenie białka jest wyłączone", style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
            Text(
                "Możesz je włączyć tutaj albo w Profilu. Wpisy z wcześniej zostały na telefonie.",
                style = MaterialTheme.typography.bodyMedium,
                color = GtColors.TextSecondary,
            )
            NeonButton("WŁĄCZ", onClick = onEnable, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ---------------------------------------------------------------- konfiguracja celu

@Composable
private fun SetupList(
    state: ProteinUiState,
    viewModel: NutritionViewModel,
    onDone: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    var manual by rememberSaveable { mutableStateOf(state.manualTarget != null && state.weightKg == null) }
    var weightText by rememberSaveable { mutableStateOf(state.weightKg?.let { Formatters.weightNumber(it) } ?: "") }
    var goal by rememberSaveable { mutableStateOf(state.goal) }
    var targetText by rememberSaveable { mutableStateOf(state.manualTarget?.toString() ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "setup") {
            GtCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GtColors.LimeDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(GtIcons.Egg, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(26.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Ile białka potrzebujesz?", style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)
                        Text(
                            if (manual) "Wpisz swój dzienny cel w gramach." else "Podaj wagę, a policzę dzienny cel.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GtColors.TextSecondary,
                        )
                    }
                }

                if (!manual) {
                    GtTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        placeholder = "Twoja waga",
                        kind = FieldKind.DECIMAL,
                        maxLength = 5,
                        suffix = "kg",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProteinGoal.entries.forEach { option ->
                        GoalOption(option, selected = option == goal, onClick = { goal = option })
                    }
                    val preview = viewModel.preview(weightText, goal)
                    Text(
                        preview?.let { "Twój cel: $it g białka dziennie" } ?: "Tu pojawi się Twój cel",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (preview != null) GtColors.Lime else GtColors.TextMuted,
                    )
                    NeonButton(
                        "ZAPISZ CEL",
                        enabled = preview != null,
                        onClick = { if (viewModel.saveFromWeight(weightText, goal)) onDone() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GhostButton("Wolę wpisać cel ręcznie", onClick = { manual = true }, modifier = Modifier.fillMaxWidth())
                } else {
                    GtTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        placeholder = "Cel w gramach, np. 100",
                        kind = FieldKind.NUMBER,
                        maxLength = 3,
                        suffix = "g",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val valid = targetText.toIntOrNull()?.let { it in ProteinCalculator.MIN_TARGET..ProteinCalculator.MAX_TARGET } == true
                    NeonButton(
                        "ZAPISZ CEL",
                        enabled = valid,
                        onClick = { if (viewModel.saveManual(targetText)) onDone() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GhostButton("Policz z wagi", onClick = { manual = false }, modifier = Modifier.fillMaxWidth())
                }
                if (onCancel != null) {
                    GhostButton("Anuluj", onClick = onCancel, modifier = Modifier.fillMaxWidth())
                }
                if (state.weightKg != null || state.manualTarget != null) {
                    Text(
                        "Usuń wagę i cel",
                        style = MaterialTheme.typography.labelLarge,
                        color = GtColors.Error,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) {
                                viewModel.clearGoal()
                                onDone()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
        item(key = "note") { PrivacyNote() }
    }
}

@Composable
private fun GoalOption(goal: ProteinGoal, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) GtColors.LimeDim else GtColors.Elevated)
            .border(1.dp, if (selected) GtColors.Lime.copy(alpha = 0.6f) else GtColors.Elevated, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) GtColors.Lime else GtColors.OutlineStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(GtColors.Lime))
        }
        Column(Modifier.weight(1f)) {
            Text(
                "${goal.label} · ${goal.gramsPerKg.toString().replace('.', ',')} g na kg",
                style = MaterialTheme.typography.titleSmall,
                color = GtColors.TextPrimary,
            )
            Text(goal.hint, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
    }
}

@Composable
private fun PrivacyNote() {
    Text(
        "Wartości są orientacyjne. Masz specjalną dietę albo problemy zdrowotne? Zapytaj lekarza lub dietetyka. " +
            "Waga zostaje tylko na tym telefonie i możesz ją w każdej chwili usunąć.",
        style = MaterialTheme.typography.bodySmall,
        color = GtColors.TextMuted,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

// ---------------------------------------------------------------- dzień

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProteinList(
    state: ProteinUiState,
    viewModel: NutritionViewModel,
    onTick: () -> Unit,
    onEditGoal: () -> Unit,
) {
    val appearRegistry = rememberAppearRegistry()
    var showAll by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("") }
    val target = state.target ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hero") {
            Box(Modifier.appear(appearRegistry, "hero", 0)) { HeroCard(state, target) }
        }
        item(key = "quick") {
            Box(Modifier.appear(appearRegistry, "quick", 1)) {
                GtCard {
                    SectionLabel("Szybko dodaj")
                    val presets = if (showAll) FoodPresets.all else FoodPresets.all.take(8)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEach { preset ->
                            FoodChip(preset.name, preset.portion, preset.proteinGrams) {
                                onTick()
                                viewModel.addPreset(preset)
                            }
                        }
                    }
                    Text(
                        if (showAll) "Pokaż mniej" else "Więcej produktów (${FoodPresets.all.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = GtColors.Lime,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(role = Role.Button) { showAll = !showAll }
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                    )
                    if (state.recentCustom.isNotEmpty()) {
                        SectionLabel("Ostatnio wpisywane")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.recentCustom.forEach { (foodName, foodGrams) ->
                                FoodChip(foodName, null, foodGrams) {
                                    onTick()
                                    viewModel.add(foodName, foodGrams)
                                }
                            }
                        }
                    }
                }
            }
        }
        item(key = "custom") {
            Box(Modifier.appear(appearRegistry, "custom", 2)) {
                GtCard {
                    SectionLabel("Własny produkt")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        GtTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "Np. kanapka z jajkiem",
                            label = "Nazwa produktu",
                            modifier = Modifier.weight(1f),
                        )
                        GtTextField(
                            value = grams,
                            onValueChange = { grams = it },
                            placeholder = "g",
                            label = "Gramy białka",
                            kind = FieldKind.NUMBER,
                            maxLength = 3,
                            suffix = "g",
                            modifier = Modifier.width(78.dp),
                            onDone = {
                                if (viewModel.addCustom(name, grams)) {
                                    onTick()
                                    name = ""
                                    grams = ""
                                }
                            },
                        )
                        IconSquareButton(
                            icon = GtIcons.Plus,
                            contentDescription = "Dodaj produkt",
                            size = 52.dp,
                            onClick = {
                                if (viewModel.addCustom(name, grams)) {
                                    onTick()
                                    name = ""
                                    grams = ""
                                }
                            },
                        )
                    }
                    Text(
                        "Ile białka? Sprawdź etykietę: „Białko … g” w 100 g produktu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GtColors.TextMuted,
                    )
                }
            }
        }
        item(key = "today") {
            Box(Modifier.appear(appearRegistry, "today", 3)) {
                GtCard(verticalSpacing = 8.dp) {
                    SectionLabel(if (state.todayEntries.isEmpty()) "Dziś" else "Dziś · ${state.todayEntries.size}")
                    if (state.todayEntries.isEmpty()) {
                        Text(
                            "Jeszcze nic. Dodaj pierwszy posiłek — jednym kliknięciem wyżej.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GtColors.TextSecondary,
                        )
                    } else {
                        state.todayEntries.forEach { entry -> EntryRow(entry, onDelete = { viewModel.delete(entry) }) }
                    }
                }
            }
        }
        item(key = "week") {
            Box(Modifier.appear(appearRegistry, "week", 4)) {
                GtCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("Ostatnie 7 dni", modifier = Modifier.weight(1f))
                        Text(
                            "w celu: ${state.daysOnTarget}/7",
                            style = MaterialTheme.typography.labelLarge,
                            color = GtColors.TextSecondary,
                        )
                    }
                    WeekBars(state.week, target)
                }
            }
        }
        item(key = "goal") {
            Box(Modifier.appear(appearRegistry, "goal", 5)) {
                GtCard {
                    SectionLabel("Twój cel")
                    Text("$target g dziennie", style = MaterialTheme.typography.titleLarge, color = GtColors.TextPrimary)
                    Text(
                        state.weightKg?.let {
                            "${viewModel.formatWeight(it)} × ${state.goal.gramsPerKg.toString().replace('.', ',')} g/kg · ${state.goal.label}"
                        } ?: "Cel ustawiony ręcznie",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GtColors.TextSecondary,
                    )
                    GhostButton("Zmień cel", onClick = onEditGoal, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        item(key = "note") { PrivacyNote() }
    }
}

@Composable
private fun HeroCard(state: ProteinUiState, target: Int) {
    val eaten = rememberCountUp(state.eatenToday, durationMillis = 700)
    GtCard(tint = if (state.reachedToday) GtColors.Lime else null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(124.dp), contentAlignment = Alignment.Center) {
                ProteinRing(state.progress, Modifier.fillMaxSize())
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$eaten",
                        style = MaterialTheme.typography.displaySmall,
                        color = GtColors.TextPrimary,
                        modifier = Modifier.semantics { contentDescription = "Zjedzone dziś: ${state.eatenToday} gramów białka z $target" },
                    )
                    Text("z $target g", style = MaterialTheme.typography.labelLarge, color = GtColors.TextSecondary)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (state.reachedToday) "Cel na dziś zrobiony" else "Zostało ${state.remaining} g",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (state.reachedToday) GtColors.Lime else GtColors.TextPrimary,
                )
                Text(
                    if (state.reachedToday) {
                        "Nie musisz dobijać więcej — tyle wystarczy."
                    } else {
                        "Najłatwiej rozłożyć białko na 3–4 posiłki."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = GtColors.TextSecondary,
                )
            }
        }
    }
}

/** Pierścień postępu: napełnia się od zera przy wejściu, potem płynnie przy każdym wpisie. */
@Composable
private fun ProteinRing(progress: Float, modifier: Modifier = Modifier) {
    val reduced = LocalGtMotion.current.reduced
    val anim = remember { Animatable(if (reduced) progress else 0f) }
    LaunchedEffect(progress, reduced) {
        if (reduced) {
            anim.snapTo(progress)
        } else {
            anim.animateTo(progress, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow))
        }
    }
    Canvas(modifier) {
        val stroke = 12.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        drawArc(GtColors.Elevated, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
        val sweep = 360f * anim.value
        if (sweep > 0.5f) {
            drawArc(GtColors.Lime, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun FoodChip(name: String, portion: String?, grams: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val scale by rememberPressScale(interaction, pressedScale = 0.94f)
    Column(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(GtColors.Elevated)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = "Dodaj $name, $grams gramów białka", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(name, style = MaterialTheme.typography.bodyMedium, color = GtColors.TextPrimary)
            Text("+$grams g", style = MaterialTheme.typography.labelLarge, color = GtColors.Lime)
        }
        if (portion != null) {
            Text(portion, style = MaterialTheme.typography.labelSmall, color = GtColors.TextMuted)
        }
    }
}

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
private fun EntryRow(entry: FoodEntry, onDelete: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            entry.createdAt.atZone(ZoneId.systemDefault()).format(TimeFormat),
            style = MaterialTheme.typography.labelMedium,
            color = GtColors.TextMuted,
        )
        Text(entry.name, style = MaterialTheme.typography.bodyLarge, color = GtColors.TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        Text("${entry.proteinGrams} g", style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(role = Role.Button, onClickLabel = "Usuń ${entry.name}", onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(GtIcons.Close, contentDescription = null, tint = GtColors.TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

/** 7 słupków (najstarszy → dziś), przerywana linia celu. Dni w celu na lime. */
@Composable
private fun WeekBars(days: List<ProteinDay>, target: Int) {
    val reduced = LocalGtMotion.current.reduced
    val grow = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) { if (!reduced) grow.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    val maxValue = maxOf(target * 1.25f, days.maxOfOrNull { it.grams }?.toFloat() ?: 0f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(96.dp)
                .semantics {
                    contentDescription = days.joinToString { "${Formatters.shortDay(it.date.dayOfWeek)} ${it.grams} g" }
                },
        ) {
            val slot = size.width / days.size
            val barWidth = slot * 0.46f
            val targetY = size.height * (1f - target / maxValue)
            days.forEachIndexed { i, day ->
                val h = (day.grams / maxValue) * size.height * grow.value
                val x = slot * i + (slot - barWidth) / 2f
                val onTarget = day.grams >= target * 0.9f
                if (day.grams > 0) {
                    drawRoundRect(
                        color = if (onTarget) GtColors.Lime else GtColors.OutlineStrong,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                } else {
                    drawRoundRect(
                        color = GtColors.Elevated,
                        topLeft = Offset(x, size.height - 4.dp.toPx()),
                        size = Size(barWidth, 4.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                }
            }
            drawLine(
                color = GtColors.TextMuted,
                start = Offset(0f, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }
        Row(Modifier.fillMaxWidth()) {
            days.forEachIndexed { i, day ->
                Text(
                    Formatters.shortDay(day.date.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == days.lastIndex) GtColors.Lime else GtColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
