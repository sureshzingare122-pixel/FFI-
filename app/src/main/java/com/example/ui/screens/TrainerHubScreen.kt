package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.ObsidianPanel
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AimReactionViewModel
import com.example.ui.viewmodel.MovementTrainerViewModel
import com.example.ui.viewmodel.SensitivityCalibratorViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.util.FeedbackEngine

enum class TrainerSubTab(val label: String) {
    MOVEMENT("MOVEMENT"),
    AIM_REACTION("AIM & REACT"),
    CALIBRATE("CALIBRATE")
}

@Composable
fun TrainerHubScreen(
    movementViewModel: MovementTrainerViewModel,
    aimReactionViewModel: AimReactionViewModel,
    calibratorViewModel: SensitivityCalibratorViewModel,
    settingsViewModel: SettingsViewModel,
    feedbackEngine: FeedbackEngine,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by rememberSaveable { mutableStateOf(TrainerSubTab.MOVEMENT) }
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("trainer_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Segmented Sub-Tab Switcher
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ObsidianPanel,
            border = androidx.compose.foundation.BorderStroke(1.dp, TacticalBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TrainerSubTab.entries.forEach { tab ->
                    val active = selectedSubTab == tab
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (active) NeonOrange else ObsidianPanel,
                        modifier = Modifier.weight(1f)
                    ) {
                        TextButton(
                            onClick = { selectedSubTab = tab },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trainer_subtab_${tab.name.lowercase()}")
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MatteBlack else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Active Sub-Module
        when (selectedSubTab) {
            TrainerSubTab.MOVEMENT -> MovementTrainerSection(
                viewModel = movementViewModel,
                feedbackEngine = feedbackEngine,
                hapticEnabled = settingsState.preferences.hapticFeedbackEnabled,
                soundEnabled = settingsState.preferences.soundEffectsEnabled
            )
            TrainerSubTab.AIM_REACTION -> AimReactionSection(
                viewModel = aimReactionViewModel,
                feedbackEngine = feedbackEngine
            )
            TrainerSubTab.CALIBRATE -> SensitivityCalibratorSection(
                viewModel = calibratorViewModel,
                feedbackEngine = feedbackEngine,
                hapticEnabled = settingsState.preferences.hapticFeedbackEnabled,
                soundEnabled = settingsState.preferences.soundEffectsEnabled
            )
        }
    }
}
