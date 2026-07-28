package com.example

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioEngine(
    private val onAudioDataReady: (ByteArray) -> Unit
) {
    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    
    private val bufferSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
    private val bufferSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
    
    private var recordJob: Job? = null
    private var isMuted = false
    private val scope = CoroutineScope(Dispatchers.IO)

    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (recordJob?.isActive == true) return
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            channelConfigIn,
            audioFormat,
            bufferSizeIn
        )

        audioRecord?.audioSessionId?.let { sessionId ->
            if (AcousticEchoCanceler.isAvailable()) {
                aec = AcousticEchoCanceler.create(sessionId)
                aec?.enabled = true
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = NoiseSuppressor.create(sessionId)
                ns?.enabled = true
            }
        }

        audioRecord?.startRecording()

        recordJob = scope.launch {
            val buffer = ByteArray(bufferSizeIn)
            while (isActive) {
                val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (readSize > 0 && !isMuted) {
                    val dataToSend = buffer.copyOf(readSize)
                    onAudioDataReady(dataToSend)
                }
            }
        }
    }

    fun stopRecording() {
        recordJob?.cancel()
        recordJob = null
        aec?.release()
        aec = null
        ns?.release()
        ns = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    fun startPlaying() {
        if (audioTrack != null) return
        
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(channelConfigOut)
                .setEncoding(audioFormat)
                .build(),
            bufferSizeOut,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.play()
    }

    fun stopPlaying() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun playAudioData(data: ByteArray) {
        audioTrack?.write(data, 0, data.size)
    }

    fun setMute(muted: Boolean) {
        isMuted = muted
    }
}
