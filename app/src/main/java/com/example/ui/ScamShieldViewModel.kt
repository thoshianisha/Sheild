package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.RiskStatus
import com.example.data.ScanReportEntity
import com.example.data.ScanRepository
import com.example.data.ScamShieldDatabase
import com.example.data.ScannerType
import com.example.data.SettingsManager
import com.example.data.UserSettings
import com.example.domain.PickedFileInfo
import com.example.domain.SecurityAlertController
import com.example.domain.SecurityScannerEngine
import com.example.network.GeminiServiceClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BottomNavTab(val route: String, val label: String) {
    HOME("home", "Home"),
    SCAN("scan", "Scan"),
    ALERTS("alerts", "Alerts"),
    REPORTS("reports", "Reports"),
    SHIELD_AI("shield_ai", "Shield AI"),
    PROFILE("profile", "Profile")
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val isUser: Boolean,
    val text: String,
    val isLiveAi: Boolean = true,
    val statusNote: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class ActiveScanState {
    object Idle : ActiveScanState()
    data class Scanning(val scannerType: ScannerType, val isDemo: Boolean, val stepText: String) : ActiveScanState()
    data class Completed(val report: ScanReportEntity) : ActiveScanState()
}

class ScamShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        ScamShieldDatabase::class.java,
        "scamshield_reports.db"
    ).fallbackToDestructiveMigration(false).build()

    private val repository = ScanRepository(db.scanReportDao())
    private val settingsManager = SettingsManager(application)
    val alertController = SecurityAlertController(application)

    val settings: StateFlow<UserSettings> = settingsManager.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    val allReports: StateFlow<List<ScanReportEntity>> = repository.allReports.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentTab = MutableStateFlow(BottomNavTab.HOME)
    val currentTab: StateFlow<BottomNavTab> = _currentTab.asStateFlow()

    // Null means showing the 8-scanner hub inside the Scan tab; non-null opens that specific scanner screen
    private val _selectedScanner = MutableStateFlow<ScannerType?>(null)
    val selectedScanner: StateFlow<ScannerType?> = _selectedScanner.asStateFlow()

    // Active scan workflow state
    private val _activeScanState = MutableStateFlow<ActiveScanState>(ActiveScanState.Idle)
    val activeScanState: StateFlow<ActiveScanState> = _activeScanState.asStateFlow()

    // Selected report for detail inspection from Home / Alerts / Reports
    private val _inspectedReport = MutableStateFlow<ScanReportEntity?>(null)
    val inspectedReport: StateFlow<ScanReportEntity?> = _inspectedReport.asStateFlow()

    // Selected file for file-based scanners (Image, Video, APK, PDF) — always null by default!
    private val _pickedFile = MutableStateFlow<PickedFileInfo?>(null)
    val pickedFile: StateFlow<PickedFileInfo?> = _pickedFile.asStateFlow()

    // Input validation error message for current scanner
    private val _scannerError = MutableStateFlow<String?>(null)
    val scannerError: StateFlow<String?> = _scannerError.asStateFlow()

    // Shield AI Chat State
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "Welcome to **Shield AI** — your personal cybersecurity assistant.\n\nAsk me about suspicious links, OTP requests, bank/loan offers, social media impersonation, APK permissions, or share any scan result for an in-depth explanation.\n\n*Privacy Reminder: Never type real OTPs, passwords, or PINs.*",
                isLiveAi = GeminiServiceClient.isApiKeyConfigured(),
                statusNote = if (GeminiServiceClient.isApiKeyConfigured()) {
                    "Connected to Gemini 3.5 Flash Security Engine"
                } else {
                    "Demo / Local Cybersecurity Knowledge Mode (Set GEMINI_API_KEY in AI Studio Secrets for live LLM calls)"
                }
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    fun selectTab(tab: BottomNavTab) {
        _currentTab.value = tab
        if (tab != BottomNavTab.SCAN) {
            _activeScanState.value = ActiveScanState.Idle
        }
    }

    fun openScanner(scannerType: ScannerType) {
        _selectedScanner.value = scannerType
        _activeScanState.value = ActiveScanState.Idle
        _pickedFile.value = null
        _scannerError.value = null
        _currentTab.value = BottomNavTab.SCAN
    }

    fun closeScannerToHub() {
        alertController.stopAlert()
        _selectedScanner.value = null
        _activeScanState.value = ActiveScanState.Idle
        _pickedFile.value = null
        _scannerError.value = null
    }

    fun resetCurrentScannerForNewScan() {
        alertController.stopAlert()
        _activeScanState.value = ActiveScanState.Idle
        _pickedFile.value = null
        _scannerError.value = null
    }

    fun clearScannerError() {
        _scannerError.value = null
    }

    fun onFilePicked(uri: Uri?) {
        if (uri == null) return
        val info = SecurityScannerEngine.extractFileInfo(getApplication(), uri)
        _pickedFile.value = info
        _scannerError.value = null
    }

    fun clearPickedFile() {
        _pickedFile.value = null
        _scannerError.value = null
    }

    fun inspectReportDetail(report: ScanReportEntity) {
        _inspectedReport.value = report
        viewModelScope.launch {
            repository.markAlertRead(report.id)
        }
    }

    fun closeInspectedReport() {
        alertController.stopAlert()
        _inspectedReport.value = null
    }

    // Run Text/URL/Social Scanner
    fun runTextBasedScan(
        scannerType: ScannerType,
        primaryInput: String,
        secondaryInput: String = "",
        platform: String = "Instagram",
        isDemo: Boolean = false
    ) {
        val trimmed = primaryInput.trim()
        if (!isDemo && trimmed.isEmpty()) {
            _scannerError.value = when (scannerType) {
                ScannerType.URL_LINK -> "Please enter or paste a URL to analyze, or tap 'Try Demo Scan'."
                ScannerType.SOCIAL_PROFILE -> "Please enter a username or profile URL to analyze, or tap 'Try Demo Scan'."
                ScannerType.OTP_MESSAGE -> "Please paste an SMS, WhatsApp, or OTP message to inspect, or tap 'Try Demo Scan'."
                ScannerType.BANK_LOAN -> "Please enter a loan offer, bank notice, or payment request, or tap 'Try Demo Scan'."
                else -> "Input cannot be empty."
            }
            return
        }
        if (!isDemo && scannerType == ScannerType.URL_LINK && !trimmed.contains(".")) {
            _scannerError.value = "Please enter a valid domain or URL (e.g., example.com or https://...)."
            return
        }

        _scannerError.value = null
        viewModelScope.launch {
            val steps = listOf(
                "Validating input & lexical structure...",
                "Running heuristic pattern & threat signature analysis...",
                "Evaluating risk indicators & generating safety guidance..."
            )
            for (step in steps) {
                _activeScanState.value = ActiveScanState.Scanning(scannerType, isDemo, step)
                delay(480)
            }

            val report = when (scannerType) {
                ScannerType.URL_LINK -> SecurityScannerEngine.analyzeUrl(trimmed, isDemo)
                ScannerType.SOCIAL_PROFILE -> SecurityScannerEngine.analyzeSocialProfile(
                    platform = platform,
                    handleOrUrl = trimmed,
                    bioOrContext = secondaryInput,
                    isDemo = isDemo
                )
                ScannerType.OTP_MESSAGE -> SecurityScannerEngine.analyzeOtpMessage(trimmed, isDemo)
                ScannerType.BANK_LOAN -> SecurityScannerEngine.analyzeBankLoanOffer(trimmed, isDemo)
                else -> SecurityScannerEngine.analyzeUrl(trimmed, isDemo)
            }

            finalizeAndPresentReport(report)
        }
    }

    // Run File-Based Scanner (Image, Video, APK, PDF)
    fun runFileBasedScan(
        scannerType: ScannerType,
        isDemo: Boolean = false
    ) {
        val currentFile = _pickedFile.value
        if (!isDemo && currentFile == null) {
            _scannerError.value = "Please select a file from your device first, or tap 'Try Demo Scan' for a simulated demonstration."
            return
        }

        _scannerError.value = null
        viewModelScope.launch {
            val steps = listOf(
                "Inspecting file container & header metadata...",
                "Analyzing security signatures & anomalies...",
                "Compiling risk score & confidence limitations..."
            )
            for (step in steps) {
                _activeScanState.value = ActiveScanState.Scanning(scannerType, isDemo, step)
                delay(520)
            }

            val context = getApplication<Application>()
            val report = when (scannerType) {
                ScannerType.IMAGE_AUTH -> SecurityScannerEngine.analyzeImageAuthenticity(context, currentFile, isDemo)
                ScannerType.VIDEO_DEEPFAKE -> SecurityScannerEngine.analyzeVideoDeepfake(context, currentFile, isDemo)
                ScannerType.APP_APK -> SecurityScannerEngine.analyzeApkRisk(context, currentFile, isDemo)
                ScannerType.PDF_DOC -> SecurityScannerEngine.analyzeDocument(context, currentFile, isDemo)
                else -> SecurityScannerEngine.analyzeDocument(context, currentFile, isDemo)
            }

            finalizeAndPresentReport(report)
        }
    }

    private suspend fun finalizeAndPresentReport(report: ScanReportEntity) {
        val currentSettings = settings.value
        val savedId = if (currentSettings.saveScanHistory) {
            repository.saveReport(report)
        } else {
            System.currentTimeMillis()
        }
        val finalReport = report.copy(id = savedId)
        _activeScanState.value = ActiveScanState.Completed(finalReport)

        // Trigger Buzzer / Voice / Vibration alert once for this scan result
        alertController.triggerAlertForResult(
            reportId = finalReport.id,
            status = finalReport.statusEnum,
            settings = currentSettings,
            forceReplay = false
        )
    }

    fun replayAlertForReport(report: ScanReportEntity) {
        alertController.triggerAlertForResult(
            reportId = report.id,
            status = report.statusEnum,
            settings = settings.value,
            forceReplay = true
        )
    }

    fun stopActiveBuzzer() {
        alertController.stopAlert()
    }

    // Share a scan report directly into Shield AI chat and explain it
    fun askShieldAiAboutReport(report: ScanReportEntity) {
        alertController.stopAlert()
        _inspectedReport.value = null
        _currentTab.value = BottomNavTab.SHIELD_AI

        val prompt = """
            Please explain my ${report.scannerEnum.title} scan result in simple terms and advise what I should do next:
            - Status: ${report.statusEnum.label} (Risk Score: ${report.riskScore}/100)
            - Category: ${report.threatCategory}
            - Subject: ${report.parsedSubject}
            - Evidence: ${report.evidenceList.joinToString("; ")}
        """.trimIndent()

        sendChatMessage(prompt)
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _isAiTyping.value) return

        val historySnapshot = _chatMessages.value.map { Pair(it.isUser, it.text) }
        val userMsg = ChatMessage(isUser = true, text = trimmed)
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiTyping.value = true

        viewModelScope.launch {
            delay(350)
            val aiResult = GeminiServiceClient.askShieldAi(historySnapshot, trimmed)
            val botMsg = ChatMessage(
                isUser = false,
                text = aiResult.text,
                isLiveAi = aiResult.isLiveModel,
                statusNote = aiResult.errorNote
            )
            _chatMessages.value = _chatMessages.value + botMsg
            _isAiTyping.value = false
        }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                isUser = false,
                text = "Chat history cleared. Ask me any cybersecurity question or share a scan report for analysis.",
                isLiveAi = GeminiServiceClient.isApiKeyConfigured()
            )
        )
    }

    // Alert & Report Management
    fun markAlertRead(id: Long) {
        viewModelScope.launch { repository.markAlertRead(id) }
    }

    fun markAllAlertsRead() {
        viewModelScope.launch { repository.markAllAlertsRead() }
    }

    fun deleteReport(id: Long) {
        viewModelScope.launch { repository.deleteReport(id) }
    }

    fun clearAllHistory() {
        viewModelScope.launch { repository.clearAll() }
    }

    // Settings updates
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setDarkMode(enabled) }
    fun setAlertSound(enabled: Boolean) = viewModelScope.launch { settingsManager.setAlertSound(enabled) }
    fun setVoiceFeedback(enabled: Boolean) = viewModelScope.launch { settingsManager.setVoiceFeedback(enabled) }
    fun setVibration(enabled: Boolean) = viewModelScope.launch { settingsManager.setVibration(enabled) }
    fun setNotifications(enabled: Boolean) = viewModelScope.launch { settingsManager.setNotifications(enabled) }
    fun setSaveHistory(enabled: Boolean) = viewModelScope.launch { settingsManager.setSaveScanHistory(enabled) }
    fun setDemoModeDashboard(enabled: Boolean) = viewModelScope.launch { settingsManager.setDemoModeDashboard(enabled) }
    fun updateProfile(name: String, role: String) = viewModelScope.launch { settingsManager.updateProfile(name, role) }

    override fun onCleared() {
        super.onCleared()
        alertController.shutdown()
    }
}
