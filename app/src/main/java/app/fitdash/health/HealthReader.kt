package app.fitdash.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.fitdash.metrics.Tuning
import java.time.Duration
import java.time.Period
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass

class HealthReader(private val context: Context) : HealthSource {
    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override val allPermissions: Set<String> =
        ALL_TYPES.map { HealthPermission.getReadPermission(it.type) }.toSet()

    override fun availability() = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_UPDATE
        else -> Availability.NOT_INSTALLED
    }

    override suspend fun grantedPermissions(): Set<String> = client.permissionController.getGrantedPermissions()

    override suspend fun read(now: ZonedDateTime): HealthSnapshot {
        val granted = grantedPermissions()
        fun can(type: KClass<out Record>) = HealthPermission.getReadPermission(type) in granted

        val start = now.toLocalDate().minusDays(Tuning.BASELINE_DAYS).atStartOfDay(now.zone)
        val range = TimeRangeFilter.between(start.toInstant(), now.toInstant())

        val records = mutableMapOf<KClass<out Record>, List<Record>>()
        val sources = ALL_TYPES.map { info ->
            val list = if (can(info.type)) readAllOrEmpty(info.type, range) else emptyList()
            records[info.type] = list
            SourceStatus(info.label, can(info.type), list.isNotEmpty(), list.map { it.metadata.dataOrigin.packageName }.toSet())
        }

        val hourly = if (can(StepsRecord::class)) {
            client.aggregateGroupByDuration(
                AggregateGroupByDurationRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        now.toLocalDate().minusDays(1).atStartOfDay(),
                        now.toLocalDateTime(),
                    ),
                    timeRangeSlicer = Duration.ofHours(1),
                ),
            ).map { HourBucket(it.startTime, it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        } else emptyList()

        val metrics = buildSet<AggregateMetric<*>> {
            if (can(StepsRecord::class)) add(StepsRecord.COUNT_TOTAL)
            if (can(DistanceRecord::class)) add(DistanceRecord.DISTANCE_TOTAL)
            if (can(TotalCaloriesBurnedRecord::class)) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
        }
        val daily = if (metrics.isEmpty()) emptyList() else {
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = metrics,
                    timeRangeFilter = TimeRangeFilter.between(start.toLocalDateTime(), now.toLocalDateTime()),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            ).map {
                DayActivity(
                    date = it.startTime.toLocalDate(),
                    steps = it.result[StepsRecord.COUNT_TOTAL] ?: 0L,
                    distanceMeters = it.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0,
                    kcal = it.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0,
                )
            }.filter { it.steps > 0 || it.distanceMeters > 0 }
        }

        val totals = GENERIC_TYPES.filter { it.dailyTotal != null && can(it.type) }
        val dailyTotals = if (totals.isEmpty()) emptyList() else {
            client.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = totals.map { it.dailyTotal!!.metric }.toSet(),
                    timeRangeFilter = TimeRangeFilter.between(start.toLocalDateTime(), now.toLocalDateTime()),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            )
        }

        val generic = GENERIC_TYPES.mapNotNull { g ->
            val total = g.dailyTotal
            val shown = if (total != null) {
                dailyTotals.mapNotNull { day ->
                    total.value(day.result)?.let { Sample(day.startTime.atZone(now.zone).toInstant(), it) }
                }
            } else records[g.type].orEmpty().mapNotNull(g::sample).sortedBy { it.time }
            if (shown.isEmpty()) null else GenericSeries(g.label, g.unit, shown)
        }

        return HealthSnapshot(
            sleep = records.of<SleepSessionRecord>().map { s ->
                SleepSession(s.startTime, s.endTime, s.stages.mapNotNull { st ->
                    stageType(st.stage)?.let { SleepStage(st.startTime, st.endTime, it) }
                })
            },
            hrv = records.of<HeartRateVariabilityRmssdRecord>().map { Sample(it.time, it.heartRateVariabilityMillis) },
            restingHr = records.of<RestingHeartRateRecord>().map { Sample(it.time, it.beatsPerMinute.toDouble()) },
            spo2 = records.of<OxygenSaturationRecord>().map { Sample(it.time, it.percentage.value) },
            respiratoryRate = records.of<RespiratoryRateRecord>().map { Sample(it.time, it.rate) },
            skinTempDelta = records.of<SkinTemperatureRecord>().flatMap { r -> r.deltas.map { Sample(it.time, it.delta.inCelsius) } },
            hourlySteps = hourly,
            dailyActivity = daily,
            exercise = records.of<ExerciseSessionRecord>().map { Interval(it.startTime, it.endTime) },
            generic = generic,
            sources = sources,
        )
    }

    // One type failing (e.g. skin temperature unsupported on this device) must not hide the rest.
    private suspend fun readAllOrEmpty(type: KClass<out Record>, range: TimeRangeFilter): List<Record> = try {
        val out = mutableListOf<Record>()
        var token: String? = null
        do {
            @Suppress("UNCHECKED_CAST")
            val res = client.readRecords(
                ReadRecordsRequest(recordType = type as KClass<Record>, timeRangeFilter = range, pageSize = 5000, pageToken = token),
            )
            out += res.records
            token = res.pageToken
        } while (token != null)
        out
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        emptyList()
    }
}

private inline fun <reified T : Record> Map<KClass<out Record>, List<Record>>.of(): List<T> =
    this[T::class].orEmpty().filterIsInstance<T>()

private fun stageType(stage: Int): StageType? = when (stage) {
    SleepSessionRecord.STAGE_TYPE_AWAKE,
    SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED,
    SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> StageType.AWAKE
    SleepSessionRecord.STAGE_TYPE_LIGHT -> StageType.LIGHT
    SleepSessionRecord.STAGE_TYPE_DEEP -> StageType.DEEP
    SleepSessionRecord.STAGE_TYPE_REM -> StageType.REM
    SleepSessionRecord.STAGE_TYPE_SLEEPING -> StageType.SLEEPING
    else -> null
}
