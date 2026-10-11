package app.fitdash.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fitdash.health.Availability
import app.fitdash.health.HealthSource
import app.fitdash.metrics.TodayData
import app.fitdash.metrics.buildToday
import app.fitdash.ui.aiSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
) : ViewModel() {
    private val _state = MutableStateFlow<TodayUiState>(TodayUiState.Loading)
    val state: StateFlow<TodayUiState> = _state

    private var job: Job? = null

    val permissions: Set<String> get() = source.allPermissions

    fun load() {
        job?.cancel()
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
            val previous = _state.value as? TodayUiState.Ready
            if (previous != null) _state.value = previous.copy(refreshing = true)
            _state.value = try {
                if (source.grantedPermissions().none { it in source.allPermissions }) {
                    TodayUiState.NeedsPermission
                } else {
                    val snapshot = source.read(now)
                    if (snapshot.sources.none { it.hasData }) TodayUiState.NoData
                    else buildToday(snapshot, now).let { TodayUiState.Ready(it, aiSummary(snapshot, it, now)) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                previous?.copy(refreshing = false, error = true) ?: TodayUiState.Failed
            }
        }
    }
}
