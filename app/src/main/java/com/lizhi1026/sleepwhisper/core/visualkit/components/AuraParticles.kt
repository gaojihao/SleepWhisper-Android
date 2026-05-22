package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class AuraMote(
    val nx: Float,          // 基准归一化 x 坐标（0..1），动画中以此为中心左右漂移
    val ny: Float,          // 基准归一化 y 坐标（0..1），动画中以此为中心上下漂移
    val baseRadiusDp: Float, // 基础半径（1.5..3.5 dp）
    val phaseSec: Float,    // 漂移正弦初始相位（秒，0..2π）
    val periodSec: Float    // 漂移周期（8..16 秒），各光点节奏各异
)

private const val AURA_MOTE_COUNT = 24
private const val AURA_MOTE_SEED = 0xBE5F12L
private const val AURA_FRAME_INTERVAL_MS = 33L  // 30fps

/**
 * # AuraParticles — 漂浮光粒子全屏叠层
 *
 * 所属层级：内容层最上方叠加层，叠加于播放器 UI 之上、弹窗之下。
 *
 * 视觉效果：
 * - 24 个小光点（mote）以固定随机种子均匀散布全屏，每个光点以各自的周期
 *   和相位做双正弦漂移（x 轴和 y 轴相位错开 1 弧度，产生椭圆轨迹）。
 * - 漂移幅度为屏幕宽/高的 ±4%，运动轻微，不干扰 UI 可读性。
 * - 光点颜色为传入的 [tint] 颜色，固定 alpha 0.30，形成朦胧光晕感。
 *
 * 典型使用场景：Player 屏幕作为氛围叠层，让房间在音频预设播放时有"活跃"感。
 *
 * 指针穿透：组件不消耗触摸事件，不影响下层 UI 交互。
 *
 * ⚠️ 无障碍：读取 [LocalReduceMotion]，为 true 时光点固定在基准位置，不漂移。
 * ⚠️ 低内存设备（isLowRamDevice）：直接 return，不绘制任何内容。
 *
 * @param tint 光点颜色，通常与当前音频预设的主题色保持一致。
 * @param modifier 外部布局修饰符。
 */
@Composable
fun AuraParticles(
    tint: Color,
    modifier: Modifier = Modifier
) {
    val reduce = LocalReduceMotion.current
    val context = LocalContext.current
    // 低内存设备检测：一次性 remember，不响应设备状态变化
    val isLowRam = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager
        am?.isLowRamDevice ?: false
    }
    // 低内存设备直接跳过，节省 GPU 合成开销
    if (isLowRam) return

    // 24 个光点，固定种子保证每次重组布局稳定，不随机跳动
    val motes = remember {
        val r = Random(AURA_MOTE_SEED)
        List(AURA_MOTE_COUNT) {
            AuraMote(
                nx = r.nextFloat(),
                ny = r.nextFloat(),
                baseRadiusDp = 1.5f + r.nextFloat() * 2.0f,
                phaseSec = r.nextFloat() * (2f * PI.toFloat()),
                periodSec = 8f + r.nextFloat() * 8f  // 8~16 秒周期，各点节奏错开
            )
        }
    }

    // 动画时钟：限速 30fps（33ms 间隔），reduce-motion 时不启动
    var clockMs by remember { mutableFloatStateOf(0f) }
    if (!reduce) {
        // LaunchedEffect：帧驱动时钟，每 33ms 更新一次 clockMs 触发重绘
        LaunchedEffect(Unit) {
            var lastTick = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (frameMs - lastTick >= AURA_FRAME_INTERVAL_MS) {
                        clockMs = frameMs.toFloat()
                        lastTick = frameMs
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.clearAndSetSemantics { }) {  // 装饰性，屏蔽无障碍语义
        val twoPi = 2f * PI.toFloat()
        val tSec = clockMs / 1000f  // 将毫秒时钟换算为秒，与 periodSec 单位对齐
        motes.forEach { m ->
            // reduce-motion 时偏移为 0（固定位置），否则双正弦产生椭圆漂移轨迹
            val dx = if (reduce) 0f else sin(tSec / m.periodSec * twoPi + m.phaseSec) * 0.04f
            // y 轴相位多加 1 弧度，使 x/y 轨迹不同步，形成自然椭圆而非直线往复
            val dy = if (reduce) 0f else sin(tSec / (m.periodSec * 0.9f) * twoPi + m.phaseSec + 1f) * 0.04f
            drawCircle(
                color = tint.copy(alpha = 0.30f),  // 固定低 alpha 保持朦胧感
                radius = m.baseRadiusDp.dp.toPx(),
                center = Offset(
                    (m.nx + dx) * size.width,
                    (m.ny + dy) * size.height
                )
            )
        }
    }
}
