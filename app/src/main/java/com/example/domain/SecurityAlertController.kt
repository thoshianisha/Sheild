package com.example.domain

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.example.data.RiskStatus
import com.example.data.UserSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class SecurityAlertController(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var buzzerJob: Job? = null
    private var activeToneGenerator: ToneGenerator? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isAlertPlaying = MutableStateFlow(false)
    val isAlertPlaying: StateFlow<Boolean> = _isAlertPlaying.asStateFlow()

    // Track scan IDs that have already triggered an alert so we never play more than once per scan result unless replayed
    private val triggeredReportIds = mutableSetOf<Long>()

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.US)
                    isTtsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                        result != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        } catch (_: Exception) {
            isTtsReady = false
        }
    }

    fun triggerAlertForResult(
        reportId: Long,
        status: RiskStatus,
        settings: UserSettings,
        forceReplay: Boolean = false
    ) {
        if (!forceReplay && triggeredReportIds.contains(reportId)) {
            return
        }
        triggeredReportIds.add(reportId)
        stopAlert()

        when (status) {
            RiskStatus.SAFE -> {
                if (settings.alertSoundEnabled) {
                    playSoftSafeTone()
                }
                if (settings.voiceFeedbackEnabled) {
                    speak("This content appears safe.")
                }
            }

            RiskStatus.SUSPICIOUS -> {
                if (settings.alertSoundEnabled) {
                    playSuspiciousTone()
                }
                if (settings.vibrationEnabled) {
                    triggerVibration(longArrayOf(0, 140, 100, 140))
                }
                if (settings.voiceFeedbackEnabled) {
                    speak("Caution. Suspicious content indicators detected. Review the evidence carefully.")
                }
            }

            RiskStatus.HIGH_RISK -> {
                _isAlertPlaying.value = true
                if (settings.vibrationEnabled) {
                    triggerVibration(longArrayOf(0, 300, 120, 300, 120, 450))
                }
                if (settings.alertSoundEnabled) {
                    playHighRiskBuzzerSequence()
                }
                if (settings.voiceFeedbackEnabled) {
                    speak("Alert. High-risk threat detected. Avoid interacting with this content.")
                }
                if (!settings.alertSoundEnabled && !settings.voiceFeedbackEnabled) {
                    // Automatically reset playing flag after pulse window if audio is muted
                    buzzerJob = scope.launch {
                        delay(2500)
                        _isAlertPlaying.value = false
                    }
                }
            }
        }
    }

    private fun playSoftSafeTone() {
        scope.launch(Dispatchers.Default) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 65)
                activeToneGenerator = toneGen
                toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                delay(220)
                toneGen.release()
                if (activeToneGenerator === toneGen) activeToneGenerator = null
            } catch (_: Exception) {
            }
        }
    }

    private fun playSuspiciousTone() {
        scope.launch(Dispatchers.Default) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                activeToneGenerator = toneGen
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 260)
                delay(300)
                toneGen.release()
                if (activeToneGenerator === toneGen) activeToneGenerator = null
            } catch (_: Exception) {
            }
        }
    }

    private fun playHighRiskBuzzerSequence() {
        buzzerJob?.cancel()
        buzzerJob = scope.launch(Dispatchers.Default) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 95)
                activeToneGenerator = toneGen
                repeat(3) {
                    if (!_isAlertPlaying.value) return@launch
                    toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 320)
                    delay(420)
                }
                toneGen.release()
                if (activeToneGenerator === toneGen) activeToneGenerator = null
            } catch (_: Exception) {
            } finally {
                // Keep alert button active if TTS is still speaking, else clear after short delay
                delay(1800)
                if (tts?.isSpeaking != true) {
                    _isAlertPlaying.value = false
                }
            }
        }
    }

    private fun speak(message: String) {
        if (!isTtsReady) return
        try {
            _isAlertPlaying.value = true
            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "scamshield_utterance")
            scope.launch {
                delay(4200)
                if (tts?.isSpeaking != true) {
                    _isAlertPlaying.value = false
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun triggerVibration(pattern: LongArray) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun stopAlert() {
        _isAlertPlaying.value = false
        buzzerJob?.cancel()
        buzzerJob = null
        try {
            activeToneGenerator?.stopTone()
            activeToneGenerator?.release()
            activeToneGenerator = null
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.cancel()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        stopAlert()
        try {
            tts?.shutdown()
        } catch (_: Exception) {
        }
    }
}
