package com.guitartuna.app.audio

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

object PitchDetector {

    private const val MIN_FREQ = 60f
    private const val MAX_FREQ = 420f
    private const val CONFIDENCE_THRESHOLD = 3.5f

    private val NOTE_NAMES = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    val GUITAR_STRINGS = listOf(
        GuitarString("E2", 82.41f),
        GuitarString("A2", 110.00f),
        GuitarString("D3", 146.83f),
        GuitarString("G3", 196.00f),
        GuitarString("B3", 246.94f),
        GuitarString("E4", 329.63f),
    )

    data class PitchResult(
        val detected: Boolean,
        val frequency: Float = 0f,
        val note: String = "",
        val cents: Float = 0f,
        val confidence: Float = 0f,
    )

    data class GuitarString(val name: String, val frequency: Float)

    fun detect(samples: ShortArray, sampleRate: Int): PitchResult {
        val n = samples.size
        val floatData = FloatArray(n) { i -> samples[i] / 32768f }

        // Hann window
        val windowed = FloatArray(n) { i ->
            floatData[i] * (0.5f * (1f - cos(2.0 * Math.PI * i / (n - 1)))).toFloat()
        }

        // RMS amplitude gate
        var rms = 0f
        for (v in windowed) rms += v * v
        rms = kotlin.math.sqrt(rms / n)
        if (rms < 0.005f) return PitchResult(detected = false)

        // Autocorrelation
        val minLag = (sampleRate / MAX_FREQ).toInt()
        val maxLag = (sampleRate / MIN_FREQ).toInt()
        val ac = FloatArray(maxLag + 1)

        for (tau in minLag..maxLag) {
            var sum = 0f
            for (i in tau until n) {
                sum += windowed[i] * windowed[i - tau]
            }
            ac[tau] = sum / (n - tau)
        }

        // Find peak
        var peakLag = minLag
        var peakVal = ac[minLag]
        var acSum = 0f
        for (tau in minLag..maxLag) {
            acSum += ac[tau]
            if (ac[tau] > peakVal) {
                peakVal = ac[tau]
                peakLag = tau
            }
        }

        val acMean = acSum / (maxLag - minLag + 1)
        val confidence = if (acMean > 0f) peakVal / acMean else 0f

        if (confidence < CONFIDENCE_THRESHOLD) {
            return PitchResult(detected = false)
        }

        // Parabolic interpolation
        val y1 = if (peakLag > minLag) ac[peakLag - 1] else peakVal
        val y2 = peakVal
        val y3 = if (peakLag < maxLag) ac[peakLag + 1] else peakVal

        val denom = y1 - 2 * y2 + y3
        val delta = if (abs(denom) > 1e-10f) (y1 - y3) / (2 * denom) else 0f
        val refinedLag = peakLag + delta

        val frequency = sampleRate / refinedLag

        // MIDI note and cents
        val midiNote = 12f * (ln(frequency / 440f) / ln(2f)) + 69f
        val nearestMidi = round(midiNote).toInt()
        val nearestFreq = 440f * 2f.pow((nearestMidi - 69) / 12f)
        val cents = 1200f * (ln(frequency / nearestFreq) / ln(2f))

        val noteName = NOTE_NAMES[((nearestMidi % 12) + 12) % 12]
        val octave = floor(nearestMidi / 12f).toInt() - 1
        val note = "$noteName$octave"

        return PitchResult(
            detected = true,
            frequency = frequency,
            note = note,
            cents = cents,
            confidence = (confidence / 10f).coerceIn(0f, 1f),
        )
    }

    fun findNearestString(frequency: Float): GuitarString {
        return GUITAR_STRINGS.minBy { abs(frequency - it.frequency) }
    }
}
