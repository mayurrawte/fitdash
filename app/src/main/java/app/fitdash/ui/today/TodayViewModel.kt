package app.fitdash.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fitdash.health.Availability
import app.fitdash.health.HealthSource
import app.fitdash.health.HealthSnapshot
import java.time.Instant
import app.fitdash.health.readFresh
import app.fitdash.health.SnapshotStore
import app.fitdash.health.CachedSnapshot
import app.fitdash.metrics.Goals
import app.fitdash.metrics.Tuning
import java.time.Duration
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.buildToday
import app.fitdash.metrics.withEstimatedRestingHr
import app.fitdash.settings.Settings
import app.fitdash.ui.aiSummary
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.coroutines.cancellation.CancellationException

// Whether Health Connect lets fitdash read while it is closed, for background refresh and widgets.
enum class BackgroundAccess { UNSUPPORTED, NOT_GRANTED, GRANTED }

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class NeedsHealthConnect(val update: Boolean) : TodayUiState
    data object NeedsPermission : TodayUiState
    data object NoData : TodayUiState
    data object Failed : TodayUiState
    data class Ready(
        val data: TodayData,
        val aiText: String = "",
        val refreshing: Boolean = false,
        val error: Boolean = false,
        // True when this build can ask for the "read older data" permission and it is not granted yet.
        val canAskHistory: Boolean = false,
        val background: BackgroundAccess = BackgroundAccess.UNSUPPORTED,
    ) : TodayUiState
}

class TodayViewModel(
    private val source: HealthSource,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
    private val settings: StateFlow<Settings> = MutableStateFlow(Settings()),
    private val work: CoroutineDispatcher = Dispatchers.Default,
    // Where a failed read is reported; the activity passes the Android logger, tests leave it silent.
    private val onReadFailed: (Throwable) -> Unit = {},
    // Last snapshot on disk, shown instantly on a cold start while the fresh read runs.
    private val store: SnapshotStore? = null,
    // After each save, so home-screen widgets redraw from the new cache.
    private val onCacheSaved: suspend () -> Unit = {},
) : ViewModel() {
    private val _state = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val state: StateFlow<TodayUiState> = _state

    private var job: Job? = null
    private var rebuildJob: Job? = null
    private var snapshot: HealthSnapshot? = null
    // The un-augmented snapshot: what incremental reads merge into and what the cache stores.
    private var raw: HealthSnapshot? = null
    private var lastNow: ZonedDateTime? = null
    private var lastRead: Instant? = null
    private var lastFullRead: Instant? = null
    private var syncPending = false

    val selectedDate = MutableStateFlow<LocalDate?>(null)

    private val permissionRequests = Channel<Unit>(Channel.CONFLATED)
    val permissionNeeded: Flow<Unit> = permissionRequests.receiveAsFlow()
    private val messageChannel = Channel<String>(Channel.CONFLATED)
    val messages: Flow<String> = messageChannel.receiveAsFlow()
    private var lastWaterId: String? = null

    init {
        viewModelScope.launch { settings.map { it.goals() }.distinctUntilChanged().drop(1).collect { rebuild() } }
    }

    fun select(date: LocalDate) {
        selectedDate.value = date.takeUnless { it == clock().toLocalDate() }
        rebuild()
    }

    private fun rebuild() {
        val snapshot = snapshot ?: return
        // Past midnight the stored "now" is yesterday; refreshing it keeps "today" meaning today until the next read.
        val now = lastNow?.let { if (it.toLocalDate() == clock().toLocalDate()) it else clock().also { n -> lastNow = n } } ?: return
        val goals = currentGoals()
        val date = selectedDate.value ?: now.toLocalDate()
        rebuildJob?.cancel()
        rebuildJob = viewModelScope.launch {
            val (data, text) = withContext(work) { build(snapshot, now, date, goals) }
            val prev = _state.value as? TodayUiState.Ready
            _state.value = TodayUiState.Ready(data, text, prev?.refreshing ?: false, prev?.error ?: false, prev?.canAskHistory ?: false, prev?.background ?: BackgroundAccess.UNSUPPORTED)
        }
    }

    private fun currentGoals() = settings.value.goals()

    private fun Settings.goals() = Goals(stepGoal, sleepHours, waterGoalMl / 1000.0, activeGoal, birthYear, gender)

    private fun build(snapshot: HealthSnapshot, now: ZonedDateTime, date: LocalDate, goals: Goals): Pair<TodayData, String> {
        val data = buildToday(snapshot, now, date, goals, augmented = true)
        return data to aiSummary(snapshot, data, now)
    }

    val permissions: Set<String> get() = source.allPermissions

    private suspend fun writeFailed() {
        val granted = source.grantedPermissions()
        if (source.allPermissions.any { ".WRITE_" in it && it !in granted }) permissionRequests.trySend(Unit)
        else messageChannel.trySend("Couldn't save to Health Connect")
    }

    fun addWater(ml: Int) {
        viewModelScope.launch {
            val id = source.addWater(ml / 1000.0, clock().toInstant())
            if (id == null) {
                writeFailed()
            } else {
                lastWaterId = id
                load()
            }
        }
    }

    fun undoWater() {
        val id = lastWaterId ?: return
        lastWaterId = null
        viewModelScope.launch {
            if (source.deleteWater(id)) load() else messageChannel.trySend("Couldn't save to Health Connect")
        }
    }

    fun setHeight(cm: Int) {
        viewModelScope.launch {
            if (source.setHeight(cm / 100.0, clock().toInstant())) load() else writeFailed()
        }
    }

    // The user opened their wearable's app to sync; the next resume reads regardless of the reload window.
    fun syncRequested() {
        syncPending = true
    }

    // Resuming within RELOAD_SECONDS of a successful read keeps the snapshot; `force` (pull to refresh, writes) always reads.
    fun load(force: Boolean = true) {
        if (!force && !syncPending) {
            val last = lastRead
            val ready = _state.value as? TodayUiState.Ready
            if (last != null && ready != null && !ready.error && Duration.between(last, clock().toInstant()).seconds < Tuning.RELOAD_SECONDS) {
                rebuild()
                return
            }
        }
        syncPending = false
        job?.cancel()
        rebuildJob?.cancel()
        job = viewModelScope.launch {
            when (source.availability()) {
                Availability.NOT_INSTALLED -> {
                    _state.value = TodayUiState.NeedsHealthConnect(update = false)
                    return@launch
                }
                Availability.NEEDS_UPDATE -> {
                    _state.value = TodayUiState.NeedsHealthConnect(update = true)
                    return@launch
                }
                Availability.AVAILABLE -> Unit
            }
            val now = clock()
            val back = snapshot?.windowDays?.minus(1) ?: (Tuning.BASELINE_DAYS - 1)
            if (selectedDate.value?.isBefore(now.toLocalDate().minusDays(back)) == true) selectedDate.value = null
            // Cold start: put the cached dashboard up first, then refresh it.
            if (raw == null && store != null && _state.value !is TodayUiState.Ready) {
                val cached = withContext(work) { store.load() }
                if (cached != null && cached.snapshot.sources.any { it.hasData }) {
                    raw = cached.snapshot
                    lastRead = cached.readAt
                    lastFullRead = cached.fullReadAt
                    val date = selectedDate.value ?: now.toLocalDate()
                    val (augmented, built) = withContext(work) {
                        val augmented = withEstimatedRestingHr(cached.snapshot, now.zone, now.toLocalDate())
                        augmented to build(augmented, now, date, currentGoals())
                    }
                    snapshot = augmented
                    lastNow = now
                    _state.value = TodayUiState.Ready(built.first, built.second, refreshing = true)
                }
            }
            val previous = _state.value as? TodayUiState.Ready
            if (previous != null) _state.value = previous.copy(refreshing = true)
            _state.value = try {
                val granted = source.grantedPermissions()
                if (granted.none { it in source.allPermissions }) {
                    TodayUiState.NeedsPermission
                } else {
                    val result = readFresh(source, raw, lastRead, lastFullRead, now)
                    val read = result.snapshot
                    if (result.full) lastFullRead = now.toInstant()
                    val canAskHistory = source.historyPermission?.let { it !in granted } ?: false
                    val background = source.backgroundPermission()?.let { if (it in granted) BackgroundAccess.GRANTED else BackgroundAccess.NOT_GRANTED } ?: BackgroundAccess.UNSUPPORTED
                    val date = selectedDate.value ?: now.toLocalDate()
                    val goals = currentGoals()
                    val (augmented, built) = withContext(work) {
                        val augmented = withEstimatedRestingHr(read, now.zone, now.toLocalDate())
                        augmented to build(augmented, now, date, goals)
                    }
                    raw = read
                    snapshot = augmented
                    lastNow = now
                    lastRead = now.toInstant()
                    store?.let { st ->
                        withContext(work) { st.save(CachedSnapshot(read, now.toInstant(), lastFullRead ?: now.toInstant())) }
                        onCacheSaved()
                    }
                    if (read.selectedDateOutOfWindow(selectedDate.value, now)) selectedDate.value = null
                    if (read.sources.none { it.hasData }) TodayUiState.NoData
                    else TodayUiState.Ready(built.first, built.second, canAskHistory = canAskHistory, background = background)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onReadFailed(e)
                (_state.value as? TodayUiState.Ready)?.copy(refreshing = false, error = true) ?: TodayUiState.Failed
            }
        }
    }
}

private fun HealthSnapshot.selectedDateOutOfWindow(date: LocalDate?, now: ZonedDateTime) =
    date != null && date.isBefore(now.toLocalDate().minusDays(windowDays - 1))
