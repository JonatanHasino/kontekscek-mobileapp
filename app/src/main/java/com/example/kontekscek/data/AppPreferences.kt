package com.aldointelligence.kontekscek.data

import android.content.Context
import com.aldointelligence.kontekscek.data.model.TaskMode

enum class ThemeMode(val label: String) {
    LIGHT("Terang"),
    DARK("Gelap"),
    SYSTEM("Sistem"),
}

enum class TextSize(val label: String) {
    NORMAL("Normal"),
    LARGE("Besar"),
}

/**
 * Preferensi tampilan & perilaku aplikasi.
 */
interface AppPreferences {
    fun settings(): KonteksSettings

    fun save(settings: KonteksSettings)
}

data class KonteksSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val textSize: TextSize = TextSize.NORMAL,
    val defaultTaskMode: TaskMode = TaskMode.KULIAH,
    val autoStartTimer: Boolean = true,
)

class SharedPreferencesAppPreferences(context: Context) : AppPreferences {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun settings(): KonteksSettings {
        val theme =
            preferences.getString(KEY_THEME, null)?.let { saved ->
                ThemeMode.entries.firstOrNull { it.name == saved }
            } ?: ThemeMode.SYSTEM
        val text =
            preferences.getString(KEY_TEXT_SIZE, null)?.let { saved ->
                TextSize.entries.firstOrNull { it.name == saved }
            } ?: TextSize.NORMAL
        val mode =
            preferences.getString(KEY_DEFAULT_MODE, null)?.let { saved ->
                TaskMode.entries.firstOrNull { it.name == saved }
            } ?: TaskMode.KULIAH
        val autoTimer = preferences.getBoolean(KEY_AUTO_TIMER, true)
        return KonteksSettings(
            themeMode = theme,
            textSize = text,
            defaultTaskMode = mode,
            autoStartTimer = autoTimer,
        )
    }

    override fun save(settings: KonteksSettings) {
        preferences
            .edit()
            .putString(KEY_THEME, settings.themeMode.name)
            .putString(KEY_TEXT_SIZE, settings.textSize.name)
            .putString(KEY_DEFAULT_MODE, settings.defaultTaskMode.name)
            .putBoolean(KEY_AUTO_TIMER, settings.autoStartTimer)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "kontekscek_preferences"
        const val KEY_THEME = "theme_mode"
        const val KEY_TEXT_SIZE = "text_size"
        const val KEY_DEFAULT_MODE = "default_mode"
        const val KEY_AUTO_TIMER = "auto_start_timer"
    }
}
