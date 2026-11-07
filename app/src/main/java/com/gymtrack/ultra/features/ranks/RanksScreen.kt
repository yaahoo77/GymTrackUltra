package com.gymtrack.ultra.features.ranks

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.ui.components.rememberAppearRegistry
import com.gymtrack.ultra.ui.components.appear
import com.gymtrack.ultra.ui.components.shine
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.domain.model.ProgressSnapshot
import com.gymtrack.ultra.domain.progression.AccountRank
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.domain.progression.MuscleMap
import com.gymtrack.ultra.domain.progression.MuscleRank
import com.gymtrack.ultra.domain.progression.MuscleRankCurve
import com.gymtrack.ultra.domain.progression.MuscleScoring
import com.gymtrack.ultra.domain.progression.Rank
import com.gymtrack.ultra.domain.progression.RankTier
import com.gymtrack.ultra.domain.repository.PlanRepository
import com.gymtrack.ultra.domain.repository.ProgressRepository
import com.gymtrack.ultra.features.achievements.AchievementsScreen
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.ScreenHeader
import com.gymtrack.ultra.ui.components.SectionTitle
import com.gymtrack.ultra.ui.components.SkeletonBlock
import com.gymtrack.ultra.ui.rank.MuscleRankRow
import com.gymtrack.ultra.ui.rank.RankEmblem
import com.gymtrack.ultra.ui.rank.palette
import com.gymtrack.ultra.ui.theme.GtColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class RanksUiState(
    val isLoading: Boolean = true,
    val progress: ProgressSnapshot = ProgressSnapshot.Empty,
    val exerciseNames: Map<String, String> = emptyMap(),
)

@HiltViewModel
class RanksViewModel @Inject constructor(
    progressRepository: ProgressRepository,
    planRepository: PlanRepository,
) : ViewModel() {
    val uiState: StateFlow<RanksUiState> = combine(
        progressRepository.observeProgress(),
        planRepository.observePlan(),
    ) { progress, plan: List<PlanDay> ->
        RanksUiState(
            isLoading = false,
            progress = progress,
            exerciseNames = plan.flatMap { it.exercises }.associate { it.exercise.id to it.exercise.namePl },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RanksUiState())
}

private val Tabs = listOf("Mięśnie", "Osiągnięcia", "Drabinka")

@Composable
fun RanksScreen(viewModel: RanksViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var openMuscle by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "Rangi", modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp))
        UnderlineTabs(selected = tab, onSelect = { tab = it })
        when (tab) {
            0 -> MusclesTab(state, onOpenMuscle = { openMuscle = it.muscle.name })
            1 -> AchievementsScreen()
            else -> LadderTab(state.progress.level)
        }
    }

    val muscle = state.progress.gamification.muscles.firstOrNull { it.muscle.name == openMuscle }
    if (muscle != null) {
        MuscleSheet(muscle, state.exerciseNames, onDismiss = { openMuscle = null })
    }
}

@Composable
private fun UnderlineTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Tabs.forEachIndexed { i, title ->
            val isSelected = i == selected
            Column(
                Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onSelect(i) }
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    }
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) GtColors.TextPrimary else GtColors.TextMuted,
                )
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(0.6f)
                        .height(2.dp)
                        .background(if (isSelected) GtColors.TextPrimary else GtColors.Background),
                )
            }
        }
    }
}

@Composable
private fun MusclesTab(state: RanksUiState, onOpenMuscle: (MuscleRank) -> Unit) {
    if (state.isLoading) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { SkeletonBlock(height = 76.dp) }
        }
        return
    }
    val muscles = state.progress.gamification.muscles.sortedWith(
        compareByDescending<MuscleRank> { it.isRanked }.thenByDescending { it.progress.totalPoints },
    )
    val appearRegistry = rememberAppearRegistry()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "account") { Box(Modifier.appear(appearRegistry, "account", 0)) { AccountCard(state.progress.level) } }
        item(key = "title") {
            Text(
                "Każda partia ma własną rangę. Rośnie z seriami z planu i z pobitymi rekordami.",
                style = MaterialTheme.typography.bodySmall,
                color = GtColors.TextSecondary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        itemsIndexed(muscles, key = { _, it -> it.muscle.name }) { index, rank ->
            Box(Modifier.appear(appearRegistry, rank.muscle.name, index + 1)) {
                MuscleRankRow(rank, onClick = { onOpenMuscle(rank) })
            }
        }
    }
}

@Composable
private fun AccountCard(level: LevelProgress) {
    val palette = level.rank.tier.palette()
    GtCard(tint = palette.text) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RankEmblem(
                rank = level.rank,
                size = 92.dp,
                contentDescription = "Ranga konta ${level.rank.label}",
                modifier = Modifier.shine(periodMillis = 5200),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Ranga konta", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
                Text(level.rank.label, style = MaterialTheme.typography.headlineSmall, color = palette.text)
                Text("LVL ${level.level} · ${level.totalXp} XP", style = MaterialTheme.typography.bodyMedium, color = GtColors.TextPrimary)
                GradientProgressBar(progress = level.fraction, height = 5.dp, color = palette.text)
                level.rank.next?.let { next ->
                    Text(
                        "${next.label} od LVL ${firstLevelFor(next)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GtColors.TextSecondary,
                    )
                }
            }
        }
    }
}

private fun firstLevelFor(rank: Rank): Int {
    var level = 1
    while (level < 99 && AccountRank.forLevel(level) < rank) level++
    return level
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleSheet(rank: MuscleRank, names: Map<String, String>, onDismiss: () -> Unit) {
    val ranked = rank.isRanked
    val palette = if (ranked) rank.progress.rank.tier.palette() else com.gymtrack.ultra.ui.rank.UnrankedPalette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = GtColors.Surface,
        contentColor = GtColors.TextPrimary,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RankEmblem(
                rank = if (ranked) rank.progress.rank else null,
                size = 128.dp,
                modifier = Modifier.shine(enabled = ranked, periodMillis = 3600, delayMillis = 400),
            )
            Text(rank.muscle.label, style = MaterialTheme.typography.headlineMedium)
            Text(if (ranked) rank.progress.rank.label else "BEZ RANGI", style = MaterialTheme.typography.titleMedium, color = palette.text)
            if (ranked && !rank.progress.isMax) {
                GradientProgressBar(progress = rank.progress.fraction, height = 6.dp, color = palette.text)
                Text(
                    "${rank.progress.pointsIntoRank} / ${rank.progress.pointsForNext} pkt do ${rank.progress.rank.next?.label.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
            Text(
                "Łącznie: ${rank.progress.totalPoints} pkt · ${rank.plannedSets} serii",
                style = MaterialTheme.typography.bodyMedium,
                color = GtColors.TextSoft,
            )
            GtCard(modifier = Modifier.fillMaxWidth(), verticalSpacing = 6.dp) {
                SectionTitle("Ćwiczenia w twoim planie")
                val exercises = MuscleMap.exercisesTraining(rank.muscle).mapNotNull { names[it] }
                Text(
                    if (exercises.isEmpty()) "Brak ćwiczeń na tę partię w planie." else exercises.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GtColors.TextSoft,
                )
            }
            Text(
                "Seria z planu: +${MuscleScoring.PRIMARY_SET} pkt (partia główna) lub +${MuscleScoring.SECONDARY_SET} (pomocnicza). " +
                    "Rekord osobisty: +${MuscleScoring.RECORD_BONUS}. Maks. ${MuscleScoring.SESSION_CAP} pkt na trening — " +
                    "rangę buduje regularność, nie jeden ciężki dzień.",
                style = MaterialTheme.typography.bodySmall,
                color = GtColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun LadderTab(level: LevelProgress) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "9 rang, każda z dywizjami I–III (Legenda bez dywizji). Ranga konta rośnie z poziomem, " +
                    "rangi mięśni — z punktami za serie.",
                style = MaterialTheme.typography.bodySmall,
                color = GtColors.TextSecondary,
            )
        }
        items(RankTier.entries.toList()) { tier ->
            val rank = Rank.of(tier, 1)
            val reached = level.rank.tier.ordinal >= tier.ordinal
            val current = level.rank.tier == tier
            val palette = tier.palette()
            Column(
                Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(if (current) palette.container else GtColors.Surface)
                    .border(1.dp, if (current) palette.border else GtColors.Divider, MaterialTheme.shapes.large)
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                RankEmblem(rank = if (reached) rank else null, size = 76.dp, showDivision = false)
                Text(
                    tier.label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (reached) palette.text else GtColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "LVL ${firstLevelFor(rank)}+ · ${MuscleRankCurve.totalFor(rank.index)} pkt",
                    style = MaterialTheme.typography.labelSmall,
                    color = GtColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
