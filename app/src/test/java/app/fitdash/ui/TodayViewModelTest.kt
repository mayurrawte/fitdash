package app.fitdash.ui

import app.fitdash.IST
import app.fitdash.TODAY
import app.fitdash.health.Availability
import app.fitdash.health.HealthSnapshot
import app.fitdash.health.HealthSource
import app.fitdash.health.SourceStatus
import app.fitdash.ui.today.TodayUiState
import app.fitdash.ui.today.TodayViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.ZonedDateTime

private class FakeSource(
    var status: Availability = Availability.AVAILABLE,
    var granted: Set<String> = setOf("read"),
    var snapshot: HealthSnapshot = HealthSnapshot(sources = listOf(SourceStatus("Sleep", true, true, setOf("com.fitbit.FitbitMobile")))),
    var fail: Boolean = false,
    var gate: CompletableDeferred<Unit>? = null,
) : HealthSource {
    override val allPermissions = setOf("read")
    override fun availability() = status
    override suspend fun grantedPermissions() = granted
    override suspend fun read(now: ZonedDateTime): HealthSnapshot {
        gate?.await()
        if (fail) error("read failed")
        return snapshot
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val now = TODAY.atTime(8, 10).atZone(IST)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun load(source: FakeSource) = TodayViewModel(source) { now }.also { it.load() }

    @Test fun `Health Connect missing or outdated`() {
        assertEquals(TodayUiState.NeedsHealthConnect(update = false), load(FakeSource(status = Availability.NOT_INSTALLED)).state.value)
        assertEquals(TodayUiState.NeedsHealthConnect(update = true), load(FakeSource(status = Availability.NEEDS_UPDATE)).state.value)
    }

    @Test fun `no permissions`() {
        assertEquals(TodayUiState.NeedsPermission, load(FakeSource(granted = emptySet())).state.value)
    }

    @Test fun `granted but nothing written`() {
        val empty = HealthSnapshot(sources = listOf(SourceStatus("Sleep", true, false, emptySet())))
        assertEquals(TodayUiState.NoData, load(FakeSource(snapshot = empty)).state.value)
    }

    @Test fun `data becomes ready`() {
        assertTrue(load(FakeSource()).state.value is TodayUiState.Ready)
    }

    @Test fun `a failed refresh keeps the previous page and flags the error`() {
        val source = FakeSource()
        val vm = load(source)
        source.fail = true
        vm.load()
        val s = vm.state.value as TodayUiState.Ready
        assertTrue(s.error)
        assertEquals(false, s.refreshing)
    }

    @Test fun `a failed first load`() {
        assertEquals(TodayUiState.Failed, load(FakeSource(fail = true)).state.value)
    }

    @Test fun `an overlapping load replaces the one in flight`() {
        val source = FakeSource(gate = CompletableDeferred())
        val first = source.gate!!
        val vm = TodayViewModel(source) { now }
        vm.load()
        source.gate = null
        vm.load()
        assertTrue(vm.state.value is TodayUiState.Ready)
        first.completeExceptionally(IllegalStateException("stale"))
        assertTrue(vm.state.value is TodayUiState.Ready)
    }
}
