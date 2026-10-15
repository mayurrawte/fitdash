package app.fitdash.metrics

import java.util.Locale
import kotlin.math.roundToInt

data class Source(val title: String, val citation: String)

// What the info sheet shows: the equation, the inputs with the numbers used, where it comes from, and an optional caveat.
data class Explanation(val formula: String, val inputs: List<Pair<String, String>>, val source: Source, val note: String? = null)

data class FitnessValue<T>(val value: T, val explanation: Explanation)

data class FitnessAge(val years: Int, val realAge: Int, val vo2: Double) {
    val delta: Int get() = years - realAge
}

val JACKSON = Source("Jackson et al. 1990", "Prediction of functional aerobic capacity without exercise testing. Med Sci Sports Exerc 22(6):863–870.")

private const val REF_BMI = 25.0
private const val REF_PAR = 3

// NASA/Jackson physical activity rating 0–7, from mean weekly workout minutes over 28 days and mean steps per day.
fun paRating(weeklyWorkoutMinutes: Double, stepsPerDay: Double): Int = when {
    weeklyWorkoutMinutes >= 420 -> 7
    weeklyWorkoutMinutes >= 240 -> 6
    weeklyWorkoutMinutes >= 150 -> 5
    weeklyWorkoutMinutes >= 90 -> 4
    weeklyWorkoutMinutes > 0 && stepsPerDay >= 10_000 -> 4
    weeklyWorkoutMinutes >= 60 -> 3
    weeklyWorkoutMinutes > 0 -> 2
    stepsPerDay >= 7_000 -> 2
    stepsPerDay >= 3_000 -> 1
    else -> 0
}

fun paRatingRule(paR: Int): String = when (paR) {
    0 -> "under 3 000 steps a day, no workouts"
    1 -> "3 000–7 000 steps a day, no workouts"
    2 -> "workouts under 60 min a week, or over 7 000 steps a day"
    3 -> "workouts 60–90 min a week"
    4 -> "workouts 90–150 min a week, or 10 000+ steps with any workout"
    5 -> "workouts 150–240 min a week"
    6 -> "workouts 240–420 min a week"
    else -> "workouts over 420 min a week"
}

private fun genderTerm(g: Gender) = if (g == Gender.MALE) 1 else 0

fun jacksonVo2(age: Int, gender: Gender, bmi: Double, paR: Int): Double =
    56.363 + 1.921 * paR - 0.381 * age - 0.754 * bmi + 10.987 * genderTerm(gender)

fun fitnessAge(age: Int, gender: Gender, bmi: Double, paR: Int): FitnessValue<FitnessAge> {
    val vo2 = jacksonVo2(age, gender, bmi, paR)
    val raw = (56.363 + 1.921 * REF_PAR - 0.754 * REF_BMI + 10.987 * genderTerm(gender) - vo2) / 0.381
    val years = raw.roundToInt().coerceIn(18, 90)
    return FitnessValue(
        FitnessAge(years, age, vo2),
        Explanation(
            formula = "VO₂max = 56.363 + 1.921 × PA-R − 0.381 × age − 0.754 × BMI + 10.987 × G, G is 1 for men and 0 for women. Fitness age is the age at which someone of your gender with BMI 25 and PA-R 3 would have your VO₂max.",
            inputs = listOf(
                "Age" to age.toString(),
                "Gender" to genderLabel(gender),
                "BMI" to String.format(Locale.ENGLISH, "%.1f", bmi),
                "Activity rating" to "$paR of 7",
            ),
            source = JACKSON,
            note = "Activity rating $paR: ${paRatingRule(paR)}. Estimated VO₂max ${String.format(Locale.ENGLISH, "%.1f", vo2)} ml/kg/min.",
        ),
    )
}

val UTH = Source("Uth et al. 2004", "Estimation of VO2max from the ratio between HRmax and HRrest. Eur J Appl Physiol 91:111–115. Max HR from Tanaka et al. 2001.")
val MIFFLIN = Source("Mifflin et al. 1990", "A new predictive equation for resting energy expenditure in healthy individuals. Am J Clin Nutr 51:241–247. Activity factors are the standard Harris-Benedict multipliers.")

fun vo2Estimate(age: Int, restingHr: Double): FitnessValue<Double> {
    val maxHr = maxHrForAge(age)
    val vo2 = 15.3 * maxHr / restingHr
    return FitnessValue(
        vo2,
        Explanation(
            formula = "VO₂max = 15.3 × (max HR ÷ resting HR)",
            inputs = listOf(
                "Age" to age.toString(),
                "Max HR" to "$maxHr bpm (208 − 0.7 × age)",
                "Resting HR" to "${restingHr.roundToInt()} bpm",
            ),
            source = UTH,
            note = "Resting HR is your 28-day mean. A watch-measured VO₂max replaces this estimate when available.",
        ),
    )
}

data class Calories(val resting: Int, val daily: Int, val factor: Double)

fun activityFactor(stepsPerDay: Double): Double = when {
    stepsPerDay >= 12_500 -> 1.9
    stepsPerDay >= 10_000 -> 1.725
    stepsPerDay >= 7_500 -> 1.55
    stepsPerDay >= 5_000 -> 1.375
    else -> 1.2
}

fun calories(age: Int, gender: Gender, weightKg: Double, heightCm: Double, stepsPerDay: Double): FitnessValue<Calories> {
    val bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + if (gender == Gender.MALE) 5 else -161
    val factor = activityFactor(stepsPerDay)
    return FitnessValue(
        Calories(bmr.roundToInt(), (bmr * factor).roundToInt(), factor),
        Explanation(
            formula = "At rest = 10 × weight + 6.25 × height − 5 × age + 5 (men) or − 161 (women). Daily need = at rest × activity factor from your 7-day steps.",
            inputs = listOf(
                "Weight" to "${String.format(Locale.ENGLISH, "%.1f", weightKg)} kg",
                "Height" to "${heightCm.roundToInt()} cm",
                "Age" to age.toString(),
                "Gender" to genderLabel(gender),
                "Steps a day" to "${stepsPerDay.roundToInt()}",
                "Activity factor" to String.format(Locale.ENGLISH, "%.3f", factor).trimEnd('0'),
            ),
            source = MIFFLIN,
        ),
    )
}
