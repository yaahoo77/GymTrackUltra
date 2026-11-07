package com.gymtrack.ultra.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.model.PlanDay
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.ScreenHeader
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.components.SkeletonBlock
import com.gymtrack.ultra.ui.theme.GtColors

@Composable
fun WorkoutPlanScreen(
    onOpenDay: (Long) -> Unit,
    viewModel: WorkoutPlanViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                eyebrow = "Workout",
                title = "Plan FBW",
                subtitle = "Full Body · ${state.days.size} dni w tygodniu",
            )
        }
        if (state.isLoading) {
            items(3) { SkeletonBlock(height = 150.dp) }
        } else {
            items(state.days, key = { it.id }) { day ->
                PlanDayCard(day = day, isToday = day.dayOfWeek == state.today, onClick = { onOpenDay(day.id) })
            }
            item(key = "recovery") { RecoveryCard() }
        }
    }
}

@Composable
private fun PlanDayCard(day: PlanDay, isToday: Boolean, onClick: () -> Unit) {
    GtCard(
        highlighted = isToday,
        onClick = onClick,
        onClickLabel = "Otwórz ${day.title}",
        verticalSpacing = 12.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DayCodeBadge(day.code, highlighted = isToday)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(day.title, style = MaterialTheme.typography.headlineSmall, color = GtColors.TextPrimary)
                Text(
                    text = (if (isToday) "Dziś · " else "") + Formatters.capitalize(Formatters.dayName(day.dayOfWeek)),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isToday) GtColors.Lime else GtColors.TextSecondary,
                )
            }
            Icon(GtIcons.ChevronRight, contentDescription = null, tint = GtColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        Text(
            text = day.exercises.joinToString(" · ") { it.exercise.namePl },
            style = MaterialTheme.typography.bodyMedium,
            color = GtColors.TextSoft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoChip(Formatters.countLabel(day.exercises.size, "ćwiczenie", "ćwiczenia", "ćwiczeń"))
            InfoChip(Formatters.countLabel(day.totalSets, "seria", "serie", "serii"))
            InfoChip("~${day.estimatedMinutes} min")
        }
    }
}

@Composable
internal fun DayCodeBadge(code: String, highlighted: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(if (highlighted) GtColors.Lime else GtColors.Elevated)
            .border(1.dp, if (highlighted) GtColors.Lime else GtColors.Outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            code,
            style = MaterialTheme.typography.headlineSmall,
            color = if (highlighted) GtColors.OnLime else GtColors.TextPrimary,
        )
    }
}

@Composable
internal fun InfoChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = GtColors.TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GtColors.Elevated)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun RecoveryCard() {
    GtCard(verticalSpacing = 8.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(GtIcons.Moon, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(20.dp))
            SectionLabel("Dni regeneracji", color = GtColors.Lime)
        }
        Text(
            "Między treningami mięśnie się odbudowują i właśnie wtedy rośnie siła. " +
                "Nie musisz trenować codziennie — 3 dobre sesje w tygodniu to świetny plan.",
            style = MaterialTheme.typography.bodyMedium,
            color = GtColors.TextSoft,
        )
        Text(
            "Ból ≠ postęp. Ostry ból w trakcie ćwiczenia = przerwij serię.",
            style = MaterialTheme.typography.bodySmall,
            color = GtColors.TextSecondary,
        )
    }
}
