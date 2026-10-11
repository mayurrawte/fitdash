package app.fitdash.health

import java.time.ZonedDateTime

enum class Availability { AVAILABLE, NOT_INSTALLED, NEEDS_UPDATE }

interface HealthSource {
    val allPermissions: Set<String>
    fun availability(): Availability
    suspend fun grantedPermissions(): Set<String>
    suspend fun read(now: ZonedDateTime): HealthSnapshot
}
