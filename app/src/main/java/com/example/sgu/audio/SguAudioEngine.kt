package com.example.sgu.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

enum class SoundMode {
    IDLE,
    KRYAK,
    MANUAL,
    WAIL1,
    WAIL2,
    SRT,
    YELP,
    HILO,
    KLAXON_HOLD,
    KLAXON_QUICK,
    RADIO_CHIRP_OPEN,
    RADIO_CHIRP_CLOSE,
    VOICE_PLAYBACK
}

class SguAudioEngine(
    private val scope: CoroutineScope
) {
    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    private var audioTrack: AudioTrack? = null
    private var synthesisJob: Job? = null

    @Volatile
    private var isRunning = false

    @Volatile
    var currentMode: SoundMode = SoundMode.IDLE
        private set

    // State flows
    private val _vuMeterLevel = MutableStateFlow(0)
    val vuMeterLevel: StateFlow<Int> = _vuMeterLevel.asStateFlow()

    private val _isVoicePlaying = MutableStateFlow(false)
    val isVoicePlaying: StateFlow<Boolean> = _isVoicePlaying.asStateFlow()

    // Filters
    private val hornFilter = ElinaHornFilter(sampleRate.toFloat())
    private val voiceFilter = MegaphoneVoiceFilter(sampleRate.toFloat())
    private val klaxonBpf = BiquadFilter(sampleRate.toFloat()).apply {
        setBandPass(760f, 1.4f)
    }
    private val chirpBpf = BiquadFilter(sampleRate.toFloat())

    // Synthesis phase variables
    private var phaseCore = 0.0
    private var phaseHarm = 0.0
    private var phaseSub = 0.0
    private var phaseLfo = 0.0
    private var phaseAmLfo = 0.0

    // Kryak timing
    private var kryakStartTimeMs = 0L
    @Volatile
    private var kryakReleaseRequested = false
    private var kryakReleaseTimeMs = 0L
    private var kryakGain = 0f
    private val minTapDurationMs = 220L

    // Manual siren timing
    private var manualStartTimeMs = 0L
    @Volatile
    private var manualReleaseRequested = false
    private var manualReleaseTimeMs = 0L
    private var manualCurrentFreq = 570f
    private var manualGain = 0f

    // Siren general timing
    private var sirenStartTimeMs = 0L

    // Klaxon quick timing
    private var klaxonQuickStartMs = 0L

    // Chirp timing
    private var chirpStartMs = 0L
    private var chirpIsOpen = true

    // Voice playback buffer
    private var voiceBuffer: ShortArray? = null
    private var voiceReadIndex = 0
    private var voiceCompleteCallback: (() -> Unit)? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        synthesisJob = scope.launch(Dispatchers.Default) {
            val chunkSamples = 1024
            val shortBuffer = ShortArray(chunkSamples)

            while (isActive && isRunning) {
                var sumSquares = 0.0

                for (i in 0 until chunkSamples) {
                    val sampleFloat = generateNextSample()
                    val clamped = sampleFloat.coerceIn(-1.0f, 1.0f)
                    val pcmSample = (clamped * 32767f).toInt().toShort()
                    shortBuffer[i] = pcmSample
                    sumSquares += (clamped * clamped)
                }

                audioTrack?.write(shortBuffer, 0, chunkSamples)

                val rms = sqrt(sumSquares / chunkSamples).toFloat()
                val activeLeds = if (rms < 0.015f) 0 else min(12, (rms * 16.5f).toInt())
                _vuMeterLevel.value = activeLeds
            }
        }
    }

    fun stop() {
        isRunning = false
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        _vuMeterLevel.value = 0
    }

    private fun generateNextSample(): Float {
        val now = System.currentTimeMillis()
        val dt = 1.0 / sampleRate

        when (currentMode) {
            SoundMode.IDLE -> return 0f

            SoundMode.KRYAK -> {
                val elapsed = (now - kryakStartTimeMs) / 1000.0
                // Attack ramp to 0.95
                if (!kryakReleaseRequested) {
                    if (kryakGain < 0.95f) {
                        kryakGain = min(0.95f, kryakGain + (1.0f / (sampleRate * 0.010f)))
                    }
                } else {
                    val timeSinceRelease = (now - kryakReleaseTimeMs) / 1000.0
                    kryakGain = max(0f, kryakGain - (1.0f / (sampleRate * 0.035f)))
                    if (kryakGain <= 0.001f) {
                        currentMode = SoundMode.IDLE
                        return 0f
                    }
                }

                // Initial frequency drop in first 22ms: 535 -> 462
                val attackProgress = min(1.0, elapsed / 0.022)
                val baseCoreFreq = 535.0 - (73.0 * attackProgress)
                val baseHarmFreq = 800.0 - (107.0 * attackProgress)
                val baseSubFreq = 270.0 - (39.0 * attackProgress)

                // 70.8 Hz square wave pitch modulation (+/- 105 Hz)
                phaseLfo = (phaseLfo + 70.8 * dt) % 1.0
                val lfoSign = if (phaseLfo < 0.5) 1.0 else -1.0
                val freqOffset = lfoSign * 105.0

                // 70.8 Hz sawtooth AM tremolo (35% depth)
                phaseAmLfo = (phaseAmLfo + 70.8 * dt) % 1.0
                val amGain = (0.65 + 0.35 * (1.0 - phaseAmLfo)).toFloat()

                // Oscillators
                val coreFreq = max(50.0, baseCoreFreq + freqOffset)
                val harmFreq = max(50.0, baseHarmFreq + freqOffset)
                val subFreq = max(50.0, baseSubFreq)

                phaseCore = (phaseCore + coreFreq * dt) % 1.0
                phaseHarm = (phaseHarm + harmFreq * dt) % 1.0
                phaseSub = (phaseSub + subFreq * dt) % 1.0

                val coreSquare = if (phaseCore < 0.5) 1.0f else -1.0f
                val harmSaw = (2.0 * phaseHarm - 1.0).toFloat()
                val subTri = (4.0 * kotlin.math.abs(phaseSub - 0.5) - 1.0).toFloat()

                val mixed = (0.72f * coreSquare + 0.35f * harmSaw + 0.40f * subTri) * amGain
                val hornFiltered = hornFilter.process(mixed)
                return hornFiltered * kryakGain
            }

            SoundMode.MANUAL -> {
                if (!manualReleaseRequested) {
                    val t = (now - manualStartTimeMs) / 1000.0
                    // Ramp frequency 570 -> 1620 over 1.8s
                    val progress = min(1.0, t / 1.8)
                    manualCurrentFreq = (570.0 * exp(progress * ln(1620.0 / 570.0))).toFloat()
                    if (manualGain < 0.92f) {
                        manualGain = min(0.92f, manualGain + (1.0f / (sampleRate * 0.08f)))
                    }
                } else {
                    val tRelease = (now - manualReleaseTimeMs) / 1000.0
                    val progress = min(1.0, tRelease / 2.1)
                    val targetFreq = 450f
                    val startFreq = manualCurrentFreq
                    val currentF = (startFreq * exp(progress * ln(targetFreq / startFreq.coerceAtLeast(451f)))).toFloat()
                    manualGain = max(0f, manualGain - (1.0f / (sampleRate * 2.15f)))

                    if (manualGain <= 0.001f || progress >= 1.0) {
                        currentMode = SoundMode.IDLE
                        return 0f
                    }
                    phaseCore = (phaseCore + currentF * dt) % 1.0
                    val saw = (2.0 * phaseCore - 1.0).toFloat()
                    return hornFilter.process(saw) * manualGain
                }

                phaseCore = (phaseCore + manualCurrentFreq * dt) % 1.0
                val saw = (2.0 * phaseCore - 1.0).toFloat()
                return hornFilter.process(saw) * manualGain
            }

            SoundMode.WAIL1 -> {
                val t = (now - sirenStartTimeMs) / 1000.0
                // Period 4.4s triangle LFO: 600 - 1580 Hz (center 1090, +/-490)
                val period = 4.4
                val lfoPhase = (t % period) / period
                val triangle = 1.0 - 2.0 * kotlin.math.abs(lfoPhase - 0.5)
                val freq = (1090.0 + 490.0 * (2.0 * triangle - 1.0)).toFloat()

                phaseCore = (phaseCore + freq * dt) % 1.0
                val saw = (2.0 * phaseCore - 1.0).toFloat()
                return hornFilter.process(saw) * 0.92f
            }

            SoundMode.WAIL2 -> {
                val t = (now - sirenStartTimeMs) / 1000.0
                // Period 2.7s triangle LFO: 680 - 1680 Hz (center 1180, +/-500)
                val period = 2.7
                val lfoPhase = (t % period) / period
                val triangle = 1.0 - 2.0 * kotlin.math.abs(lfoPhase - 0.5)
                val freq = (1180.0 + 500.0 * (2.0 * triangle - 1.0)).toFloat()

                phaseCore = (phaseCore + freq * dt) % 1.0
                val saw = (2.0 * phaseCore - 1.0).toFloat()
                return hornFilter.process(saw) * 0.92f
            }

            SoundMode.SRT -> {
                val t = (now - sirenStartTimeMs) / 1000.0
                // 1.55s cycle: 0.40s scream ascent 590 -> 1640, 1.15s sobbing fall 1640 -> 540
                val cycleT = t % 1.55
                val baseFreq = if (cycleT < 0.40) {
                    val p = cycleT / 0.40
                    590.0 * exp(p * ln(1640.0 / 590.0))
                } else {
                    val p = (cycleT - 0.40) / 1.15
                    1640.0 * exp(p * ln(540.0 / 1640.0))
                }

                // 5.8 Hz sine weep vibrato (+/-32 Hz)
                phaseLfo = (phaseLfo + 5.8 * dt) % 1.0
                val vibrato = 32.0 * sin(2.0 * PI * phaseLfo)
                val finalFreq = (baseFreq + vibrato).toFloat()

                phaseCore = (phaseCore + finalFreq * dt) % 1.0
                val saw = (2.0 * phaseCore - 1.0).toFloat()
                return hornFilter.process(saw) * 0.94f
            }

            SoundMode.YELP -> {
                val t = (now - sirenStartTimeMs) / 1000.0
                // 3.8 Hz sawtooth modulation 640 - 1580 Hz
                phaseLfo = (phaseLfo + 3.8 * dt) % 1.0
                val sawLfo = 2.0 * phaseLfo - 1.0
                val freq = (1110.0 + 470.0 * sawLfo).toFloat()

                phaseCore = (phaseCore + freq * dt) % 1.0
                val saw = (2.0 * phaseCore - 1.0).toFloat()
                return hornFilter.process(saw) * 0.92f
            }

            SoundMode.HILO -> {
                val t = (now - sirenStartTimeMs) / 1000.0
                // Alternates 940 and 680 Hz every 0.53s
                val isHigh = (t % 1.06) < 0.53
                val freq = if (isHigh) 940f else 680f

                phaseCore = (phaseCore + freq * dt) % 1.0
                val square = if (phaseCore < 0.5) 1.0f else -1.0f
                return hornFilter.process(square) * 0.85f
            }

            SoundMode.KLAXON_HOLD -> {
                // Dual horn: 340 + 425 + 170 Hz
                phaseCore = (phaseCore + 340.0 * dt) % 1.0
                phaseHarm = (phaseHarm + 425.0 * dt) % 1.0
                phaseSub = (phaseSub + 170.0 * dt) % 1.0

                val sawLow = (2.0 * phaseCore - 1.0).toFloat()
                val sawHigh = (2.0 * phaseHarm - 1.0).toFloat()
                val triSub = (4.0 * kotlin.math.abs(phaseSub - 0.5) - 1.0).toFloat()

                val mixed = (sawLow + sawHigh + triSub * 0.6f) * 0.5f
                return klaxonBpf.process(mixed) * 0.92f
            }

            SoundMode.KLAXON_QUICK -> {
                val t = (now - klaxonQuickStartMs) / 1000.0
                val isSounding = (t in 0.0..0.11) || (t in 0.17..0.40)
                if (t > 0.43) {
                    currentMode = SoundMode.IDLE
                    return 0f
                }
                if (!isSounding) return 0f

                phaseCore = (phaseCore + 340.0 * dt) % 1.0
                phaseHarm = (phaseHarm + 425.0 * dt) % 1.0

                val sawLow = (2.0 * phaseCore - 1.0).toFloat()
                val sawHigh = (2.0 * phaseHarm - 1.0).toFloat()
                val mixed = (sawLow + sawHigh) * 0.5f
                return klaxonBpf.process(mixed) * 0.90f
            }

            SoundMode.RADIO_CHIRP_OPEN, SoundMode.RADIO_CHIRP_CLOSE -> {
                val t = (now - chirpStartMs) / 1000.0
                if (t > 0.055) {
                    currentMode = SoundMode.IDLE
                    return 0f
                }
                val p = (t / 0.045).coerceIn(0.0, 1.0)
                val freq = if (chirpIsOpen) {
                    1200.0 * exp(p * ln(450.0 / 1200.0))
                } else {
                    750.0 * exp(p * ln(220.0 / 750.0))
                }
                val gain = (0.35 * exp(-p * 5.0)).toFloat()

                phaseCore = (phaseCore + freq * dt) % 1.0
                val tri = (4.0 * kotlin.math.abs(phaseCore - 0.5) - 1.0).toFloat()
                return chirpBpf.process(tri) * gain
            }

            SoundMode.VOICE_PLAYBACK -> {
                val buf = voiceBuffer
                if (buf == null || voiceReadIndex >= buf.size) {
                    currentMode = SoundMode.IDLE
                    _isVoicePlaying.value = false
                    val cb = voiceCompleteCallback
                    voiceCompleteCallback = null
                    scope.launch(Dispatchers.Main) {
                        playRadioChirp(false)
                        cb?.invoke()
                    }
                    return 0f
                }

                val sampleShort = buf[voiceReadIndex++]
                val sampleNormalized = sampleShort / 32768.0f
                val processed = voiceFilter.process(sampleNormalized)
                return processed.coerceIn(-1.0f, 1.0f) * 0.95f
            }
        }
    }

    // Controls
    fun startKryak() {
        hornFilter.reset()
        phaseCore = 0.0
        phaseHarm = 0.0
        phaseSub = 0.0
        phaseLfo = 0.0
        phaseAmLfo = 0.0
        kryakStartTimeMs = System.currentTimeMillis()
        kryakReleaseRequested = false
        kryakGain = 0f
        currentMode = SoundMode.KRYAK
    }

    fun stopKryak() {
        if (currentMode != SoundMode.KRYAK) return
        val now = System.currentTimeMillis()
        val elapsed = now - kryakStartTimeMs
        if (elapsed < minTapDurationMs) {
            val remaining = minTapDurationMs - elapsed
            scope.launch {
                delay(remaining)
                if (currentMode == SoundMode.KRYAK) {
                    kryakReleaseRequested = true
                    kryakReleaseTimeMs = System.currentTimeMillis()
                }
            }
        } else {
            kryakReleaseRequested = true
            kryakReleaseTimeMs = now
        }
    }

    fun playDoubleKryak(onComplete: (() -> Unit)? = null) {
        scope.launch {
            stopAll()
            startKryak()
            delay(200)
            stopKryak()
            delay(110)
            startKryak()
            delay(200)
            stopKryak()
            delay(100)
            onComplete?.invoke()
        }
    }

    fun startManual() {
        hornFilter.reset()
        phaseCore = 0.0
        manualStartTimeMs = System.currentTimeMillis()
        manualReleaseRequested = false
        manualGain = 0f
        manualCurrentFreq = 570f
        currentMode = SoundMode.MANUAL
    }

    fun stopManual() {
        if (currentMode != SoundMode.MANUAL) return
        manualReleaseRequested = true
        manualReleaseTimeMs = System.currentTimeMillis()
    }

    fun startSiren(mode: SoundMode) {
        hornFilter.reset()
        phaseCore = 0.0
        phaseLfo = 0.0
        sirenStartTimeMs = System.currentTimeMillis()
        currentMode = mode
    }

    fun startBibikaContinuous() {
        klaxonBpf.reset()
        phaseCore = 0.0
        phaseHarm = 0.0
        phaseSub = 0.0
        currentMode = SoundMode.KLAXON_HOLD
    }

    fun stopBibikaContinuous() {
        if (currentMode == SoundMode.KLAXON_HOLD) {
            currentMode = SoundMode.IDLE
        }
    }

    fun playBibikaQuick() {
        klaxonBpf.reset()
        klaxonQuickStartMs = System.currentTimeMillis()
        phaseCore = 0.0
        phaseHarm = 0.0
        currentMode = SoundMode.KLAXON_QUICK
    }

    fun playRadioChirp(isOpen: Boolean) {
        chirpIsOpen = isOpen
        chirpStartMs = System.currentTimeMillis()
        chirpBpf.reset()
        if (isOpen) {
            chirpBpf.setBandPass(2500f, 4.2f)
        } else {
            chirpBpf.setBandPass(1750f, 4.2f)
        }
        phaseCore = 0.0
        currentMode = if (isOpen) SoundMode.RADIO_CHIRP_OPEN else SoundMode.RADIO_CHIRP_CLOSE
    }

    fun playProcessedVoice(samples: ShortArray, onComplete: () -> Unit) {
        voiceFilter.reset()
        voiceBuffer = samples
        voiceReadIndex = 0
        voiceCompleteCallback = onComplete
        _isVoicePlaying.value = true
        playRadioChirp(true)
        scope.launch {
            delay(50)
            currentMode = SoundMode.VOICE_PLAYBACK
        }
    }

    fun stopAll() {
        kryakReleaseRequested = true
        manualReleaseRequested = true
        currentMode = SoundMode.IDLE
        _isVoicePlaying.value = false
        voiceBuffer = null
    }
}
