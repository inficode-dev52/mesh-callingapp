package com.example

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class MeshDevice(
    val endpointId: String,
    val name: String,
    val isConnecting: Boolean = false
)

enum class CallState {
    IDLE,
    RINGING_INCOMING,
    RINGING_OUTGOING,
    IN_CALL
}

class MeshConnectionManager(private val context: Context) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val strategy = Strategy.P2P_CLUSTER // Mesh-like topology
    private val serviceId = "com.example.meshcall"
    
    private val myName = "User_${Random.nextInt(1000, 9999)}"

    private val _devices = MutableStateFlow<List<MeshDevice>>(emptyList())
    val devices: StateFlow<List<MeshDevice>> = _devices.asStateFlow()

    private val _callState = MutableStateFlow(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private val _activeEndpoint = MutableStateFlow<MeshDevice?>(null)
    val activeEndpoint: StateFlow<MeshDevice?> = _activeEndpoint.asStateFlow()

    var onAudioReceived: ((ByteArray) -> Unit)? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                payload.asBytes()?.let { bytes ->
                    val message = String(bytes)
                    when (message) {
                        "CALL_REQUEST" -> {
                            _activeEndpoint.value = MeshDevice(endpointId, "Caller", false)
                            _callState.value = CallState.RINGING_INCOMING
                        }
                        "CALL_ACCEPT" -> {
                            _callState.value = CallState.IN_CALL
                        }
                        "CALL_REJECT", "CALL_END" -> {
                            endCall()
                        }
                        else -> {
                            // Assume audio data
                            onAudioReceived?.invoke(bytes)
                        }
                    }
                }
            }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Auto accept connection for mesh simplicity
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            val name = info.endpointName
            _devices.update { current ->
                val existing = current.find { it.endpointId == endpointId }
                if (existing != null) {
                    current.map { if (it.endpointId == endpointId) it.copy(isConnecting = true, name = name) else it }
                } else {
                    current + MeshDevice(endpointId, name, true)
                }
            }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                _devices.update { current ->
                    current.map { if (it.endpointId == endpointId) it.copy(isConnecting = false) else it }
                }
                // If this is a reconnect during a call, restore call state
                val active = _activeEndpoint.value
                if (active?.endpointId == endpointId && _callState.value == CallState.IN_CALL) {
                    Log.d("MeshCall", "Successfully reconnected")
                }
            } else {
                _devices.update { current -> current.filter { it.endpointId != endpointId } }
            }
        }

        override fun onDisconnected(endpointId: String) {
            _devices.update { current -> current.filter { it.endpointId != endpointId } }
            val active = _activeEndpoint.value
            if (active?.endpointId == endpointId && _callState.value == CallState.IN_CALL) {
                Log.d("MeshCall", "Connection dropped during call. Attempting reconnect.")
                connectToDevice(active)
            } else if (active?.endpointId == endpointId) {
                endCall()
            }
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            _devices.update { current ->
                if (current.none { it.endpointId == endpointId }) {
                    current + MeshDevice(endpointId, info.endpointName)
                } else current
            }
        }

        override fun onEndpointLost(endpointId: String) {
            _devices.update { current -> current.filter { it.endpointId != endpointId } }
        }
    }

    fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            myName, serviceId, connectionLifecycleCallback, options
        ).addOnSuccessListener {
            Log.d("MeshCall", "Advertising started")
        }.addOnFailureListener {
            Log.e("MeshCall", "Advertising failed", it)
        }
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId, endpointDiscoveryCallback, options
        ).addOnSuccessListener {
            Log.d("MeshCall", "Discovery started")
        }.addOnFailureListener {
            Log.e("MeshCall", "Discovery failed", it)
        }
    }

    fun connectToDevice(device: MeshDevice) {
        connectionsClient.requestConnection(myName, device.endpointId, connectionLifecycleCallback)
    }

    fun initiateCall(device: MeshDevice) {
        connectToDevice(device) // Ensure connected
        _activeEndpoint.value = device
        _callState.value = CallState.RINGING_OUTGOING
        sendString(device.endpointId, "CALL_REQUEST")
    }

    fun acceptCall() {
        _activeEndpoint.value?.let { device ->
            sendString(device.endpointId, "CALL_ACCEPT")
            _callState.value = CallState.IN_CALL
        }
    }

    fun rejectCall() {
        _activeEndpoint.value?.let { device ->
            sendString(device.endpointId, "CALL_REJECT")
        }
        resetCallState()
    }

    fun endCall() {
        _activeEndpoint.value?.let { device ->
            sendString(device.endpointId, "CALL_END")
        }
        resetCallState()
    }
    
    private fun resetCallState() {
        _callState.value = CallState.IDLE
        _activeEndpoint.value = null
    }

    fun sendAudio(data: ByteArray) {
        if (_callState.value == CallState.IN_CALL) {
            _activeEndpoint.value?.let { device ->
                connectionsClient.sendPayload(device.endpointId, Payload.fromBytes(data))
            }
        }
    }

    private fun sendString(endpointId: String, text: String) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(text.toByteArray()))
    }

    fun stopAll() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
    }
}
