package com.lizhi1026.sleepwhisper.app

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.R
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing
import com.lizhi1026.sleepwhisper.core.visualkit.SWTheme
import com.lizhi1026.sleepwhisper.core.visualkit.components.ToastOverlay
import com.lizhi1026.sleepwhisper.features.home.EventEditSheet
import com.lizhi1026.sleepwhisper.features.home.HomeScreen
import com.lizhi1026.sleepwhisper.features.onboarding.OnboardingScreen
import com.lizhi1026.sleepwhisper.features.onboarding.WelcomeRitualScreen
import com.lizhi1026.sleepwhisper.features.player.PlayerScreen
import com.lizhi1026.sleepwhisper.features.settings.SettingsScreen
import com.lizhi1026.sleepwhisper.features.sleeping.SleepSummaryOverlay
import com.lizhi1026.sleepwhisper.features.sleeping.SleepingScreen
import com.lizhi1026.sleepwhisper.features.trends.TrendsScreen
import kotlinx.coroutines.launch

/**
 * Root composable. Maps [AppStateContainer.rootKey] to the four top-level destinations.
 *
 * Insets handling: the activity is edge-to-edge (see [MainActivity]). Each top-level
 * destination receives a [WindowInsets.statusBars]-aware top padding via
 * [TopBarInsetPadding]; the [MainScaffold] additionally pads its bottom tab with the
 * navigation-bar inset. Sleeping is intentionally edge-to-edge with no padding because
 * it's a full-screen immersive screen.
 */
@Composable
fun RootRoute(app: AppStateContainer) {
    val scheme by app.themeProvider.scheme.observeAsState(SWScheme.DAY)
    SWTheme(scheme) {
        val rootKey by app.rootKey.observeAsState("onboarding")
        val toast by app.toast.events.observeAsState(null)
        val summary by app.lastSleepSummary.observeAsState(null)
        val baby by app.baby.observeAsState(null)
        val editTarget by app.pendingEditTarget.observeAsState(null)
        val scope = rememberCoroutineScope()

        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = rootKey, animationSpec = tween(SWMotion.screenInMs), label = "root") { key ->
                when (key) {
                    "onboarding" -> WithStatusBarPadding { OnboardingScreen() }
                    "welcome"    -> WelcomeRitualScreen(app)              // edge-to-edge cinematic
                    "sleeping"   -> SleepingScreen()                       // edge-to-edge immersive
                    else         -> MainScaffold()
                }
            }

            summary?.let { s ->
                SleepSummaryOverlay(
                    summary = s,
                    babyName = baby?.name,
                    onDismiss = app::clearSleepSummary
                )
            }

            editTarget?.let { t ->
                EventEditSheet(
                    target = t,
                    onDismiss = app::clearPendingEdit,
                    onSaveFeeding = { f -> scope.launch { app.updateFeeding(f) } },
                    onSaveDiaper = { d -> scope.launch { app.updateDiaper(d) } }
                )
            }

            // Toast sits above the system nav bar.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                ToastOverlay(
                    item = toast,
                    onDismiss = app.toast::consume,
                    onUndo = toast?.undo,
                    onEditAction = toast?.editAction
                )
            }
        }
    }
}

@Composable
private fun WithStatusBarPadding(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) { content() }
}

@Composable
private fun MainScaffold() {
    var tab by remember { mutableStateOf(0) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = statusBarTop)
        ) {
            when (tab) {
                0 -> HomeScreen(onOpenPlayer = { tab = 3 })
                1 -> TrendsScreen()
                2 -> SettingsScreen()
                3 -> PlayerScreen()
            }
        }
        BottomTabs(tab) { tab = it }
    }
}

@Composable
private fun BottomTabs(selected: Int, onSelect: (Int) -> Unit) {
    val scheme = LocalSWScheme.current
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SWColor.surfaceElevated(scheme))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            TabItem("Home", R.drawable.ic_tab_home, selected == 0) { onSelect(0) }
            TabItem("Trends", R.drawable.ic_tab_trends, selected == 1) { onSelect(1) }
            TabItem("Settings", R.drawable.ic_tab_settings, selected == 2) { onSelect(2) }
        }
        // System gesture / nav bar inset.
        Box(modifier = Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    label: String,
    @DrawableRes iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalSWScheme.current
    val color = if (selected) SWColor.primary(scheme) else SWColor.textSecondary(scheme)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .height(22.dp)
            )
            BasicText(label, style = SWFont.labelSM().copy(color = color))
        }
    }
}
