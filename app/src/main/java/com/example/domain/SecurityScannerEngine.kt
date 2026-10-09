package com.example.domain

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.RiskStatus
import com.example.data.ScanReportEntity
import com.example.data.ScannerType
import com.example.network.GeminiServiceClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URI

data class PickedFileInfo(
    val uri: Uri,
    val fileName: String,
    val fileSizeBytes: Long,
    val mimeType: String
)

object SecurityScannerEngine {

    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "is.gd", "rb.gy", "cutt.ly",
        "shorturl.at", "ow.ly", "tiny.cc", "rebrand.ly", "v.gd"
    )

    private val highRiskTlds = setOf(
        ".top", ".xyz", ".zip", ".mov", ".icu", ".buzz", ".gq", ".tk",
        ".ml", ".cf", ".rest", ".loan", ".click", ".work", ".cam"
    )

    private val brandTargets = listOf(
        "paypal", "amazon", "chase", "citibank", "wellsfargo", "hdfc", "sbi",
        "icici", "axisbank", "binance", "coinbase", "microsoft", "apple",
        "google", "netflix", "instagram", "telegram", "whatsapp", "dhl", "fedex"
    )

    fun extractFileInfo(context: Context, uri: Uri): PickedFileInfo {
        var name = "selected_file"
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {
        }
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        return PickedFileInfo(uri = uri, fileName = name, fileSizeBytes = size, mimeType = mime)
    }

    // 1. URL / LINK SCANNER
    fun analyzeUrl(rawInput: String, isDemo: Boolean): ScanReportEntity {
        val trimmed = rawInput.trim()
        val normalized = if (!trimmed.startsWith("http://", true) && !trimmed.startsWith("https://", true)) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val parsedHost = try {
            URI(normalized).host?.lowercase()?.removePrefix("www.") ?: trimmed.lowercase()
        } catch (_: Exception) {
            trimmed.substringAfter("://").substringBefore("/").lowercase()
        }

        var score = 8
        val evidence = mutableListOf<String>()
        val suspiciousTokens = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        if (trimmed.startsWith("http://", ignoreCase = true)) {
            score += 22
            evidence.add("Unencrypted HTTP protocol detected — connection lacks TLS transport encryption.")
            suspiciousTokens.add("http://")
        } else {
            evidence.add("HTTPS protocol prefix present (Note: phishing sites also frequently use free HTTPS certificates).")
        }

        // Check IP literal host
        if (Regex("""^\d{1,3}(\.\d{1,3}){3}$""").matches(parsedHost)) {
            score += 45
            evidence.add("Raw IPv4 address ($parsedHost) used instead of a registered domain name — common in phishing infrastructure.")
            suspiciousTokens.add(parsedHost)
        }

        // Check "@" credential trick in URL
        if (trimmed.contains("@")) {
            score += 35
            evidence.add("URL contains '@' symbol, which can disguise the true destination host in browsers.")
            suspiciousTokens.add("@")
        }

        // Check punycode / homograph
        if (parsedHost.contains("xn--")) {
            score += 40
            evidence.add("Internationalized Punycode domain ('xn--') detected — frequently exploited for visual homograph impersonation.")
            suspiciousTokens.add("xn--")
        }

        // Check URL shorteners
        if (urlShorteners.any { parsedHost == it || parsedHost.endsWith(".$it") }) {
            score += 32
            evidence.add("URL shortener service ($parsedHost) masks the final destination domain and redirect chain.")
            suspiciousTokens.add(parsedHost)
        }

        // Check high-risk TLDs
        val matchedTld = highRiskTlds.firstOrNull { parsedHost.endsWith(it) }
        if (matchedTld != null) {
            score += 26
            evidence.add("Domain uses a high-abuse top-level domain ('$matchedTld') frequently associated with disposable scam domains.")
            suspiciousTokens.add(matchedTld)
        }

        // Check excessive subdomains or hyphens
        val dotCount = parsedHost.count { it == '.' }
        val hyphenCount = parsedHost.count { it == '-' }
        if (dotCount >= 3) {
            score += 18
            evidence.add("Unusual multi-level subdomain nesting ($dotCount dots in host) often used to mimic legitimate portals.")
        }
        if (hyphenCount >= 2) {
            score += 18
            evidence.add("Multiple hyphens ($hyphenCount) in domain name — typical of lookalike login domains.")
        }

        // Check brand lookalike / typosquatting patterns
        val normalisedLookalike = parsedHost
            .replace('0', 'o')
            .replace('1', 'l')
            .replace('3', 'e')
            .replace('5', 's')
        for (brand in brandTargets) {
            val baseDomain = parsedHost.substringBeforeLast(".")
            val containsBrandOrLookalike = parsedHost.contains(brand) || normalisedLookalike.contains(brand)
            val isOfficialRoot = parsedHost == "$brand.com" || parsedHost.endsWith(".$brand.com") ||
                parsedHost == "$brand.org" || parsedHost == "$brand.in" || parsedHost == "$brand.co"
            if (containsBrandOrLookalike && !isOfficialRoot && baseDomain != brand) {
                score += 38
                evidence.add("Potential brand impersonation or typosquatting targeting '${brand.uppercase()}' on non-official host '$parsedHost'.")
                suspiciousTokens.add(parsedHost)
                break
            }
        }

        // Check deceptive path/query keywords
        val phishingKeywords = listOf(
            "verify", "kyc", "update-account", "login", "secure", "suspend",
            "wallet", "claim", "reward", "bonus", "unlock", "otp", "pan-update", "refund"
        )
        val matchedKeywords = phishingKeywords.filter { trimmed.lowercase().contains(it) }
        if (matchedKeywords.isNotEmpty()) {
            score += (matchedKeywords.size * 12).coerceAtMost(32)
            evidence.add("Deceptive urgency/credential keywords found in URL: ${matchedKeywords.joinToString(", ")}.")
            suspiciousTokens.addAll(matchedKeywords)
        }

        val finalScore = score.coerceIn(5, 96)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 32 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        if (evidence.size <= 1 && status == RiskStatus.SAFE) {
            evidence.add("Domain structure '$parsedHost' shows no obvious typosquatting, IP obfuscation, or deceptive phishing tokens.")
        }

        when (status) {
            RiskStatus.HIGH_RISK -> {
                recommendations.add("Do NOT open or interact with this URL.")
                recommendations.add("Never enter login credentials, OTPs, or banking details on '$parsedHost'.")
                recommendations.add("Navigate to the institution's official website manually or via its verified app.")
            }
            RiskStatus.SUSPICIOUS -> {
                recommendations.add("Avoid clicking this link directly from an unsolicited message.")
                recommendations.add("Verify the sender and destination domain independently before proceeding.")
                recommendations.add("If this is a shortened URL, inspect the destination carefully without logging in.")
            }
            RiskStatus.SAFE -> {
                recommendations.add("No obvious structural threats found, but always verify the sender before entering credentials.")
                recommendations.add("Ensure Two-Factor Authentication (2FA) is active on your accounts.")
            }
        }

        return ScanReportEntity(
            scannerType = ScannerType.URL_LINK.name,
            submittedContent = trimmed,
            parsedSubject = "Domain: $parsedHost",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = when (status) {
                RiskStatus.HIGH_RISK -> "Phishing / Deceptive Domain"
                RiskStatus.SUSPICIOUS -> "Unverified / Obfuscated Link"
                RiskStatus.SAFE -> "Standard URL Structure"
            },
            summary = when (status) {
                RiskStatus.HIGH_RISK -> "High-risk phishing and domain spoofing indicators detected on '$parsedHost'."
                RiskStatus.SUSPICIOUS -> "Suspicious structural signals found in '$parsedHost'. Exercise caution."
                RiskStatus.SAFE -> "No obvious structural phishing indicators detected in '$parsedHost'."
            },
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = suspiciousTokens.distinct().joinToString("||"),
            detectionMethod = if (isDemo) "DEMO Simulation + Structural URL Heuristics" else "On-Device Structural URL & Typosquatting Heuristics",
            confidenceNote = "Limitation: Structural URL heuristics inspect lexical patterns, lookalike characters, and TLD risk without opening or executing the URL. No live external domain reputation database was contacted. Automated scores are not calibrated probabilities and never guarantee 100% safety.",
            recommendationsPipeSeparated = recommendations.joinToString("||"),
            isDemo = isDemo
        )
    }

    // 2. SOCIAL PROFILE SCANNER
    fun analyzeSocialProfile(
        platform: String,
        handleOrUrl: String,
        bioOrContext: String,
        isDemo: Boolean
    ): ScanReportEntity {
        val cleanHandle = handleOrUrl.trim().removePrefix("@")
        val combined = "$cleanHandle $bioOrContext".lowercase()
        var score = 10
        val evidence = mutableListOf<String>()
        val suspiciousPhrases = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        // Check lookalike characters / excessive underscores / trailing digits
        val underscoreCount = cleanHandle.count { it == '_' || it == '.' }
        val digitCount = cleanHandle.count { it.isDigit() }
        if (underscoreCount >= 3) {
            score += 20
            evidence.add("Handle '$cleanHandle' contains $underscoreCount separators ('_' or '.'), a frequent pattern in impersonator accounts.")
            suspiciousPhrases.add(cleanHandle)
        }
        if (digitCount >= 4) {
            score += 18
            evidence.add("Handle contains $digitCount numeric digits, common in mass-generated or lookalike support accounts.")
        }

        val impersonationKeywords = listOf(
            "official", "support", "helpdesk", "customer_care", "customercare",
            "helpline", "refund", "admin", "verify", "giveaway", "airdrop"
        )
        val matchedHandleWords = impersonationKeywords.filter { cleanHandle.lowercase().contains(it) }
        if (matchedHandleWords.isNotEmpty()) {
            score += 28
            evidence.add("Handle contains high-risk authority/impersonation terms: ${matchedHandleWords.joinToString(", ")}.")
            suspiciousPhrases.addAll(matchedHandleWords)
        }

        val scamBioPatterns = listOf(
            "dm for", "send dm", "guaranteed profit", "forex", "crypto recovery",
            "account recovery", "whatsapp", "t.me/", "bit.ly", "double your",
            "instant payout", "cashapp", "send fee", "official support desk"
        )
        val matchedBio = scamBioPatterns.filter { combined.contains(it) }
        if (matchedBio.isNotEmpty()) {
            score += (matchedBio.size * 18).coerceAtMost(45)
            evidence.add("Profile description/context contains solicitation or off-platform redirection signals: ${matchedBio.joinToString(", ")}.")
            suspiciousPhrases.addAll(matchedBio)
        }

        val finalScore = score.coerceIn(8, 95)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 35 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        if (evidence.isEmpty()) {
            evidence.add("Handle '@$cleanHandle' on $platform shows no obvious impersonation tokens or scam solicitation phrases in the provided input.")
        }

        when (status) {
            RiskStatus.HIGH_RISK -> {
                recommendations.add("Do NOT send direct messages, money, or account credentials to @$cleanHandle.")
                recommendations.add("Legitimate customer support rarely reaches out via unsolicited social DMs.")
                recommendations.add("Report this profile for impersonation/scam directly inside $platform.")
            }
            RiskStatus.SUSPICIOUS -> {
                recommendations.add("Verify whether @$cleanHandle is linked from the organization's official website.")
                recommendations.add("Avoid clicking external Telegram, WhatsApp, or shortened links in the bio.")
            }
            RiskStatus.SAFE -> {
                recommendations.add("Always verify identity through a secondary channel before conducting financial transactions.")
                recommendations.add("Never share OTPs or recovery codes in social media messages.")
            }
        }

        val displayInput = if (bioOrContext.isNotBlank()) {
            "[$platform] @$cleanHandle — Bio/Notes: $bioOrContext"
        } else {
            "[$platform] @$cleanHandle"
        }

        return ScanReportEntity(
            scannerType = ScannerType.SOCIAL_PROFILE.name,
            submittedContent = displayInput,
            parsedSubject = "$platform Profile: @$cleanHandle",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = when (status) {
                RiskStatus.HIGH_RISK -> "Profile Impersonation / Social Scam"
                RiskStatus.SUSPICIOUS -> "Unverified Social Account"
                RiskStatus.SAFE -> "Low-Risk Handle Pattern"
            },
            summary = when (status) {
                RiskStatus.HIGH_RISK -> "Strong impersonation or scam solicitation indicators detected for @$cleanHandle on $platform."
                RiskStatus.SUSPICIOUS -> "Suspicious username or bio indicators detected for @$cleanHandle on $platform."
                RiskStatus.SAFE -> "No obvious impersonation patterns found in the submitted $platform profile input."
            },
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = suspiciousPhrases.distinct().joinToString("||"),
            detectionMethod = if (isDemo) "DEMO Simulation + Social Handle & Bio Heuristics" else "Public Input & Handle Impersonation Heuristics",
            confidenceNote = "Limitation: Analysis evaluates only the username and profile text you provided. ScamShield AI does not inspect private $platform profiles, nor does it fabricate follower counts, account creation dates, or verification badges without live platform API authorization.",
            recommendationsPipeSeparated = recommendations.joinToString("||"),
            isDemo = isDemo
        )
    }

    // 3. OTP / MESSAGE SCANNER
    fun analyzeOtpMessage(rawMessage: String, isDemo: Boolean): ScanReportEntity {
        val text = rawMessage.trim()
        val lower = text.lowercase()
        var score = 6
        val evidence = mutableListOf<String>()
        val suspiciousPhrases = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        val otpTheftPhrases = listOf(
            "share otp", "send otp", "tell me the otp", "provide otp", "enter otp",
            "verification code", "share your pin", "cvv", "password", "secret code"
        )
        val matchedOtp = otpTheftPhrases.filter { lower.contains(it) }
        if (matchedOtp.isNotEmpty()) {
            score += 42
            evidence.add("Direct request to disclose an OTP, PIN, or secret verification code detected.")
            suspiciousPhrases.addAll(matchedOtp)
        } else if (lower.contains("otp")) {
            score += 15
            evidence.add("Message references an OTP (One-Time Password). Verify whether someone is asking you to forward it.")
            suspiciousPhrases.add("OTP")
        }

        val urgencyPhrases = listOf(
            "account will be blocked", "suspended immediately", "within 24 hours",
            "urgent action", "kyc expired", "update pan", "sim card block",
            "electricity disconnected", "power cut tonight", "account frozen"
        )
        val matchedUrgency = urgencyPhrases.filter { lower.contains(it) }
        if (matchedUrgency.isNotEmpty()) {
            score += 34
            evidence.add("Coercive urgency / account suspension threat designed to cause panic: ${matchedUrgency.joinToString(", ")}.")
            suspiciousPhrases.addAll(matchedUrgency)
        }

        val prizeLotteryPhrases = listOf(
            "won a lottery", "lucky draw", "cash prize", "claim your reward",
            "congratulations you won", "free gift", "processing fee"
        )
        val matchedPrize = prizeLotteryPhrases.filter { lower.contains(it) }
        if (matchedPrize.isNotEmpty()) {
            score += 32
            evidence.add("Prize, lottery, or reward lure detected: ${matchedPrize.joinToString(", ")}.")
            suspiciousPhrases.addAll(matchedPrize)
        }

        // Embedded URL check
        val urlRegex = Regex("""(https?://\S+|www\.\S+|\b[a-z0-9.-]+\.(top|xyz|ly|in|com|net|org|icu|click)/\S*)""", RegexOption.IGNORE_CASE)
        val foundUrls = urlRegex.findAll(text).map { it.value }.toList()
        if (foundUrls.isNotEmpty()) {
            score += 24
            evidence.add("Embedded link(s) detected inside message: ${foundUrls.joinToString(", ")}.")
            suspiciousPhrases.addAll(foundUrls)
        }

        val finalScore = score.coerceIn(5, 97)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 32 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        if (evidence.isEmpty()) {
            evidence.add("No explicit OTP solicitation, account-blocking threats, or suspicious links detected in the message text.")
        }

        when (status) {
            RiskStatus.HIGH_RISK -> {
                recommendations.add("NEVER share an OTP, PIN, CVV, or password with anyone for any reason.")
                recommendations.add("Do NOT click any links or call phone numbers listed inside this message.")
                recommendations.add("Contact your bank or service provider directly using their official app or website.")
            }
            RiskStatus.SUSPICIOUS -> {
                recommendations.add("Treat urgent KYC, utility, or delivery notifications with caution.")
                recommendations.add("Verify the claim by logging into the official app independently without clicking message links.")
            }
            RiskStatus.SAFE -> {
                recommendations.add("Even if a message looks routine, never forward one-time verification codes to anyone.")
            }
        }

        return ScanReportEntity(
            scannerType = ScannerType.OTP_MESSAGE.name,
            submittedContent = text,
            parsedSubject = "Message (${text.length} chars)",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = when (status) {
                RiskStatus.HIGH_RISK -> "OTP Theft / Smishing Attack"
                RiskStatus.SUSPICIOUS -> "Suspicious Urgent Message"
                RiskStatus.SAFE -> "Standard Text Message"
            },
            summary = when (status) {
                RiskStatus.HIGH_RISK -> "High-risk social engineering and OTP/credential phishing patterns detected."
                RiskStatus.SUSPICIOUS -> "Suspicious urgency or verification prompts found in message."
                RiskStatus.SAFE -> "No obvious smishing or OTP theft patterns detected."
            },
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = suspiciousPhrases.distinct().joinToString("||"),
            detectionMethod = if (isDemo) "DEMO Simulation + Smishing & OTP NLP Heuristics" else "On-Device Smishing & Social Engineering Heuristics",
            confidenceNote = "Limitation: Text analysis inspects linguistic urgency, credential requests, and embedded URLs. Scammers constantly evolve phrasing, so a low risk score does not guarantee 100% safety.",
            recommendationsPipeSeparated = recommendations.joinToString("||"),
            isDemo = isDemo
        )
    }

    // 4. BANK & LOAN SCAM DETECTOR
    fun analyzeBankLoanOffer(rawOffer: String, isDemo: Boolean): ScanReportEntity {
        val text = rawOffer.trim()
        val lower = text.lowercase()
        var score = 7
        val evidence = mutableListOf<String>()
        val suspiciousPhrases = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        val upfrontFeeSignals = listOf(
            "processing fee", "advance fee", "insurance fee", "gst fee",
            "pay upfront", "security deposit", "noc charge", "clearance fee",
            "registration fee", "transfer fee before"
        )
        val matchedFees = upfrontFeeSignals.filter { lower.contains(it) }
        if (matchedFees.isNotEmpty()) {
            score += 42
            evidence.add("Upfront fee demand detected (${matchedFees.joinToString(", ")}) — legitimate lenders deduct fees from disbursed funds rather than demanding advance transfers.")
            suspiciousPhrases.addAll(matchedFees)
        }

        val unrealisticLoanSignals = listOf(
            "no credit check", "no cibil", "0% interest", "instant approval in 5",
            "guaranteed loan", "pre-approved loan", "without documents", "100% guaranteed return",
            "double your money", "daily 10% profit"
        )
        val matchedLoan = unrealisticLoanSignals.filter { lower.contains(it) }
        if (matchedLoan.isNotEmpty()) {
            score += 34
            evidence.add("Unrealistic loan approval or investment guarantee detected: ${matchedLoan.joinToString(", ")}.")
            suspiciousPhrases.addAll(matchedLoan)
        }

        val offChannelSignals = listOf(
            "whatsapp", "telegram", "@gmail.com", "@yahoo.com", "@outlook.com",
            "personal upi", "gift card", "usdt", "crypto wallet"
        )
        val matchedChannel = offChannelSignals.filter { lower.contains(it) }
        if (matchedChannel.isNotEmpty()) {
            score += 26
            evidence.add("Unverified contact or payment channel used (${matchedChannel.joinToString(", ")}) — regulated banks do not use free webmail or personal chat apps for loan underwriting.")
            suspiciousPhrases.addAll(matchedChannel)
        }

        val urlRegex = Regex("""(https?://\S+|www\.\S+)""", RegexOption.IGNORE_CASE)
        val urls = urlRegex.findAll(text).map { it.value }.toList()
        if (urls.isNotEmpty()) {
            score += 18
            evidence.add("Associated URL(s) found in financial message: ${urls.joinToString(", ")}.")
            suspiciousPhrases.addAll(urls)
        }

        val finalScore = score.coerceIn(6, 97)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 32 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        val financialCategory = when {
            matchedFees.isNotEmpty() -> "Advance-Fee Loan Fraud"
            matchedLoan.isNotEmpty() -> "Predatory Loan / Investment Lure"
            matchedChannel.isNotEmpty() -> "Bank / Lender Impersonation"
            else -> "General Financial Notice"
        }

        if (evidence.isEmpty()) {
            evidence.add("No upfront fee demands, guaranteed-approval lures, or personal webmail/UPI payment instructions detected.")
        }

        when (status) {
            RiskStatus.HIGH_RISK -> {
                recommendations.add("NEVER pay an upfront processing, insurance, or clearance fee to receive a loan.")
                recommendations.add("Do not upload identity documents or install unverified loan apps linked via SMS/WhatsApp.")
                recommendations.add("Verify the lender on your central bank's official registered financial institution directory.")
            }
            RiskStatus.SUSPICIOUS -> {
                recommendations.add("Independently contact the bank using the phone number on your physical card or official website.")
                recommendations.add("Review all interest rates, regulatory registration numbers, and fee disclosures carefully.")
            }
            RiskStatus.SAFE -> {
                recommendations.add("Even when a financial message appears standard, verify transactions inside your official banking app.")
            }
        }

        return ScanReportEntity(
            scannerType = ScannerType.BANK_LOAN.name,
            submittedContent = text,
            parsedSubject = "Category: $financialCategory",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = financialCategory,
            summary = when (status) {
                RiskStatus.HIGH_RISK -> "High-risk financial fraud indicators ($financialCategory) detected."
                RiskStatus.SUSPICIOUS -> "Suspicious lending or payment terms found ($financialCategory)."
                RiskStatus.SAFE -> "No obvious advance-fee or predatory loan scam signals detected."
            },
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = suspiciousPhrases.distinct().joinToString("||"),
            detectionMethod = if (isDemo) "DEMO Simulation + Financial Fraud Heuristics" else "On-Device Financial & Loan Scam Heuristics",
            confidenceNote = "Limitation: ScamShield AI analyzes message text for advance-fee fraud and impersonation patterns. It does not independently verify a lender's regulatory license against live central-bank databases.",
            recommendationsPipeSeparated = recommendations.joinToString("||"),
            isDemo = isDemo
        )
    }

    // 5. IMAGE AUTHENTICITY SCANNER
    suspend fun analyzeImageAuthenticity(
        context: Context,
        fileInfo: PickedFileInfo?,
        isDemo: Boolean
    ): ScanReportEntity = withContext(Dispatchers.IO) {
        if (isDemo || fileInfo == null) {
            return@withContext ScanReportEntity(
                scannerType = ScannerType.IMAGE_AUTH.name,
                submittedContent = "DEMO_AI_SYNTHETIC_RECEIPT_892.png (1024x1024, Stripped EXIF)",
                parsedSubject = "Image: DEMO_AI_SYNTHETIC_RECEIPT_892.png",
                riskScore = 76,
                riskStatus = RiskStatus.HIGH_RISK.name,
                threatCategory = "Synthetic / Manipulated Image Indicators (DEMO)",
                summary = "DEMO Assessment: Image exhibits exact 1024x1024 diffusion resolution, stripped camera EXIF metadata, and synthetic rendering markers.",
                evidencePipeSeparated = listOf(
                    "DEMO File: Exact 1024x1024 square aspect ratio characteristic of standard AI image generators.",
                    "Complete absence of camera Make/Model/Exposure EXIF metadata headers.",
                    "Filename and metadata profile match synthetic or edited screenshot patterns."
                ).joinToString("||"),
                suspiciousPhrasesPipeSeparated = "1024x1024 Square Grid||Missing Camera EXIF||Synthetic Texture Profile",
                detectionMethod = "DEMO Simulation — Image Metadata & Resolution Forensics",
                confidenceNote = "Limitation: Neither visual inspection nor missing EXIF metadata can definitively prove an image is 100% fake or 100% authentic. Messaging apps routinely strip EXIF headers.",
                recommendationsPipeSeparated = listOf(
                    "Do not rely on screenshots of bank receipts as proof of payment — check your actual bank balance.",
                    "Run a reverse image search to check if this photo was taken from stock or AI repositories.",
                    "Verify urgent identity or payment claims via a direct voice/video call."
                ).joinToString("||"),
                isDemo = true
            )
        }

        var score = 18
        val evidence = mutableListOf<String>()
        val indicators = mutableListOf<String>()
        var width = 0
        var height = 0

        val bitmap = try {
            context.contentResolver.openInputStream(fileInfo.uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }

        if (bitmap != null) {
            width = bitmap.width
            height = bitmap.height
            evidence.add("Decoded image dimensions: ${width}x${height} px (${fileInfo.fileSizeBytes / 1024} KB).")

            val aiSquareSizes = setOf(512, 768, 1024, 1536, 2048)
            if (width == height && width in aiSquareSizes) {
                score += 28
                evidence.add("Exact square resolution (${width}x${height}) matches common AI diffusion generator output dimensions (512/1024/2048 px).")
                indicators.add("${width}x${height} AI Grid Dimension")
            }
        } else {
            score += 20
            evidence.add("Could not decode standard bitmap stream from '${fileInfo.fileName}'.")
        }

        // Inspect raw header bytes for EXIF / Software markers
        try {
            context.contentResolver.openInputStream(fileInfo.uri)?.use { stream ->
                val headerBytes = ByteArray(16384)
                val read = stream.read(headerBytes)
                if (read > 0) {
                    val asciiHeader = String(headerBytes, 0, read, Charsets.ISO_8859_1)
                    val hasExif = asciiHeader.contains("Exif", ignoreCase = true)
                    val aiTools = listOf("Midjourney", "Stable Diffusion", "DALL", "ComfyUI", "Adobe Firefly", "Photoshop", "Canva", "GIMP")
                    val matchedTool = aiTools.firstOrNull { asciiHeader.contains(it, ignoreCase = true) }

                    if (matchedTool != null) {
                        score += 36
                        evidence.add("Embedded software/generation signature detected in file header: '$matchedTool'.")
                        indicators.add("Software Tag: $matchedTool")
                    } else if (!hasExif) {
                        score += 16
                        evidence.add("No camera EXIF capture metadata (Make/Model/Timestamp) found in file header. (Note: social apps also strip EXIF).")
                        indicators.add("Missing Camera EXIF")
                    } else {
                        evidence.add("Standard EXIF header block present in image container.")
                    }
                }
            }
        } catch (_: Exception) {
        }

        val lowerName = fileInfo.fileName.lowercase()
        if (lowerName.contains("ai") || lowerName.contains("generated") || lowerName.contains("midjourney") || lowerName.contains("fake")) {
            score += 25
            evidence.add("Filename '${fileInfo.fileName}' contains synthetic/editing keywords.")
            indicators.add(fileInfo.fileName)
        }

        // Optional Gemini multimodal check if API key is configured and bitmap was decoded
        if (bitmap != null && GeminiServiceClient.isApiKeyConfigured()) {
            val aiNote = GeminiServiceClient.inspectImageWithAiIfAvailable(bitmap, evidence.joinToString("; "))
            if (!aiNote.isNullOrBlank()) {
                evidence.add("Gemini Vision Analysis: $aiNote")
            }
        }

        val finalScore = score.coerceIn(12, 92)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 34 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        val recommendations = listOf(
            "Never treat a screenshot of a payment receipt as settled funds — verify directly in your bank account.",
            "Remember that missing EXIF metadata can occur from normal WhatsApp/Instagram compression and is not definitive proof of forgery.",
            "Corroborate high-stakes images using reverse image search and trusted original sources."
        )

        ScanReportEntity(
            scannerType = ScannerType.IMAGE_AUTH.name,
            submittedContent = "${fileInfo.fileName} (${width}x${height} px, ${fileInfo.fileSizeBytes / 1024} KB)",
            parsedSubject = "Image: ${fileInfo.fileName}",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = when (status) {
                RiskStatus.HIGH_RISK -> "High Manipulation / Synthetic Indicators"
                RiskStatus.SUSPICIOUS -> "Metadata Anomaly / Unverified Media"
                RiskStatus.SAFE -> "Standard Image Container"
            },
            summary = "Analyzed '${fileInfo.fileName}' (${width}x${height} px) for EXIF headers, software signatures, and resolution anomalies.",
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = indicators.ifEmpty { listOf("Unverified Source Origin") }.joinToString("||"),
            detectionMethod = if (GeminiServiceClient.isApiKeyConfigured()) "Container EXIF Forensics + Gemini Multimodal Vision" else "On-Device EXIF Header & Resolution Forensics",
            confidenceNote = "Confidence Limitation: Automated analysis cannot prove an image is 100% authentic or 100% fake based solely on visual appearance or missing metadata.",
            recommendationsPipeSeparated = recommendations.joinToString("||"),
            isDemo = false
        )
    }

    // 6. VIDEO / DEEPFAKE SCANNER
    suspend fun analyzeVideoDeepfake(
        context: Context,
        fileInfo: PickedFileInfo?,
        isDemo: Boolean
    ): ScanReportEntity = withContext(Dispatchers.IO) {
        if (isDemo || fileInfo == null) {
            return@withContext ScanReportEntity(
                scannerType = ScannerType.VIDEO_DEEPFAKE.name,
                submittedContent = "DEMO_CEO_URGENT_WIRE_TRANSFER.mp4 (00:18s, 720p)",
                parsedSubject = "Video: DEMO_CEO_URGENT_WIRE_TRANSFER.mp4",
                riskScore = 82,
                riskStatus = RiskStatus.HIGH_RISK.name,
                threatCategory = "Preliminary Synthetic Video / Deepfake Alert (DEMO)",
                summary = "DEMO Preliminary Assessment: Frame-rate jitter and re-encoded audio/video container anomalies flagged at 00:04s and 00:11s.",
                evidencePipeSeparated = listOf(
                    "Timeline Segment [00:00s - 00:04s]: Baseline container initialization — stripped hardware encoder tags.",
                    "Timeline Segment [00:04s - 00:09s]: Temporal facial boundary & lip-sync cadence anomaly (Simulated Demo Signal).",
                    "Timeline Segment [00:11s - 00:15s]: Audio-visual bitrate discontinuity consistent with voice-cloning splice."
                ).joinToString("||"),
                suspiciousPhrasesPipeSeparated = "00:04s Boundary Flicker (DEMO)||00:11s Audio Splice (DEMO)||Stripped Encoder Metadata",
                detectionMethod = "DEMO Simulation — Preliminary Video Container & Frame Heuristics",
                confidenceNote = "Limitation: Full cloud neural deepfake-detection infrastructure is not active in local standalone mode; this is a clearly labelled preliminary/demo assessment.",
                recommendationsPipeSeparated = listOf(
                    "Do not authorize financial transfers or share sensitive data based solely on a video message.",
                    "Call the person back on a known, trusted phone number and ask an unscripted verification question.",
                    "Check official channels to verify public announcements or celebrity giveaway videos."
                ).joinToString("||"),
                isDemo = true
            )
        }

        var score = 22
        val evidence = mutableListOf<String>()
        val timelineMarkers = mutableListOf<String>()
        var durationMs = 0L
        var width = 0
        var height = 0

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, fileInfo.uri)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val writer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
            val date = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)

            val durationSec = (durationMs / 1000).coerceAtLeast(1)
            evidence.add("Preliminary Container Inspection: ${width}x${height} px, duration ${durationSec}s, size ${fileInfo.fileSizeBytes / 1024} KB.")

            if (date.isNullOrBlank()) {
                score += 15
                evidence.add("Timeline [00:00s]: Capture timestamp metadata is missing from the video container.")
                timelineMarkers.add("00:00s Missing Capture Date")
            }
            if (!writer.isNullOrBlank()) {
                evidence.add("Container muxer/writer tag: '$writer'.")
                if (writer.contains("Lavf", ignoreCase = true) || writer.contains("ffmpeg", ignoreCase = true)) {
                    score += 18
                    evidence.add("Timeline [00:${(durationSec / 3).toString().padStart(2, '0')}s]: Video was processed/re-encoded with FFmpeg/Lavf pipeline rather than raw camera output.")
                    timelineMarkers.add("FFmpeg/Lavf Re-encode")
                }
            }
            if (bitrate in 1..450_000 && width >= 720) {
                score += 16
                evidence.add("Timeline [00:${(durationSec / 2).toString().padStart(2, '0')}s]: Unusually low bitrate (${bitrate / 1000} kbps) for ${width}p video — heavy compression can mask facial manipulation artifacts.")
                timelineMarkers.add("Heavy Compression Artifact Risk")
            }
        } catch (_: Exception) {
            score += 15
            evidence.add("Could not extract complete media container metadata from '${fileInfo.fileName}'.")
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        val finalScore = score.coerceIn(18, 88)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 34 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        evidence.add("Preliminary Assessment Notice: On-device inspection checks container encoding, muxer signatures, and bitrate consistency. Full neural frame-by-frame deepfake inference requires dedicated cloud GPU infrastructure.")

        ScanReportEntity(
            scannerType = ScannerType.VIDEO_DEEPFAKE.name,
            submittedContent = "${fileInfo.fileName} (${width}x${height} px, ${durationMs / 1000}s)",
            parsedSubject = "Video: ${fileInfo.fileName}",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = "Preliminary Video Container & Encoding Assessment",
            summary = "Preliminary container and encoding inspection completed for '${fileInfo.fileName}'. See confidence limitations below.",
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = timelineMarkers.ifEmpty { listOf("Preliminary Container Scan Only") }.joinToString("||"),
            detectionMethod = "Preliminary On-Device MediaMetadataRetriever & Encoding Heuristics",
            confidenceNote = "Confidence Limitation: This is a preliminary on-device container and encoding assessment, not a full neural deepfake-detection model. Never treat a low risk score as proof that a video is 100% authentic.",
            recommendationsPipeSeparated = listOf(
                "Verify any urgent financial or sensitive request in a video via an independent voice call.",
                "Watch for unnatural blinking, lip-sync lag, or blurred edges around the jawline and hair.",
                "Check trusted news or official organization channels before sharing sensational clips."
            ).joinToString("||"),
            isDemo = false
        )
    }

    // 7. APP / APK RISK SCANNER
    suspend fun analyzeApkRisk(
        context: Context,
        fileInfo: PickedFileInfo?,
        isDemo: Boolean
    ): ScanReportEntity = withContext(Dispatchers.IO) {
        if (isDemo || fileInfo == null) {
            return@withContext ScanReportEntity(
                scannerType = ScannerType.APP_APK.name,
                submittedContent = "QuickBank_KYC_Update_v4.apk (Package: com.fastkyc.updater.spy)",
                parsedSubject = "APK: QuickBank_KYC_Update_v4.apk",
                riskScore = 93,
                riskStatus = RiskStatus.HIGH_RISK.name,
                threatCategory = "High-Risk Sideloaded APK / Spyware Permissions (DEMO)",
                summary = "DEMO Assessment: APK requests SMS interception, Accessibility control, and Contact harvesting permissions typical of banking trojans.",
                evidencePipeSeparated = listOf(
                    "Dangerous Permission: android.permission.READ_SMS & RECEIVE_SMS (Can intercept bank OTPs).",
                    "Dangerous Permission: android.permission.BIND_ACCESSIBILITY_SERVICE (Can read screen & automate clicks).",
                    "Dangerous Permission: android.permission.SYSTEM_ALERT_WINDOW (Can draw fake login overlays).",
                    "Unverified sideloaded package source outside official app store."
                ).joinToString("||"),
                suspiciousPhrasesPipeSeparated = "READ_SMS||RECEIVE_SMS||BIND_ACCESSIBILITY_SERVICE||SYSTEM_ALERT_WINDOW",
                detectionMethod = "DEMO Simulation — Static Package Archive & Permission Inspector",
                confidenceNote = "Limitation: Static APK inspection evaluates declared manifest permissions and package metadata without executing the untrusted APK.",
                recommendationsPipeSeparated = listOf(
                    "Do NOT install this APK file. Delete it from your device immediately.",
                    "Banks and government agencies never send '.apk' updates via WhatsApp, SMS, or Telegram.",
                    "Ensure Google Play Protect is enabled on your device."
                ).joinToString("||"),
                isDemo = true
            )
        }

        var score = 20
        val evidence = mutableListOf<String>()
        val flaggedPerms = mutableListOf<String>()

        // Copy URI to a temporary cache file to inspect with PackageManager.getPackageArchiveInfo (NEVER executing it)
        val tempFile = File(context.cacheDir, "inspect_${System.currentTimeMillis()}.apk")
        var parsedPkgName = fileInfo.fileName
        try {
            context.contentResolver.openInputStream(fileInfo.uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val pkgInfo = pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.GET_PERMISSIONS)

            if (pkgInfo != null) {
                parsedPkgName = pkgInfo.packageName ?: fileInfo.fileName
                evidence.add("Parsed APK Package Name: '$parsedPkgName' (Version: ${pkgInfo.versionName ?: "Unknown"}). Static inspection only — APK was NOT executed.")

                val requestedPerms = pkgInfo.requestedPermissions?.toList().orEmpty()
                if (requestedPerms.isEmpty()) {
                    evidence.add(" APK declares 0 runtime permissions in AndroidManifest.")
                } else {
                    evidence.add("APK declares ${requestedPerms.size} total permissions.")
                }

                val highRiskPermissionMap = mapOf(
                    "READ_SMS" to "Can read incoming SMS messages and steal bank OTPs",
                    "RECEIVE_SMS" to "Can intercept incoming SMS verification codes",
                    "SEND_SMS" to "Can silently send premium or phishing SMS from your number",
                    "BIND_ACCESSIBILITY_SERVICE" to "Can monitor screen content and perform automated taps",
                    "SYSTEM_ALERT_WINDOW" to "Can draw phishing overlays on top of banking apps",
                    "REQUEST_INSTALL_PACKAGES" to "Can prompt installation of additional malicious payloads",
                    "READ_CALL_LOG" to "Can harvest private call history",
                    "READ_CONTACTS" to "Can exfiltrate your address book for extortion/spam",
                    "RECORD_AUDIO" to "Can record microphone audio"
                )

                for (perm in requestedPerms) {
                    val shortPerm = perm.substringAfterLast(".")
                    val reason = highRiskPermissionMap[shortPerm]
                    if (reason != null) {
                        score += 22
                        evidence.add("High-Risk Permission '$shortPerm': $reason.")
                        flaggedPerms.add(shortPerm)
                    }
                }
            } else {
                score += 25
                evidence.add("File '${fileInfo.fileName}' could not be parsed as a valid signed Android Package archive.")
            }
        } catch (e: Exception) {
            score += 20
            evidence.add("Static archive inspection encountered an error: ${e.localizedMessage ?: "Invalid archive format"}.")
        } finally {
            try {
                if (tempFile.exists()) tempFile.delete()
            } catch (_: Exception) {
            }
        }

        // Sideloading baseline risk
        score += 15
        evidence.add("Sideloaded APK Warning: Installing APK files outside verified app stores bypasses store screening.")

        val finalScore = score.coerceIn(15, 97)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 34 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        ScanReportEntity(
            scannerType = ScannerType.APP_APK.name,
            submittedContent = "${fileInfo.fileName} (${fileInfo.fileSizeBytes / 1024} KB)",
            parsedSubject = "Package: $parsedPkgName",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = when (status) {
                RiskStatus.HIGH_RISK -> "High-Risk APK Permissions / Spyware Risk"
                RiskStatus.SUSPICIOUS -> "Unverified Sideloaded APK"
                RiskStatus.SAFE -> "Low-Permission APK Archive"
            },
            summary = "Static manifest and permission analysis for '${fileInfo.fileName}' ($parsedPkgName).",
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = flaggedPerms.ifEmpty { listOf("External APK Source") }.joinToString("||"),
            detectionMethod = "On-Device PackageManager Archive Inspector (Zero-Execution)",
            confidenceNote = "Limitation: Static permission inspection does not decompile obfuscated DEX bytecode or contact cloud antivirus engines. Never install an APK sent by an unknown contact.",
            recommendationsPipeSeparated = listOf(
                "Never install APK files received via SMS, WhatsApp, Telegram, or browser pop-ups.",
                "Never grant Accessibility Service or SMS permissions to utility or 'KYC update' apps.",
                "Install banking and financial apps exclusively from official app stores."
            ).joinToString("||"),
            isDemo = false
        )
    }

    // 8. PDF / DOCUMENT SCANNER
    suspend fun analyzeDocument(
        context: Context,
        fileInfo: PickedFileInfo?,
        isDemo: Boolean
    ): ScanReportEntity = withContext(Dispatchers.IO) {
        if (isDemo || fileInfo == null) {
            return@withContext ScanReportEntity(
                scannerType = ScannerType.PDF_DOC.name,
                submittedContent = "DEMO_Urgent_Invoice_Wire_Instruction_2026.pdf (148 KB)",
                parsedSubject = "Document: DEMO_Urgent_Invoice_Wire_Instruction_2026.pdf",
                riskScore = 88,
                riskStatus = RiskStatus.HIGH_RISK.name,
                threatCategory = "Fraudulent Invoice / Embedded Phishing URI (DEMO)",
                summary = "DEMO Assessment: Document contains embedded `/URI` redirection actions, urgent wire-transfer modification claims, and deceptive payment links.",
                evidencePipeSeparated = listOf(
                    "Embedded PDF Action Tag `/URI` detected pointing to an external login/payment portal.",
                    "Urgent payment language detected: 'Updated Bank Routing — Immediate Wire Required Within 2 Hours'.",
                    "Bank impersonation and advance-fee settlement terms identified in document stream."
                ).joinToString("||"),
                suspiciousPhrasesPipeSeparated = "/URI Action Tag||Immediate Wire Required||Updated Bank Routing||pay-verify-invoice.top",
                detectionMethod = "DEMO Simulation — Document Stream & URI Heuristic Scanner",
                confidenceNote = "Limitation: Document scanning inspects accessible text streams and PDF action tags. Absence of suspicious text never guarantees a document is 100% safe.",
                recommendationsPipeSeparated = listOf(
                    "Do not click links or scan QR codes embedded inside unsolicited PDF invoices.",
                    "Verify any change in vendor or bank wire instructions by calling a known contact person directly.",
                    "Never enable macros or external scripts in downloaded documents."
                ).joinToString("||"),
                isDemo = true
            )
        }

        var score = 16
        val evidence = mutableListOf<String>()
        val suspiciousTokens = mutableListOf<String>()

        try {
            context.contentResolver.openInputStream(fileInfo.uri)?.use { stream ->
                val maxBytes = 256 * 1024 // Read up to 256 KB safely
                val buffer = ByteArray(maxBytes)
                val bytesRead = stream.read(buffer)
                if (bytesRead > 0) {
                    val rawText = String(buffer, 0, bytesRead, Charsets.ISO_8859_1)
                    val lower = rawText.lowercase()

                    evidence.add("Scanned ${bytesRead / 1024} KB of accessible document stream from '${fileInfo.fileName}'.")

                    // Check PDF specific active/script tags
                    if (rawText.contains("/JavaScript", ignoreCase = true) || rawText.contains("/JS", ignoreCase = true)) {
                        score += 48
                        evidence.add("Embedded PDF `/JavaScript` tag detected — legitimate invoices and statements rarely require embedded scripts.")
                        suspiciousTokens.add("/JavaScript Tag")
                    }
                    if (rawText.contains("/Launch", ignoreCase = true) || rawText.contains("/EmbeddedFiles", ignoreCase = true)) {
                        score += 40
                        evidence.add("PDF `/Launch` or `/EmbeddedFiles` action tag detected — can trigger external file launching.")
                        suspiciousTokens.add("/Launch / EmbeddedFiles")
                    }
                    if (rawText.contains("/URI", ignoreCase = true)) {
                        score += 22
                        evidence.add("PDF contains clickable `/URI` hyperlink annotations.")
                        suspiciousTokens.add("/URI Hyperlink")
                    }

                    // Extract visible URLs in stream
                    val urlRegex = Regex("""https?://[a-zA-Z0-9./?=_-]{6,80}""")
                    val foundUrls = urlRegex.findAll(rawText).map { it.value }.distinct().take(4).toList()
                    if (foundUrls.isNotEmpty()) {
                        score += 20
                        evidence.add("Embedded URLs found in document stream: ${foundUrls.joinToString(", ")}.")
                        suspiciousTokens.addAll(foundUrls)
                    }

                    // Check financial scam keywords in accessible text
                    val docScamWords = listOf(
                        "wire transfer", "overdue invoice", "account suspended",
                        "processing fee", "verify kyc", "urgent payment", "bitcoin",
                        "gift card", "updated bank account", "lottery winner"
                    )
                    val matchedWords = docScamWords.filter { lower.contains(it) }
                    if (matchedWords.isNotEmpty()) {
                        score += (matchedWords.size * 15).coerceAtMost(38)
                        evidence.add("Suspicious financial or urgency phrases found in document: ${matchedWords.joinToString(", ")}.")
                        suspiciousTokens.addAll(matchedWords)
                    }
                }
            }
        } catch (e: Exception) {
            score += 14
            evidence.add("Could not extract plain text stream (${e.localizedMessage ?: "compressed/encrypted stream"}).")
        }

        evidence.add("Important Note: Compressed image-only PDFs or encrypted streams can hide visual text from raw stream inspection. Absence of suspicious text does NOT mean a document is safe.")

        val finalScore = score.coerceIn(15, 95)
        val status = when {
            finalScore >= 65 -> RiskStatus.HIGH_RISK
            finalScore >= 32 -> RiskStatus.SUSPICIOUS
            else -> RiskStatus.SAFE
        }

        ScanReportEntity(
            scannerType = ScannerType.PDF_DOC.name,
            submittedContent = "${fileInfo.fileName} (${fileInfo.fileSizeBytes / 1024} KB)",
            parsedSubject = "Document: ${fileInfo.fileName}",
            riskScore = finalScore,
            riskStatus = status.name,
            threatCategory = when (status) {
                RiskStatus.HIGH_RISK -> "High-Risk Document / Phishing Invoice"
                RiskStatus.SUSPICIOUS -> "Document with Embedded Links / Actions"
                RiskStatus.SAFE -> "No Obvious Stream Threats Found (Verify Origin)"
            },
            summary = "Inspected '${fileInfo.fileName}' for PDF script tags, clickable `/URI` links, and fraudulent payment instructions.",
            evidencePipeSeparated = evidence.joinToString("||"),
            suspiciousPhrasesPipeSeparated = suspiciousTokens.ifEmpty { listOf("Verify Sender Origin") }.joinToString("||"),
            detectionMethod = "On-Device PDF Action Tag & Text Stream Heuristics",
            confidenceNote = "Confidence Limitation: Never assume a scanned document is safe merely because no suspicious text was found. Scammers often embed text as flattened images inside PDFs.",
            recommendationsPipeSeparated = listOf(
                "Do not click hyperlinks or scan QR codes inside unexpected PDF attachments.",
                "Always verify invoice payment details or bank account changes via a trusted phone number.",
                "Keep your PDF viewer updated and disable automatic script execution."
            ).joinToString("||"),
            isDemo = false
        )
    }

    // DEMO SAMPLE DATA GENERATOR FOR EACH SCANNER (Used ONLY when user explicitly taps "Try Demo Scan")
    fun getDemoSampleText(scannerType: ScannerType): String {
        return when (scannerType) {
            ScannerType.URL_LINK -> "http://secure-paypa1-kyc-update.verify-login.top/account/unlock?id=8921"
            ScannerType.SOCIAL_PROFILE -> "official_support_helpdesk_9921"
            ScannerType.OTP_MESSAGE -> "URGENT ALERT: Your bank account will be blocked within 24 hours due to expired KYC. Share your 6-digit OTP immediately or click http://kyc-fast-verify.top to avoid suspension."
            ScannerType.BANK_LOAN -> "CONGRATULATIONS! Pre-approved Instant Loan of $25,000 at 0% interest with NO credit check! Pay an upfront processing fee & insurance fee of $199 via WhatsApp +1-555-0199 to disburse in 5 minutes."
            else -> ""
        }
    }

    fun getDemoSocialBio(): String {
        return "Official Customer Recovery Desk. DM us your registered phone number and recovery code for instant refund and crypto wallet unlock: http://bit.ly/support-claim"
    }
}
