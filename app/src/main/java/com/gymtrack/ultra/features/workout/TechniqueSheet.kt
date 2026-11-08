package com.gymtrack.ultra.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gymtrack.ultra.domain.model.Exercise
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.exercise.ExerciseDemo
import com.gymtrack.ultra.ui.exercise.ExerciseMotions
import com.gymtrack.ultra.ui.theme.GtColors

/** Karta techniki: animacja ruchu, kroki i bezpieczeństwo. Otwierana z planu i z treningu na żywo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechniqueSheet(
    exercise: Exercise,
    subtitle: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GtColors.Surface,
        contentColor = GtColors.TextPrimary,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GtColors.OutlineStrong),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel(subtitle, color = GtColors.Lime)
                Text(
                    exercise.namePl,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    "${exercise.nameEn} · ${exercise.muscleGroup}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GtColors.TextSecondary,
                )
            }

            if (ExerciseMotions.forId(exercise.id) != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(270.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(GtColors.Background)
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
                ) {
                    ExerciseDemo(exercise.id, Modifier.fillMaxSize())
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("Technika")
                if (exercise.technique.isEmpty()) {
                    Text(
                        "To Twoje własne ćwiczenie. Zapisz wskazówki w notatce podczas treningu — zobaczysz je następnym razem.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GtColors.TextSecondary,
                    )
                }
                exercise.technique.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(GtColors.Elevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", style = MaterialTheme.typography.titleSmall, color = GtColors.Lime)
                        }
                        Text(step, style = MaterialTheme.typography.bodyLarge, color = GtColors.TextPrimary, modifier = Modifier.weight(1f))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(GtColors.Elevated)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(GtIcons.Shield, contentDescription = null, tint = GtColors.Lime, modifier = Modifier.size(18.dp))
                    SectionLabel("Bezpieczeństwo", color = GtColors.Lime)
                }
                exercise.safety.forEach { tip ->
                    Text("•  $tip", style = MaterialTheme.typography.bodyMedium, color = GtColors.TextPrimary)
                }
                Text(
                    "Trenuj w zakresie powtórzeń, nie do upadku. Jeśli coś boli — przerwij i zapytaj trenera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSoft,
                )
            }
        }
    }
}
