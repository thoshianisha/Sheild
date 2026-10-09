package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.RiskStatus
import com.example.data.ScanReportEntity
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SuspiciousAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultCard(
    report: ScanReportEntity,
    isAlertPlaying: Boolean,
    onStopAlert: () -> Unit,
    onReplayAlert: () -> Unit,
    onAskShieldAi: () -> Unit,
    onNewScan: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val status = report.statusEnum
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            RiskStatus.SAFE -> SafeGreen
            RiskStatus.SUSPICIOUS -> SuspiciousAmber
            RiskStatus.HIGH_RISK -> HighRiskRed
        },
        animationSpec = tween(450),
        label = "status_color"
    )

    val formattedTime = SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US)
        .format(Date(report.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        statusColor.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                    )
                )
            )
            .border(1.5.dp, statusColor.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
            .padding(20.dp)
            .testTag("scan_result_card")
    ) {
        // DEMO Label if applicable
        if (report.isDemo) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(50))
                    .background(SuspiciousAmber.copy(alpha = 0.22f))
                    .border(1.dp, SuspiciousAmber, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "DEMO SIMULATION RESULT — NOT A LIVE INVESTIGATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = SuspiciousAmber,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Animated Shield + Status Banner
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            PulsingStatusShield(status = status, size = 82.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = status.bannerTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("result_status_banner")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${report.scannerEnum.title} • $formattedTime",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stop Buzzer / Replay Alert Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isAlertPlaying || status == RiskStatus.HIGH_RISK) {
                Button(
                    onClick = onStopAlert,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HighRiskRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("stop_alert_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = "Stop Sound / Dismiss Alert",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAlertPlaying) "Stop Sound / Dismiss Alert" else "Alert Silenced",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            OutlinedButton(
                onClick = onReplayAlert,
                modifier = Modifier
                    .weight(0.75f)
                    .testTag("replay_alert_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Replay Audio Alert",
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Replay Alert",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Circular Risk Score + Summary Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
                .padding(14.dp)
        ) {
            AnimatedCircularScoreMeter(
                score = report.riskScore,
                label = "RISK SCORE",
                subLabel = "0–100 Scale",
                isSecurityScore = false,
                size = 118.dp
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.threatCategory,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.parsedSubject,
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = report.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Submitted Content Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .padding(12.dp)
        ) {
            Text(
                text = "SUBMITTED CONTENT / FILE",
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = report.submittedContent,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Flagged Signals / Suspicious Phrases
        if (report.suspiciousPhrasesList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "DETECTED SIGNALS & INDICATORS",
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                report.suspiciousPhrasesList.forEach { token ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.16f))
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = token,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor
                        )
                    }
                }
            }
        }

        // Evidence & Detection Reasons
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "EVIDENCE & DETECTION REASONS",
            style = MaterialTheme.typography.labelMedium,
            color = ElectricCyan
        )
        Spacer(modifier = Modifier.height(8.dp))
        report.evidenceList.forEach { reason ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Smart Recommendations
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "RECOMMENDED SAFETY ACTIONS",
            style = MaterialTheme.typography.labelMedium,
            color = SafeGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        report.recommendationsList.forEach { rec ->
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = rec,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Detection Method & Confidence / Uncertainty Limitations
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Method: ${report.detectionMethod}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ElectricCyan
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = report.confidenceNote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons: Ask Shield AI + Scan Another
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAskShieldAi,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonPurple,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ask_shield_ai_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Ask Shield AI",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ask Shield AI", style = MaterialTheme.typography.labelLarge)
            }

            if (onNewScan != null) {
                OutlinedButton(
                    onClick = onNewScan,
                    modifier = Modifier
                        .weight(0.85f)
                        .testTag("scan_another_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Another",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Scan", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
