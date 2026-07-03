package com.guitartuna.app.audio

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.round

object PitchDetector {

    private const val MIN_FREQ = 60f
    private const val MAX_FREQ = 420f
    private const val CONFIDENCE_THRESHOLD = 4.5f

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

        // RMS amplitude gate
        var rms = 0f
        for (v in floatData) rms += v * v
        rms = kotlin.math.sqrt(rms / n)
        if (rms < 0.01f) return PitchResult(detected = false)

        // Autocorrelation
        val minLag = (sampleRate / MAX_FREQ).toInt()
        val maxLag = (sampleRate / MIN_FREQ).toInt()
        val ac = FloatArray(maxLag + 1)

        for (tau in minLag..maxLag) {
            var sum = 0f
            for (i in tau until n) {
                sum += floatData[i] * floatData[i - tau]
            }
            ac[tau] = sum / (n - tau)
        }

        // Compute mean and threshold
        var acSum = 0f
        for (tau in minLag..maxLag) acSum += ac[tau]
        val acMean = acSum / (maxLag - minLag + 1)
        val threshold = acMean * CONFIDENCE_THRESHOLD

        // Collect all local peaks above threshold
        data class Peak(val lag: Int, val value: Float)
        val peaks = mutableListOf<Peak>()
        for (tau in minLag + 1 until maxLag) {
            if (ac[tau] > threshold && ac[tau] > ac[tau - 1] && ac[tau] >= ac[tau + 1]) {
                peaks.add(Peak(tau, ac[tau]))
            }
        }

        var peakLag: Int
        var peakVal: Float

        if (peaks.isEmpty()) {
            // No significant peaks — use global max
            peakLag = minLag
            peakVal = ac[minLag]
            for (tau in minLag..maxLag) {
                if (ac[tau] > peakVal) {
                    peakVal = ac[tau]
                    peakLag = tau
                }
            }
        } else {
            // Score peaks: prefer ones matching guitar string frequencies
            var bestLag = peaks.first().lag
            var bestVal = peaks.first().value
            var bestScore = -1f

            for ((lag, value) in peaks) {
                val freq = sampleRate / lag.toFloat()
                val nearest = findNearestString(freq)
                val centsDist = abs(ln(freq / nearest.frequency))
                // Score: strong peak close to a guitar string wins
                val score = value / (0.01f + centsDist)

                if (score > bestScore) {
                    bestScore = score
                    bestLag = lag
                    bestVal = value
                }
            }
            peakLag = bestLag
            peakVal = bestVal
        }

        // Sub-harmonic check: if there's a stronger peak at 1/2 or 1/3 the lag,
        // it's the true fundamental (e.g. E4 detected as A2 via 3x-period peak)
        for (divisor in 2..3) {
            val candidateLag = peakLag / divisor
            if (candidateLag >= minLag) {
                val start = (candidateLag - 2).coerceAtLeast(minLag)
                val end = (candidateLag + 2).coerceAtMost(maxLag)
                var bestLocal = 0f
                var bestLocalLag = candidateLag
                for (tau in start..end) {
                    if (ac[tau] > bestLocal) {
                        bestLocal = ac[tau]
                        bestLocalLag = tau
                    }
                }
                if (bestLocal > peakVal) {
                    peakLag = bestLocalLag
                    peakVal = bestLocal
                }
            }
        }

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
        return GUITAR_STRINGS.minBy { abs(ln(frequency / it.frequency)) }
    }
}
