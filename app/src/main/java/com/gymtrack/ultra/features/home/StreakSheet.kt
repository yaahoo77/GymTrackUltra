package com.gymtrack.ultra.features.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.progression.StreakCalculator
import com.gymtrack.ultra.domain.progression.StreakState
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.components.StatTile
import com.gymtrack.ultra.ui.components.StreakFlame
import com.gymtrack.ultra.ui.theme.GtColors

/** Szczegóły serii: płomień, rekord, tarcze i zasady (bez straszenia). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakSheet(
    streak: StreakState,
    week: List<WeekDayUi>,
    onDismiss: () -> Unit,
) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StreakFlame(lit = streak.isLit, size = 96.dp)
            Text(
                streak.current.toString(),
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 52.sp),
                color = if (streak.isLit) GtColors.Fire else GtColors.TextMuted,
            )
            Text(
                if (streak.isLit) "treningów w serii" else "Zrób trening, żeby rozpalić ogień",
                style = MaterialTheme.typography.bodyLarge,
                color = GtColors.TextSecondary,
            )
            WeekRow(week)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(streak.longest.toString(), "Najdłuższa", Modifier.weight(1f))
                StatTile("${streak.thisWeekCount}/${streak.weeklyMinimum}", "Ten tydzień", Modifier.weight(1f))
                StatTile(streak.shields.toString(), "Tarcze", Modifier.weight(1f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Rule(GtIcons.Flame, "Seria liczy treningi. Wystarczy ${Formatters.countLabel(streak.weeklyMinimum, "trening", "treningi", "treningów")} w tygodniu.")
                Rule(GtIcons.Moon, "Dni wolne nigdy nie przerywają serii — regeneracja to część planu.")
                Rule(
                    GtIcons.Shield,
                    "Za każde ${StreakCalculator.WEEKS_PER_SHIELD} pełne tygodnie dostajesz tarczę (maks. ${StreakCalculator.MAX_SHIELDS}). " +
                        "Ochroni serię, gdy zachorujesz albo wyjedziesz.",
                )
            }
        }
    }
}

@Composable
private fun Rule(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, contentDescription = null, tint = GtColors.TextSecondary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = GtColors.TextSoft, textAlign = TextAlign.Start)
    }
}
