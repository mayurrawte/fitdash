package app.fitdash

import android.content.Intent
import app.fitdash.ui.share.clearShareCache
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.graphics.Color.TRANSPARENT
import app.fitdash.health.HealthReader
import app.fitdash.settings.SettingsStore
import app.fitdash.ui.LoadingScreen
import app.fitdash.ui.MessageScreen
import app.fitdash.ui.pages.PagerScreen
import app.fitdash.ui.theme.FitdashTheme
import app.fitdash.ui.theme.THEMES
import app.fitdash.ui.today.TodayUiState
import app.fitdash.ui.today.TodayViewModel

private const val SPLASH_MAX_MILLIS = 1200L
private const val SPLASH_EXIT_MILLIS = 500L

class MainActivity : ComponentActivity() {
    private val splashStart = SystemClock.uptimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        clearShareCache(applicationContext)
        val vm: TodayViewModel by viewModels { viewModelFactory { initializer { TodayViewModel(HealthReader(applicationContext), settings = SettingsStore.get(applicationContext).flow) } } }
        splash.setKeepOnScreenCondition { vm.state.value == TodayUiState.Loading && SystemClock.uptimeMillis() - splashStart < SPLASH_MAX_MILLIS }
        splash.setOnExitAnimationListener { provider ->
            val view = provider.view
            val icon = provider.iconView
            view.animate().alpha(0f).setDuration(SPLASH_EXIT_MILLIS).setInterpolator(DecelerateInterpolator()).start()
            icon.animate().scaleX(1.35f).scaleY(1.35f).alpha(0f).setDuration(SPLASH_EXIT_MILLIS).setInterpolator(DecelerateInterpolator())
                .withEndAction { provider.remove() }.start()
        }
        setContent {
            val store = SettingsStore.get(this@MainActivity)
            val settings by store.flow.collectAsStateWithLifecycle()
            val light = THEMES.getValue(settings.theme).lightSystemBars
            LaunchedEffect(settings.theme) {
                (this@MainActivity as ComponentActivity).enableEdgeToEdge(
                    statusBarStyle = if (light) SystemBarStyle.light(TRANSPARENT, TRANSPARENT) else SystemBarStyle.dark(TRANSPARENT),
                    navigationBarStyle = if (light) SystemBarStyle.light(TRANSPARENT, TRANSPARENT) else SystemBarStyle.dark(TRANSPARENT),
                )
            }
            FitdashTheme(settings.theme) { App(store) }
        }
    }
}

private const val HEALTH_CONNECT_STORE =
    "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"

@Composable
private fun App(store: SettingsStore) {
    val context = LocalContext.current
    val vm: TodayViewModel = viewModel { TodayViewModel(HealthReader(context.applicationContext), settings = store.flow) }
    val state by vm.state.collectAsStateWithLifecycle()
    val askPermissions = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { vm.load() }

    LaunchedEffect(vm) { vm.permissionNeeded.collect { askPermissions.launch(vm.permissions) } }

    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose { }
    }

    when (val s = state) {
        TodayUiState.Loading -> LoadingScreen()
        is TodayUiState.NeedsHealthConnect -> MessageScreen(
            title = if (s.update) "Update Health Connect" else "Install Health Connect",
            body = "fitdash reads your Fitbit data through Health Connect.",
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
        TodayUiState.NoData -> MessageScreen(
            title = "No data yet",
            body = "In the Fitbit app, turn on Health Connect sync, then come back.",
            action = "Check again",
            onAction = vm::load,
        )
        TodayUiState.Failed -> MessageScreen(
            title = "Couldn't read Health Connect",
            body = "Something went wrong reading your data.",
            action = "Retry",
            onAction = vm::load,
        )
        is TodayUiState.Ready -> PagerScreen(s, store, onRefresh = vm::load, onSelectDate = vm::select, onAddWater = vm::addWater, onUndoWater = vm::undoWater, onSetHeight = vm::setHeight)
    }
}
