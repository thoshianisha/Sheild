package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.RiskStatus
import com.example.data.ScanReportEntity
import com.example.data.ScannerType
import com.example.data.UserSettings
import com.example.ui.components.AnimatedCircularScoreMeter
import com.example.ui.components.AnimatedThreatRadarChart
import com.example.ui.theme.CyberIndigo
import com.example.ui.theme.CyberPink
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SuspiciousAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun scannerIconAndColors(type: ScannerType): Triple<ImageVector, Color, Color> {
    return when (type) {
        ScannerType.URL_LINK -> Triple(Icons.Default.Link, ElectricCyan, ElectricBlue)
        ScannerType.SOCIAL_PROFILE -> Triple(Icons.Default.PersonSearch, NeonPurple, CyberPink)
        ScannerType.OTP_MESSAGE -> Triple(Icons.Default.Sms, CyberTeal, ElectricCyan)
        ScannerType.BANK_LOAN -> Triple(Icons.Default.AccountBalance, SuspiciousAmber, CyberPink)
        ScannerType.IMAGE_AUTH -> Triple(Icons.Default.Image, CyberIndigo, ElectricCyan)
        ScannerType.VIDEO_DEEPFAKE -> Triple(Icons.Default.Videocam, CyberPink, NeonPurple)
        ScannerType.APP_APK -> Triple(Icons.Default.Android, SafeGreen, CyberTeal)
        ScannerType.PDF_DOC -> Triple(Icons.Default.Description, ElectricBlue, NeonPurple)
    }
}

@Composable
fun HomeDashboardScreen(
    reports: List<ScanReportEntity>,
    settings: UserSettings,
    onToggleDemoDashboard: (Boolean) -> Unit,
    onOpenScanner: (ScannerType) -> Unit,
    onInspectReport: (ScanReportEntity) -> Unit,
    onOpenShieldAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter or synthesize clearly-labelled demo metrics when Demo Mode is toggled on and no real scans exist
    val useDemoSampleStats = settings.demoModeDashboardEnabled
    val totalScans = if (useDemoSampleStats && reports.isEmpty()) 14 else reports.size
    val safeCount = if (useDemoSampleStats && reports.isEmpty()) 8 else reports.count { it.statusEnum == RiskStatus.SAFE }
    val suspiciousCount = if (useDemoSampleStats && reports.isEmpty()) 4 else reports.count { it.statusEnum == RiskStatus.SUSPICIOUS }
    val highRiskCount = if (useDemoSampleStats && reports.isEmpty()) 2 else reports.count { it.statusEnum == RiskStatus.HIGH_RISK }

    val hasScoreData = totalScans > 0
    val unifiedSecurityScore = if (!hasScoreData) {
        0
    } else if (useDemoSampleStats && reports.isEmpty()) {
        84
    } else {
        val avgRisk = reports.map { it.riskScore }.average().toInt()
        (100 - (avgRisk * 0.75).toInt()).coerceIn(12, 99)
    }

    // Compute 6-axis threat radar values
    val radarMap: Map<String, Float> = if (useDemoSampleStats && reports.isEmpty()) {
        mapOf(
            "Phishing" to 0.72f,
            "Social Scams" to 0.58f,
            "Financial Scams" to 0.64f,
            "Fake Identities" to 0.45f,
            "Malware Risk" to 0.38f,
            "Media Manipulation" to 0.52f
        )
    } else if (reports.isNotEmpty()) {
        val categories = listOf(
            "Phishing", "Social Scams", "Financial Scams",
            "Fake Identities", "Malware Risk", "Media Manipulation"
        )
        categories.associateWith { cat ->
            val matching = reports.filter { it.scannerEnum.radarCategory == cat }
            if (matching.isEmpty()) 0.14f else (matching.map { it.riskScore }.average().toFloat() / 100f).coerceIn(0.15f, 1f)
        }
    } else {
        emptyMap()
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("home_dashboard_list")
    ) {
        // 1. HERO CYBERSECURITY BANNER CARD
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(listOf(ElectricCyan, NeonPurple)),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_cyber_banner_1791537276115),
                    contentDescription = "ScamShield AI Command Center Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x66060A17),
                                    Color(0xEB060A17)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.2f))
                                        .border(1.dp, ElectricCyan, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "SCAMSHIELD AI",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Personal Cyber Safety & Risk Platform",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                            }

                            Button(
                                onClick = onOpenShieldAi,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonPurple,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("hero_ask_ai_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Shield AI", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Column {
                            Text(
                                text = "Multi-Engine Digital Threat Defense",
                                style = MaterialTheme.typography.headlineMedium.copy(color = Color.White)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inspect suspicious URLs, OTP texts, bank offers, profiles, media & APKs on demand.",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFD0DEFF))
                            )
                        }
                    }
                }
            }
        }

        // 2. DEMO DASHBOARD MODE BANNER / TOGGLE
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (useDemoSampleStats) SuspiciousAmber.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (useDemoSampleStats) SuspiciousAmber else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (useDemoSampleStats) "DEMO DASHBOARD TELEMETRY ACTIVE" else "Live Local Scan Telemetry",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (useDemoSampleStats) SuspiciousAmber else ElectricCyan
                    )
                    Text(
                        text = if (useDemoSampleStats) {
                            "Showing clearly labelled sample dashboard metrics for demonstration."
                        } else {
                            "Metrics reflect only your real completed scans stored on device."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = useDemoSampleStats,
                    onCheckedChange = onToggleDemoDashboard,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SuspiciousAmber
                    ),
                    modifier = Modifier.testTag("demo_dashboard_switch")
                )
            }
        }

        // 3. UNIFIED SECURITY SCORE & SUMMARY METRICS CARD
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (hasScoreData) {
                        AnimatedCircularScoreMeter(
                            score = unifiedSecurityScore,
                            label = if (useDemoSampleStats && reports.isEmpty()) "DEMO SCORE" else "SECURITY",
                            subLabel = "0–100 Index",
                            isSecurityScore = true,
                            size = 142.dp
                        )
                    } else {
                        // Honest Empty State when 0 scans completed
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.08f))
                                .border(2.dp, ElectricCyan.copy(alpha = 0.35f), CircleShape)
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "NO SCANS YET",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                                Text(
                                    text = "Run a scan first",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unified Posture Score",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (hasScoreData) {
                                "Calculated from $totalScans scan(s) across URL, SMS, financial, profile, APK, and media checks. Kept separate from individual scan risk scores."
                            } else {
                                "No scans completed yet. Run any of the 8 scanners or enable Demo Telemetry above to preview sample posture metrics."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4 Colorful Stat Cards Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DashboardStatPill(
                        title = "Safe Scans",
                        value = "$safeCount",
                        accent = SafeGreen,
                        icon = Icons.Default.GppGood,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatPill(
                        title = "Suspicious",
                        value = "$suspiciousCount",
                        accent = SuspiciousAmber,
                        icon = Icons.Default.GppMaybe,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardStatPill(
                        title = "High Risk",
                        value = "$highRiskCount",
                        accent = HighRiskRed,
                        icon = Icons.Default.GppBad,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. QUICK-ACCESS 8 SCANNERS CAROUSEL & GRID
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quick-Access Security Scanners",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "8 Engines Ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(ScannerType.entries) { scanner ->
                        val (icon, c1, c2) = scannerIconAndColors(scanner)
                        Column(
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(c1.copy(alpha = 0.20f), c2.copy(alpha = 0.12f))
                                    )
                                )
                                .border(1.dp, c1.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                                .clickable { onOpenScanner(scanner) }
                                .padding(14.dp)
                                .testTag("quick_scanner_${scanner.id.lowercase()}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(c1.copy(alpha = 0.22f))
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = scanner.title,
                                    tint = c1,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = scanner.shortTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = scanner.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 5. ANIMATED THREAT RADAR & DISTRIBUTION CHART
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                    .border(1.dp, NeonPurple.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "Threat Radar & Risk Distribution",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "6-Vector Cyber Risk Telemetry",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                    if (useDemoSampleStats && reports.isEmpty()) {
                        Text(
                            text = "DEMO DATA",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuspiciousAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                AnimatedThreatRadarChart(
                    axisScores = radarMap,
                    hasData = hasScoreData
                )

                Spacer(modifier = Modifier.height(14.dp))
                ThreatDistributionBar(
                    safeCount = safeCount,
                    suspiciousCount = suspiciousCount,
                    highRiskCount = highRiskCount
                )
            }
        }

        // 6. RECENT SCANS & SECURITY ACTIVITY TIMELINE
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Text(
                    text = "Recent Security Activity Timeline",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (reports.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ElectricCyan.copy(alpha = 0.7f),
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No Scans Recorded Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap any scanner card above to inspect a URL, message, social profile, bank offer, image, video, APK, or PDF.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    reports.take(5).forEach { report ->
                        RecentTimelineRow(
                            report = report,
                            onClick = { onInspectReport(report) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardStatPill(
    title: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                color = accent,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ThreatDistributionBar(
    safeCount: Int,
    suspiciousCount: Int,
    highRiskCount: Int
) {
    val total = (safeCount + suspiciousCount + highRiskCount).coerceAtLeast(1)
    val safeFrac by animateFloatAsState(
        targetValue = safeCount.toFloat() / total,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "safe_frac"
    )
    val suspFrac by animateFloatAsState(
        targetValue = suspiciousCount.toFloat() / total,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "susp_frac"
    )
    val highFrac by animateFloatAsState(
        targetValue = highRiskCount.toFloat() / total,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "high_frac"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Scan Outcome Distribution",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (safeCount + suspiciousCount + highRiskCount == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ElectricCyan.copy(alpha = 0.25f))
                )
            } else {
                if (safeFrac > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(safeFrac.coerceAtLeast(0.05f))
                            .height(12.dp)
                            .background(SafeGreen)
                    )
                }
                if (suspFrac > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(suspFrac.coerceAtLeast(0.05f))
                            .height(12.dp)
                            .background(SuspiciousAmber)
                    )
                }
                if (highFrac > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(highFrac.coerceAtLeast(0.05f))
                            .height(12.dp)
                            .background(HighRiskRed)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentTimelineRow(
    report: ScanReportEntity,
    onClick: () -> Unit
) {
    val status = report.statusEnum
    val color = when (status) {
        RiskStatus.SAFE -> SafeGreen
        RiskStatus.SUSPICIOUS -> SuspiciousAmber
        RiskStatus.HIGH_RISK -> HighRiskRed
    }
    val timeStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(report.timestamp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
        ) {
            Text(
                text = "${report.riskScore}",
                style = MaterialTheme.typography.labelLarge.copy(color = color)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = report.scannerEnum.shortTitle,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (report.isDemo) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[DEMO]",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuspiciousAmber
                    )
                }
            }
            Text(
                text = report.parsedSubject,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
