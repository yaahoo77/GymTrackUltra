package com.gymtrack.ultra.features.achievements

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.progression.AchievementProgress
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.HexBadge
import com.gymtrack.ultra.ui.components.ScreenHeader
import com.gymtrack.ultra.ui.components.achievementGlyph
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.LocalGtMotion
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                ScreenHeader(
                    eyebrow = "Achievements",
                    title = "Kolekcja",
                    trailing = {
                        Text(
                            "${state.unlockedCount} / ${state.total}",
                            style = MaterialTheme.typography.titleMedium,
                            color = GtColors.Lime,
                        )
                    },
                )
                GradientProgressBar(
                    progress = if (state.total == 0) 0f else state.unlockedCount.toFloat() / state.total,
                    height = 6.dp,
                )
            }
        }
        itemsIndexed(state.items, key = { _, item -> item.definition.code }) { index, item ->
            AchievementCard(item = item, index = index)
        }
    }
}

@Composable
private fun AchievementCard(item: AchievementProgress, index: Int) {
    val motion = LocalGtMotion.current
    // Wejście kart: krótki, stopniowany spring (wyłączony przy ograniczonym ruchu).
    val appear = remember(item.definition.code) { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(item.definition.code) {
        if (!motion.reduced) {
            delay(index * 35L)
            appear.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))
        }
    }

    val unlocked = item.isUnlocked
    val shape = MaterialTheme.shapes.medium
    val stateText = when {
        unlocked && item.unlockedAt != null -> "odblokowane ${formatDate(item.unlockedAt)}"
        unlocked -> "odblokowane"
        else -> "${item.current} / ${item.target}"
    }

    Column(
        modifier = Modifier
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 24f
                scaleX = 0.96f + appear.value * 0.04f
                scaleY = 0.96f + appear.value * 0.04f
            }
            .clip(shape)
            .then(
                if (unlocked) {
                    Modifier
                        .background(Brush.linearGradient(listOf(GtColors.Elevated, GtColors.Surface)))
                        .border(1.dp, GtColors.Lime.copy(alpha = 0.3f), shape)
                } else {
                    Modifier
                        .background(GtColors.Surface.copy(alpha = 0.6f))
                        .border(1.dp, GtColors.Elevated, shape)
                },
            )
            .padding(14.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${item.definition.title}. ${item.definition.description}. $stateText"
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HexBadge(
            fill = if (unlocked) GtColors.LimeDim else GtColors.Surface,
            stroke = if (unlocked) GtColors.Lime else GtColors.OutlineStrong,
            glyph = achievementGlyph(item.definition.code),
            size = 40.dp,
        )
        Text(
            item.definition.title,
            style = MaterialTheme.typography.titleSmall,
            color = if (unlocked) GtColors.TextPrimary else GtColors.TextSoft,
        )
        Text(item.definition.description, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        if (item.showsProgressBar) {
            GradientProgressBar(progress = item.fraction, height = 5.dp)
        }
        Text(
            stateText,
            style = MaterialTheme.typography.bodySmall,
            color = if (unlocked) GtColors.Success else GtColors.TextSecondary,
        )
    }
}

private fun formatDate(epochMillis: Long): String =
    Formatters.dayMonth(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
