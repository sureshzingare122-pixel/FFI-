package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CrosshairStyle
import com.example.ui.components.HudSecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.TacticalCard
import com.example.ui.components.drawTacticalCrosshair
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.ElevatedDarkGrey
import com.example.ui.theme.MatteBlack
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SettingsViewModel
import com.example.util.FeedbackEngine
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    feedbackEngine: FeedbackEngine,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.preferences
    val view = LocalView.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MatteBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "SIMULATOR SETTINGS",
            subtitle = "Local DataStore preferences & offline security posture"
        )

        state.statusBanner?.let { msg ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElevatedDarkGrey,
                border = BorderStroke(1.dp, TacticalGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TacticalGreen,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Tactile & Audio Feedback
        TacticalCard {
            Text(
                text = "FEEDBACK & HUD OVERLAYS",
                style = MaterialTheme.typography.titleMedium,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            SettingsSwitchRow(
                title = "Haptic Feedback",
                subtitle = "Vibrate subtly on target hits and HUD actions",
                checked = prefs.hapticFeedbackEnabled,
                testTag = "switch_haptics",
                onCheckedChange = {
                    viewModel.setHapticFeedback(it)
                    if (it) feedbackEngine.onActionTap(view, true, prefs.soundEffectsEnabled)
                }
            )
            SettingsSwitchRow(
                title = "Sound Effects",
                subtitle = "Synthesized offline acoustic hit/miss cues",
                checked = prefs.soundEffectsEnabled,
                testTag = "switch_sound",
                onCheckedChange = {
                    viewModel.setSoundEffects(it)
                    if (it) feedbackEngine.onActionTap(view, prefs.hapticFeedbackEnabled, true)
                }
            )
            SettingsSwitchRow(
                title = "Arena Tactical Grid",
                subtitle = "Show range rings and metric coordinates on practice canvas",
                checked = prefs.showHudGrid,
                testTag = "switch_hud_grid",
                onCheckedChange = { viewModel.setShowHudGrid(it) }
            )
        }

        // Crosshair Style + Preview
        TacticalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CROSSHAIR RETICLE",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonOrange
                    )
                    Text(
                        text = "Applies across Practice Arena & Aim Trainer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                Canvas(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MatteBlack, RoundedCornerShape(10.dp))
                ) {
                    drawTacticalCrosshair(
                        center = Offset(size.width / 2f, size.height / 2f),
                        style = prefs.crosshairStyle
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CrosshairStyle.entries.forEach { style ->
                    val selected = prefs.crosshairStyle == style
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setCrosshairStyle(style) },
                        label = { Text(style.displayName, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrange,
                            selectedLabelColor = MatteBlack,
                            containerColor = ElevatedDarkGrey,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("crosshair_style_${style.name.lowercase()}")
                    )
                }
            }
        }

        // Practice Session Timer & Active Profile
        TacticalCard {
            Text(
                text = "PRACTICE DURATION",
                style = MaterialTheme.typography.titleMedium,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 45, 60).forEach { secs ->
                    val selected = prefs.practiceDurationSeconds == secs
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setPracticeDuration(secs) },
                        label = { Text("${secs}s", style = MaterialTheme.typography.labelLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonOrange,
                            selectedLabelColor = MatteBlack,
                            containerColor = ElevatedDarkGrey,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("duration_chip_${secs}s")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ACTIVE MOVEMENT PROFILE",
                style = MaterialTheme.typography.titleMedium,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("RUSH", "DEFENSIVE", "SNIPER").forEach { pid ->
                    val selected = prefs.activeMovementProfileId == pid
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setActiveProfile(pid) },
                        label = { Text(pid, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CrimsonRed,
                            selectedLabelColor = TextPrimary,
                            containerColor = ElevatedDarkGrey,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Current Sensitivity Preset: ${state.sensitivitySettings.activePreset}  " +
                    "(Aim ${String.format(Locale.US, "%.2fx", state.sensitivitySettings.crosshairSensitivity)} / " +
                    "Stick ${String.format(Locale.US, "%.2fx", state.sensitivitySettings.joystickResponse)})",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }

        // Offline Privacy & Security Declaration
        TacticalCard(borderColor = TacticalGreen.copy(alpha = 0.6f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = TacticalGreen
                )
                Text(
                    text = "PRIVACY & SECURITY DECLARATION",
                    style = MaterialTheme.typography.titleMedium,
                    color = TacticalGreen
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.privacy_declaration),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = NeonOrange,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "v1.0.0  •  com.ffitrainerpro.app  •  0 Network Permissions",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }

        HudSecondaryButton(
            text = "RESET SIMULATOR DEFAULTS",
            icon = Icons.Filled.Restore,
            borderColor = TacticalBorder,
            contentColor = TextPrimary,
            onClick = { viewModel.resetAllPreferencesAndProfiles() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_defaults_button")
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MatteBlack,
                checkedTrackColor = NeonOrange,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ElevatedDarkGrey
            )
        )
    }
}
