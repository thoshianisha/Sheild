package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserSettings
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SuspiciousAmber

@Composable
fun ProfileAndSettingsScreen(
    settings: UserSettings,
    totalSavedReports: Int,
    onSetDarkMode: (Boolean) -> Unit,
    onSetAlertSound: (Boolean) -> Unit,
    onSetVoiceFeedback: (Boolean) -> Unit,
    onSetVibration: (Boolean) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onSetSaveHistory: (Boolean) -> Unit,
    onSetDemoDashboard: (Boolean) -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onClearAllHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingProfile by rememberSaveable { mutableStateOf(false) }
    var nameInput by rememberSaveable(settings.userName) { mutableStateOf(settings.userName) }
    var roleInput by rememberSaveable(settings.userRole) { mutableStateOf(settings.userRole) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("profile_settings_screen")
    ) {
        // 1. User Profile Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                ElectricBlue.copy(alpha = 0.25f),
                                NeonPurple.copy(alpha = 0.25f)
                            )
                        )
                    )
                    .border(1.dp, ElectricCyan.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = 0.2f))
                            .border(2.dp, ElectricCyan, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile Avatar",
                            tint = ElectricCyan,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = settings.userName,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = settings.userRole,
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalSavedReports Saved Local Scan Report(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { editingProfile = !editingProfile },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Text(if (editingProfile) "Close" else "Edit")
                    }
                }

                if (editingProfile) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = roleInput,
                        onValueChange = { roleInput = it },
                        label = { Text("Security Profile / Role") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onUpdateProfile(nameInput, roleInput)
                            editingProfile = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Color(0xFF04121C)
                        )
                    ) {
                        Text("Save Profile")
                    }
                }
            }
        }

        // 2. App Theme & Audio/Buzzer Alert Settings
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Theme & Security Buzzer Preferences",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All preferences persist automatically across app restarts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                SettingToggleItem(
                    icon = Icons.Default.DarkMode,
                    title = "Premium Dark Mode",
                    subtitle = "Toggle between futuristic Midnight Navy dark theme and high-contrast light mode",
                    checked = settings.isDarkMode,
                    onCheckedChange = onSetDarkMode,
                    accent = ElectricCyan,
                    testTag = "toggle_dark_mode"
                )
                SettingToggleItem(
                    icon = Icons.Default.VolumeUp,
                    title = "Security Buzzer & Alert Tones",
                    subtitle = "Play audible warning buzzer on HIGH RISK threats and soft tones on SAFE scans",
                    checked = settings.alertSoundEnabled,
                    onCheckedChange = onSetAlertSound,
                    accent = HighRiskRed,
                    testTag = "toggle_alert_sound"
                )
                SettingToggleItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Voice Feedback (Text-to-Speech)",
                    subtitle = "Announce scan safety verdicts aloud ('This content appears safe' or 'Alert. High-risk threat detected')",
                    checked = settings.voiceFeedbackEnabled,
                    onCheckedChange = onSetVoiceFeedback,
                    accent = NeonPurple,
                    testTag = "toggle_voice_feedback"
                )
                SettingToggleItem(
                    icon = Icons.Default.Vibration,
                    title = "Tactile Haptic & Vibration Alerts",
                    subtitle = "Trigger distinct vibration pulses when suspicious or high-risk content is detected",
                    checked = settings.vibrationEnabled,
                    onCheckedChange = onSetVibration,
                    accent = SuspiciousAmber,
                    testTag = "toggle_vibration"
                )
                SettingToggleItem(
                    icon = Icons.Default.Notifications,
                    title = "In-App Alert Notifications",
                    subtitle = "Log threat alerts in the Alert Center badge counter",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = onSetNotifications,
                    accent = SafeGreen,
                    testTag = "toggle_notifications"
                )
            }
        }

        // 3. Scan History & Privacy Controls
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Privacy & Scan History Management",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                SettingToggleItem(
                    icon = Icons.Default.History,
                    title = "Save Local Scan History",
                    subtitle = "Store completed scan reports locally in encrypted-ready SQLite/Room database",
                    checked = settings.saveScanHistory,
                    onCheckedChange = onSetSaveHistory,
                    accent = ElectricCyan,
                    testTag = "toggle_save_history"
                )

                SettingToggleItem(
                    icon = Icons.Default.Science,
                    title = "Hackathon Demo Telemetry Mode",
                    subtitle = "Display clearly labelled sample metrics on Home Dashboard when no live scans exist",
                    checked = settings.demoModeDashboardEnabled,
                    onCheckedChange = onSetDemoDashboard,
                    accent = SuspiciousAmber,
                    testTag = "toggle_demo_mode"
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onClearAllHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = HighRiskRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete All Local Scan History ($totalSavedReports)",
                        color = HighRiskRed,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        // 4. Privacy & Ethical Security Guarantee
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PrivacyTip,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Privacy & Security Transparency",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Zero Credential Harvesting: ScamShield AI never asks for real OTP values, PINs, or passwords.\n" +
                        "• Safe Static Inspection: Submitted URLs are never automatically opened, and selected APK files are never executed.\n" +
                        "• Remote AI Transparency: Data is sent to the Gemini API only when you actively query Shield AI or run multimodal image checks with a configured API key.\n" +
                        "• Honest Uncertainty: Automated detection can produce false positives and false negatives; risk scores (0–100) are heuristic indicators, never a 100% guarantee.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. About ScamShield AI
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About ScamShield AI v1.0",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ScamShield AI is an AI-Powered Digital Risk Protection & Personal Cyber Safety Platform built to help everyday users identify phishing links, social impersonation, OTP theft, predatory loan scams, risky APKs, suspicious documents, and manipulated media.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accent: Color,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.18f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
