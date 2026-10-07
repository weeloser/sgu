package com.example.sgu.audio

import kotlin.math.tanh

/**
 * Acoustic simulation of the metallic horn and compression driver
 * of the Elina GRD-100 (200W/400W) emergency vehicle siren.
 */
class ElinaHornFilter(sampleRate: Float = 44100f) {
    private val hpf = BiquadFilter(sampleRate).apply {
        setHighPass(330f, 0.707f)
    }
    private val peak1 = BiquadFilter(sampleRate).apply {
        setPeaking(1240f, 2.2f, 11.0f)
    }
    private val peak2 = BiquadFilter(sampleRate).apply {
        setPeaking(2420f, 2.6f, 6.5f)
    }
    private val lpf = BiquadFilter(sampleRate).apply {
        setLowPass(3850f, 0.707f)
    }

    private val distortionK = 3.8
    private val tanhNorm = tanh(distortionK)

    fun reset() {
        hpf.reset()
        peak1.reset()
        peak2.reset()
        lpf.reset()
    }

    fun process(input: Float): Float {
        var s = hpf.process(input)
        s = peak1.process(s)
        s = peak2.process(s)
        s = lpf.process(s)
        // Metallic compression driver saturation
        val saturated = (tanh(distortionK * s) / tanhNorm).toFloat()
        return saturated
    }
}

/**
 * 200W Public Address Megaphone horn filter for voice announcements.
 */
class MegaphoneVoiceFilter(sampleRate: Float = 44100f) {
    private val hpf = BiquadFilter(sampleRate).apply {
        setHighPass(460f, 1.3f)
    }
    private val peak = BiquadFilter(sampleRate).apply {
        setPeaking(1800f, 2.4f, 12.0f)
    }
    private val lpf = BiquadFilter(sampleRate).apply {
        setLowPass(3300f, 0.707f)
    }

    private val distortionK = 4.2
    private val tanhNorm = tanh(distortionK)

    fun reset() {
        hpf.reset()
        peak.reset()
        lpf.reset()
    }

    fun process(input: Float): Float {
        var s = hpf.process(input)
        s = peak.process(s)
        s = lpf.process(s)
        val saturated = (tanh(distortionK * s) / tanhNorm).toFloat()
        return saturated * 2.4f
    }
}
