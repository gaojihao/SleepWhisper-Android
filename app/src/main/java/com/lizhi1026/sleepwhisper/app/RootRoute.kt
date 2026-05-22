/**
 * 根路由 Composable：应用的顶层导航入口。
 *
 * **职责**：
 * - 观察 [AppStateContainer.rootKey]，根据其值在四个顶层目标之间切换：
 *   `"onboarding"` → 新手引导、`"welcome"` → 欢迎仪式、`"sleeping"` → 睡眠监控、`"main"` → 主界面（含底部 Tab）。
 * - ⚠️ [AppStateContainer.rootKey] 是导航的唯一入口；若需改变导航目标，必须通过
 *   [AppStateContainer] 的写操作方法间接驱动 `rootKey` 变化，不可在此处硬编码跳转逻辑。
 * - 全局叠加 [SleepSummaryOverlay]、[EventEditSheet]、[ToastOverlay]，确保它们显示在所有路由之上。
 * - 管理 hero 变形动画的生命周期：在 `ON_RESUME` 时通过 [AppStateContainer.snapMorphToCurrent]
 *   对齐进度，避免应用切回前台时出现半途动画。
 *
 * **所在层**：app 模块 / 导航层。
 *
 * **与谁交互**：
 * - [AppStateContainer]（单例）— 读取所有顶层状态。
 * - 各功能 Feature Screen（Home、Sleeping、Trends、Settings、Onboarding 等）。
 *
 * **关键约定**：
 * - Activity 设置为 edge-to-edge（见 [MainActivity]）；各目标屏幕通过 [WithStatusBarPadding] 或
 *   [MainScaffold] 自行处理状态栏内边距；SleepingScreen 为全屏沉浸式，不加顶部内边距。
 * - Home ↔ Sleeping 切换使用 hero morph 时长；其余路由使用标准屏幕淡入淡出时长。
 */
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
 * 根 Composable，将 [AppStateContainer.rootKey] 映射到四个顶层目标。
 *
 * **内边距策略**：Activity 为 edge-to-edge（见 [MainActivity]）。各顶层目标通过
 * [WithStatusBarPadding] 获得状态栏顶部内边距；[MainScaffold] 的底部 Tab 额外
 * 处理导航栏内边距；SleepingScreen 为全沉浸式，故意不添加内边距。
 *
 * @param app 全局状态容器，提供 rootKey、toast、sleepSummary、editTarget 等所有顶层 LiveData。
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

            // 生命周期观察器：在 ON_RESUME 时将 morphProgress 对齐到 rootKey 当前值，
            // 防止应用在动画进行到一半时切入后台，再恢复时出现卡在中间的异常状态。
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
                            // Home ↔ Sleeping 使用 hero 变形时长；其他路由切换使用标准屏幕淡入淡出时长。
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

/**
 * 为内容添加状态栏高度的顶部内边距，供非全屏目标屏幕（如 OnboardingScreen）使用。
 *
 * @param content 需要状态栏内边距保护的子 Composable 内容。
 */
@Composable
private fun WithStatusBarPadding(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    ) { content() }
}

/**
 * 主界面脚手架：包含底部三 Tab 导航栏和对应的内容区域。
 *
 * 内容区域顶部留出状态栏高度；底部 Tab 行附加导航栏高度内边距（由 [BottomTabs] 内部处理）。
 *
 * @param sharedScope 共享元素过渡作用域，透传给 [HomeScreen] 以支持 CTA 卡片 hero 动画。
 * @param animScope  [AnimatedContent] 动画可见性作用域，透传给 [HomeScreen]。
 */
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

/**
 * 底部 Tab 导航栏：包含首页、趋势、设置三个 Tab 项。
 *
 * 底部额外插入导航栏系统手势区域高度，确保内容不被系统手势条遮挡。
 *
 * @param selected 当前选中的 Tab 索引（0=首页，1=趋势，2=设置）。
 * @param onSelect Tab 被点击时的回调，参数为目标 Tab 索引。
 */
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
        // 系统手势区 / 导航栏内边距占位，避免 Toast 被导航栏遮挡。
        Box(modifier = Modifier.fillMaxWidth().windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

/**
 * 单个 Tab 按钮，占满父 [Row] 的等分宽度。
 *
 * 已为无障碍访问设置 `contentDescription` 和 `selected` 语义，避免 TalkBack 重复播报图标。
 *
 * @param label    Tab 显示文字，同时用作无障碍内容描述。
 * @param iconRes  Tab 图标的 drawable 资源 ID。
 * @param selected 当前 Tab 是否处于选中状态（影响颜色高亮）。
 * @param onClick  Tab 被点击时的回调。
 */
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
                // 父 Box 已通过 contentDescription 声明 Tab 名称，图标标记为装饰性以防 TalkBack 重复播报。
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
