package com.example.motioncues

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.motioncues.prefs.MotionMode
import com.example.motioncues.prefs.PrefsManager
import com.example.motioncues.service.MotionCuesService
import com.example.motioncues.ui.theme.MotionCuesTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PrefsManager
    private var hasOverlayPermission by mutableStateOf(false)
    private var currentMode by mutableStateOf(MotionMode.OFF)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshPermissionState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefsManager = PrefsManager(this)
        refreshPermissionState()
        currentMode = prefsManager.motionMode

        setContent {
            MotionCuesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        currentMode = currentMode,
                        onModeChanged = { mode -> setMotionMode(mode) },
                        onSettingsClick = { openSettings() },
                        hasOverlayPermission = hasOverlayPermission,
                        onRequestPermission = { requestOverlayPermission() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
        currentMode = prefsManager.motionMode
    }

    private fun refreshPermissionState() {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun setMotionMode(mode: MotionMode) {
        prefsManager.motionMode = mode
        currentMode = mode

        when (mode) {
            MotionMode.OFF -> {
                try {
                    stopService(Intent(this, MotionCuesService::class.java))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            MotionMode.ON, MotionMode.AUTO -> {
                if (!hasOverlayPermission) {
                    requestOverlayPermission()
                } else {
                    notifyModeChanged()
                    startCuesService()
                }
            }
        }
    }

    private fun notifyModeChanged() {
        try {
            val intent = Intent(this, MotionCuesService::class.java).apply {
                action = MotionCuesService.ACTION_MODE_CHANGED
            }
            startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCuesService() {
        val intent = Intent(this, MotionCuesService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentMode: MotionMode,
    onModeChanged: (MotionMode) -> Unit,
    onSettingsClick: () -> Unit,
    hasOverlayPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Motion Cues") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Vehicle Motion Cues",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Animated dots on screen edges help reduce motion sickness by showing vehicle movement",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!hasOverlayPermission) {
                PermissionCard(onRequestPermission = onRequestPermission)
                Spacer(modifier = Modifier.height(16.dp))
            }

            ModeSelector(
                currentMode = currentMode,
                onModeChanged = onModeChanged,
                enabled = hasOverlayPermission
            )

            Spacer(modifier = Modifier.height(24.dp))

            StatusCard(currentMode = currentMode)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Overlay Permission Required",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Motion Cues needs permission to draw over other apps to display the animated dots",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
fun ModeSelector(
    currentMode: MotionMode,
    onModeChanged: (MotionMode) -> Unit,
    enabled: Boolean = true
) {
    val modes = listOf(
        MotionMode.OFF to "Off",
        MotionMode.ON to "On - Always show cues",
        MotionMode.AUTO to "Auto - Detect vehicle motion"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .selectableGroup()
        ) {
            Text(
                text = "Motion Cues Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            modes.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .selectable(
                            selected = currentMode == mode,
                            onClick = { onModeChanged(mode) },
                            enabled = enabled,
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = null,
                        enabled = enabled
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (enabled)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusCard(currentMode: MotionMode) {
    val (statusText, statusColor) = when (currentMode) {
        MotionMode.OFF -> "Motion Cues Off" to MaterialTheme.colorScheme.onSurfaceVariant
        MotionMode.ON -> "Motion Cues Active" to MaterialTheme.colorScheme.primary
        MotionMode.AUTO -> "Auto-detecting vehicle motion" to MaterialTheme.colorScheme.tertiary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = if (currentMode == MotionMode.OFF)
                            MaterialTheme.colorScheme.outline
                        else
                            statusColor,
                        shape = RoundedCornerShape(6.dp)
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = statusColor
            )
        }
    }
}
