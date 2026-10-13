package app.fitdash.settings

import android.content.Context
import app.fitdash.ui.theme.NumbersFont
import app.fitdash.ui.theme.ThemeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

data class Settings(
    val theme: ThemeId = ThemeId.SOFT,
    val stepGoal: Int = 9000,
    val sleepHours: Double = 8.0,
    val miles: Boolean = false,
    val waterGoalMl: Int = 2500,
    val heightCm: Int? = null,
    val activeGoal: Int = 30,
    val clock24: Boolean = false,
    val weightLb: Boolean = false,
    val fahrenheit: Boolean = false,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val showWater: Boolean = true,
    val showWorkouts: Boolean = true,
    val showHeartRate: Boolean = true,
    val showSuggestions: Boolean = true,
    val showBody: Boolean = true,
    val numbersFont: NumbersFont = NumbersFont.OUTFIT,
    // Stored as a year so the age (and the zones from it) move on by themselves.
    val birthYear: Int? = null,
    // Read Health Connect every few hours while closed, for the widget and a fresh cold open.
    val backgroundRefresh: Boolean = false,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("fitdash", Context.MODE_PRIVATE)
    private val _flow = MutableStateFlow(read())
    val flow: StateFlow<Settings> = _flow

    private fun read() = Settings(
        theme = runCatching { ThemeId.valueOf(prefs.getString("theme", null) ?: "SOFT") }.getOrDefault(ThemeId.TEAL),
        stepGoal = prefs.getInt("stepGoal", 9000),
        sleepHours = prefs.getInt("sleepTenths", 80) / 10.0,
        miles = prefs.getBoolean("miles", false),
        waterGoalMl = prefs.getInt("waterGoalMl", 2500),
        heightCm = prefs.getInt("heightCm", 0).takeIf { it > 0 },
        activeGoal = prefs.getInt("activeGoal", 30),
        clock24 = prefs.getBoolean("clock24", false),
        weightLb = prefs.getBoolean("weightLb", false),
        fahrenheit = prefs.getBoolean("fahrenheit", false),
        reduceMotion = prefs.getBoolean("reduceMotion", false),
        haptics = prefs.getBoolean("haptics", true),
        showWater = prefs.getBoolean("showWater", true),
        showWorkouts = prefs.getBoolean("showWorkouts", true),
        showHeartRate = prefs.getBoolean("showHeartRate", true),
        showSuggestions = prefs.getBoolean("showSuggestions", true),
        showBody = prefs.getBoolean("showBody", true),
        // Installs that saved settings before Outfit existed keep Inter; only fresh installs start on Outfit.
        birthYear = prefs.getInt("birthYear", 0).takeIf { it > 0 },
        backgroundRefresh = prefs.getBoolean("backgroundRefresh", false),
        numbersFont = runCatching { NumbersFont.valueOf(prefs.getString("numbersFont", null) ?: if (prefs.contains("theme")) "INTER" else "OUTFIT") }.getOrDefault(NumbersFont.OUTFIT),
    )

    fun update(change: (Settings) -> Settings) {
        val s = change(_flow.value)
        prefs.edit().putString("theme", s.theme.name).putInt("stepGoal", s.stepGoal)
            .putInt("sleepTenths", (s.sleepHours * 10).roundToInt()).putBoolean("miles", s.miles)
            .putInt("waterGoalMl", s.waterGoalMl).putInt("heightCm", s.heightCm ?: 0).putInt("activeGoal", s.activeGoal)
            .putBoolean("clock24", s.clock24).putBoolean("weightLb", s.weightLb).putBoolean("fahrenheit", s.fahrenheit)
            .putBoolean("reduceMotion", s.reduceMotion).putBoolean("haptics", s.haptics)
            .putBoolean("showWater", s.showWater).putBoolean("showWorkouts", s.showWorkouts).putBoolean("showHeartRate", s.showHeartRate)
            .putBoolean("showSuggestions", s.showSuggestions).putBoolean("showBody", s.showBody)
            .putString("numbersFont", s.numbersFont.name).putInt("birthYear", s.birthYear ?: 0).putBoolean("backgroundRefresh", s.backgroundRefresh).apply()
        _flow.value = s
    }

    companion object {
        @Volatile private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
