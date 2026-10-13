package app.fitdash.health

import java.time.Instant
import java.time.ZonedDateTime

enum class Availability { AVAILABLE, NOT_INSTALLED, NEEDS_UPDATE }

interface HealthSource {
    val allPermissions: Set<String>
    // The "read older data" permission when this build declares it, else null.
    val historyPermission: String? get() = null
    // The "read while closed" permission when this build declares it and this Health Connect supports it, else null.
    suspend fun backgroundPermission(): String? = null
    fun availability(): Availability
    suspend fun grantedPermissions(): Set<String>
    // `since` limits record reads to that instant onwards (aggregates still cover the whole window); null reads everything.
    suspend fun read(now: ZonedDateTime, since: Instant? = null): HealthSnapshot
    suspend fun addWater(liters: Double, at: Instant): String?
    suspend fun deleteWater(id: String): Boolean
    suspend fun setHeight(meters: Double, at: Instant): Boolean
}
