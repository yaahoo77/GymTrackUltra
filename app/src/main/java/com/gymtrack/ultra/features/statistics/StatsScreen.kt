package com.gymtrack.ultra.features.statistics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.ui.components.EmptyState
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.ScreenHeader
import com.gymtrack.ultra.ui.components.StatTile
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.GtDurations
import com.gymtrack.ultra.ui.theme.LocalGtMotion

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { ScreenHeader(title = "Statystyki") }
        item(key = "range") { RangeSelector(state.range, onSelect = viewModel::selectRange) }
        item(key = "tiles") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(state.workouts.toString(), "Treningi", Modifier.weight(1f))
                StatTile(Formatters.durationMinutes(state.totalMinutes), "Czas", Modifier.weight(1f))
            }
        }
        item(key = "tiles2") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(state.completedSets.toString(), "Serie", Modifier.weight(1f))
                StatTile(
                    if (state.workouts == 0) "—" else Formatters.durationMinutes(state.averageMinutes),
                    "Śr. trening",
                    Modifier.weight(1f),
                )
            }
        }
        if (!state.isLoading && state.workouts == 0) {
            item(key = "empty") {
                GtCard {
                    EmptyState(
                        title = if (state.hasAnyHistory) "Pusto w tym zakresie" else "Laboratorium czeka na dane",
                        message = if (state.hasAnyHistory) {
                            "Wybierz dłuższy okres, żeby zobaczyć wcześniejsze treningi."
                        } else {
                            "Zrób pierwszy trening — wykresy ciężaru i kalendarz pojawią się tu same."
                        },
                        icon = GtIcons.Stats,
                    )
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(selected: StatsRange, onSelect: (StatsRange) -> Unit) {
    val reduced = LocalGtMotion.current.reduced
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(GtColors.Surface)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatsRange.entries.forEach { range ->
            val isSelected = range == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) GtColors.Lime else GtColors.Surface,
                animationSpec = tween(if (reduced) 0 else GtDurations.Fast),
                label = "rangeBg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(background)
                    .clickable { onSelect(range) }
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    range.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) GtColors.OnLime else GtColors.TextSecondary,
                )
            }
        }
    }
}
