package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = 0.3f,
    val topP: Float? = 0.9f,
    val topK: Int? = 40
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList()
)

@Serializable
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

data class ShieldAiResult(
    val text: String,
    val isLiveModel: Boolean,
    val errorNote: String? = null
)

object GeminiServiceClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiApiService::class.java)
    }

    fun isApiKeyConfigured(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotBlank() && key != "MY_GEMINI_API_KEY" && !key.startsWith("YOUR_")
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 78, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private val shieldSystemInstruction = Content(
        parts = listOf(
            Part(
                text = """
                You are Shield AI, the intelligent cybersecurity assistant built into ScamShield AI.
                Follow these strict ethical and technical principles:
                1. Explain digital risks, phishing links, OTP scams, fake bank/loan offers, impersonation profiles, APK permissions, and synthetic media in clear, accessible language.
                2. Clearly distinguish general AI cybersecurity guidance and pattern analysis from verified live threat-intelligence or institutional checks.
                3. Never claim a content item is 100% safe or 100% fake. Always note confidence limitations.
                4. Never ask users for real OTPs, PINs, passwords, or full bank account numbers. Remind users to redact sensitive data.
                5. Provide actionable, calm, step-by-step safety recommendations (e.g., contacting banks via official numbers, reporting fraud, revoking permissions).
                """.trimIndent()
            )
        )
    )

    suspend fun askShieldAi(
        conversationHistory: List<Pair<Boolean, String>>, // Pair(isUser, message)
        latestUserPrompt: String
    ): ShieldAiResult = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) {
            return@withContext ShieldAiResult(
                text = generateContextualOfflineResponse(latestUserPrompt),
                isLiveModel = false,
                errorNote = "Live Gemini API key not configured in AI Studio Secrets — using local Shield AI knowledge engine (Demo / Local Mode)."
            )
        }

        try {
            val contents = mutableListOf<Content>()
            conversationHistory.takeLast(8).forEach { (isUser, msg) ->
                contents.add(
                    Content(
                        role = if (isUser) "user" else "model",
                        parts = listOf(Part(text = msg))
                    )
                )
            }
            contents.add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = latestUserPrompt))
                )
            )

            val request = GenerateContentRequest(
                contents = contents,
                generationConfig = GenerationConfig(temperature = 0.35f),
                systemInstruction = shieldSystemInstruction
            )

            val response = apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
            val reply = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!reply.isNullOrBlank()) {
                ShieldAiResult(text = reply.trim(), isLiveModel = true)
            } else {
                ShieldAiResult(
                    text = generateContextualOfflineResponse(latestUserPrompt),
                    isLiveModel = false,
                    errorNote = "Empty response from remote AI model — fell back to local Shield AI analysis."
                )
            }
        } catch (e: Exception) {
            ShieldAiResult(
                text = generateContextualOfflineResponse(latestUserPrompt),
                isLiveModel = false,
                errorNote = "Cloud AI service unreachable (${e.localizedMessage ?: "Network/API error"}). Showing local Shield AI cybersecurity guidance."
            )
        }
    }

    suspend fun inspectImageWithAiIfAvailable(
        bitmap: Bitmap,
        localSummary: String
    ): String? = withContext(Dispatchers.IO) {
        if (!isApiKeyConfigured()) return@withContext null
        try {
            val b64 = bitmapToBase64(bitmap)
            val prompt = """
                Analyze this image for potential digital manipulation, AI-generation artifacts, scam screenshot fabrication (such as fake payment receipts or fake KYC notices), or visual inconsistencies.
                Local metadata check summary: $localSummary
                Provide 2-3 concise bullet points of visual observations and explicitly state that visual inspection alone cannot guarantee 100% authenticity.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        role = "user",
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = b64))
                        )
                    )
                ),
                generationConfig = GenerationConfig(temperature = 0.2f),
                systemInstruction = shieldSystemInstruction
            )
            val response = apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun generateContextualOfflineResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("otp") || lower.contains("pin") || lower.contains("password") -> """
                ### Critical OTP & Credential Safety Guidance
                *(Local Shield AI Engine — Demo / Offline Mode)*

                1. **Never Share Your OTP or PIN:** Legitimate banks, government agencies, telecom providers, and payment apps will **never** ask you to read back, forward, or type your One-Time Password (OTP) into an unverified link or chat.
                2. **Why Attackers Ask for It:** Scammers use social engineering (claiming your account will be blocked, KYC is expiring, or a refund is pending) to trick you into authorizing an instant fund transfer or account takeover.
                3. **Recommended Next Steps:**
                   - Stop communicating with the sender immediately.
                   - Do not click any links or call phone numbers inside the suspicious message.
                   - If you already shared an OTP or password, immediately lock your bank card/account via your official banking app and call the official fraud helpline printed on the back of your debit/credit card.
            """.trimIndent()

            lower.contains("url") || lower.contains("link") || lower.contains("phishing") || lower.contains("click") -> """
                ### Suspicious Link & Phishing Analysis
                *(Local Shield AI Engine — Demo / Offline Mode)*

                1. **How Phishing Links Work:** Attackers register lookalike domains (typosquatting like `paypa1-secure-verify.top`), use URL shorteners (`bit.ly`, `tinyurl`), or embed deceptive subdomains to mimic banks, delivery services, or login portals.
                2. **If You Already Clicked a Suspicious Link:**
                   - **Close the browser tab immediately** and do not enter any credentials or download any files.
                   - **If you entered a password:** Change that password right away on the official website from a clean session, and enable Two-Factor Authentication (2FA).
                   - **If a file or `.apk` downloaded:** Delete it immediately without opening or installing it.
                   - **Note on Verification:** Local heuristic checks inspect domain structure and deceptive keywords, but cannot verify live domain ownership without a real-time threat-intelligence lookup.
            """.trimIndent()

            lower.contains("loan") || lower.contains("bank") || lower.contains("kyc") || lower.contains("fee") -> """
                ### Bank & Predatory Loan Scam Indicators
                *(Local Shield AI Engine — Demo / Offline Mode)*

                1. **Upfront Fee Red Flag:** Legitimate regulated lenders deduct processing fees from the disbursed loan or disclose them transparently through official channels—they **never** demand upfront "insurance", "GST", or "clearance" fees via personal UPI/wallet transfers or gift cards.
                2. **No-Credit-Check Guarantees:** Offers promising "Instant $10,000 / ₹5,00,000 loan in 5 minutes with zero credit check" are classic advance-fee or predatory debt-trap scams.
                3. **Safe Verification:**
                   - Independently look up the bank or lender on your country's official central bank registry.
                   - Contact your bank only using the number on your physical bank card or official app.
            """.trimIndent()

            lower.contains("instagram") || lower.contains("profile") || lower.contains("impersonat") || lower.contains("social") -> """
                ### Identifying Fake Social Media Profiles
                *(Local Shield AI Engine — Demo / Offline Mode)*

                1. **Username Lookalikes:** Check for subtle extra underscores, numbers, or swapped letters (e.g., `official_support_help_01` or `1` instead of `l`).
                2. **Off-Platform Redirection:** Scammers frequently place Telegram, WhatsApp, or shortened crypto/giveaway links in their bio to move victims away from platform moderation.
                3. **Verification Limitations:** Automated profile checks only evaluate username patterns and provided bio text; they cannot inspect private accounts or verify real-world identity without platform API authentication.
            """.trimIndent()

            lower.contains("image") || lower.contains("deepfake") || lower.contains("video") || lower.contains("ai-generated") -> """
                ### Evaluating Potential AI-Generated or Manipulated Media
                *(Local Shield AI Engine — Demo / Offline Mode)*

                1. **Visual & Metadata Signals:** Missing EXIF camera metadata, unusual aspect ratios, asymmetric lighting/reflections, distorted background text, or unnatural lip-syncing in videos can indicate synthetic generation or editing.
                2. **Important Limitation:** Social media apps (WhatsApp, Instagram, Telegram) automatically strip EXIF metadata and re-compress media when shared. Therefore, missing metadata alone **does not** prove an image or video is fake, nor does clean metadata prove it is authentic.
                3. **Safe Action:** Perform a reverse image search and verify dramatic claims (especially emergency money requests from family/colleagues) via a direct voice call or pre-agreed family safe word.
            """.trimIndent()

            lower.contains("report") || lower.contains("fraud") -> """
                ### How to Report Online Fraud & Cybercrime
                *(Local Shield AI Engine — Demo / Offline Mode)*

                1. **Preserve Evidence First:** Take screenshots of the sender's phone number/handle, full URL, message timestamps, and any transaction reference numbers before blocking the scammer.
                2. **Contact Financial Institutions Immediately:** Call your bank's official fraud department to freeze unauthorized transfers or request a chargeback.
                3. **Official Reporting Channels:**
                   - Report phishing websites to Google Safe Browsing or your national CERT / Cybercrime portal (e.g., IC3.gov in the US, Action Fraud in the UK, Cybercrime.gov.in / 1930 in India, Scamwatch in Australia).
                   - Report the offending account directly inside WhatsApp, Instagram, Telegram, or X.
            """.trimIndent()

            else -> """
                ### Shield AI Cybersecurity Assessment
                *(Local Shield AI Engine — Demo / Offline Mode — Configure `GEMINI_API_KEY` in AI Studio Secrets for live cloud LLM reasoning)*

                Based on your query: **"$prompt"**

                - **Core Safety Principle:** Online scams rely on **artificial urgency**, **authority impersonation**, **too-good-to-be-true rewards**, or **off-platform payment/credential requests**.
                - **What You Should Do:**
                  1. Do not click embedded links, scan unknown QR codes to receive money, or install `.apk` files sent via messaging apps.
                  2. Never share OTPs, PINs, CVVs, or screen-sharing codes (AnyDesk/TeamViewer).
                  3. Use the **Scan** tab in ScamShield AI to run a dedicated heuristic inspection on the specific URL, SMS, Bank/Loan offer, Social Profile, Image, Video, APK, or PDF document.
            """.trimIndent()
        }
    }
}
