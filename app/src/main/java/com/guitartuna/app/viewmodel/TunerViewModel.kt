package com.guitartuna.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guitartuna.app.audio.AudioEngine
import com.guitartuna.app.audio.PitchDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class TunerState { IDLE, LISTENING, DETECTING, LOCKED, ERROR }

enum class TuningMode { AUTO, MANUAL }

data class TunerUiState(
    val tunerState: TunerState = TunerState.IDLE,
    val detectedNote: String = "--",
    val detectedFrequency: Float = 0f,
    val centsFromTarget: Float = 0f,
    val targetNote: String = "E2",
    val targetFrequency: Float = 82.41f,
    val mode: TuningMode = TuningMode.AUTO,
    val selectedStringIndex: Int = 0,
    val confidence: Float = 0f,
    val errorMessage: String = "",
)

class TunerViewModel : ViewModel() {

    private val engine = AudioEngine()
    private var captureJob: Job? = null

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState

    private var smoothedFrequency = 0f
    private var smoothedCents = 0f
    private val smoothingFactor = 0.25f
    private var inTuneFrameCount = 0
    private val inTuneThreshold = 2f
    private val lockFrames = 15

    fun isRunning(): Boolean = engine.isRunning

    fun startTuning() {
        if (engine.isRunning) return
        captureJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(tunerState = TunerState.LISTENING, errorMessage = "") }
                engine.start()
            } catch (e: SecurityException) {
                _uiState.update { it.copy(tunerState = TunerState.ERROR, errorMessage = "Microphone permission denied") }
            } catch (e: Exception) {
                _uiState.update { it.copy(tunerState = TunerState.ERROR, errorMessage = e.message ?: "Audio init failed") }
            }
        }

        viewModelScope.launch {
            engine.pitchResult.collect { result ->
                processPitchResult(result)
            }
        }
    }

    fun stopTuning() {
        captureJob?.cancel()
        captureJob = null
        engine.stop()
        smoothedFrequency = 0f
        smoothedCents = 0f
        inTuneFrameCount = 0
        _uiState.update { it.copy(tunerState = TunerState.IDLE, detectedNote = "--", detectedFrequency = 0f, centsFromTarget = 0f, confidence = 0f) }
    }

    fun selectString(index: Int) {
        val gs = PitchDetector.GUITAR_STRINGS.getOrElse(index) { PitchDetector.GUITAR_STRINGS[0] }
        _uiState.update {
            it.copy(
                mode = TuningMode.MANUAL,
                selectedStringIndex = index,
                targetNote = gs.name,
                targetFrequency = gs.frequency,
            )
        }
    }

    fun selectAuto() {
        _uiState.update { it.copy(mode = TuningMode.AUTO) }
    }

    private fun processPitchResult(result: PitchDetector.PitchResult) {
        val currentState = _uiState.value

        if (!result.detected) {
            if (currentState.tunerState != TunerState.LISTENING) {
                _uiState.update { it.copy(tunerState = TunerState.LISTENING, detectedNote = "--", detectedFrequency = 0f, centsFromTarget = 0f, confidence = 0f) }
            }
            inTuneFrameCount = 0
            return
        }

        // Exponential smoothing
        smoothedFrequency = if (smoothedFrequency == 0f) {
            result.frequency
        } else {
            smoothingFactor * result.frequency + (1 - smoothingFactor) * smoothedFrequency
        }

        // Determine target
        val state = _uiState.value
        val targetFreq: Float
        val targetNote: String

        if (state.mode == TuningMode.AUTO) {
            val nearest = PitchDetector.findNearestString(smoothedFrequency)
            targetFreq = nearest.frequency
            targetNote = nearest.name
        } else {
            targetFreq = state.targetFrequency
            targetNote = state.targetNote
        }

        val cents = 1200f * (kotlin.math.ln(smoothedFrequency / targetFreq) / kotlin.math.ln(2f))
        smoothedCents = smoothingFactor * cents + (1 - smoothingFactor) * smoothedCents

        // Lock detection
        if (abs(smoothedCents) <= inTuneThreshold) {
            inTuneFrameCount++
        } else {
            inTuneFrameCount = 0
        }

        val newState = when {
            inTuneFrameCount >= lockFrames -> TunerState.LOCKED
            abs(smoothedCents) <= 15f -> TunerState.DETECTING
            else -> TunerState.LISTENING
        }

        // Auto-select string
        val autoIndex = if (state.mode == TuningMode.AUTO) {
            PitchDetector.GUITAR_STRINGS.indexOfFirst { it.name == targetNote }.coerceAtLeast(0)
        } else {
            state.selectedStringIndex
        }

        _uiState.update {
            it.copy(
                tunerState = newState,
                detectedNote = result.note,
                detectedFrequency = smoothedFrequency,
                centsFromTarget = smoothedCents,
                targetNote = targetNote,
                targetFrequency = targetFreq,
                selectedStringIndex = autoIndex,
                confidence = result.confidence,
            )
        }
    }
}

private inline fun <T> MutableStateFlow<T>.update(block: (T) -> T) {
    value = block(value)
}
