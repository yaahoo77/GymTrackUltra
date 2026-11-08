package com.gymtrack.ultra.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.ExperienceLevel
import com.gymtrack.ultra.domain.model.TrainingGoal
import com.gymtrack.ultra.domain.nutrition.ProteinGoal
import com.gymtrack.ultra.ui.components.FieldKind
import com.gymtrack.ultra.ui.components.GtTextField
import com.gymtrack.ultra.ui.components.Iskra
import com.gymtrack.ultra.ui.components.IskraMood
import com.gymtrack.ultra.ui.components.NeonButton
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.components.rememberGtHaptics
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.delay
import java.time.DayOfWeek

private const val STEPS = 5

/** Kreator startowy: 5 kroków, każdy do pominięcia. Nic tu nie jest obowiązkowe. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val defaults by viewModel.defaults.collectAsStateWithLifecycle()
    if (!defaults.loaded) return
    val haptics = rememberGtHaptics()
    val reduced = LocalGtMotion.current.reduced

    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf(defaults.name) }
    var goal by rememberSaveable { mutableStateOf(defaults.goal) }
    var experience by rememberSaveable { mutableStateOf(defaults.experience) }
    var days by rememberSaveable { mutableStateOf(defaults.days.map { it.value }.sorted()) }
    var weightText by rememberSaveable { mutableStateOf("") }
    var proteinGoal by rememberSaveable { mutableStateOf(ProteinGoal.TRAINING) }
    var reduceMotion by rememberSaveable { mutableStateOf(defaults.reduceMotion) }
    var hapticsOn by rememberSaveable { mutableStateOf(defaults.haptics) }
    var celebrations by rememberSaveable { mutableStateOf(defaults.celebrations) }
    var iskraJumps by rememberSaveable { mutableIntStateOf(0) }

    BackHandler(enabled = step > 0) { step -= 1 }
    LaunchedEffect(step) {
        delay(250)
        iskraJumps += 1
    }

    val daysValid = days.size == defaults.planDays
    val canContinue = step != 2 || daysValid

    fun finish() {
        viewModel.finish(
            OnboardingResult(
                name = name.trim(),
                goal = goal,
                experience = experience,
                days = days.map { DayOfWeek.of(it) }.toSet(),
                weightKg = viewModel.proteinPreview(weightText, proteinGoal)?.let { weightText.trim().replace(',', '.').toDoubleOrNull() },
                proteinGoal = proteinGoal,
                reduceMotion = reduceMotion,
                haptics = hapticsOn,
                celebrations = celebrations,
            ),
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(GtColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Pasek: postęp + pomiń
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(STEPS) { index ->
                    val width by animateFloatAsState(if (index == step) 22f else 8f, tween(if (reduced) 0 else 250), label = "dot")
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(width.dp)
                            .clip(CircleShape)
                            .background(if (index <= step) GtColors.Lime else GtColors.Elevated),
                    )
                }
            }
            Text(
                "Pomiń",
                style = MaterialTheme.typography.labelLarge,
                color = GtColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClickLabel = "Pomiń kreator", onClick = viewModel::skip)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (reduced) {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                } else {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it / 3 else -it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally { if (forward) -it / 3 else it / 3 } + fadeOut(tween(160)))
                }
            },
            label = "onboardingStep",
            modifier = Modifier.weight(1f),
        ) { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val mood = when (current) {
                    0 -> IskraMood.CHEER
                    2 -> IskraMood.FOCUS
                    else -> IskraMood.HAPPY
                }
                Iskra(mood = mood, size = if (current == 0) 120.dp else 72.dp, reactKey = iskraJumps)
                when (current) {
                    0 -> {
                        Title("Cześć! Jestem Iskra.")
                        Body("Poprowadzę Cię przez trening: plan, ciężary, przerwy i postępy. Bez konta — wszystko zostaje na telefonie.")
                        SectionLabel("Jak mam się do Ciebie zwracać? (opcjonalnie)")
                        GtTextField(value = name, onValueChange = { name = it }, placeholder = "Imię albo ksywka", maxLength = 24, modifier = Modifier.fillMaxWidth())
                    }
                    1 -> {
                        Title("Po co trenujesz?")
                        Body("Dopasuję podpowiedzi. Zawsze możesz to zmienić w Profilu.")
                        TrainingGoal.entries.forEach { option ->
                            OptionRow(option.label, selected = option == goal) { goal = option; haptics.tick() }
                        }
                        SectionLabel("Doświadczenie")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExperienceLevel.entries.forEach { option ->
                                Chip(option.label, selected = option == experience) { experience = option; haptics.tick() }
                            }
                        }
                    }
                    2 -> {
                        Title("Kiedy ćwiczysz?")
                        Body("Wybierz ${defaults.planDays} dni — tyle ma Twój plan. Pozostałe to regeneracja, która nie przerywa serii.")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            DayOfWeek.values().forEach { day ->
                                val selected = day.value in days
                                DayChip(Formatters.shortDay(day), selected) {
                                    days = if (selected) {
                                        days - day.value
                                    } else if (days.size < defaults.planDays) {
                                        (days + day.value).sorted()
                                    } else {
                                        days
                                    }
                                    haptics.tick()
                                }
                            }
                        }
                        val codes = listOf("A", "B", "C", "D", "E", "F", "G")
                        Text(
                            if (daysValid) {
                                days.mapIndexed { i, d -> "${codes.getOrElse(i) { "?" }} → ${Formatters.dayName(DayOfWeek.of(d))}" }.joinToString("   ")
                            } else {
                                "Wybrane: ${days.size} z ${defaults.planDays}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (daysValid) GtColors.Lime else GtColors.TextSecondary,
                        )
                    }
                    3 -> {
                        Title("Białko (opcjonalnie)")
                        Body("Podaj wagę, a policzę dzienny cel białka. Możesz to pominąć — waga zostaje tylko na telefonie.")
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
                            OptionRow(
                                "${option.label} · ${option.gramsPerKg.toString().replace('.', ',')} g/kg",
                                selected = option == proteinGoal,
                            ) { proteinGoal = option }
                        }
                        val preview = viewModel.proteinPreview(weightText, proteinGoal)
                        Text(
                            preview?.let { "Twój cel: $it g białka dziennie" } ?: "Bez wagi — ten krok zostanie pominięty.",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (preview != null) GtColors.Lime else GtColors.TextMuted,
                        )
                    }
                    else -> {
                        Title("Jak ma się zachowywać aplikacja?")
                        Body("Bez uzależniających powiadomień i bez kar za odpoczynek. Reszta zależy od Ciebie.")
                        ToggleRow("Ogranicz animacje", "Tylko krótkie przenikania, bez drobinek.", reduceMotion) { reduceMotion = it }
                        ToggleRow("Wibracje", "Po zaliczonej serii i na koniec przerwy.", hapticsOn) { hapticsOn = it }
                        ToggleRow("Efekty celebracji", "Konfetti po treningu i awansie.", celebrations) { celebrations = it }
                    }
                }
            }
        }

        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            NeonButton(
                text = when {
                    step == STEPS - 1 -> "ZACZYNAMY"
                    step == 3 && weightText.isBlank() -> "POMIŃ TEN KROK"
                    else -> "DALEJ"
                },
                enabled = canContinue,
                onClick = {
                    haptics.tick()
                    if (step < STEPS - 1) step += 1 else finish()
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.headlineMedium, color = GtColors.TextPrimary)
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge, color = GtColors.TextSecondary)
}

@Composable
private fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) GtColors.LimeDim else GtColors.Surface)
            .border(1.dp, if (selected) GtColors.Lime.copy(alpha = 0.6f) else GtColors.Outline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        Text(text, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) GtColors.Lime else GtColors.TextSoft,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) GtColors.LimeDim else GtColors.Surface)
            .border(1.dp, if (selected) GtColors.Lime.copy(alpha = 0.6f) else GtColors.Outline, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    )
}

@Composable
private fun DayChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (selected) GtColors.Lime else GtColors.Surface)
            .border(1.dp, if (selected) GtColors.Lime else GtColors.Outline, CircleShape)
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) GtColors.OnLime else GtColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ToggleRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(GtColors.Surface)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = GtColors.TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
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
