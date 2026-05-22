package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * visualkit 渐变 token 层（Aurora 重设计 Phase 0）。
 *
 * 职责：将所有 UI 渐变集中定义，按 [SWScheme] 返回对应 [Brush]。
 * 与 [SWColor] 分离，避免在 Composable 层内联构造 Brush，便于主题统一切换。
 *
 * 方向约定：
 *  - `topToBottom`           — 上→下线性（`start = (0,0)` → `end = (0, ∞)`）
 *  - `topLeftToBottomRight`  — 左上→右下对角（`end = (∞, ∞)`）
 *  - 径向渐变由各函数内直接构造（`Brush.radialGradient`）
 *
 * 调用方：AuroraBackdrop、HeroSection、CardList、PlayerScreen。
 */
object SWGradient {

    /** 上→下线性渐变，均匀色标。 */
    private fun topToBottom(colors: List<Color>): Brush =
        Brush.linearGradient(colors = colors, start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))

    /** 上→下线性渐变，自定义色标位置（0f~1f）。 */
    private fun topToBottomStops(colorStops: Array<Pair<Float, Color>>): Brush =
        Brush.linearGradient(colorStops = colorStops, start = Offset(0f, 0f), end = Offset(0f, Float.POSITIVE_INFINITY))

    /** 左上→右下对角渐变，均匀色标。 */
    private fun topLeftToBottomRight(colors: List<Color>): Brush =
        Brush.linearGradient(colors = colors, start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))

    /** 左上→右下对角渐变，自定义色标位置。 */
    private fun topLeftToBottomRightStops(colorStops: Array<Pair<Float, Color>>): Brush =
        Brush.linearGradient(colorStops = colorStops, start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))

    /**
     * 页面底层背景渐变——对应 [SWColor.surface] 的渐变版本。
     * DAY 微妙上→下，DARK/NIGHT 三停对角以增加深度感。
     */
    fun surface(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topToBottom(listOf(
            Color(0.984f, 0.984f, 0.988f),
            Color(0.972f, 0.976f, 0.984f)
        ))
        SWScheme.DARK -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0.04f, 0.07f, 0.16f),
            0.5f to Color(0.11f, 0.11f, 0.23f),
            1.0f to Color(0.18f, 0.12f, 0.30f)
        ))
        SWScheme.NIGHT -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0.10f, 0.02f, 0.02f),
            0.5f to Color(0.16f, 0.03f, 0.03f),
            1.0f to Color(0.24f, 0.06f, 0.06f)
        ))
    }

    /**
     * 主色渐变——暮光橙上→下，用于主要按钮填充、进度条轨道。
     * DARK/NIGHT 切换为低饱和蓝调/暗红调以保护夜视。
     */
    fun primary(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topToBottom(listOf(
            Color(1.00f, 0.60f, 0.46f), Color(0.96f, 0.47f, 0.33f)
        ))
        SWScheme.DARK -> topToBottom(listOf(
            Color(0.64f, 0.72f, 0.88f), Color(0.48f, 0.58f, 0.79f)
        ))
        SWScheme.NIGHT -> topToBottom(listOf(
            Color(0.78f, 0.45f, 0.45f), Color(0.55f, 0.28f, 0.28f)
        ))
    }

    /**
     * 强调渐变——左上→右下对角，用于图标背景、高亮装饰块。
     * 与 [primary] 同色系但方向不同，形成视觉层次对比。
     */
    fun accent(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topLeftToBottomRight(listOf(
            Color(1.00f, 0.60f, 0.46f), Color(0.96f, 0.47f, 0.33f)
        ))
        SWScheme.DARK -> topLeftToBottomRight(listOf(
            Color(0.96f, 0.79f, 0.69f), Color(0.82f, 0.62f, 0.49f)
        ))
        SWScheme.NIGHT -> topLeftToBottomRight(listOf(
            Color(0.85f, 0.55f, 0.42f), Color(0.62f, 0.32f, 0.22f)
        ))
    }

    /**
     * 卡片遮罩渐变——叠加在卡片内容顶部，增强文字可读性。
     * DAY 完全透明（卡片背景已足够对比），DARK/NIGHT 用细微白色玻璃光。
     */
    fun cardOverlay(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topToBottom(listOf(Color.Transparent, Color.Transparent))
        SWScheme.DARK, SWScheme.NIGHT -> topToBottom(listOf(
            Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.04f)
        ))
    }

    /**
     * Aurora 光晕幕布——多停对角彩带，AuroraBackdrop 的底层图层，三层叠加时相位错开。
     * DAY 为暖米白→晨雾粉→淡紫，DARK/NIGHT 为深蓝→深紫层次。
     */
    fun auroraBackdrop(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0xFFF4F2EE),
            0.5f to Color(0xFFFAE9DC),  // 晨雾桃：日间 Aurora 暖调中停
            1.0f to Color(0xFFEFE4F2)   // 淡紫：与星光紫主轴呼应
        ))
        SWScheme.DARK -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0xFF07101F),
            0.4f to Color(0xFF0F1A2D),
            0.7f to Color(0xFF1A2240),
            1.0f to Color(0xFF2A1F3A)
        ))
        SWScheme.NIGHT -> topLeftToBottomRightStops(arrayOf(
            0.0f to Color(0.10f, 0.02f, 0.02f),
            0.5f to Color(0.16f, 0.03f, 0.03f),
            1.0f to Color(0.24f, 0.06f, 0.06f)
        ))
    }

    /**
     * Aurora 辉光——径向紫→橙渐变，用于 Hero CTA 按钮光晕、正在播放时的氛围光圈。
     * 从绘制中心向外扩散，外边缘 alpha=0 自然消隐。
     */
    fun auroraGlow(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> Brush.radialGradient(
            colors = listOf(Color(0xFFFFB088), Color(0xFFB8A4FF).copy(alpha = 0f))
        )
        SWScheme.DARK -> Brush.radialGradient(
            colors = listOf(Color(0xFFB8A4FF), Color(0xFFFFB088).copy(alpha = 0f)) // 星光紫→橙消隐
        )
        SWScheme.NIGHT -> Brush.radialGradient(
            colors = listOf(Color(0.78f, 0.45f, 0.45f), Color(0.55f, 0.28f, 0.28f, alpha = 0f))
        )
    }

    /**
     * 月光晕圈——柔白银径向渐变，叠加于卡片内侧顶部，模拟深色模式下的内光效果。
     * DAY 几乎透明，可被调用方忽略；DARK/NIGHT 呈现月光银质感。
     */
    fun moonHalo(scheme: SWScheme): Brush = when (scheme) {
        SWScheme.DAY -> Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
        )
        SWScheme.DARK -> Brush.radialGradient(
            colors = listOf(Color(0xFFECF2F8).copy(alpha = 0.18f), Color.Transparent) // 月光银内晕
        )
        SWScheme.NIGHT -> Brush.radialGradient(
            colors = listOf(Color(1.00f, 0.75f, 0.75f).copy(alpha = 0.08f), Color.Transparent)
        )
    }
}
