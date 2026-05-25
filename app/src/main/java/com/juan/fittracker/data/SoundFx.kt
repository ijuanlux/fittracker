package com.juan.fittracker.data

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object SoundFx {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun playSuccess() {
        scope.launch { playChime(listOf(523.25f, 659.25f, 783.99f), 110) }
    }

    fun playAchievement() {
        scope.launch { playChime(listOf(523.25f, 659.25f, 783.99f, 1046.50f), 110) }
    }

    private fun playChime(freqs: List<Float>, noteDurationMs: Int) {
        val sampleRate = 44100
        val samplesPerNote = noteDurationMs * sampleRate / 1000
        val total = freqs.size * samplesPerNote
        val buffer = ShortArray(total)
        var idx = 0
        for (freq in freqs) {
            for (i in 0 until samplesPerNote) {
                val t = i.toFloat() / sampleRate
                var amp = 0.22f * sin(2.0 * PI * freq * t).toFloat()
                val fadeSamples = (samplesPerNote * 0.18f).toInt().coerceAtLeast(1)
                val env = when {
                    i < fadeSamples -> i.toFloat() / fadeSamples
                    i > samplesPerNote - fadeSamples -> (samplesPerNote - i).toFloat() / fadeSamples
                    else -> 1f
                }
                amp *= env
                buffer[idx++] = (amp * Short.MAX_VALUE).toInt().toShort()
            }
        }
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buffer, 0, buffer.size)
            track.play()
            scope.launch {
                delay((freqs.size * noteDurationMs + 200).toLong())
                runCatching { track.release() }
            }
        } catch (_: Exception) {
            // Audio not available — silently skip
        }
    }
}
