package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.ScanReportEntity
import com.example.data.ScannerType
import com.example.domain.PickedFileInfo
import com.example.domain.SecurityScannerEngine
import com.example.ui.ActiveScanState
import com.example.ui.components.AnimatedRadarScannerOverlay
import com.example.ui.components.ScanResultCard
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.HighRiskRed
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.SuspiciousAmber

@Composable
fun ScannersHubAndDetailScreen(
    selectedScanner: ScannerType?,
    activeScanState: ActiveScanState,
    pickedFile: PickedFileInfo?,
    scannerError: String?,
    isAlertPlaying: Boolean,
    onSelectScanner: (ScannerType) -> Unit,
    onBackToHub: () -> Unit,
    onClearError: () -> Unit,
    onFilePicked: (android.net.Uri?) -> Unit,
    onClearPickedFile: () -> Unit,
    onRunTextScan: (ScannerType, String, String, String, Boolean) -> Unit,
    onRunFileScan: (ScannerType, Boolean) -> Unit,
    onResetScanner: () -> Unit,
    onStopAlert: () -> Unit,
    onReplayAlert: (ScanReportEntity) -> Unit,
    onAskShieldAi: (ScanReportEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedScanner == null) {
        ScannerDirectoryHub(
            onSelectScanner = onSelectScanner,
            modifier = modifier
        )
    } else {
        BackHandler {
            onBackToHub()
        }
        IndividualScannerWorkspace(
            scannerType = selectedScanner,
            activeScanState = activeScanState,
            pickedFile = pickedFile,
            scannerError = scannerError,
            isAlertPlaying = isAlertPlaying,
            onBackToHub = onBackToHub,
            onClearError = onClearError,
            onFilePicked = onFilePicked,
            onClearPickedFile = onClearPickedFile,
            onRunTextScan = onRunTextScan,
            onRunFileScan = onRunFileScan,
            onResetScanner = onResetScanner,
            onStopAlert = onStopAlert,
            onReplayAlert = onReplayAlert,
            onAskShieldAi = onAskShieldAi,
            modifier = modifier
        )
    }
}

@Composable
private fun ScannerDirectoryHub(
    onSelectScanner: (ScannerType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("scanners_hub_list")
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "8 Dedicated Cyber Scanners",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select any specialized engine below. All inputs start clean and empty so you can inspect your own content or run an optional labelled Demo Scan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        items(ScannerType.entries) { scanner ->
            val (icon, c1, c2) = scannerIconAndColors(scanner)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                c1.copy(alpha = 0.16f),
                                c2.copy(alpha = 0.08f),
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .border(1.dp, c1.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable { onSelectScanner(scanner) }
                    .padding(16.dp)
                    .testTag("hub_scanner_card_${scanner.id.lowercase()}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c1.copy(alpha = 0.22f))
                        .border(1.dp, c1, RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = scanner.title,
                        tint = c1,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = scanner.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = scanner.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Radar Vector: ${scanner.radarCategory}",
                        style = MaterialTheme.typography.labelSmall,
                        color = c1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndividualScannerWorkspace(
    scannerType: ScannerType,
    activeScanState: ActiveScanState,
    pickedFile: PickedFileInfo?,
    scannerError: String?,
    isAlertPlaying: Boolean,
    onBackToHub: () -> Unit,
    onClearError: () -> Unit,
    onFilePicked: (android.net.Uri?) -> Unit,
    onClearPickedFile: () -> Unit,
    onRunTextScan: (ScannerType, String, String, String, Boolean) -> Unit,
    onRunFileScan: (ScannerType, Boolean) -> Unit,
    onResetScanner: () -> Unit,
    onStopAlert: () -> Unit,
    onReplayAlert: (ScanReportEntity) -> Unit,
    onAskShieldAi: (ScanReportEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // MANDATORY REQUIREMENT: All scanner text inputs MUST start EMPTY by default!
    var primaryInput by rememberSaveable(scannerType) { mutableStateOf("") }
    var secondaryBioInput by rememberSaveable(scannerType) { mutableStateOf("") }
    var selectedPlatform by rememberSaveable(scannerType) { mutableStateOf("Instagram") }

    val clipboardManager = LocalClipboardManager.current
    val (icon, accent1, accent2) = scannerIconAndColors(scannerType)

    // Launchers for file-based scanners (Zero-permission Photo Picker for images/videos, OpenDocument for APK/PDF)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> onFilePicked(uri) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> onFilePicked(uri) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> onFilePicked(uri) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Header Bar with Back Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBackToHub,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("scanner_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to Scanners",
                    tint = ElectricCyan
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent1.copy(alpha = 0.2f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent1,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scannerType.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = scannerType.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Privacy & Redaction Notice Banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(ElectricCyan.copy(alpha = 0.1f))
                .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PrivacyTip,
                contentDescription = null,
                tint = ElectricCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Privacy Protection: Input starts empty. Never enter real OTP values, passwords, or PINs. Redact personal data before scanning.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Card (Always shown or collapsible when result is ready)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .border(1.dp, accent1.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .padding(18.dp)
        ) {
            when (scannerType) {
                ScannerType.URL_LINK,
                ScannerType.SOCIAL_PROFILE,
                ScannerType.OTP_MESSAGE,
                ScannerType.BANK_LOAN -> {
                    if (scannerType == ScannerType.SOCIAL_PROFILE) {
                        Text(
                            text = "Select Social Platform",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val platforms = listOf("Instagram", "Facebook", "X", "LinkedIn", "Telegram")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            platforms.forEach { platform ->
                                val selected = selectedPlatform == platform
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedPlatform = platform },
                                    label = { Text(platform) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accent1.copy(alpha = 0.25f),
                                        selectedLabelColor = accent1
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    val fieldLabel = when (scannerType) {
                        ScannerType.URL_LINK -> "Enter or paste URL to inspect"
                        ScannerType.SOCIAL_PROFILE -> "Enter $selectedPlatform username or profile URL"
                        ScannerType.OTP_MESSAGE -> "Paste SMS, WhatsApp, or OTP text message"
                        ScannerType.BANK_LOAN -> "Enter bank notice, loan offer, KYC alert, or investment text"
                        else -> "Enter content"
                    }

                    val fieldPlaceholder = when (scannerType) {
                        ScannerType.URL_LINK -> "Leave empty until you type or paste your own link..."
                        ScannerType.SOCIAL_PROFILE -> "e.g., username or profile link (empty by default)"
                        ScannerType.OTP_MESSAGE -> "Paste suspicious message here (redact real OTP numbers)..."
                        ScannerType.BANK_LOAN -> "Paste loan offer, fee demand, or banking message here..."
                        else -> ""
                    }

                    OutlinedTextField(
                        value = primaryInput,
                        onValueChange = {
                            primaryInput = it
                            onClearError()
                        },
                        label = { Text(fieldLabel) },
                        placeholder = { Text(fieldPlaceholder) },
                        minLines = if (scannerType == ScannerType.URL_LINK || scannerType == ScannerType.SOCIAL_PROFILE) 2 else 4,
                        maxLines = 7,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("scanner_primary_input")
                    )

                    if (scannerType == ScannerType.SOCIAL_PROFILE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = secondaryBioInput,
                            onValueChange = { secondaryBioInput = it },
                            label = { Text("Optional: Paste public bio text or DM snippet") },
                            placeholder = { Text("Paste any public bio links or suspicious claims (optional)...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("scanner_secondary_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Paste & Clear Utility Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text
                                if (!clip.isNullOrBlank()) {
                                    primaryInput = clip
                                    onClearError()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("paste_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste from Clipboard",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste")
                        }

                        OutlinedButton(
                            onClick = {
                                primaryInput = ""
                                secondaryBioInput = ""
                                onClearError()
                                onResetScanner()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_input_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Input",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear")
                        }
                    }

                    if (scannerError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = scannerError,
                            style = MaterialTheme.typography.bodySmall,
                            color = HighRiskRed
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Analyze Button + Separate "Try Demo Scan" Button
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                onRunTextScan(
                                    scannerType,
                                    primaryInput,
                                    secondaryBioInput,
                                    selectedPlatform,
                                    false
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent1,
                                contentColor = Color(0xFF04121C)
                            ),
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("analyze_content_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (scannerType == ScannerType.URL_LINK) "Analyze URL" else "Analyze Now",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                // Never overwrite user's typed text if they already typed something! Run demo scan with separate demo sample data.
                                val demoSample = SecurityScannerEngine.getDemoSampleText(scannerType)
                                val demoBio = if (scannerType == ScannerType.SOCIAL_PROFILE) {
                                    SecurityScannerEngine.getDemoSocialBio()
                                } else ""
                                onRunTextScan(
                                    scannerType,
                                    demoSample,
                                    demoBio,
                                    selectedPlatform,
                                    true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("try_demo_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Try Demo Scan",
                                tint = SuspiciousAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Try Demo Scan",
                                style = MaterialTheme.typography.labelLarge,
                                color = SuspiciousAmber
                            )
                        }
                    }
                }

                // FILE-BASED SCANNERS: IMAGE, VIDEO, APK, PDF
                ScannerType.IMAGE_AUTH,
                ScannerType.VIDEO_DEEPFAKE,
                ScannerType.APP_APK,
                ScannerType.PDF_DOC -> {
                    val selectButtonText = when (scannerType) {
                        ScannerType.IMAGE_AUTH -> "Select Image from Gallery"
                        ScannerType.VIDEO_DEEPFAKE -> "Select Video from Device"
                        ScannerType.APP_APK -> "Select APK File to Inspect"
                        ScannerType.PDF_DOC -> "Select PDF or Document"
                        else -> "Select File"
                    }

                    if (pickedFile == null) {
                        // Clean Empty State for File Selection
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
                                .border(1.dp, accent1.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                                .padding(22.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent1,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No File Selected Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose a file from your device for local inspection, or tap 'Try Demo Scan' below for a labelled hackathon demonstration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    when (scannerType) {
                                        ScannerType.IMAGE_AUTH -> imagePickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                        ScannerType.VIDEO_DEEPFAKE -> videoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                        )
                                        ScannerType.APP_APK -> documentPickerLauncher.launch(
                                            arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")
                                        )
                                        ScannerType.PDF_DOC -> documentPickerLauncher.launch(
                                            arrayOf("application/pdf", "text/plain", "*/*")
                                        )
                                        else -> {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accent1,
                                    contentColor = Color(0xFF04121C)
                                ),
                                modifier = Modifier.testTag("select_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectButtonText, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        // Selected File Preview Card + Remove Button
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, accent1, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            if (scannerType == ScannerType.IMAGE_AUTH) {
                                AsyncImage(
                                    model = pickedFile.uri,
                                    contentDescription = "Selected Image Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pickedFile.fileName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Size: ${pickedFile.fileSizeBytes / 1024} KB • Type: ${pickedFile.mimeType}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan
                                    )
                                }
                                OutlinedButton(
                                    onClick = onClearPickedFile,
                                    modifier = Modifier.testTag("remove_file_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove File",
                                        tint = HighRiskRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", color = HighRiskRed)
                                }
                            }
                        }
                    }

                    if (scannerError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = scannerError,
                            style = MaterialTheme.typography.bodySmall,
                            color = HighRiskRed
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onRunFileScan(scannerType, false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent1,
                                contentColor = Color(0xFF04121C)
                            ),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("analyze_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyze File", style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = { onRunFileScan(scannerType, true) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("try_demo_file_scan_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "Try Demo Scan",
                                tint = SuspiciousAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Try Demo Scan",
                                style = MaterialTheme.typography.labelLarge,
                                color = SuspiciousAmber
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Active Scan Progress Overlay OR Final Result Card
        when (activeScanState) {
            is ActiveScanState.Idle -> {
                // Helpful instructions when idle
            }
            is ActiveScanState.Scanning -> {
                AnimatedRadarScannerOverlay(
                    scannerTitle = scannerType.title,
                    stepText = activeScanState.stepText,
                    isDemo = activeScanState.isDemo
                )
            }
            is ActiveScanState.Completed -> {
                val report = activeScanState.report
                // If Video Deepfake Scanner, also show a visual timeline bar
                if (scannerType == ScannerType.VIDEO_DEEPFAKE) {
                    VideoTimelineAnomalyCard(report = report)
                    Spacer(modifier = Modifier.height(14.dp))
                }
                ScanResultCard(
                    report = report,
                    isAlertPlaying = isAlertPlaying,
                    onStopAlert = onStopAlert,
                    onReplayAlert = { onReplayAlert(report) },
                    onAskShieldAi = { onAskShieldAi(report) },
                    onNewScan = onResetScanner
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun VideoTimelineAnomalyCard(report: ScanReportEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "VIDEO TIMELINE INSPECTION TRACK",
            style = MaterialTheme.typography.labelMedium,
            color = ElectricCyan
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(3.dp)
        ) {
            Box(modifier = Modifier.weight(0.25f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(SafeGreen))
            Box(modifier = Modifier.weight(0.25f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(if (report.riskScore >= 65) HighRiskRed else SuspiciousAmber))
            Box(modifier = Modifier.weight(0.2f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(SafeGreen))
            Box(modifier = Modifier.weight(0.3f).height(12.dp).clip(RoundedCornerShape(3.dp)).background(if (report.riskScore >= 65) HighRiskRed else SuspiciousAmber))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("00:00s", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("00:04s Flag", style = MaterialTheme.typography.labelSmall, color = HighRiskRed)
            Text("00:11s Flag", style = MaterialTheme.typography.labelSmall, color = SuspiciousAmber)
            Text("End", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
