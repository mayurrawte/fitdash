package app.fitdash.ui

import app.fitdash.health.Availability
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.HealthSource
import app.fitdash.health.ReadProgress
import app.fitdash.settings.NutrientKind
import java.time.Instant
import java.time.ZonedDateTime

// The preview sample days served as if they came from Health Connect. Nothing is read or written.
object DemoSource : HealthSource {
    override val allPermissions = setOf("demo")
    override fun availability() = Availability.AVAILABLE
    override suspend fun grantedPermissions() = allPermissions
    override suspend fun read(now: ZonedDateTime, since: Instant?, onProgress: (ReadProgress) -> Unit): HealthSnapshot = PreviewData.snapshot(now.toLocalDate())
    override suspend fun addWater(liters: Double, at: Instant): String? = null
    override suspend fun deleteWater(id: String) = false
    override suspend fun addSteps(start: Instant, end: Instant, count: Long) = false
    override suspend fun addSleep(start: Instant, end: Instant): String? = null
    override suspend fun deleteSleep(id: String) = false
    override suspend fun setHeight(meters: Double, at: Instant) = false
    override suspend fun addNutrient(kind: NutrientKind, amount: Double, at: Instant): String? = null
    override suspend fun deleteNutrient(id: String) = false
}
