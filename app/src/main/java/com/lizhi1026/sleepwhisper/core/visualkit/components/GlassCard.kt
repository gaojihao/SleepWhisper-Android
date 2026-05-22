package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWGradient
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWRadius
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWShadow
import com.lizhi1026.sleepwhisper.core.visualkit.SWSpacing

/**
 * 卡片阴影层级枚举，控制双层阴影的远景阴影强度。
 *
 * - [SOFT]：轻柔阴影，适用于列表项、小卡片
 * - [MEDIUM]：中等阴影，适用于模块卡片
 * - [STRONG]：强阴影，适用于弹层卡片
 * - [HERO]：英雄级阴影（额外 +16dp 半径 +4dp 偏移），适用于首页主视觉焦点卡片
 */
enum class ElevationLevel { SOFT, MEDIUM, STRONG, HERO }

/**
 * 全局毛玻璃卡片容器，是 visualkit/components 层的核心基底卡片组件。
 *
 * **视觉效果**：
 * - DARK/NIGHT 主题：月晕径向渐变（左上角）+ 极光辉光（右下角，alpha=0.06 低调叠加）
 * - DAY 主题：顶部 30% 白色高光渐变 + 1dp 边框描边
 * - 所有主题均叠加双层阴影（近距紧凑 + 远距扩散），实现电影级景深质感
 *
 * **典型使用场景**：HomeScreen 主卡片、PlayerScreen 播放器面板、设置页分组容器
 *
 * **层级**：visualkit/components — 设计系统基础组件层
 *
 * @param modifier 外部传入的 Modifier，挂载在最外层 Box
 * @param cornerRadius 圆角半径，默认 [SWRadius.lg]
 * @param contentPadding 内容区内边距，默认 [SWSpacing.md] 四边等距
 * @param elevation 阴影层级，控制远景阴影强度，见 [ElevationLevel]
 * @param hero 是否启用呼吸缩放动画（0.998↔1.002，周期 [SWMotion.breathCycleMs] ≈4.2s）；
 *             `LocalReduceMotion=true` 时自动降级为静态，无障碍友好
 * @param surfaceTintOverride 自定义背景色；为 null 时使用 [SWColor.surfaceElevated]
 * @param content 卡片内部可组合内容插槽，作用域为 [BoxScope]
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    hero: Boolean = false,
    surfaceTintOverride: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = LocalSWScheme.current
    val reduce = LocalReduceMotion.current
    val shape = RoundedCornerShape(cornerRadius)

    // 近距阴影：始终使用 soft 级别，贴合卡片边缘形成轮廓感
    val close = SWShadow.soft(scheme)
    // 远距阴影：根据 elevation 等级选择扩散半径，实现分层景深
    val distant = when (elevation) {
        ElevationLevel.SOFT -> SWShadow.soft(scheme)
        ElevationLevel.MEDIUM -> SWShadow.medium(scheme)
        ElevationLevel.STRONG -> SWShadow.strong(scheme)
        ElevationLevel.HERO -> SWShadow.Spec(
            color = SWShadow.strong(scheme).color,
            radius = SWShadow.strong(scheme).radius + 16.dp,
            y = SWShadow.strong(scheme).y + 4.dp
        )
    }

    // 呼吸缩放：仅在 hero=true 且未开启减弱动效时启用无限循环动画
    val breathScale = if (hero && !reduce) {
        val t = rememberInfiniteTransition(label = "card-breath")
        val v by t.animateFloat(
            initialValue = 0.998f,
            targetValue = 1.002f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWMotion.breathCycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        v
    } else 1f  // 减弱动效或非 hero 模式：固定比例 1f，不做缩放

    // DAY 主题需要 1dp 边框描边以增强卡片边界感；暗色主题依靠阴影与背景区分，无需边框
    val borderMod = if (scheme == SWScheme.DAY) {
        Modifier.border(1.dp, SWColor.border(scheme), shape)
    } else Modifier

    Box(
        modifier = modifier
            .scale(breathScale)
            // dual-layer shadow — close tight + distant wide for cinematic depth
            .shadow(close.radius, shape, clip = false, ambientColor = close.color, spotColor = close.color)
            .shadow(distant.radius, shape, clip = false, ambientColor = distant.color, spotColor = distant.color)
            .clip(shape)
            .background(surfaceTintOverride ?: SWColor.surfaceElevated(scheme))
            .then(borderMod)
    ) {
        if (scheme != SWScheme.DAY) {
            // Moon halo top-left + aurora glow whisper bottom-right.
            // The glow is dimmed via Modifier.alpha because Compose's Brush has no
            // built-in alpha multiplier — the wrapping Box scales the gradient down
            // to a whisper without changing the gradient's stop colors.
            // 月晕渐变层：从左上角向右下放射，模拟月光洒落感
            Box(Modifier.matchParentSize().background(SWGradient.moonHalo(scheme)))
            // 极光辉光层：alpha=0.06 极低透明度叠加，右下角轻微暖调光晕，避免抢镜
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(0.06f)
                    .background(SWGradient.auroraGlow(scheme))
            )
        } else {
            // DAY: inner highlight — top 30% slightly brighter
            // 白天模式内高光：顶部白色渐变覆盖 30%，模拟光线从上方照入的玻璃质感
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.5f),
                        0.3f to Color.Transparent,
                        1f to Color.Transparent
                    )
                )
            )
        }
        // 内容区：统一施加 contentPadding，内部 Composable 内容在此渲染
        Box(Modifier.padding(contentPadding), content = content)
    }
}
