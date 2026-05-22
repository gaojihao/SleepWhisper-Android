package com.lizhi1026.sleepwhisper.app

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lizhi1026.sleepwhisper.core.visualkit.LocalHeroBackdropController
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RootRoute(app: AppStateContainer) {
    val scheme by app.themeProvider.scheme.observeAsState(SWScheme.DAY)
    SWTheme(scheme) {
        CompositionLocalProvider(LocalHeroBackdropController provides app.heroBackdrop) {
            val rootKey by app.rootKey.observeAsState("onboarding")
            val toast by app.toast.events.observeAsState(null)
            val summary by app.lastSleepSummary.observeAsState(null)
            val baby by app.baby.observeAsState(null)
            val editTarget by app.pendingEditTarget.observeAsState(null)
            val scope = rememberCoroutineScope()

            // Lifecycle observer — snap morphProgress to match rootKey on ON_RESUME
            // so a backgrounded mid-morph doesn't resume to a half-animated state.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        app.snapMorphToCurrent()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                SharedTransitionLayout {
                    AnimatedContent(
                        targetState = rootKey,
                        transitionSpec = {
                            // Home ↔ Sleeping uses the hero morph timing; other transitions
                            // keep the standard screen-in/out fade.
                            val isHeroCrossing =
                                (initialState == "sleeping") != (targetState == "sleeping")
                            val durIn  = if (isHeroCrossing) SWMotion.heroMorphMs else SWMotion.screenInMs
                            val durOut = if (isHeroCrossing) SWMotion.heroMorphMs else SWMotion.screenOutMs
                            fadeIn(animationSpec = androidx.compose.animation.core.tween(durIn)) togetherWith
                                fadeOut(animationSpec = androidx.compose.animation.core.tween(durOut))
                        },
                        label = "root"
                    ) { key ->
                        when (key) {
                            "onboarding" -> WithStatusBarPadding { OnboardingScreen() }
                            "welcome"    -> WelcomeRitualScreen(app)
                            "sleeping"   -> SleepingScreen(
                                sharedScope = this@SharedTransitionLayout,
                                animScope = this@AnimatedContent
                            )
                            else         -> MainScaffold(
                                sharedScope = this@SharedTransitionLayout,
                                animScope = this@AnimatedContent
                            )
                        }
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
}

@Composable
private fun WithStatusBarPadding(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) { content() }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainScaffold(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
) {
    var tab by remember { mutableStateOf(0) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(top = statusBarTop)
        ) {
            when (tab) {
                0 -> HomeScreen(
                    sharedScope = sharedScope,
                    animScope = animScope
                )
                1 -> TrendsScreen()
                2 -> SettingsScreen()
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
            TabItem(stringResource(R.string.tabbar_home), R.drawable.ic_tab_home, selected == 0) { onSelect(0) }
            TabItem(stringResource(R.string.tabbar_trends), R.drawable.ic_tab_trends, selected == 1) { onSelect(1) }
            TabItem(stringResource(R.string.tabbar_settings), R.drawable.ic_tab_settings, selected == 2) { onSelect(2) }
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
            .heightIn(min = 48.dp)
            .clickable(
                onClickLabel = label,
                role = androidx.compose.ui.semantics.Role.Tab,
                onClick = onClick
            )
            .semantics {
                this.selected = selected
                this.contentDescription = label
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                // contentDescription on the parent Box already announces the tab name;
                // marking the icon as decorative avoids TalkBack reading it twice.
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
