package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class MeshViewModel(application: Application) : AndroidViewModel(application) {
    init {
        MeshRepository.init(application)
        MeshRepository.startService(application)
    }

    private val connectionManager = MeshRepository.getConnectionManager()
    private val audioEngine = MeshRepository.getAudioEngine()
    private val callHistoryDao = MeshRepository.getDatabase().callHistoryDao()

    val devices = connectionManager.devices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val callState = connectionManager.callState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CallState.IDLE)
        
    val activeEndpoint = connectionManager.activeEndpoint
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val callHistory = callHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()
    private var timerJob: Job? = null

    // Track state to know when to save
    private var isIncomingCall = false
    private var lastCallerName = ""

    init {
        // Monitor call state to stop audio and save history when another side ends call
        viewModelScope.launch {
            connectionManager.callState.collect { state ->
                if (state == CallState.RINGING_INCOMING) {
                    isIncomingCall = true
                } else if (state == CallState.RINGING_OUTGOING) {
                    isIncomingCall = false
                }
                
                if (state != CallState.IDLE) {
                    lastCallerName = connectionManager.activeEndpoint.value?.name ?: "Unknown"
                }

                if (state == CallState.IDLE) {
                    val duration = _callDuration.value
                    if (duration > 0 || isIncomingCall) {
                        saveCallHistory(lastCallerName, duration, isIncomingCall)
                    }
                    stopAudio()
                }
            }
        }
    }

    private fun saveCallHistory(name: String, duration: Long, incoming: Boolean) {
        if (name.isNotEmpty()) {
            viewModelScope.launch {
                callHistoryDao.insertCall(
                    CallHistory(
                        callerName = name,
                        durationSeconds = duration,
                        isIncoming = incoming
                    )
                )
            }
        }
    }

    fun startScanning() {
        connectionManager.startAdvertising()
        connectionManager.startDiscovery()
    }

    fun connectToDevice(device: MeshDevice) {
        connectionManager.connectToDevice(device)
    }

    fun initiateCall(device: MeshDevice) {
        connectionManager.initiateCall(device)
    }

    fun acceptCall() {
        connectionManager.acceptCall()
        startAudio()
    }

    fun rejectCall() {
        connectionManager.rejectCall()
    }

    fun endCall() {
        connectionManager.endCall()
    }

    fun toggleMute(muted: Boolean) {
        audioEngine.setMute(muted)
    }

    // Called when an outgoing call is accepted
    fun startAudio() {
        audioEngine.startPlaying()
        audioEngine.startRecording()
        timerJob?.cancel()
        _callDuration.value = 0L
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _callDuration.value += 1
            }
        }
    }

    private fun stopAudio() {
        audioEngine.stopRecording()
        audioEngine.stopPlaying()
        timerJob?.cancel()
        _callDuration.value = 0L
    }

    override fun onCleared() {
        super.onCleared()
        // We don't stop the connectionManager or service here, 
        // it continues running in the foreground.
        // If we wanted to stop it, we would call:
        // MeshRepository.stopService(getApplication())
    }
}
