/**
 * PresetAuraOrb — 音频预设"气场球"视觉组件。
 *
 * 视觉效果（由下至上四层叠加）：
 *   1. 径向渐变圆盘：从外到内依次为 aura.bottom → aura.mid → aura.top
 *   2. innerHighlight：顶部白色半透明新月高光，增强玻璃质感
 *   3. 4 个随机漂移粒子：以 aura.top 60% 透明度着色，使用固定随机种子保证一致性
 *   4. （仅激活态）外发光呼吸阴影：aura.mid 着色，
 *      透明度在 30%–60% 间以 [SWMotion.breathCycleMs] 周期循环
 *
 * 所在层：core/visualkit — 设计令牌层（可视组件工具包）
 *
 * 典型使用场景：PlayerScreen 预设列表行，每个 [AudioPreset] 通过
 * `AudioPreset.AuraColors` 得到专属配色，在列表中形成视觉区分。
 *
 * 降级策略：
 *   - `LocalReduceMotion = true`：粒子静止，呼吸阴影固定在中间值 45%
 *   - 低内存设备（`ActivityManager.isLowRamDevice`）：粒子层整体跳过
 */
package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion
import com.lizhi1026.sleepwhisper.model.AuraColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 单个漂移粒子的数据模型。
 *
 * @property nx        归一化 X 坐标（0.0–1.0，相对于 orb 宽度）
 * @property ny        归一化 Y 坐标（0.0–1.0，相对于 orb 高度）
 * @property baseRadiusDp 粒子基础半径（dp），随机分布在 0.8–1.4dp
 * @property phaseSec  正弦漂移的相位偏移（弧度），使每个粒子运动错开
 * @property periodSec 完成一次漂移循环所需秒数，随机分布在 4–7s
 */
private data class OrbParticle(
    val nx: Float,
    val ny: Float,
    val baseRadiusDp: Float,
    val phaseSec: Float,
    val periodSec: Float
)

/** 气场球中的粒子数量，固定 4 个以保持轻量 */
private const val ORB_PARTICLE_COUNT = 4
/** 粒子随机种子，固定值保证每次重组粒子布局一致 */
private const val ORB_PARTICLE_SEED = 0xA42C0BL
/** 粒子 Canvas 刷帧节流阈值：约 30fps，降低 GPU 负担 */
private const val ORB_FRAME_INTERVAL_MS = 33L  // 30fps

/**
 * 音频预设气场球 Composable。
 *
 * 根据 [aura] 的三色配方渲染一个带粒子与呼吸发光效果的圆形气场球，
 * 激活时（[isActive] = true）额外绘制外发光呼吸阴影。
 * 组件自动读取 [LocalReduceMotion] 与 `ActivityManager.isLowRamDevice`
 * 以适配无障碍与低内存设备。
 *
 * @param aura     预设的三色光晕配色（top/mid/bottom）
 * @param isActive 是否为当前激活的预设，控制呼吸阴影的开关
 * @param modifier Compose Modifier，可用于定位/边距等
 * @param sizeDp   气场球直径，默认 64dp
 */
@Composable
fun PresetAuraOrb(
    aura: AuraColors,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 64.dp
) {
    // 读取无障碍降低动效标志
    val reduce = LocalReduceMotion.current
    val context = LocalContext.current
    // 检测低内存设备，低内存时跳过粒子渲染层
    val isLowRam = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager
        am?.isLowRamDevice ?: false
    }
    val density = LocalDensity.current

    // 计算外发光阴影透明度：
    //   激活 + 正常动效 → 无限循环 30%~60%
    //   激活 + 降低动效 → 静止 45%（中间值）
    //   未激活         → 0（不显示阴影）
    val shadowAlpha: Float = if (isActive && !reduce) {
        // 使用无限过渡动画驱动呼吸效果
        val t = rememberInfiniteTransition(label = "orb-breath")
        val v by t.animateFloat(
            initialValue = 0.30f,
            targetValue = 0.60f,
            animationSpec = infiniteRepeatable(
                animation = tween(SWMotion.breathCycleMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse  // 来回反复，形成呼吸节律
            ),
            label = "shadow-alpha"
        )
        v
    } else if (isActive && reduce) 0.45f else 0f

    // 使用固定随机种子生成粒子，确保重组后布局不变
    val particles = remember {
        val r = Random(ORB_PARTICLE_SEED)
        List(ORB_PARTICLE_COUNT) {
            OrbParticle(
                nx = 0.20f + r.nextFloat() * 0.60f,       // 留 20% 边距，避免粒子贴边
                ny = 0.20f + r.nextFloat() * 0.60f,
                baseRadiusDp = 0.8f + r.nextFloat() * 0.6f,
                phaseSec = r.nextFloat() * (2f * PI.toFloat()),
                periodSec = 4f + r.nextFloat() * 3f
            )
        }
    }
    // particleClockMs 驱动粒子位移计算，由帧循环更新
    var particleClockMs by remember { mutableFloatStateOf(0f) }
    if (!reduce && !isLowRam) {
        // 帧驱动循环：节流到 ~30fps，减少高频重绘开销
        LaunchedEffect(Unit) {
            var lastTick = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    // 距上次更新超过节流阈值才推进时钟
                    if (frameMs - lastTick >= ORB_FRAME_INTERVAL_MS) {
                        particleClockMs = frameMs.toFloat()
                        lastTick = frameMs
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .then(
                // 激活态才添加外发光阴影，非激活态不产生任何 elevation 开销
                if (isActive) Modifier.shadow(
                    elevation = 24.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = aura.mid.copy(alpha = shadowAlpha),
                    spotColor = aura.mid.copy(alpha = shadowAlpha)
                ) else Modifier
            )
            .clip(CircleShape)
            // 径向渐变底层：top（中心）→ mid → bottom（外缘）
            .background(
                Brush.radialGradient(
                    colors = listOf(aura.top, aura.mid, aura.bottom),
                    radius = with(density) { sizeDp.toPx() * 0.7f }
                )
            )
            // 顶部新月高光（innerHighlight 为扩展函数，绘制白→透明渐变）
            .innerHighlight(cornerRadius = sizeDp / 2)
    ) {
        // 低内存设备跳过粒子层，其余设备渲染 Canvas 粒子
        if (!isLowRam) {
            Canvas(modifier = Modifier.size(sizeDp)) {
                val twoPi = 2f * PI.toFloat()
                val tSec = particleClockMs / 1000f  // 毫秒转秒，用于正弦函数计算
                particles.forEach { p ->
                    // 降低动效时偏移量归零，粒子保持静止
                    val dx = if (reduce) 0f else sin(tSec / p.periodSec * twoPi + p.phaseSec) * 0.05f
                    val dy = if (reduce) 0f else sin(tSec / (p.periodSec * 0.8f) * twoPi + p.phaseSec + 1f) * 0.05f
                    drawCircle(
                        color = aura.top.copy(alpha = 0.60f),  // 60% 透明度，与底层渐变自然融合
                        radius = p.baseRadiusDp.dp.toPx(),
                        center = Offset(
                            (p.nx + dx) * size.width,
                            (p.ny + dy) * size.height
                        )
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFF07101F,
    widthDp = 80,
    heightDp = 80
)
@Composable
private fun PreviewPresetAuraOrbActive() {
    PresetAuraOrb(
        aura = AuraColors(
            top = Color(0xFFC46B8C),
            mid = Color(0xFF5B4880),
            bottom = Color(0xFF1A0F1E)
        ),
        isActive = true,
        sizeDp = 64.dp
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    backgroundColor = 0xFF07101F,
    widthDp = 80,
    heightDp = 80
)
@Composable
private fun PreviewPresetAuraOrbIdle() {
    PresetAuraOrb(
        aura = AuraColors(
            top = Color(0xFFB7C3CC),
            mid = Color(0xFF4A5A6E),
            bottom = Color(0xFF0F1620)
        ),
        isActive = false,
        sizeDp = 64.dp
    )
}
