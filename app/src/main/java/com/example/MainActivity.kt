package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BottomNavTab
import com.example.ui.ScamShieldViewModel
import com.example.ui.components.CyberGridBackground
import com.example.ui.components.ScanResultCard
import com.example.ui.screens.AlertCenterScreen
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.ReportsHistoryScreen
import com.example.ui.screens.ScannersHubAndDetailScreen
import com.example.ui.screens.ShieldAiChatScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.ScamShieldTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ScamShieldViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            ScamShieldTheme(darkTheme = settings.isDarkMode) {
                ScamShieldRootApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ScamShieldRootApp(viewModel: ScamShieldViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val selectedScanner by viewModel.selectedScanner.collectAsStateWithLifecycle()
    val activeScanState by viewModel.activeScanState.collectAsStateWithLifecycle()
    val pickedFile by viewModel.pickedFile.collectAsStateWithLifecycle()
    val scannerError by viewModel.scannerError.collectAsStateWithLifecycle()
    val allReports by viewModel.allReports.collectAsStateWithLifecycle()
    val inspectedReport by viewModel.inspectedReport.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiTyping by viewModel.isAiTyping.collectAsStateWithLifecycle()
    val isAlertPlaying by viewModel.alertController.isAlertPlaying.collectAsStateWithLifecycle()

    val unreadAlertsCount = allReports.count { !it.isAlertRead }

    // Handle Android Back button for sub-tabs
    BackHandler(enabled = inspectedReport != null || currentTab != BottomNavTab.HOME) {
        when {
            inspectedReport != null -> viewModel.closeInspectedReport()
            currentTab == BottomNavTab.SCAN && selectedScanner != null -> viewModel.closeScannerToHub()
            else -> viewModel.selectTab(BottomNavTab.HOME)
        }
    }

    CyberGridBackground(isDark = settings.isDarkMode) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                ScamShieldBottomNavigationBar(
                    currentTab = currentTab,
                    unreadAlertsCount = if (settings.notificationsEnabled) unreadAlertsCount else 0,
                    onSelectTab = { tab ->
                        viewModel.closeInspectedReport()
                        viewModel.selectTab(tab)
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // If user tapped a report from Home, Alerts, or Reports, show the full Report Detail Inspector
                if (inspectedReport != null) {
                    val report = inspectedReport!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = { viewModel.closeInspectedReport() },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("close_inspected_report_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = ElectricCyan
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Security Report Inspector",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = report.scannerEnum.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricCyan
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        ScanResultCard(
                            report = report,
                            isAlertPlaying = isAlertPlaying,
                            onStopAlert = { viewModel.stopActiveBuzzer() },
                            onReplayAlert = { viewModel.replayAlertForReport(report) },
                            onAskShieldAi = { viewModel.askShieldAiAboutReport(report) }
                        )
                    }
                } else {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180))
                        },
                        label = "bottom_nav_transition"
                    ) { tab ->
                        when (tab) {
                            BottomNavTab.HOME -> HomeDashboardScreen(
                                reports = allReports,
                                settings = settings,
                                onToggleDemoDashboard = { viewModel.setDemoModeDashboard(it) },
                                onOpenScanner = { scanner -> viewModel.openScanner(scanner) },
                                onInspectReport = { report -> viewModel.inspectReportDetail(report) },
                                onOpenShieldAi = { viewModel.selectTab(BottomNavTab.SHIELD_AI) }
                            )

                            BottomNavTab.SCAN -> ScannersHubAndDetailScreen(
                                selectedScanner = selectedScanner,
                                activeScanState = activeScanState,
                                pickedFile = pickedFile,
                                scannerError = scannerError,
                                isAlertPlaying = isAlertPlaying,
                                onSelectScanner = { viewModel.openScanner(it) },
                                onBackToHub = { viewModel.closeScannerToHub() },
                                onClearError = { viewModel.clearScannerError() },
                                onFilePicked = { viewModel.onFilePicked(it) },
                                onClearPickedFile = { viewModel.clearPickedFile() },
                                onRunTextScan = { type, primary, secondary, platform, isDemo ->
                                    viewModel.runTextBasedScan(type, primary, secondary, platform, isDemo)
                                },
                                onRunFileScan = { type, isDemo ->
                                    viewModel.runFileBasedScan(type, isDemo)
                                },
                                onResetScanner = { viewModel.resetCurrentScannerForNewScan() },
                                onStopAlert = { viewModel.stopActiveBuzzer() },
                                onReplayAlert = { viewModel.replayAlertForReport(it) },
                                onAskShieldAi = { viewModel.askShieldAiAboutReport(it) }
                            )

                            BottomNavTab.ALERTS -> AlertCenterScreen(
                                reports = allReports,
                                demoModeEnabled = settings.demoModeDashboardEnabled,
                                onInspectReport = { viewModel.inspectReportDetail(it) },
                                onMarkRead = { viewModel.markAlertRead(it) },
                                onMarkAllRead = { viewModel.markAllAlertsRead() },
                                onClearHistory = { viewModel.clearAllHistory() }
                            )

                            BottomNavTab.REPORTS -> ReportsHistoryScreen(
                                reports = allReports,
                                onInspectReport = { viewModel.inspectReportDetail(it) },
                                onDeleteReport = { viewModel.deleteReport(it) },
                                onClearAllReports = { viewModel.clearAllHistory() }
                            )

                            BottomNavTab.SHIELD_AI -> ShieldAiChatScreen(
                                messages = chatMessages,
                                isTyping = isAiTyping,
                                onSendMessage = { viewModel.sendChatMessage(it) },
                                onClearChat = { viewModel.clearChatHistory() }
                            )

                            BottomNavTab.PROFILE -> ProfileAndSettingsScreen(
                                settings = settings,
                                totalSavedReports = allReports.size,
                                onSetDarkMode = { viewModel.setDarkMode(it) },
                                onSetAlertSound = { viewModel.setAlertSound(it) },
                                onSetVoiceFeedback = { viewModel.setVoiceFeedback(it) },
                                onSetVibration = { viewModel.setVibration(it) },
                                onSetNotifications = { viewModel.setNotifications(it) },
                                onSetSaveHistory = { viewModel.setSaveHistory(it) },
                                onSetDemoDashboard = { viewModel.setDemoModeDashboard(it) },
                                onUpdateProfile = { name, role -> viewModel.updateProfile(name, role) },
                                onClearAllHistory = { viewModel.clearAllHistory() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScamShieldBottomNavigationBar(
    currentTab: BottomNavTab,
    unreadAlertsCount: Int,
    onSelectTab: (BottomNavTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 8.dp
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = currentTab == tab
            val icon = when (tab) {
                BottomNavTab.HOME -> Icons.Default.Home
                BottomNavTab.SCAN -> Icons.Default.Radar
                BottomNavTab.ALERTS -> Icons.Default.Notifications
                BottomNavTab.REPORTS -> Icons.Default.Assessment
                BottomNavTab.SHIELD_AI -> Icons.Default.AutoAwesome
                BottomNavTab.PROFILE -> Icons.Default.Person
            }

            val activeTint = if (tab == BottomNavTab.SHIELD_AI) NeonPurple else ElectricCyan

            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    if (tab == BottomNavTab.ALERTS && unreadAlertsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = HighRiskRed) {
                                    Text(
                                        text = if (unreadAlertsCount > 9) "9+" else "$unreadAlertsCount",
                                        color = Color.White
                                    )
                                }
                            }
                        ) {
                            Icon(imageVector = icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(imageVector = icon, contentDescription = tab.label)
                    }
                },
                label = {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = activeTint,
                    selectedTextColor = activeTint,
                    indicatorColor = activeTint.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_${tab.route}")
            )
        }
    }
}
