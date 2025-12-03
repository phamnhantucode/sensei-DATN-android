package com.phamnhantucode.aicareercoach.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.utils.NetworkConnectivityObserver
import kotlinx.coroutines.delay

/**
 * A banner that shows when there's no internet connection.
 * Appears at the top of the screen with an animation.
 */
@Composable
fun NoInternetBanner(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    var showBanner by remember { mutableStateOf(!isConnected) }
    var showReconnectedMessage by remember { mutableStateOf(false) }
    var wasDisconnected by remember { mutableStateOf(false) }
    
    LaunchedEffect(isConnected) {
        if (!isConnected) {
            wasDisconnected = true
            showBanner = true
            showReconnectedMessage = false
        } else if (wasDisconnected) {
            // Show reconnected message briefly
            showBanner = true
            showReconnectedMessage = true
            delay(2000)
            showBanner = false
            showReconnectedMessage = false
        }
    }
    
    AnimatedVisibility(
        visible = showBanner,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        val backgroundColor = if (showReconnectedMessage) {
            Color(0xFF4CAF50) // Green for reconnected
        } else {
            MaterialTheme.colorScheme.error
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (showReconnectedMessage) {
                    "Back online"
                } else {
                    "No internet connection"
                },
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * A wrapper composable that shows content with a network connectivity banner.
 * The banner appears at the top when there's no internet connection.
 */
@Composable
fun NetworkAwareContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val connectivityObserver = remember { NetworkConnectivityObserver.getInstance(context) }
    val isConnected by connectivityObserver.observe().collectAsState(initial = true)
    
    Box(modifier = modifier.fillMaxSize()) {
        content()
        
        NoInternetBanner(
            isConnected = isConnected,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
