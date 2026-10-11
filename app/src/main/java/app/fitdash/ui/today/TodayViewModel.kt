package app.fitdash.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fitdash.health.Availability
import app.fitdash.health.HealthSource
import app.fitdash.health.HealthSnapshot
import app.fitdash.metrics.Goals
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

sealed interface TodayUiState {
    data object Loading : TodayUiState
    data class NeedsHealthConnect(val update: Boolean) : TodayUiState
    data object NeedsPermission : TodayUiState
    data object NoData : TodayUiState
    data object Failed : TodayUiState
    data class Ready(val data: TodayData, val aiText: String = "", val refreshing: Boolean = false, val error: Boolean = false) : TodayUiState
}

class TodayViewModel(
    private val source: HealthSource,
    private val clock: () -> ZonedDateTime = { ZonedDateTime.now() },
    private val settings: StateFlow<Settings> = MutableStateFlow(Settings()),
    private val work: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _state = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val state: StateFlow<TodayUiState> = _state

    private var job: Job? = null
    private var rebuildJob: Job? = null
    private var snapshot: HealthSnapshot? = null
    private var lastNow: ZonedDateTime? = null

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
        selectedDate.value = date.takeUnless { it == (lastNow ?: clock()).toLocalDate() }
        rebuild()
    }

    private fun rebuild() {
        val snapshot = snapshot ?: return
        val now = lastNow ?: return
        val goals = currentGoals()
        val date = selectedDate.value ?: now.toLocalDate()
        rebuildJob?.cancel()
        rebuildJob = viewModelScope.launch {
            val (data, text) = withContext(work) { build(snapshot, now, date, goals) }
            val prev = _state.value as? TodayUiState.Ready
            _state.value = TodayUiState.Ready(data, text, prev?.refreshing ?: false, prev?.error ?: false)
        }
    }

    private fun currentGoals() = settings.value.goals()

    private fun Settings.goals() = Goals(stepGoal, sleepHours, waterGoalMl / 1000.0)

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

    fun load() {
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
            if (selectedDate.value?.isBefore(now.toLocalDate().minusDays(29)) == true) selectedDate.value = null
            val previous = _state.value as? TodayUiState.Ready
            if (previous != null) _state.value = previous.copy(refreshing = true)
            _state.value = try {
                if (source.grantedPermissions().none { it in source.allPermissions }) {
                    TodayUiState.NeedsPermission
                } else {
                    val read = source.read(now)
                    val date = selectedDate.value ?: now.toLocalDate()
                    val goals = currentGoals()
                    val (augmented, built) = withContext(work) {
                        val augmented = withEstimatedRestingHr(read, now.zone, now.toLocalDate())
                        augmented to build(augmented, now, date, goals)
                    }
                    snapshot = augmented
                    lastNow = now
                    if (read.sources.none { it.hasData }) TodayUiState.NoData
                    else TodayUiState.Ready(built.first, built.second)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                (_state.value as? TodayUiState.Ready)?.copy(refreshing = false, error = true) ?: TodayUiState.Failed
            }
        }
    }
}
