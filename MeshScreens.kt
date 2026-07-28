package com.example

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MeshViewModel,
    onNavigateToCall: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val activeEndpoint by viewModel.activeEndpoint.collectAsState()
    val callHistory by viewModel.callHistory.collectAsState()

    LaunchedEffect(callState) {
        if (callState != CallState.IDLE) {
            onNavigateToCall()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MeshCall", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.startScanning() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Status", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Bluetooth & Wi-Fi Direct Mesh Active")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Nearby Devices", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (devices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No devices found. Tap scan to search.")
                    }
                }
            } else {
                items(devices) { device ->
                    DeviceItem(
                        device = device,
                        onConnectClick = { viewModel.connectToDevice(device) },
                        onCallClick = { viewModel.initiateCall(device) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recent Calls", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (callHistory.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No recent calls.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(callHistory) { history ->
                    CallHistoryItem(history = history)
                }
            }
            
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
fun CallHistoryItem(history: CallHistory) {
    val formatter = remember { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()) }
    val dateString = formatter.format(java.util.Date(history.timestamp))
    val minutes = history.durationSeconds / 60
    val seconds = history.durationSeconds % 60
    val durationString = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (history.isIncoming) Icons.Default.Call else Icons.Default.CallEnd, // Simplified icon choice
                    contentDescription = null,
                    tint = if (history.isIncoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(history.callerName, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (history.isIncoming) "Incoming" else "Outgoing",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(" • ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dateString, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(durationString, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DeviceItem(
    device: MeshDevice,
    onConnectClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnectClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, fontWeight = FontWeight.SemiBold)
                if (device.isConnecting) {
                    Text("Connecting...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Available", style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(
                onClick = onCallClick,
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call")
            }
        }
    }
}

@Composable
fun CallingScreen(
    viewModel: MeshViewModel,
    onNavigateBack: () -> Unit
) {
    val callState by viewModel.callState.collectAsState()
    val activeEndpoint by viewModel.activeEndpoint.collectAsState()
    
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }

    // Start audio automatically when transitioning to IN_CALL if we initiated
    LaunchedEffect(callState) {
        if (callState == CallState.IN_CALL) {
            viewModel.startAudio()
        }
        if (callState == CallState.IDLE) {
            onNavigateBack()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mesh Active Badge
                Row(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "MESH ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Icon(
                    Icons.Default.Wifi,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Caller Profile Section
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Simulated Pulse Rings
                    if (callState == CallState.RINGING_INCOMING || callState == CallState.RINGING_OUTGOING) {
                        Box(modifier = Modifier.size(200.dp).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape))
                        Box(modifier = Modifier.size(160.dp).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape))
                    }
                    
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)
                                )
                            )
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeEndpoint?.name?.take(2)?.uppercase() ?: "RP",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = activeEndpoint?.name ?: "Unknown",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val statusText = when (callState) {
                    CallState.RINGING_INCOMING -> "Incoming Call..."
                    CallState.RINGING_OUTGOING -> "Calling..."
                    CallState.IN_CALL -> "Connected"
                    else -> ""
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (callState == CallState.IN_CALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (callState == CallState.IN_CALL) {
                        Text(
                            text = "via Bluetooth Relay",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (callState == CallState.IN_CALL) {
                    val duration by viewModel.callDuration.collectAsState()
                    val minutes = duration / 60
                    val seconds = duration % 60
                    val timeString = String.format("%02d:%02d", minutes, seconds)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            // Call Controls Layer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(30.dp, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 32.dp, vertical = 40.dp)
            ) {
                if (callState == CallState.RINGING_INCOMING) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.rejectCall() }) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .shadow(24.dp, spotColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CallEnd, contentDescription = "Reject", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("REJECT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.acceptCall() }) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .shadow(24.dp, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Accept", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(32.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ACCEPT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // First Row Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                                        .clickable {
                                            isMuted = !isMuted
                                            viewModel.toggleMute(isMuted)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Mute",
                                        tint = if (isMuted) MaterialTheme.colorScheme.background else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("MUTE", style = MaterialTheme.typography.labelSmall, color = if (isMuted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.background)
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Dialpad, contentDescription = "Keypad", tint = Color.White)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("KEYPAD", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(if (isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background)
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), CircleShape)
                                        .clickable { isSpeakerOn = !isSpeakerOn },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = "Speaker",
                                        tint = if (isSpeakerOn) MaterialTheme.colorScheme.background else Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("SPEAKER", style = MaterialTheme.typography.labelSmall, color = if (isSpeakerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        // End Call Button
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                                .clickable { viewModel.endCall() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "End Call",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
