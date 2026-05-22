package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.dp

/**
 * visualkit 间距 token 层——4dp 基准网格。
 *
 * 所有间距值均为 4 的整数倍，与 Material Design 8dp 网格向上兼容。
 * 调用方直接引用常量，禁止在 UI 层出现任意 `.dp` 魔法数字。
 *
 * 调用方：所有 Composable 的 padding / arrangement / spacedBy 参数。
 */
object SWSpacing {
    val xxs = 4.dp     // 最小间距：图标内边距、密集列表行间距
    val xs = 8.dp      // 紧凑间距：标签内边距、行内图文间距
    val sm = 12.dp     // 小间距：卡片内边距辅助、输入框图标间距
    val md = 16.dp     // 标准间距：卡片内边距、列表项垂直间距
    val lg = 20.dp     // 中大间距：Section 内部分组间距
    val xl = 24.dp     // 大间距：卡片圆角同规格、屏幕水平边距
    val xxl = 32.dp    // 超大间距：区块间距、Hero 区域内部留白
    val xxxl = 40.dp   // 极大间距：屏幕垂直分区边距
    val huge = 56.dp   // 巨大间距：底部安全区补偿、播放器展开上留白
    val giant = 72.dp  // 超巨间距：全屏 Hero 上下边距
    val mammoth = 96.dp // 最大间距：欢迎屏顶部留白、超大展示场景
}

/**
 * visualkit 圆角 token 层——与 [SWSpacing] 网格对齐。
 *
 * `pill` 用于胶囊形按钮，数值足够大保证任意高度的组件都呈现完整圆端。
 * 调用方：CardComponent、Button、InputField、BottomSheet、Tag。
 */
object SWRadius {
    val sm = 8.dp      // 小圆角：输入框、小型标签
    val md = 12.dp     // 中圆角：列表项、迷你卡片
    val lg = 16.dp     // 大圆角：标准卡片
    val xl = 24.dp     // 超大圆角：Hero 卡片、播放器面板
    val pill = 999.dp  // 胶囊形：圆角按钮、标签芯片完整圆端
}

/**
 * visualkit 动效时长 token 层——电影感呼吸节奏。
 *
 * 设计哲学：按下如吸气（快速响应），松开如呼气（舒缓），屏幕切换如翻页（沉稳）。
 * Aurora 新增 Hero Morph / Aurora 漂移 / 卡片呼吸三个长周期动效。
 *
 * 调用方：所有动画 `durationMillis`、`tween`、`animate*AsState` 参数。
 */
object SWMotion {
    const val pressInMs = 80          // 按下响应：快速吸气（原 100ms 提速）
    const val pressOutMs = 260        // 抬起回弹：舒缓呼气（原 150ms 延长）
    const val screenInMs = 420        // 屏幕进场：翻页入场（原 250ms 延长）
    const val screenOutMs = 320       // 屏幕退场：翻页离场（原 200ms 延长）
    const val heroMorphMs = 1200      // Hero 形态变换：Aurora 新增，卡片→全屏过渡
    const val backdropDriftMs = 18000 // Aurora 彩带漂移周期：18s 一轮，极慢氛围动效
    const val breathCycleMs = 4200    // 卡片呼吸周期：≈4.2s，模拟人均呼吸节律
    const val toastMs = 200           // Toast 淡入淡出
    const val bannerMs = 200          // Banner 展开收起
    const val numberSwitchMs = 100    // 数字切换（计时器每秒跳变）
    const val longPressMs = 600L      // SleepingScreen 长按结束阈值：600ms 触发停止睡眠
    const val fadeInMs = 1500L        // 渐显：封面/背景图加载淡入
    const val defaultFadeOutMs = 5000L // 渐隐：SleepingScreen 操作控件自动隐藏延时
}

/**
 * visualkit 弹簧动画规格——全局统一触觉语言。
 *
 * 所有交互元素（按钮缩放、卡片展开、抽屉滑动）应优先使用 [gentle]，
 * 保证整个 App 的触感一致，避免各处弹性参数各自为政。
 */
object SWSpring {
    /**
     * 标准弹簧——阻尼 0.85（轻微过冲），刚度 180（中等弹性），适合绝大多数交互反馈。
     */
    val gentle: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,   // 欠阻尼：轻微回弹，有弹性但不夸张
        stiffness = 180f,       // 中刚度：响应不迟钝，不过于弹跳
        visibilityThreshold = Spring.DefaultDisplacementThreshold
    )
}
