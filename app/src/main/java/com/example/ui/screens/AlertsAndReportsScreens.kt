package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.RiskStatus
import com.example.data.ScanReportEntity
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SuspiciousAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertCenterScreen(
    reports: List<ScanReportEntity>,
    demoModeEnabled: Boolean,
    onInspectReport: (ScanReportEntity) -> Unit,
    onMarkRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }

    val filteredReports = reports.filter { report ->
        when (selectedFilter) {
            "CRITICAL" -> report.riskScore >= 80
            "HIGH_RISK" -> report.statusEnum == RiskStatus.HIGH_RISK
            "MEDIUM_RISK" -> report.statusEnum == RiskStatus.SUSPICIOUS
            "SAFE" -> report.statusEnum == RiskStatus.SAFE
            else -> true
        }
    }

    val unreadCount = reports.count { !it.isAlertRead }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("alert_center_screen")
    ) {
        // Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Security Alert Center",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$unreadCount unread alert(s) • Generated from completed scans",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onMarkAllRead,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("mark_all_read_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark All Read",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Read", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier.testTag("alerts_clear_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Alert History",
                        tint = HighRiskRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips: All, Critical, High-Risk, Medium-Risk, Safe
        val filters = listOf(
            "ALL" to "All (${reports.size})",
            "CRITICAL" to "Critical",
            "HIGH_RISK" to "High-Risk",
            "MEDIUM_RISK" to "Medium Warning",
            "SAFE" to "Safe"
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filters.forEach { (key, label) ->
                val selected = selectedFilter == key
                FilterChip(
                    selected = selected,
                    onClick = { selectedFilter = key },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                        selectedLabelColor = ElectricCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredReports.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Security Alerts in This Filter",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (demoModeEnabled) {
                            "Demo Mode is enabled on the Dashboard. Run any scan or tap 'Try Demo Scan' inside a scanner to generate real-time alert entries."
                        } else {
                            "Alerts are generated automatically from your completed scans. Run a URL, OTP, Bank, Social, Media, or APK scan to populate alerts."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    AlertItemCard(
                        report = report,
                        onViewDetails = { onInspectReport(report) },
                        onMarkRead = { onMarkRead(report.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertItemCard(
    report: ScanReportEntity,
    onViewDetails: () -> Unit,
    onMarkRead: () -> Unit
) {
    val status = report.statusEnum
    val color = when (status) {
        RiskStatus.SAFE -> SafeGreen
        RiskStatus.SUSPICIOUS -> SuspiciousAmber
        RiskStatus.HIGH_RISK -> HighRiskRed
    }
    val icon = when (status) {
        RiskStatus.SAFE -> Icons.Default.GppGood
        RiskStatus.SUSPICIOUS -> Icons.Default.GppMaybe
        RiskStatus.HIGH_RISK -> Icons.Default.GppBad
    }
    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(report.timestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (!report.isAlertRead) color.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            )
            .border(
                width = if (!report.isAlertRead) 1.5.dp else 1.dp,
                color = if (!report.isAlertRead) color else color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onViewDetails)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = report.threatCategory,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (report.isDemo) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DEMO",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuspiciousAmber
                            )
                        }
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (report.riskScore >= 80) "CRITICAL (${report.riskScore})" else "${status.label} (${report.riskScore})",
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = report.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!report.isAlertRead) {
                OutlinedButton(
                    onClick = onMarkRead,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Mark as Read",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mark Read", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Button(
                onClick = onViewDetails,
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = Color(0xFF04121C)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "View Details",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inspect Report", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportsHistoryScreen(
    reports: List<ScanReportEntity>,
    onInspectReport: (ScanReportEntity) -> Unit,
    onDeleteReport: (Long) -> Unit,
    onClearAllReports: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }

    val filtered = reports.filter { report ->
        when (selectedFilter) {
            "SAFE" -> report.statusEnum == RiskStatus.SAFE
            "SUSPICIOUS" -> report.statusEnum == RiskStatus.SUSPICIOUS
            "THREATS" -> report.statusEnum == RiskStatus.HIGH_RISK
            else -> true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("reports_history_screen")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Security Reports & History",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Stored locally on device • ${reports.size} total report(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
                )
            }

            if (reports.isNotEmpty()) {
                OutlinedButton(
                    onClick = onClearAllReports,
                    modifier = Modifier.testTag("clear_all_reports_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Scan History",
                        tint = HighRiskRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", color = HighRiskRed, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Required Filters: All, Safe, Suspicious, Threats
        val filterOptions = listOf(
            "ALL" to "All (${reports.size})",
            "SAFE" to "Safe (${reports.count { it.statusEnum == RiskStatus.SAFE }})",
            "SUSPICIOUS" to "Suspicious (${reports.count { it.statusEnum == RiskStatus.SUSPICIOUS }})",
            "THREATS" to "Threats (${reports.count { it.statusEnum == RiskStatus.HIGH_RISK }})"
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterOptions.forEach { (key, title) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                        selectedLabelColor = ElectricCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filtered.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                        .padding(28.dp)
                ) {
                    Text(
                        text = "No Scan Reports Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Completed scans from any of the 8 scanners are automatically saved here when Scan History is enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered, key = { it.id }) { report ->
                    val status = report.statusEnum
                    val color = when (status) {
                        RiskStatus.SAFE -> SafeGreen
                        RiskStatus.SUSPICIOUS -> SuspiciousAmber
                        RiskStatus.HIGH_RISK -> HighRiskRed
                    }
                    val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date(report.timestamp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            .clickable { onInspectReport(report) }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f))
                                        .border(1.dp, color, CircleShape)
                                ) {
                                    Text(
                                        text = "${report.riskScore}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = color,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = report.scannerEnum.title,
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
                                        text = "${report.threatCategory} • $dateStr",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteReport(report.id) },
                                modifier = Modifier.testTag("delete_report_${report.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Report",
                                    tint = HighRiskRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = report.submittedContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recommended: ${report.recommendationsList.firstOrNull() ?: "Inspect report for details."}",
                            style = MaterialTheme.typography.labelSmall,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
