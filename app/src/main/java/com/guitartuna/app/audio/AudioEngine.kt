package com.guitartuna.app.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

class AudioEngine {

    private var audioRecord: AudioRecord? = null
    private val sampleRate = 44100
    private val frameSize = 2048

    private val _pitchResult = MutableStateFlow(PitchDetector.PitchResult(detected = false))
    val pitchResult: StateFlow<PitchDetector.PitchResult> = _pitchResult

    val isRunning: Boolean get() = audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    suspend fun start() = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBuf * 2, frameSize * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            throw IllegalStateException("Failed to initialize AudioRecord")
        }

        audioRecord?.startRecording()

        val samples = ShortArray(frameSize)
        while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val read = audioRecord?.read(samples, 0, frameSize) ?: break
            if (read <= 0) continue

            val result = PitchDetector.detect(samples.copyOf(read), sampleRate)
            _pitchResult.value = result
        }
    }

    fun stop() {
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
            release()
        }
        audioRecord = null
    }
}
