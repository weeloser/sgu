package com.example.sgu.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Standard biquad filter implementation (Audio EQ Cookbook).
 */
class BiquadFilter(
    private val sampleRate: Float = 44100f
) {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun reset() {
        x1 = 0.0
        x2 = 0.0
        y1 = 0.0
        y2 = 0.0
    }

    fun setLowPass(frequency: Float, q: Float = 0.707f) {
        val w0 = 2.0 * PI * frequency / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val a0 = 1.0 + alpha
        b0 = ((1.0 - cosW0) / 2.0) / a0
        b1 = (1.0 - cosW0) / a0
        b2 = ((1.0 - cosW0) / 2.0) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }

    fun setHighPass(frequency: Float, q: Float = 0.707f) {
        val w0 = 2.0 * PI * frequency / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val a0 = 1.0 + alpha
        b0 = ((1.0 + cosW0) / 2.0) / a0
        b1 = (-(1.0 + cosW0)) / a0
        b2 = ((1.0 + cosW0) / 2.0) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }

    fun setBandPass(frequency: Float, q: Float = 1.0f) {
        val w0 = 2.0 * PI * frequency / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val a0 = 1.0 + alpha
        b0 = (alpha) / a0
        b1 = 0.0
        b2 = (-alpha) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha) / a0
    }

    fun setPeaking(frequency: Float, q: Float = 1.0f, gainDb: Float = 0.0f) {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * frequency / sampleRate
        val alpha = sin(w0) / (2.0 * q)
        val cosW0 = cos(w0)

        val a0 = 1.0 + alpha / a
        b0 = (1.0 + alpha * a) / a0
        b1 = (-2.0 * cosW0) / a0
        b2 = (1.0 - alpha * a) / a0
        a1 = (-2.0 * cosW0) / a0
        a2 = (1.0 - alpha / a) / a0
    }

    fun process(sample: Float): Float {
        val x0 = sample.toDouble()
        val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
        x2 = x1
        x1 = x0
        y2 = y1
        y1 = y0
        return y0.toFloat()
    }
}
