package com.gymtrack.ultra.features.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtrack.ultra.ui.components.shine
import com.gymtrack.ultra.BuildConfig
import com.gymtrack.ultra.core.format.Formatters
import com.gymtrack.ultra.domain.progression.LevelProgress
import com.gymtrack.ultra.ui.components.GradientProgressBar
import com.gymtrack.ultra.ui.components.GtCard
import com.gymtrack.ultra.ui.components.ScreenHeader
import com.gymtrack.ultra.ui.components.SectionLabel
import com.gymtrack.ultra.ui.rank.RankEmblem
import com.gymtrack.ultra.ui.rank.palette
import com.gymtrack.ultra.ui.theme.GtColors
import com.gymtrack.ultra.ui.theme.rememberSystemReducedMotion

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val systemReduced = rememberSystemReducedMotion()
    val settings = state.settings

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            ScreenHeader(
                title = settings.displayName.ifBlank { "Profil" },
                subtitle = "${settings.goal.label} · ${settings.experience.label}",
            )
        }
        item(key = "rank") { RankCard(state.level, state.completedWorkouts) }
        item(key = "days") {
            GtCard(verticalSpacing = 6.dp) {
                SectionLabel("Dni treningowe")
                Text(
                    settings.trainingDays.sorted().joinToString(" · ") { Formatters.shortDay(it) },
                    style = MaterialTheme.typography.titleLarge,
                    color = GtColors.TextPrimary,
                )
                Text(
                    "Pozostałe dni to regeneracja. Brak treningu nigdy nie odbiera XP.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
        }
        item(key = "settings") {
            GtCard(verticalSpacing = 0.dp) {
                SectionLabel("Wrażenia")
                SettingSwitch(
                    title = "Ogranicz animacje",
                    description = if (systemReduced) {
                        "Animacje są wyłączone w ustawieniach Androida — respektujemy to."
                    } else {
                        "Tylko krótkie przenikania, bez drobinek i konfetti."
                    },
                    checked = settings.reduceMotion || systemReduced,
                    enabled = !systemReduced,
                    onCheckedChange = viewModel::setReduceMotion,
                )
                HorizontalDivider(color = GtColors.Divider)
                SettingSwitch(
                    title = "Wibracje",
                    description = "Krótka haptyka po zaliczonej serii i końcu przerwy.",
                    checked = settings.hapticsEnabled,
                    onCheckedChange = viewModel::setHaptics,
                )
                HorizontalDivider(color = GtColors.Divider)
                SettingSwitch(
                    title = "Efekty celebracji",
                    description = "Konfetti i drobinki po treningu i awansie.",
                    checked = settings.celebrationsEnabled,
                    onCheckedChange = viewModel::setCelebrations,
                )
                HorizontalDivider(color = GtColors.Divider)
                SettingSwitch(
                    title = "Przypomnienie o rozgrzewce",
                    description = "Podpowiedź na karcie treningu.",
                    checked = settings.warmupReminder,
                    onCheckedChange = viewModel::setWarmupReminder,
                )
            }
        }
        item(key = "privacy") {
            GtCard(verticalSpacing = 6.dp) {
                SectionLabel("Twoje dane")
                Text(
                    "Wszystko zostaje na tym telefonie. Bez konta, bez reklam, bez śledzenia. " +
                        "Nie pytamy o wagę, wiek ani zdjęcia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = GtColors.TextSoft,
                )
                Text(
                    "Wersja ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextMuted,
                )
            }
        }
    }
}

@Composable
private fun RankCard(level: LevelProgress, completedWorkouts: Int) {
    val palette = level.rank.tier.palette()
    GtCard(tint = palette.text, verticalSpacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RankEmblem(
                rank = level.rank,
                size = 84.dp,
                contentDescription = "Ranga ${level.rank.label}",
                modifier = Modifier.shine(periodMillis = 5200),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(level.rank.label, style = MaterialTheme.typography.titleMedium, color = palette.text)
                Text("LVL ${level.level}", style = MaterialTheme.typography.displaySmall, color = GtColors.TextPrimary)
                Text(
                    Formatters.countLabel(completedWorkouts, "trening", "treningi", "treningów"),
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row {
                Text(
                    if (level.isMaxLevel) "Maksymalny poziom" else "Do LVL ${level.level + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = GtColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
                if (!level.isMaxLevel) {
                    Text(
                        "${level.xpIntoLevel} / ${level.xpForNextLevel} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = GtColors.TextPrimary,
                    )
                }
            }
            GradientProgressBar(progress = level.fraction, color = palette.text)
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = GtColors.TextPrimary)
            Text(description, style = MaterialTheme.typography.bodySmall, color = GtColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GtColors.OnLime,
                checkedTrackColor = GtColors.Lime,
                checkedBorderColor = GtColors.Lime,
                uncheckedThumbColor = GtColors.TextSecondary,
                uncheckedTrackColor = GtColors.Elevated,
                uncheckedBorderColor = GtColors.OutlineStrong,
            ),
        )
    }
}
