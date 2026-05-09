package com.example.motioncues.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

enum class MotionMode {
    OFF, ON, AUTO
}

enum class DotPattern {
    REGULAR, DYNAMIC
}

enum class DotCount {
    NORMAL, MORE
}

data class CueColor(
    val name: String,
    val color: Color,
    val androidColor: Int
) {
    companion object {
        val AUTO = CueColor("Auto", Color.Unspecified, 0)
        val BLUE = CueColor("Blue", Color(0xFF4A90D9), 0xFF4A90D9.toInt())
        val GREEN = CueColor("Green", Color(0xFF34C759), 0xFF34C759.toInt())
        val ORANGE = CueColor("Orange", Color(0xFFFF9500), 0xFFFF9500.toInt())
        val PURPLE = CueColor("Purple", Color(0xFFAF52DE), 0xFFAF52DE.toInt())
        val RED = CueColor("Red", Color(0xFFFF3B30), 0xFFFF3B30.toInt())

        val all = listOf(AUTO, BLUE, GREEN, ORANGE, PURPLE, RED)
    }
}

class PrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "motion_cues_prefs",
        Context.MODE_PRIVATE
    )

    var motionMode: MotionMode
        get() = runCatching { MotionMode.valueOf(prefs.getString(KEY_MOTION_MODE, MotionMode.OFF.name) ?: MotionMode.OFF.name) }.getOrDefault(MotionMode.OFF)
        set(value) = prefs.edit().putString(KEY_MOTION_MODE, value.name).apply()

    var pattern: DotPattern
        get() = runCatching { DotPattern.valueOf(prefs.getString(KEY_PATTERN, DotPattern.REGULAR.name) ?: DotPattern.REGULAR.name) }.getOrDefault(DotPattern.REGULAR)
        set(value) = prefs.edit().putString(KEY_PATTERN, value.name).apply()

    var dotColorIndex: Int
        get() = prefs.getInt(KEY_COLOR_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_COLOR_INDEX, value).apply()

    var dotSize: Float
        get() = prefs.getFloat(KEY_DOT_SIZE, 5f)
        set(value) = prefs.edit().putFloat(KEY_DOT_SIZE, value).apply()

    var dotCount: DotCount
        get() = runCatching { DotCount.valueOf(prefs.getString(KEY_DOT_COUNT, DotCount.NORMAL.name) ?: DotCount.NORMAL.name) }.getOrDefault(DotCount.NORMAL)
        set(value) = prefs.edit().putString(KEY_DOT_COUNT, value.name).apply()

    fun getSelectedColor(): CueColor {
        val index = dotColorIndex
        return if (index >= 0 && index < CueColor.all.size) {
            CueColor.all[index]
        } else {
            CueColor.AUTO
        }
    }

    companion object {
        private const val KEY_MOTION_MODE = "motion_mode"
        private const val KEY_PATTERN = "pattern"
        private const val KEY_COLOR_INDEX = "color_index"
        private const val KEY_DOT_SIZE = "dot_size"
        private const val KEY_DOT_COUNT = "dot_count"
    }
}
