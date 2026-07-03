package com.guitartuna.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sin

class ReferenceToneGenerator {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    var isPlaying = false
        private set

    suspend fun play(frequency: Float) = withContext(Dispatchers.IO) {
        stop()

        val fadeInSamples = (sampleRate * 0.03).toInt()
        val totalSamples = (sampleRate * 2.0).toInt()
        val samples = ShortArray(totalSamples)
        val omega = 2.0 * Math.PI * frequency / sampleRate
        val amplitude = (Short.MAX_VALUE * 0.6).toInt()

        for (i in 0 until totalSamples) {
            val envelope = when {
                i < fadeInSamples -> i.toDouble() / fadeInSamples
                i > totalSamples - fadeInSamples -> (totalSamples - i).toDouble() / fadeInSamples
                else -> 1.0
            }
            samples[i] = (amplitude * envelope * sin(omega * i)).toInt().toShort()
        }

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(totalSamples * 2)

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            0,
        )

        isPlaying = true
        audioTrack?.play()

        var written = 0
        while (isActive && written < totalSamples && isPlaying) {
            val chunk = samples.copyOfRange(written, minOf(written + 2048, totalSamples))
            val result = audioTrack?.write(chunk, 0, chunk.size) ?: break
            if (result < 0) break
            written += result
        }

        if (isPlaying) {
            // Let the tail of the audio play out
            kotlinx.coroutines.delay(100)
        }

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        isPlaying = false
    }

    fun stop() {
        isPlaying = false
        audioTrack?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        audioTrack = null
    }
}
