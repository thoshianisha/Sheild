package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

enum class ScannerType(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val radarCategory: String
) {
    URL_LINK(
        id = "URL_LINK",
        title = "URL / Link Scanner",
        shortTitle = "URL Scanner",
        subtitle = "Detect phishing domains, typosquatting, redirects & fake portals",
        radarCategory = "Phishing"
    ),
    SOCIAL_PROFILE(
        id = "SOCIAL_PROFILE",
        title = "Social Profile Scanner",
        shortTitle = "Social Profile",
        subtitle = "Check Instagram, X, Facebook, LinkedIn & Telegram impersonators",
        radarCategory = "Fake Identities"
    ),
    OTP_MESSAGE(
        id = "OTP_MESSAGE",
        title = "OTP / Message Scanner",
        shortTitle = "OTP & SMS",
        subtitle = "Inspect SMS, WhatsApp texts, urgent KYC & OTP theft attempts",
        radarCategory = "Social Scams"
    ),
    BANK_LOAN(
        id = "BANK_LOAN",
        title = "Bank & Loan Scam Detector",
        shortTitle = "Bank & Loan",
        subtitle = "Identify predatory loans, upfront fee demands & bank fraud",
        radarCategory = "Financial Scams"
    ),
    IMAGE_AUTH(
        id = "IMAGE_AUTH",
        title = "Image Authenticity Scanner",
        shortTitle = "Image Auth",
        subtitle = "Examine photos for AI generation, EXIF anomalies & tampering",
        radarCategory = "Media Manipulation"
    ),
    VIDEO_DEEPFAKE(
        id = "VIDEO_DEEPFAKE",
        title = "Video / Deepfake Scanner",
        shortTitle = "Deepfake Video",
        subtitle = "Inspect video frames & metadata for synthetic media indicators",
        radarCategory = "Media Manipulation"
    ),
    APP_APK(
        id = "APP_APK",
        title = "App / APK Risk Scanner",
        shortTitle = "APK Risk",
        subtitle = "Analyze APK permissions, package signatures & spyware risks",
        radarCategory = "Malware Risk"
    ),
    PDF_DOC(
        id = "PDF_DOC",
        title = "PDF / Document Scanner",
        shortTitle = "PDF & Docs",
        subtitle = "Scan documents for embedded phishing links & fake invoices",
        radarCategory = "Phishing"
    )
}

enum class RiskStatus(val label: String, val bannerTitle: String) {
    SAFE("SAFE", "SAFE — NO OBVIOUS THREAT DETECTED"),
    SUSPICIOUS("SUSPICIOUS", "SUSPICIOUS CONTENT DETECTED"),
    HIGH_RISK("HIGH RISK", "THREAT DETECTED — DO NOT INTERACT")
}

@Entity(tableName = "scan_reports")
data class ScanReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scannerType: String,
    val submittedContent: String,
    val parsedSubject: String,
    val riskScore: Int, // 0..100
    val riskStatus: String, // SAFE, SUSPICIOUS, HIGH_RISK
    val threatCategory: String,
    val summary: String,
    val evidencePipeSeparated: String, // separated by "||"
    val suspiciousPhrasesPipeSeparated: String, // separated by "||"
    val detectionMethod: String,
    val confidenceNote: String,
    val recommendationsPipeSeparated: String, // separated by "||"
    val isDemo: Boolean,
    val isAlertRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    val evidenceList: List<String>
        get() = evidencePipeSeparated.split("||").filter { it.isNotBlank() }

    val suspiciousPhrasesList: List<String>
        get() = suspiciousPhrasesPipeSeparated.split("||").filter { it.isNotBlank() }

    val recommendationsList: List<String>
        get() = recommendationsPipeSeparated.split("||").filter { it.isNotBlank() }

    val statusEnum: RiskStatus
        get() = try {
            RiskStatus.valueOf(riskStatus)
        } catch (_: Exception) {
            RiskStatus.SAFE
        }

    val scannerEnum: ScannerType
        get() = try {
            ScannerType.valueOf(scannerType)
        } catch (_: Exception) {
            ScannerType.URL_LINK
        }
}

@Dao
interface ScanReportDao {
    @Query("SELECT * FROM scan_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ScanReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ScanReportEntity): Long

    @Query("UPDATE scan_reports SET isAlertRead = 1 WHERE id = :id")
    suspend fun markAlertRead(id: Long)

    @Query("UPDATE scan_reports SET isAlertRead = 1")
    suspend fun markAllAlertsRead()

    @Query("DELETE FROM scan_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM scan_reports")
    suspend fun clearAllReports()

    @Query("DELETE FROM scan_reports WHERE isDemo = 1")
    suspend fun clearDemoReports()
}

@Database(entities = [ScanReportEntity::class], version = 1, exportSchema = false)
abstract class ScamShieldDatabase : RoomDatabase() {
    abstract fun scanReportDao(): ScanReportDao
}

class ScanRepository(private val dao: ScanReportDao) {
    val allReports: Flow<List<ScanReportEntity>> = dao.getAllReports()

    suspend fun saveReport(report: ScanReportEntity): Long = dao.insertReport(report)
    suspend fun markAlertRead(id: Long) = dao.markAlertRead(id)
    suspend fun markAllAlertsRead() = dao.markAllAlertsRead()
    suspend fun deleteReport(id: Long) = dao.deleteReportById(id)
    suspend fun clearAll() = dao.clearAllReports()
    suspend fun clearDemos() = dao.clearDemoReports()
}
