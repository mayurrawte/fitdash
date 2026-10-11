package app.fitdash.settings

import android.content.Context
import app.fitdash.ui.theme.ThemeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(val theme: ThemeId = ThemeId.TEAL, val stepGoal: Int = 9000, val sleepHours: Double = 8.0, val miles: Boolean = false)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("fitdash", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(read())
    val flow: StateFlow<Settings> = _flow

    private fun read() = Settings(
        theme = runCatching { ThemeId.valueOf(prefs.getString("theme", null) ?: "TEAL") }.getOrDefault(ThemeId.TEAL),
        stepGoal = prefs.getInt("stepGoal", 9000),
        sleepHours = prefs.getFloat("sleepHours", 8f).toDouble(),
        miles = prefs.getBoolean("miles", false),
    )

    fun update(change: (Settings) -> Settings) {
        val s = change(_flow.value)
        prefs.edit().putString("theme", s.theme.name).putInt("stepGoal", s.stepGoal)
            .putFloat("sleepHours", s.sleepHours.toFloat()).putBoolean("miles", s.miles).apply()
        _flow.value = s
    }
}
