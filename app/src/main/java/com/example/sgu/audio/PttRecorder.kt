package com.example.sgu.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PttRecorder(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordElapsedSec = MutableStateFlow(0f)
    val recordElapsedSec: StateFlow<Float> = _recordElapsedSec.asStateFlow()

    private var lastBuffer: ShortArray? = null
    private val _hasRecording = MutableStateFlow(false)
    val hasRecording: StateFlow<Boolean> = _hasRecording.asStateFlow()

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startRecording(onStarted: () -> Unit, onError: (String) -> Unit) {
        if (_isRecording.value) return
        if (!hasPermission()) {
            onError("Нет разрешения на микрофон")
            return
        }

        try {
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                maxOf(minBufferSize, 4096)
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                onError("Ошибка инициализации микрофона")
                return
            }

            audioRecord = record
            record.startRecording()
            _isRecording.value = true
            _recordElapsedSec.value = 0f
            onStarted()

            recordingJob = scope.launch(Dispatchers.IO) {
                val byteStream = ByteArrayOutputStream()
                val tempBuf = ByteArray(2048)
                val startTime = System.currentTimeMillis()

                while (isActive && _isRecording.value) {
                    val read = record.read(tempBuf, 0, tempBuf.size)
                    if (read > 0) {
                        byteStream.write(tempBuf, 0, read)
                    }
                    val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                    _recordElapsedSec.value = elapsed
                }

                val allBytes = byteStream.toByteArray()
                if (allBytes.isNotEmpty()) {
                    val shortBuffer = ShortArray(allBytes.size / 2)
                    ByteBuffer.wrap(allBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer)
                    lastBuffer = shortBuffer
                    _hasRecording.value = true
                }
            }
        } catch (e: Exception) {
            _isRecording.value = false
            onError(e.message ?: "Ошибка записи")
        }
    }

    fun stopRecording(): ShortArray? {
        if (!_isRecording.value) return lastBuffer
        _isRecording.value = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        recordingJob?.cancel()
        recordingJob = null
        return lastBuffer
    }

    fun getLastRecording(): ShortArray? = lastBuffer
}
