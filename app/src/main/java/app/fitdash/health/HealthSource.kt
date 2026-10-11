package app.fitdash.health

import java.time.Instant
import java.time.ZonedDateTime

enum class Availability { AVAILABLE, NOT_INSTALLED, NEEDS_UPDATE }

interface HealthSource {
    val allPermissions: Set<String>
    fun availability(): Availability
    suspend fun grantedPermissions(): Set<String>
    suspend fun read(now: ZonedDateTime): HealthSnapshot
    suspend fun addWater(liters: Double, at: Instant): String?
    suspend fun deleteWater(id: String)
    suspend fun setHeight(meters: Double, at: Instant): Boolean
}
