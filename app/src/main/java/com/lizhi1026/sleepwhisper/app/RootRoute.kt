package com.lizhi1026.sleepwhisper.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWFont
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
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

/** Root composable wiring [AppStateContainer.rootKey] to the four top-level destinations. */
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
                    "onboarding" -> OnboardingScreen()
                    "welcome"    -> WelcomeRitualScreen(app)
                    "sleeping"   -> SleepingScreen()
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

            ToastOverlay(
                item = toast,
                onDismiss = app.toast::consume,
                onUndo = toast?.undo,
                onEditAction = toast?.editAction
            )
        }
    }
}

@Composable
private fun MainScaffold() {
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(SWColor.surfaceElevated(scheme))
    ) {
        TabItem("Home", selected == 0) { onSelect(0) }
        TabItem("Trends", selected == 1) { onSelect(1) }
        TabItem("Settings", selected == 2) { onSelect(2) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = LocalSWScheme.current
    val color = if (selected) SWColor.primary(scheme) else SWColor.textSecondary(scheme)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BasicText(label, style = SWFont.labelMD().copy(color = color))
    }
}
