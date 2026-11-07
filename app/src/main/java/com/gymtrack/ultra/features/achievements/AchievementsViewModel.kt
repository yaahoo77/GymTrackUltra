package com.gymtrack.ultra.features.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtrack.ultra.domain.progression.AchievementProgress
import com.gymtrack.ultra.domain.repository.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementsUiState(
    val isLoading: Boolean = true,
    /** Odblokowane najpierw, potem najbliższe ukończenia. */
    val items: List<AchievementProgress> = emptyList(),
) {
    val unlockedCount: Int get() = items.count { it.isUnlocked }
    val total: Int get() = items.size
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    progressRepository: ProgressRepository,
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> = progressRepository.observeProgress()
        .map { progress ->
            AchievementsUiState(
                isLoading = false,
                items = progress.achievements.sortedWith(
                    compareByDescending<AchievementProgress> { it.isUnlocked }
                        .thenByDescending { if (it.isUnlocked) 0f else it.fraction },
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AchievementsUiState())
}
