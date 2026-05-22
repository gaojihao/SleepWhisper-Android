package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.lizhi1026.sleepwhisper.core.visualkit.LocalReduceMotion
import com.lizhi1026.sleepwhisper.core.visualkit.TimeOfDayPalette
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay
import java.time.LocalTime

private data class TwinkleStar(
    val nx: Float,        // 归一化 x 坐标（0..1）
    val ny: Float,        // 归一化 y 坐标（0..1）
    val baseSizeDp: Float, // 基础半径（0.4..1.6 dp）
    val phase: Float,     // 闪烁正弦初始相位（0..2π）
    val periodMs: Float   // 闪烁周期（3000..5000 ms）
)

private const val STAR_COUNT = 80
private const val STAR_SEED = 0xA110A4L

private data class DriftingStar(
    var nx: Float,           // 当前归一化 x（可变，随时间向右漂移）
    val ny: Float,            // 固定归一化 y
    val baseSizeDp: Float,
    var alpha: Float,         // 当前透明度（0..1，重生时淡入）
    val phase: Float,
    val periodMs: Float,
    val velocityDpPerSec: Float  // 漂移速度（约 0.5 dp/s）
)

private const val DRIFTING_STAR_COUNT = 6
private const val DRIFTING_STAR_SEED = 0xD81FA1L
private const val DRIFTING_RESPAWN_FADE_MS = 800L

private data class Meteor(
    val startX: Float,       // 流星头部起始归一化 x（通常为 -0.05，屏幕左侧外）
    val startY: Float,       // 0.1..0.6 归一化 y
    val angleRad: Float,     // 运动方向角度（水平 ±30°，弧度）
    val spawnedAtMs: Long    // 生成时的系统时间戳（毫秒）
)

private const val METEOR_LIFE_MS = 600L
private const val METEOR_FADE_START_MS = 400L  // fade begins at 400ms of 600ms life
private const val METEOR_HEAD_DP = 16f
private const val METEOR_TAIL_LENGTH_DP = 60f
private const val METEOR_TAIL_THICKNESS_DP = 2f
private const val METEOR_MIN_INTERVAL_MS = 90_000L   // 90s
private const val METEOR_MAX_INTERVAL_MS = 180_000L  // 180s

private data class CloudWisp(
    var nx: Float,
    var ny: Float,
    val widthDp: Float,        // 云雾椭圆宽度（30..50 dp）
    val heightDp: Float,       // 云雾椭圆高度（10..14 dp）
    var alpha: Float,          // 当前透明度（0..0.02，极低以免遮挡星星）
    val velocityDpPerSec: Float = 0.1f,  // 水平漂移速度
    var lifetimeMs: Long       // 已存活时间（ms）；超过 MAX_LIFETIME 时重生
)

private const val WISP_COUNT = 2
private const val WISP_MAX_LIFETIME_MS = 120_000L
private const val WISP_FADE_EDGE_MS = 8_000L

// Canvas 帧间隔。33ms ≈ 30fps。高于主规范"星星闪烁 10fps"的目标值，
// 因为同一动画循环还驱动流星和漂移星渲染，两者需要 30fps 才流畅。
// 星星 alpha 由慢速正弦计算，额外帧变化微不可见。
// reduce-motion 时循环暂停（0fps）。
private const val FRAME_INTERVAL_MS = 33L

/**
 * # NightSkyCanvas — 睡眠屏动态星空画布
 *
 * 所属层级：Sleeping 屏幕的背景层，叠加于 [AuroraBackdrop] 之上、内容 UI 之下。
 *
 * Canvas 内 5 层绘制顺序（从底到顶）：
 *   1. 极光渐变网格 — 根据真实时钟通过 [TimeOfDayPalette] 选色的天空渐变
 *   2. 静态闪烁星星（约 80 颗） — 正弦驱动透明度，形成自然呼吸感
 *   3. 漂移明亮星星（约 6 颗） — 缓慢向右漂移，超出屏幕后从左侧重生并淡入
 *   4. 稀有流星（每 90~180 秒一次）— 600ms 生命周期，低内存设备跳过
 *   5. 大气云雾（22:00-03:59 可见）— 极低透明度椭圆，边缘 8s 淡入淡出
 *
 * 典型使用场景：Sleeping 屏幕全屏背景，与 [AuroraBackdrop] 叠用时需设置
 * `morphProgress` 以配合 Hero 过渡动画的淡入效果。
 *
 * ⚠️ 无障碍：读取 [LocalReduceMotion]，为 true 时所有动画停止（星星固定透明度 0.6）。
 * ⚠️ 低内存设备（isLowRamDevice）：流星和云雾层跳过，节省 CPU。
 *
 * @param morphProgress Hero 形变驱动的淡入进度（0..1），来自 Phase 1 过渡动画。
 * @param timeProvider 时间源，默认为系统时钟，可注入用于预览/测试。
 */
@Composable
fun NightSkyCanvas(
    modifier: Modifier = Modifier,
    morphProgress: Float = 1f,
    timeProvider: () -> LocalTime = { LocalTime.now() }
) {
    // 每分钟轮询一次时钟，更新调色盘（日出/日落色彩随时间自然过渡）
    var palette by remember { mutableStateOf(TimeOfDayPalette.forTime(timeProvider())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            palette = TimeOfDayPalette.forTime(timeProvider())
        }
    }

    // 静态闪烁星星：固定种子保证每次重组位置不变
    val stars = remember {
        val r = Random(STAR_SEED)
        List(STAR_COUNT) {
            TwinkleStar(
                nx = r.nextFloat(),
                ny = r.nextFloat(),
                baseSizeDp = 0.4f + r.nextFloat() * 1.2f,
                phase = r.nextFloat() * (2f * PI.toFloat()),
                periodMs = 3000f + r.nextFloat() * 2000f
            )
        }
    }

    // 漂移星星：可变列表（alpha、nx 在动画帧中被直接修改）
    val driftingStars = remember {
        val r = Random(DRIFTING_STAR_SEED)
        mutableListOf<DriftingStar>().apply {
            repeat(DRIFTING_STAR_COUNT) {
                add(DriftingStar(
                    nx = r.nextFloat(),
                    ny = r.nextFloat(),
                    baseSizeDp = 0.8f + r.nextFloat() * 1.0f,
                    alpha = 1f,
                    phase = r.nextFloat() * (2f * PI.toFloat()),
                    periodMs = 3000f + r.nextFloat() * 2000f,
                    velocityDpPerSec = 0.5f
                ))
            }
        }
    }

    // 闪烁时钟：由帧回调更新，驱动星星 alpha 的正弦计算
    var twinkleClockMs by remember { mutableFloatStateOf(0f) }
    if (!LocalReduceMotion.current) {
        // LaunchedEffect 1：驱动静态星星闪烁的时钟（限速 30fps）
        LaunchedEffect(Unit) {
            var lastTick = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (frameMs - lastTick >= FRAME_INTERVAL_MS) {
                        twinkleClockMs = frameMs.toFloat()
                        lastTick = frameMs
                    }
                }
            }
        }
    }
    if (!LocalReduceMotion.current) {
        // LaunchedEffect 2：驱动漂移星星位置更新（每帧按 dt 积分 x 坐标）
        LaunchedEffect(Unit) {
            var lastFrameMs = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (lastFrameMs == 0L) {
                        lastFrameMs = frameMs
                        return@withInfiniteAnimationFrameMillis
                    }
                    val dt = (frameMs - lastFrameMs).coerceAtMost(100L)  // 防止 app 切回时 dt 爆大
                    lastFrameMs = frameMs
                    val dtSec = dt / 1000f
                    driftingStars.forEachIndexed { i, ds ->
                        // 向右漂移（速度 / 屏幕宽 dp * dtSec）
                        ds.nx += ds.velocityDpPerSec / 360f * dtSec
                        if (ds.nx > 1.05f) {
                            // 超出右边界：从左侧外重生，随机新 y，alpha 从 0 开始淡入
                            driftingStars[i] = ds.copy(nx = -0.05f, ny = Random.nextFloat(), alpha = 0f)
                        } else if (ds.alpha < 1f) {
                            // 淡入阶段：按 DRIFTING_RESPAWN_FADE_MS 速率提升 alpha
                            ds.alpha = (ds.alpha + dtSec * (1000f / DRIFTING_RESPAWN_FADE_MS)).coerceAtMost(1f)
                        }
                    }
                }
            }
        }
    }
    val reduce = LocalReduceMotion.current

    // 第 4 层 — 流星。同时只存在一颗，每 90~180s 随机触发一次。
    // 低内存设备和 reduce-motion 时禁用。
    val context = androidx.compose.ui.platform.LocalContext.current
    val isLowRam = remember {
        val am = context.getSystemService(android.content.Context.ACTIVITY_SERVICE)
            as? android.app.ActivityManager
        am?.isLowRamDevice ?: false
    }
    var activeMeteor by remember { mutableStateOf<Meteor?>(null) }
    if (!reduce && !isLowRam) {
        // LaunchedEffect 3：流星调度协程，随机等待后生成流星，600ms 后清除
        LaunchedEffect(Unit) {
            while (true) {
                val nextDelay = Random.nextLong(METEOR_MIN_INTERVAL_MS, METEOR_MAX_INTERVAL_MS)
                delay(nextDelay)
                activeMeteor = Meteor(
                    startX = -0.05f,
                    startY = 0.1f + Random.nextFloat() * 0.5f,
                    angleRad = ((-30 + Random.nextFloat() * 60) * PI / 180f).toFloat(),
                    spawnedAtMs = System.currentTimeMillis()
                )
                delay(METEOR_LIFE_MS)
                activeMeteor = null
            }
        }
    }

    // 第 5 层 — 大气云雾。仅在 22:00-03:59 时段可见，低内存设备禁用。
    val currentHour = remember(palette) {
        timeProvider().hour
    }
    val wispsVisible = currentHour in 22..23 || currentHour in 0..3
    val wisps = remember {
        val r = Random(0xC10D55L)
        mutableListOf<CloudWisp>().apply {
            repeat(WISP_COUNT) {
                add(CloudWisp(
                    nx = r.nextFloat(),
                    ny = 0.2f + r.nextFloat() * 0.6f,
                    widthDp = 30f + r.nextFloat() * 20f,
                    heightDp = 10f + r.nextFloat() * 4f,
                    alpha = 0f,
                    lifetimeMs = r.nextLong(0, WISP_MAX_LIFETIME_MS)  // 随机初始寿命，避免同步出现
                ))
            }
        }
    }
    if (!reduce && !isLowRam && wispsVisible) {
        // LaunchedEffect 4：云雾漂移与生命周期管理，每帧更新位置和淡入淡出 alpha
        LaunchedEffect(wispsVisible) {
            var lastFrameMs = 0L
            while (true) {
                withInfiniteAnimationFrameMillis { frameMs ->
                    if (lastFrameMs == 0L) {
                        lastFrameMs = frameMs
                        return@withInfiniteAnimationFrameMillis
                    }
                    val dt = (frameMs - lastFrameMs).coerceAtMost(100L)
                    lastFrameMs = frameMs
                    val dtSec = dt / 1000f
                    wisps.forEachIndexed { i, w ->
                        w.lifetimeMs += dt
                        w.nx += w.velocityDpPerSec / 360f * dtSec  // 向右缓慢漂移
                        // 生命周期两端各 8s 淡入/淡出，中间维持 alpha 0.02
                        val targetAlpha = when {
                            w.lifetimeMs < WISP_FADE_EDGE_MS ->
                                0.02f * (w.lifetimeMs.toFloat() / WISP_FADE_EDGE_MS)
                            w.lifetimeMs > WISP_MAX_LIFETIME_MS - WISP_FADE_EDGE_MS ->
                                0.02f * ((WISP_MAX_LIFETIME_MS - w.lifetimeMs).toFloat() / WISP_FADE_EDGE_MS).coerceAtLeast(0f)
                            else -> 0.02f
                        }
                        w.alpha = targetAlpha
                        if (w.lifetimeMs >= WISP_MAX_LIFETIME_MS) {
                            // 寿命结束：从屏幕左侧外重生，随机新 y，重置生命周期
                            wisps[i] = w.copy(
                                nx = -0.1f,
                                ny = 0.2f + Random.nextFloat() * 0.6f,
                                lifetimeMs = 0L
                            )
                        }
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .alpha(morphProgress.coerceIn(0f, 1f))  // 配合 Hero 过渡动画的整体淡入
            .clearAndSetSemantics { }                 // 装饰性组件，屏蔽无障碍树
    ) {
        // 第 1 层：极光天空渐变（bottom 在顶部，top 在底部，形成由深到浅的自然天空色）
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.bottom, palette.top),
                startY = 0f,
                endY = size.height
            ),
            topLeft = Offset.Zero,
            size = size
        )

        // 第 2 层：静态闪烁星星（reduce-motion 时固定 alpha 0.6，否则正弦插值 0.3..0.9）
        val twoPi = 2f * PI.toFloat()
        stars.forEach { star ->
            val alpha = if (reduce) {
                0.6f
            } else {
                val sinVal = sin(twinkleClockMs / star.periodMs * twoPi + star.phase)
                0.3f + 0.6f * (sinVal * 0.5f + 0.5f)  // 映射正弦值到 [0.3, 0.9]
            }
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.baseSizeDp.dp.toPx(),
                center = Offset(star.nx * size.width, star.ny * size.height)
            )
        }
        // 第 3 层：漂移明亮星星（alpha 叠乘 ds.alpha 支持淡入过渡）
        driftingStars.forEach { ds ->
            val alphaBase = if (reduce) 0.85f else {
                val sinVal = sin(twinkleClockMs / ds.periodMs * twoPi + ds.phase)
                0.5f + 0.5f * (sinVal * 0.5f + 0.5f)  // 映射到 [0.5, 1.0]，比静态星更亮
            }
            drawCircle(
                color = Color.White.copy(alpha = alphaBase * ds.alpha),  // ds.alpha 控制淡入
                radius = ds.baseSizeDp.dp.toPx(),
                center = Offset(ds.nx * size.width, ds.ny * size.height)
            )
        }
        // 第 4 层：流星条纹（如果当前有活跃流星）
        // 注意：读取 twinkleClockMs 状态值是为了让此处随帧重组，否则流星位置只在
        // activeMeteor 变更（约每 120s）时更新一次，导致流星看起来静止。
        if (activeMeteor != null) {
            // 触碰 twinkleClockMs 以确保每帧重组
            @Suppress("UNUSED_EXPRESSION") twinkleClockMs
        }
        activeMeteor?.let { m ->
            val ageMs = System.currentTimeMillis() - m.spawnedAtMs
            val lifeFraction = ageMs.toFloat() / METEOR_LIFE_MS.toFloat()
            if (lifeFraction in 0f..1f) {
                // 流星头部当前归一化坐标（从 startX 线性移动到 1.1）
                val headX = m.startX + (1.1f - m.startX) * lifeFraction
                val headY = m.startY + sin(m.angleRad) * (lifeFraction * 0.3f)
                // 尾部方向向量（与运动方向相反）
                val tailDx = -kotlin.math.cos(m.angleRad) * (METEOR_TAIL_LENGTH_DP.dp.toPx())
                val tailDy = -sin(m.angleRad) * (METEOR_TAIL_LENGTH_DP.dp.toPx())
                // 400ms 后开始淡出（生命周期最后 200ms 线性减弱）
                val alpha = if (ageMs < METEOR_FADE_START_MS) 1f
                            else 1f - (ageMs - METEOR_FADE_START_MS) / (METEOR_LIFE_MS - METEOR_FADE_START_MS).toFloat()

                // 尾迹：从头部向后拉的渐细线条，alpha 乘以 0.6 使其比头部更淡
                drawLine(
                    color = Color.White.copy(alpha = alpha * 0.6f),
                    start = Offset(headX * size.width + tailDx, headY * size.height + tailDy),
                    end = Offset(headX * size.width, headY * size.height),
                    strokeWidth = METEOR_TAIL_THICKNESS_DP.dp.toPx()
                )
                // 头部：亮白圆点，全 alpha
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = METEOR_HEAD_DP.dp.toPx() / 2f,
                    center = Offset(headX * size.width, headY * size.height)
                )
            }
        }
        // 第 5 层：大气云雾（以填充椭圆模拟，极低 alpha 避免遮挡星星）
        if (wispsVisible) {
            // 触碰 twinkleClockMs 确保云雾漂移随帧刷新
            @Suppress("UNUSED_EXPRESSION") twinkleClockMs
            wisps.forEach { w ->
                if (w.alpha > 0f) {
                    drawOval(
                        color = Color.White.copy(alpha = w.alpha),
                        topLeft = Offset(
                            x = w.nx * size.width - w.widthDp.dp.toPx() / 2f,
                            y = w.ny * size.height - w.heightDp.dp.toPx() / 2f
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            w.widthDp.dp.toPx(),
                            w.heightDp.dp.toPx()
                        )
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PreviewNightSkyCanvasEvening() {
    NightSkyCanvas(
        modifier = Modifier.fillMaxSize(),
        timeProvider = { LocalTime.of(21, 0) }
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PreviewNightSkyCanvasDeadNight() {
    NightSkyCanvas(
        modifier = Modifier.fillMaxSize(),
        timeProvider = { LocalTime.of(3, 0) }
    )
}
