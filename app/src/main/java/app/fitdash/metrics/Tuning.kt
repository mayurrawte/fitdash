package app.fitdash.metrics

object Tuning {
    const val BASELINE_DAYS = 30L
    const val MIN_NIGHTS = 7

    const val WEIGHT_HRV = 0.4
    const val WEIGHT_RHR = 0.3
    const val WEIGHT_SLEEP = 0.3

    const val AT_BASELINE = 70.0
    const val FLOOR = 30.0
    const val HRV_PCT_SPAN = 15.0
    const val RHR_BPM_SPAN = 5.0

    const val SLEEP_TARGET_HOURS = 8.0
    const val STAGE_ADJUST_MAX = 10.0

    const val BAND_STEADY = 40
    const val BAND_PRIMED = 70

    const val STEP_GOAL = 9000
    const val EVENING_HOUR = 16
    const val TREND_DAYS = 14L
}
