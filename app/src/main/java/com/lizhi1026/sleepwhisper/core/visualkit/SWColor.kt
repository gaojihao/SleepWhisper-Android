package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color

/**
 * visualkit 颜色 token 层（Aurora 重设计 Phase 0）。
 *
 * 职责：将所有具名语义色映射到三态 scheme（[SWScheme.DAY] / [SWScheme.DARK] / [SWScheme.NIGHT]），
 * 与 Material3 ColorScheme **正交**——上层 UI 通过 [LocalSWScheme] 读取当前 scheme，
 * 再调用此对象的函数取色，不直接使用硬编码色值。
 *
 * 色彩主轴：暮光橙 `#FF8A5E`（primary）+ 星光紫 `#9B85FF`（accentSecondary）。
 *
 * NIGHT scheme 专注夜间护眼——所有文本色对比度已校对至 ≥ 4.5:1，**禁止随意调整**。
 * NIGHT 与系统深色模式（DARK）正交：NIGHT 是夜间时段内的更深红调节态。
 *
 * 调用方：SWTheme、HomeScreen、SleepingScreen、TrendsScreen 等所有 Composable。
 */
object SWColor {

    /**
     * 品牌主色——暮光橙系列。
     * DAY 使用饱和橙，DARK 亮度上移以保证对比，NIGHT 转为暗红调以保护夜视。
     */
    fun primary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF8A5E)  // 暮光橙：品牌主色
        SWScheme.DARK  -> Color(0xFFFFB088)  // 暮光橙亮化：DARK 模式提亮
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f) // 夜间暗红调，护眼保留值
    }

    /** 主色悬停态——比 [primary] 更深，用于按钮 hover 反馈。 */
    fun primaryHover(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF7A4A)
        SWScheme.DARK  -> Color(0xFFFFC5A4)
        SWScheme.NIGHT -> Color(0.78f, 0.50f, 0.50f)
    }

    /** 主色按下态——比 hover 更深，用于触摸/点击反馈。 */
    fun primaryActive(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFE87646)
        SWScheme.DARK  -> Color(0xFFFFA378)
        SWScheme.NIGHT -> Color(0.85f, 0.55f, 0.55f)
    }

    /**
     * 强调色（第一主轴）——与 [primary] 同色系，用于图标、进度环等装饰性元素。
     * 与 [primary] 数值相同，但语义上保持独立以便未来单独调整。
     */
    fun accent(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFFF8A5E)
        SWScheme.DARK  -> Color(0xFFFFB088)
        SWScheme.NIGHT -> Color(0.80f, 0.45f, 0.45f)
    }

    /**
     * 强调色（第二主轴）——星光紫。Aurora 新增双主轴设计语言。
     * NIGHT scheme 夜间回退到 primary 暗红调，避免紫色刺激视觉。
     */
    fun accentSecondary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF9B85FF)  // 星光紫：第二品牌色
        SWScheme.DARK  -> Color(0xFFB8A4FF)  // 星光紫亮化
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f) // 夜间回退至 primary 暗红调
    }

    /**
     * 页面/屏幕底层背景色。
     * DAY 为暖米白，DARK 为深夜蓝黑，NIGHT 为极深暗红（近黑）。
     */
    fun surface(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFF4F2EE) // 暖米白：纸张质感
        SWScheme.DARK  -> Color(0xFF07101F) // 深夜蓝黑：沉浸感底色
        SWScheme.NIGHT -> Color(0.04f, 0.02f, 0.02f)
    }

    /** 浮层/卡片背景——比 [surface] 高一级的提升层，用于卡片、底部弹窗。 */
    fun surfaceElevated(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0xFF0F1A2D)
        SWScheme.NIGHT -> Color(0.10f, 0.04f, 0.04f)
    }

    /** 下沉区域背景——比 [surface] 低一级，用于输入框、内嵌列表等凹陷区域。 */
    fun surfaceSunken(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFECEAE5)
        SWScheme.DARK  -> Color(0xFF050912)
        SWScheme.NIGHT -> Color(0.02f, 0.01f, 0.01f)
    }

    /** 分割线 / 边框色——卡片描边、列表分隔线。 */
    fun border(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFFE2DDD2)
        SWScheme.DARK  -> Color(0xFF1A2438)
        SWScheme.NIGHT -> Color(0.16f, 0.07f, 0.07f)
    }

    /**
     * 主要正文文字色。
     * NIGHT 值已校对对比度 ≥ 4.5:1，**禁止修改**。
     */
    fun textPrimary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF0A1428)
        SWScheme.DARK  -> Color(0xFFECF2F8) // 月光银：DARK 主文字
        SWScheme.NIGHT -> Color(1.00f, 0.75f, 0.75f) // 对比度 ≥ 4.5:1 — 禁止修改
    }

    /** 次要文字色——副标题、说明文字、时间戳。 */
    fun textSecondary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF4A5573)
        SWScheme.DARK  -> Color(0xFF8A93A8)
        SWScheme.NIGHT -> Color(0.85f, 0.55f, 0.55f)
    }

    /** 三级文字色——占位符、禁用态标签、辅助提示。 */
    fun textTertiary(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0xFF7A859C)
        SWScheme.DARK  -> Color(0xFF5C667A)
        SWScheme.NIGHT -> Color(0.70f, 0.42f, 0.42f)
    }

    /** 反色文字——用于深色按钮/徽章内的浅色字，与 [surface] 形成对比。 */
    fun textInverse(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color.White
        SWScheme.DARK  -> Color(0xFF0F1A2D)
        SWScheme.NIGHT -> Color(0.10f, 0.04f, 0.04f)
    }

    /** 成功状态色——睡眠达标、目标完成等正向反馈。 */
    fun success(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.20f, 0.71f, 0.51f)
        SWScheme.DARK  -> Color(0.58f, 0.76f, 0.55f)
        SWScheme.NIGHT -> Color(0.55f, 0.32f, 0.32f)
    }

    /** 警告状态色——睡眠不足提醒、轻度异常指标。 */
    fun warning(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.96f, 0.62f, 0.07f)
        SWScheme.DARK  -> Color(0.89f, 0.72f, 0.45f)
        SWScheme.NIGHT -> Color(0.75f, 0.42f, 0.42f)
    }

    /** 危险/错误状态色——设备断连、严重异常、删除操作确认。 */
    fun danger(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.94f, 0.27f, 0.27f)
        SWScheme.DARK  -> Color(0.85f, 0.54f, 0.54f)
        SWScheme.NIGHT -> Color(0.95f, 0.42f, 0.42f)
    }

    /** 柔桃色——标签芯片、情绪标记背景（暮光橙系柔化版）。 */
    fun softPeach(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(1.000f, 0.898f, 0.851f)
        SWScheme.DARK  -> Color(0.40f, 0.28f, 0.24f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.16f, 0.16f, alpha = 0.5f)
    }

    /** 柔薄荷绿——成功类标签、正向情绪背景。 */
    fun softMint(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.859f, 0.961f, 0.898f)
        SWScheme.DARK  -> Color(0.24f, 0.40f, 0.32f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.20f, 0.20f, alpha = 0.5f)
    }

    /** 柔淡紫——星光紫系柔化版，用于次要标签、冥想/放松类内容背景。 */
    fun softLilac(s: SWScheme): Color = when (s) {
        SWScheme.DAY   -> Color(0.918f, 0.890f, 1.000f)
        SWScheme.DARK  -> Color(0.32f, 0.28f, 0.48f, alpha = 0.5f)
        SWScheme.NIGHT -> Color(0.40f, 0.16f, 0.16f, alpha = 0.5f)
    }
}
