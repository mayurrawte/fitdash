package app.fitdash

import app.fitdash.background.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.glance.appwidget.updateAll
import app.fitdash.health.BACKGROUND_PERMISSION
import app.fitdash.background.BackgroundRefresh
import app.fitdash.widget.EXTRA_PAGE
import app.fitdash.widget.FitdashWidget
import android.content.Intent
import app.fitdash.ui.share.clearShareCache
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.os.SystemClock
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.Color.TRANSPARENT
import app.fitdash.health.HealthReader
import androidx.health.connect.client.HealthConnectClient
import app.fitdash.health.SnapshotCache
import app.fitdash.settings.SettingsStore
import app.fitdash.ui.LoadingScreen
import app.fitdash.ui.MessageScreen
import app.fitdash.ui.NoDataScreen
import app.fitdash.ui.sourceAppName
import app.fitdash.health.SYNC_APPS
import app.fitdash.ui.pages.PagerScreen
import app.fitdash.ui.theme.FitdashTheme
import app.fitdash.ui.theme.LocalTheme
import app.fitdash.ui.theme.Palette
import androidx.compose.runtime.CompositionLocalProvider
import app.fitdash.ui.theme.LocalReduceMotion
import app.fitdash.ui.LocalPrefs
import app.fitdash.ui.today.TodayUiState
import app.fitdash.ui.today.TodayViewModel

private const val SPLASH_MAX_MILLIS = 1200L
private const val SPLASH_EXIT_MILLIS = 500L

class MainActivity : ComponentActivity() {
    private val splashStart = SystemClock.uptimeMillis()
    // The page a widget tap opened us on, for a cold start and for onNewIntent alike.
    private val openPage = MutableStateFlow<Int?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openPage.value = intent.pageExtra()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        openPage.value = intent.pageExtra()
        super.onCreate(savedInstanceState)
        clearShareCache(applicationContext)
        val vm: TodayViewModel by viewModels { viewModelFactory { initializer { TodayViewModel(HealthReader(applicationContext), settings = SettingsStore.get(applicationContext).flow, onReadFailed = { Log.w("fitdash", "Health Connect read failed", it) }, store = SnapshotCache(applicationContext), onCacheSaved = { FitdashWidget().updateAll(applicationContext) }) } } }
        splash.setKeepOnScreenCondition { vm.state.value == TodayUiState.Loading && SystemClock.uptimeMillis() - splashStart < SPLASH_MAX_MILLIS }
        splash.setOnExitAnimationListener { provider ->
            val view = provider.view
            // The system splash can arrive without an icon view (seen on Pixel when relaunched from recents); the library then throws.
            val icon = runCatching { provider.iconView }.getOrNull()
            view.animate().alpha(0f).setDuration(SPLASH_EXIT_MILLIS).setInterpolator(DecelerateInterpolator())
                .withEndAction { if (icon == null) provider.remove() }.start()
            icon?.animate()?.scaleX(1.35f)?.scaleY(1.35f)?.alpha(0f)?.setDuration(SPLASH_EXIT_MILLIS)?.setInterpolator(DecelerateInterpolator())
                ?.withEndAction { provider.remove() }?.start()
        }
        setContent {
            val store = SettingsStore.get(this@MainActivity)
            val settings by store.flow.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                val bars = SystemBarStyle.light(TRANSPARENT, TRANSPARENT)
                (this@MainActivity as ComponentActivity).enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            FitdashTheme {
                CompositionLocalProvider(LocalPrefs provides settings, LocalReduceMotion provides settings.reduceMotion) { App(store, openPage) }
            }
        }
    }
}

private fun Intent?.pageExtra(): Int? = this?.getIntExtra(EXTRA_PAGE, -1)?.takeIf { it >= 0 }

private const val HEALTH_CONNECT_STORE =
    "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"

@Composable
private fun App(store: SettingsStore, openPage: MutableStateFlow<Int?>) {
    val context = LocalContext.current
    val vm: TodayViewModel = viewModel {
        TodayViewModel(
            HealthReader(context.applicationContext), settings = store.flow, onReadFailed = { Log.w("fitdash", "Health Connect read failed", it) },
            store = SnapshotCache(context.applicationContext), onCacheSaved = { FitdashWidget().updateAll(context.applicationContext) },
        )
    }
    val settings by store.flow.collectAsStateWithLifecycle()
    val page by openPage.collectAsStateWithLifecycle()
    LaunchedEffect(settings.backgroundRefresh) { BackgroundRefresh.apply(context.applicationContext, settings.backgroundRefresh) }
    LaunchedEffect(settings.waterReminders, settings.moveNudge) { Reminders.apply(context.applicationContext, settings.waterReminders || settings.moveNudge) }
    val state by vm.state.collectAsStateWithLifecycle()
    val askPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.load() }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    LaunchedEffect(vm) { vm.permissionNeeded.collect { askPermissions.launch(vm.permissions) } }

    LifecycleResumeEffect(Unit) {
        vm.load(force = false)
        onPauseOrDispose { }
    }

    Box(Modifier.fillMaxSize()) {
        when (val s = state) {
            TodayUiState.Loading -> LoadingScreen(firstRun = true, progressFlow = vm.progress)
            is TodayUiState.NeedsHealthConnect -> MessageScreen(
                title = if (s.update) "Update Health Connect" else "Install Health Connect",
                body = "fitdash reads your watch, ring or phone data through Health Connect.",
                action = if (s.update) "Update" else "Install",
                onAction = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(HEALTH_CONNECT_STORE)).setPackage("com.android.vending"))
                },
            )
            TodayUiState.NeedsPermission -> MessageScreen(
                title = "Welcome to fitdash",
                body = "Allow access in Health Connect. Nothing leaves your phone.",
                action = "Grant access",
                onAction = { askPermissions.launch(vm.permissions) },
            )
            TodayUiState.NoData -> NoDataScreen(
                installed = SYNC_APPS.filter { context.packageManager.getLaunchIntentForPackage(it) != null }.map { it to sourceAppName(it) },
                onOpen = { pkg -> context.packageManager.getLaunchIntentForPackage(pkg)?.let { context.startActivity(it) } },
                onStore = { pkg -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).setPackage("com.android.vending")) },
                onCheck = vm::load,
            )
            TodayUiState.Failed -> MessageScreen(
                title = "Couldn't read Health Connect",
                body = "Something went wrong reading your data.",
                action = "Retry",
                onAction = vm::load,
            )
            is TodayUiState.Ready -> PagerScreen(
                s, store,
                progress = vm.progress,
                onRefresh = { vm.load() },
                onSelectDate = vm::select,
                onAddWater = vm::addWater,
                onUndoWater = vm::undoWater,
                onSetHeight = vm::setHeight,
                onAskHistory = { askPermissions.launch(vm.permissions) },
                onAskBackground = { askPermissions.launch(setOf(BACKGROUND_PERMISSION)) },
                openPage = page,
                onPageOpened = { openPage.value = null },
                onSync = { pkg ->
                    // Open the wearable's app so it pushes to Health Connect; we read again when the user comes back.
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                    if (intent != null) {
                        vm.syncRequested()
                        context.startActivity(intent)
                    } else {
                        context.startActivity(HealthConnectClient.getHealthConnectManageDataIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                },
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp)) {
            Snackbar(it, containerColor = LocalTheme.current.bgMid, contentColor = Palette.Text)
        }
    }
}
