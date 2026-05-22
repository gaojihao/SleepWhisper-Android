package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * visualkit 阴影 token 层（Aurora 重设计 Phase 0）。
 *
 * 职责：统一管理所有阴影规格，按 [SWScheme] 返回 [Spec]（颜色、模糊半径、Y 偏移）。
 * DAY 阴影透明度轻薄，DARK/NIGHT 阴影更深以在深色背景上保持层次感。
 *
 * 使用方式：通过 `Modifier.shadow(spec.radius, ..., ambientColor = spec.color)` 应用，
 * 或传入自定义 Canvas 绘制逻辑。
 *
 * 调用方：CardComponent、BottomSheet、PlayerMiniBar、FloatingActionButton。
 */
object SWShadow {

    /**
     * 阴影规格——三元组描述单层阴影。
     *
     * @param color  阴影颜色（含 alpha），DAY 透明度远低于 DARK/NIGHT
     * @param radius 模糊半径（越大越柔和）
     * @param y      Y 轴偏移（正值向下，模拟光源从上方照射）
     */
    data class Spec(val color: Color, val radius: Dp, val y: Dp)

    /**
     * 柔阴影——卡片默认层级，轻薄不抢眼。
     * DAY: alpha 0.04，blur 8dp，y 2dp；DARK/NIGHT: alpha 0.25。
     */
    fun soft(scheme: SWScheme): Spec =
        Spec(Color.Black.copy(alpha = if (scheme == SWScheme.DAY) 0.04f else 0.25f), 8.dp, 2.dp)

    /**
     * 中阴影——浮层、底部弹窗、下拉菜单等中等高度组件。
     * DAY: alpha 0.05，blur 16dp，y 6dp；DARK/NIGHT: alpha 0.30。
     */
    fun medium(scheme: SWScheme): Spec =
        Spec(Color.Black.copy(alpha = if (scheme == SWScheme.DAY) 0.05f else 0.30f), 16.dp, 6.dp)

    /**
     * 强阴影——全屏 Modal、播放器展开态等最高层级组件。
     * DAY: alpha 0.07，blur 32dp，y 12dp；DARK/NIGHT: alpha 0.40。
     */
    fun strong(scheme: SWScheme): Spec =
        Spec(Color.Black.copy(alpha = if (scheme == SWScheme.DAY) 0.07f else 0.40f), 32.dp, 12.dp)

    /**
     * 彩色辉光阴影——用于主色/强调色按钮的发光效果（不随 scheme 变化）。
     * alpha 固定 0.35，blur 40dp，y=0 形成正圆晕圈。
     *
     * @param tint 辉光底色，通常传入 [SWColor.primary] 或 [SWColor.accentSecondary]。
     */
    fun glow(tint: Color): Spec =
        Spec(tint.copy(alpha = 0.35f), 40.dp, 0.dp) // 彩色辉光：y=0 纯扩散，无方向偏移
}
