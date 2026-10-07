package com.example.sgu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sgu.audio.PttRecorder
import com.example.sgu.audio.SguAudioEngine
import com.example.sgu.audio.SoundMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SguUiState(
    val statusText: String = "ГОТОВ",
    val activeSiren: String? = null,
    val isKryakActive: Boolean = false,
    val isManualActive: Boolean = false,
    val isKlaxonActive: Boolean = false,
    val isSgoActive: Boolean = false,
    val isRecording: Boolean = false,
    val isVoicePlaying: Boolean = false,
    val pttStatusInfo: String = "Зажмите, скажите распоряжение",
    val recordTimerText: String = "00:00.0",
    val hasRecordedAudio: Boolean = false,
    val strobeLeftOn: Boolean = false,
    val strobeRightOn: Boolean = false,
    val headerStrobeLeftOn: Boolean = false,
    val headerStrobeRightOn: Boolean = false,
    val vuMeterLevel: Int = 0
)

class SguViewModel(application: Application) : AndroidViewModel(application) {

    private val audioEngine = SguAudioEngine(viewModelScope)
    private val pttRecorder = PttRecorder(application.applicationContext, viewModelScope)

    private val _uiState = MutableStateFlow(SguUiState())
    val uiState: StateFlow<SguUiState> = _uiState.asStateFlow()

    private var sgoJob: Job? = null
    private var headerStrobeJob: Job? = null
    private var hotkeyJob: Job? = null
    private var klaxonHoldJob: Job? = null
    private var isBibikaHeld = false

    // Fast strobe sequence (matching original web app)
    private val sgoSteps = listOf(
        Pair(true to false, 24L),
        Pair(false to false, 18L),
        Pair(true to false, 24L),
        Pair(false to false, 18L),
        Pair(true to false, 24L),
        Pair(false to false, 36L),
        Pair(false to true, 24L),
        Pair(false to false, 18L),
        Pair(false to true, 24L),
        Pair(false to false, 18L),
        Pair(false to true, 24L),
        Pair(false to false, 42L)
    )

    init {
        audioEngine.start()

        viewModelScope.launch {
            audioEngine.vuMeterLevel.collect { level ->
                _uiState.value = _uiState.value.copy(vuMeterLevel = level)
            }
        }

        viewModelScope.launch {
            audioEngine.isVoicePlaying.collect { playing ->
                _uiState.value = _uiState.value.copy(isVoicePlaying = playing)
                if (playing) {
                    startHeaderStrobes()
                    _uiState.value = _uiState.value.copy(
                        statusText = "РУПОР: 200W",
                        pttStatusInfo = "Выдача через рупор СГУ (200W)…"
                    )
                } else if (_uiState.value.statusText == "РУПОР: 200W") {
                    stopHeaderStrobes()
                    _uiState.value = _uiState.value.copy(
                        statusText = "ГОТОВ",
                        pttStatusInfo = "Зажмите PTT для новой фразы"
                    )
                }
            }
        }

        viewModelScope.launch {
            pttRecorder.recordElapsedSec.collect { elapsed ->
                val seconds = elapsed.toInt()
                val tenths = ((elapsed - seconds) * 10).toInt()
                val formatted = String.format("00:%02d.%01d", seconds, tenths)
                _uiState.value = _uiState.value.copy(recordTimerText = formatted)
            }
        }

        viewModelScope.launch {
            pttRecorder.hasRecording.collect { hasRec ->
                _uiState.value = _uiState.value.copy(hasRecordedAudio = hasRec)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        pttRecorder.stopRecording()
    }

    fun hasRecordPermission(): Boolean = pttRecorder.hasPermission()

    // 1. КРЯК (HORN 70HZ)
    fun startKryak() {
        stopAllSirensInternal()
        audioEngine.startKryak()
        startHeaderStrobes(redOnly = true)
        _uiState.value = _uiState.value.copy(
            isKryakActive = true,
            statusText = "HORN 70HZ"
        )
    }

    fun stopKryak() {
        audioEngine.stopKryak()
        stopHeaderStrobes()
        _uiState.value = _uiState.value.copy(
            isKryakActive = false,
            statusText = if (_uiState.value.isSgoActive) "СГО: СТРОБЫ" else "ГОТОВ"
        )
    }

    fun playDoubleKryak() {
        stopAllSirensInternal()
        startHeaderStrobes(redOnly = true)
        _uiState.value = _uiState.value.copy(
            statusText = "КРЯК ×2",
            isKryakActive = true
        )
        audioEngine.playDoubleKryak {
            stopHeaderStrobes()
            _uiState.value = _uiState.value.copy(
                isKryakActive = false,
                statusText = if (_uiState.value.isSgoActive) "СГО: СТРОБЫ" else "ГОТОВ"
            )
        }
    }

    // 2. MANUAL (РУЧНОЙ ПОДВЫВ)
    fun startManual() {
        stopAllSirensInternal()
        audioEngine.startManual()
        startHeaderStrobes(blueOnly = true)
        _uiState.value = _uiState.value.copy(
            isManualActive = true,
            statusText = "MANUAL ▲"
        )
    }

    fun stopManual() {
        audioEngine.stopManual()
        _uiState.value = _uiState.value.copy(
            statusText = "MANUAL ▼"
        )
        viewModelScope.launch {
            delay(2100)
            stopHeaderStrobes()
            _uiState.value = _uiState.value.copy(
                isManualActive = false,
                statusText = if (_uiState.value.isSgoActive) "СГО: СТРОБЫ" else "ГОТОВ"
            )
        }
    }

    // 3. СИРЕНЫ
    fun toggleSiren(name: String) {
        if (_uiState.value.activeSiren == name) {
            stopAll()
            return
        }

        stopAllSirensInternal()
        startHeaderStrobes()

        when (name) {
            "wail1" -> {
                audioEngine.startSiren(SoundMode.WAIL1)
                _uiState.value = _uiState.value.copy(
                    activeSiren = "wail1",
                    statusText = "WAIL 1 (ГОСТ)"
                )
            }
            "wail2" -> {
                audioEngine.startSiren(SoundMode.WAIL2)
                _uiState.value = _uiState.value.copy(
                    activeSiren = "wail2",
                    statusText = "WAIL 2 (2.7С)"
                )
            }
            "srt" -> {
                audioEngine.startSiren(SoundMode.SRT)
                _uiState.value = _uiState.value.copy(
                    activeSiren = "srt",
                    statusText = "SRT «ПЛАЧ»"
                )
            }
            "yelp" -> {
                audioEngine.startSiren(SoundMode.YELP)
                _uiState.value = _uiState.value.copy(
                    activeSiren = "yelp",
                    statusText = "YELP (ВИЗГ)"
                )
            }
            "hilo" -> {
                audioEngine.startSiren(SoundMode.HILO)
                _uiState.value = _uiState.value.copy(
                    activeSiren = "hilo",
                    statusText = "HI-LO (2 ТОНА)"
                )
            }
            "hotkey" -> {
                audioEngine.startSiren(SoundMode.YELP)
                _uiState.value = _uiState.value.copy(
                    activeSiren = "hotkey",
                    statusText = "ГОРЯЧАЯ (15С)"
                )
                hotkeyJob = viewModelScope.launch {
                    delay(15000)
                    stopAll()
                }
            }
        }
    }

    // 4. КЛАКСОН
    fun onBibikaPressStart() {
        stopAllSirensInternal()
        isBibikaHeld = false
        klaxonHoldJob = viewModelScope.launch {
            delay(170)
            isBibikaHeld = true
            audioEngine.startBibikaContinuous()
            _uiState.value = _uiState.value.copy(
                isKlaxonActive = true,
                statusText = "КЛАКСОН"
            )
        }
    }

    fun onBibikaPressEnd() {
        klaxonHoldJob?.cancel()
        klaxonHoldJob = null

        if (isBibikaHeld) {
            isBibikaHeld = false
            audioEngine.stopBibikaContinuous()
            _uiState.value = _uiState.value.copy(
                isKlaxonActive = false,
                statusText = if (_uiState.value.isSgoActive) "СГО: СТРОБЫ" else "ГОТОВ"
            )
        } else {
            audioEngine.playBibikaQuick()
            _uiState.value = _uiState.value.copy(
                isKlaxonActive = true,
                statusText = "КЛАКСОН ×2"
            )
            viewModelScope.launch {
                delay(430)
                _uiState.value = _uiState.value.copy(
                    isKlaxonActive = false,
                    statusText = if (_uiState.value.isSgoActive) "СГО: СТРОБЫ" else "ГОТОВ"
                )
            }
        }
    }

    // 5. СГО (СТРОБОСКОПЫ)
    fun toggleSgo() {
        if (_uiState.value.isSgoActive) {
            stopSgo()
        } else {
            startSgo()
        }
    }

    fun startSgo() {
        if (_uiState.value.isSgoActive) return
        _uiState.value = _uiState.value.copy(
            isSgoActive = true,
            statusText = "СГО: СТРОБЫ"
        )
        sgoJob?.cancel()
        sgoJob = viewModelScope.launch {
            var stepIndex = 0
            while (isActive && _uiState.value.isSgoActive) {
                val step = sgoSteps[stepIndex]
                _uiState.value = _uiState.value.copy(
                    strobeLeftOn = step.first.first,
                    strobeRightOn = step.first.second
                )
                stepIndex = (stepIndex + 1) % sgoSteps.size
                delay(step.second)
            }
        }
    }

    fun stopSgo() {
        sgoJob?.cancel()
        sgoJob = null
        _uiState.value = _uiState.value.copy(
            isSgoActive = false,
            strobeLeftOn = false,
            strobeRightOn = false,
            statusText = if (_uiState.value.activeSiren != null) _uiState.value.statusText else "ГОТОВ"
        )
    }

    // 6. PTT ТАНГЕНТА
    fun startPtt() {
        stopAllSirensInternal()
        audioEngine.playRadioChirp(true)
        pttRecorder.startRecording(
            onStarted = {
                _uiState.value = _uiState.value.copy(
                    isRecording = true,
                    statusText = "ТАНГЕНТА: ПЕРЕДАЧА",
                    pttStatusInfo = "Говорите команду в микрофон…"
                )
            },
            onError = { msg ->
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    pttStatusInfo = msg
                )
            }
        )
    }

    fun stopPtt() {
        if (!_uiState.value.isRecording) return
        val recordedSamples = pttRecorder.stopRecording()
        audioEngine.playRadioChirp(false)
        _uiState.value = _uiState.value.copy(
            isRecording = false,
            statusText = "ГОТОВ"
        )

        if (recordedSamples != null && recordedSamples.isNotEmpty()) {
            audioEngine.playProcessedVoice(recordedSamples) {
                _uiState.value = _uiState.value.copy(
                    pttStatusInfo = "Зажмите PTT для новой фразы"
                )
            }
        }
    }

    fun replayVoice() {
        val last = pttRecorder.getLastRecording()
        if (last != null && last.isNotEmpty()) {
            stopAllSirensInternal()
            audioEngine.playProcessedVoice(last) {
                _uiState.value = _uiState.value.copy(
                    pttStatusInfo = "Зажмите PTT для новой фразы"
                )
            }
        }
    }

    // 7. СТОП ВСЕ
    fun stopAll() {
        hotkeyJob?.cancel()
        hotkeyJob = null
        stopAllSirensInternal()
        if (!_uiState.value.isSgoActive) {
            stopHeaderStrobes()
            _uiState.value = _uiState.value.copy(statusText = "ГОТОВ")
        }
    }

    private fun stopAllSirensInternal() {
        hotkeyJob?.cancel()
        hotkeyJob = null
        audioEngine.stopAll()
        _uiState.value = _uiState.value.copy(
            activeSiren = null,
            isKryakActive = false,
            isManualActive = false,
            isKlaxonActive = false
        )
    }

    private fun startHeaderStrobes(redOnly: Boolean = false, blueOnly: Boolean = false) {
        headerStrobeJob?.cancel()
        headerStrobeJob = viewModelScope.launch {
            var toggle = false
            while (isActive) {
                toggle = !toggle
                _uiState.value = _uiState.value.copy(
                    headerStrobeLeftOn = if (blueOnly) false else toggle,
                    headerStrobeRightOn = if (redOnly) false else !toggle
                )
                delay(120)
            }
        }
    }

    private fun stopHeaderStrobes() {
        headerStrobeJob?.cancel()
        headerStrobeJob = null
        _uiState.value = _uiState.value.copy(
            headerStrobeLeftOn = false,
            headerStrobeRightOn = false
        )
    }
}
