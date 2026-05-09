package com.example.motioncues

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.motioncues.prefs.*
import com.example.motioncues.service.MotionCuesService
import com.example.motioncues.ui.theme.MotionCuesTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefsManager = PrefsManager(this)

        setContent {
            MotionCuesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        prefsManager = prefsManager,
                        onNavigateBack = { finish() },
                        onSettingsChanged = { notifyServiceUpdate() }
                    )
                }
            }
        }
    }

    private fun notifyServiceUpdate() {
        val intent = Intent(this, MotionCuesService::class.java).apply {
            action = MotionCuesService.ACTION_UPDATE_APPEARANCE
        }
        try {
            startService(intent)
        } catch (e: Exception) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefsManager: PrefsManager,
    onNavigateBack: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    var pattern by remember { mutableStateOf(prefsManager.pattern) }
    var colorIndex by remember { mutableStateOf(prefsManager.dotColorIndex) }
    var dotSize by remember { mutableStateOf(prefsManager.dotSize) }
    var dotCount by remember { mutableStateOf(prefsManager.dotCount) }

    val updateSetting: () -> Unit = {
        prefsManager.pattern = pattern
        prefsManager.dotColorIndex = colorIndex
        prefsManager.dotSize = dotSize
        prefsManager.dotCount = dotCount
        onSettingsChanged()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Customize Appearance") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            PatternSection(
                currentPattern = pattern,
                onPatternChanged = { newPattern ->
                    pattern = newPattern
                    updateSetting()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ColorSection(
                currentIndex = colorIndex,
                onColorChanged = { newIndex ->
                    colorIndex = newIndex
                    updateSetting()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DotSizeSection(
                currentSize = dotSize,
                onSizeChanged = { newSize ->
                    dotSize = newSize
                    updateSetting()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            DotCountSection(
                currentCount = dotCount,
                onCountChanged = { newCount ->
                    dotCount = newCount
                    updateSetting()
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun PatternSection(
    currentPattern: DotPattern,
    onPatternChanged: (DotPattern) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Pattern",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Regular is stable and predictable. Dynamic is more engaging.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                listOf(
                    DotPattern.REGULAR to "Regular",
                    DotPattern.DYNAMIC to "Dynamic"
                ).forEach { (pattern, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onPatternChanged(pattern) }
                            .padding(horizontal = 4.dp)
                    ) {
                        OutlinedCard(
                            border = BorderStroke(
                                width = if (currentPattern == pattern) 2.dp else 1.dp,
                                color = if (currentPattern == pattern)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (currentPattern == pattern)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSection(
    currentIndex: Int,
    onColorChanged: (Int) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Color",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CueColor.all.forEachIndexed { index, cueColor ->
                    val isSelected = index == currentIndex
                    val color = if (cueColor.color != Color.Unspecified) {
                        cueColor.color
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .selectable(
                                selected = isSelected,
                                onClick = { onColorChanged(index) }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(color)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DotSizeSection(
    currentSize: Float,
    onSizeChanged: (Float) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Dot Size",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${currentSize.toInt()}dp",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = currentSize,
                onValueChange = onSizeChanged,
                valueRange = 3f..12f,
                steps = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Small", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Large", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DotCountSection(
    currentCount: DotCount,
    onCountChanged: (DotCount) -> Unit
) {
    SettingsCard {
        Column {
            Text(
                text = "Dot Count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                listOf(
                    DotCount.NORMAL to "Normal",
                    DotCount.MORE to "More"
                ).forEach { (count, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCountChanged(count) }
                            .padding(horizontal = 4.dp)
                    ) {
                        OutlinedCard(
                            border = BorderStroke(
                                width = if (currentCount == count) 2.dp else 1.dp,
                                color = if (currentCount == count)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = if (currentCount == count)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
