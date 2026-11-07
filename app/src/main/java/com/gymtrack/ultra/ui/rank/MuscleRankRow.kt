package com.gymtrack.ultra.ui.rank

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtrack.ultra.domain.progression.MuscleRank
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.GtIcons
import com.gymtrack.ultra.ui.theme.GtColors

/** Wiersz partii mięśni w stylu "karta rangi": kolor karty = kolor rangi. */
@Composable
fun MuscleRankRow(
    rank: MuscleRank,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ranked = rank.isRanked
    val palette = if (ranked) rank.progress.rank.tier.palette() else UnrankedPalette
    val shape = MaterialTheme.shapes.large
    val label = if (ranked) rank.progress.rank.label else "BEZ RANGI"
    val detail = when {
        !ranked -> "Zalicz serię, żeby zdobyć rangę"
        rank.progress.isMax -> "${rank.progress.totalPoints} pkt"
        else -> "${rank.progress.pointsIntoRank}/${rank.progress.pointsForNext} pkt"
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (ranked) palette.container else GtColors.Surface)
            .border(1.dp, if (ranked) palette.border else GtColors.Divider, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${rank.muscle.label}: $label, $detail" }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RankEmblem(rank = if (ranked) rank.progress.rank else null, size = 52.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(rank.muscle.label, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp), color = palette.text)
                Text("  ·  $detail", style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
            }
            if (ranked && !rank.progress.isMax) {
                GradientProgressBar(
                    progress = rank.progress.fraction,
                    height = 4.dp,
                    color = palette.text,
                    trackColor = GtColors.Background.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Icon(GtIcons.ChevronRight, contentDescription = null, tint = GtColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}
