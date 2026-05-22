package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.time.LocalTime

/**
 * 天空色板数据类——描述背景渐变的底色、顶色与胶片颗粒密度。
 *
 * 用于 [TimeOfDayPalette.forTime] 的返回值，由 SkyBackdrop / AuroraBackdrop
 * 直接消费，构造 `Brush.linearGradient(bottom, top)` 并叠加噪点纹理。
 *
 * @param bottom       渐变底部颜色（屏幕下边缘）
 * @param top          渐变顶部颜色（屏幕上边缘）
 * @param grainDensity 胶片颗粒密度（0f~1f），极夜时段提高至 0.12f 增强深邃质感
 */
data class SkyPalette(
    val bottom: Color,
    val top: Color,
    val grainDensity: Float = 0.08f
)

/**
 * 根据真实墙钟时间返回对应天空色板，驱动背景渐变实时变化。
 *
 * 设计原理：6 个时间锚点定义天空色彩关键帧，相邻锚点间线性插值，
 * 确保 30 分钟短睡视觉静止、8 小时通宵睡眠呈现完整黄昏→深夜→黎明色彩历程。
 *
 * 时间轴使用"扩展小时"（extended hours）避免跨 24h 边界插值异常：
 *  - 19.0 = 当天 19:00（黄昏）
 *  - 24.0 = 次日 00:00（午夜）
 *  - 29.0 = 次日 05:00（黎明）
 * 凌晨 0:00~06:29 的时刻在映射时加 24，统一落入 24.0~30.29 区间。
 *
 * 调用方：SkyBackdropViewModel，每分钟更新一次（或睡眠开始/结束时立即刷新）。
 */
object TimeOfDayPalette {

    /** 内部时间锚点——(扩展小时, 天空色板) 键值对。 */
    private data class Stop(val hour: Float, val palette: SkyPalette)

    /**
     * 6 个时间锚点，覆盖完整昼夜循环：
     *  06:30 → 晨曦回转（睡醒蓝底+暖橙顶）
     *  19:00 → 黄昏（蓝底+暮光橙顶）
     *  21:00 → 傍晚（深蓝底+星光紫顶）
     *  24:00 → 午夜（极深蓝底+深靛顶）
     *  03:00 → 极夜（同午夜色，grainDensity 提至 0.12）
     *  05:00 → 黎明（回归晨曦暖橙，与 06:30 形成闭环）
     */
    private val STOPS = listOf(
        Stop(6.5f,  SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E))),                          // 06:30 晨曦：蓝底暖橙顶
        Stop(19.0f, SkyPalette(Color(0xFF1A2540), Color(0xFFFFB088))),                          // 19:00 黄昏：暮光橙主轴
        Stop(21.0f, SkyPalette(Color(0xFF0F1B30), Color(0xFFB8A4FF))),                          // 21:00 傍晚：星光紫主轴
        Stop(24.0f, SkyPalette(Color(0xFF050912), Color(0xFF1F2848))),                          // 00:00 午夜：极深沉静
        Stop(27.0f, SkyPalette(Color(0xFF050912), Color(0xFF1F2848), grainDensity = 0.12f)),    // 03:00 极夜：颗粒密度提升增质感
        Stop(29.0f, SkyPalette(Color(0xFF1F2540), Color(0xFFFFA98E)))                           // 05:00 黎明：回归暖橙，接近闭环
    )

    /**
     * 根据墙钟时间 [t] 返回线性插值后的 [SkyPalette]。
     *
     * 插值逻辑：
     * 1. 将 [t] 转换为扩展小时（凌晨 0~06:29 映射为 24~30.29）。
     * 2. 二分查找包围区间的上下锚点。
     * 3. 对 bottom / top / grainDensity 三字段分别线性插值。
     *
     * @param t 当前系统本地时间
     * @return 当前时刻的天空色板（已插值，可直接用于渐变构造）
     */
    fun forTime(t: LocalTime): SkyPalette {
        val rawHour = t.hour + t.minute / 60f
        // 凌晨 0:00~06:29 加 24，统一映射到扩展时间轴 24~30.29
        val effective = if (rawHour < 6.5f) rawHour + 24f else rawHour

        val upperIdx = STOPS.indexOfFirst { it.hour >= effective }
        if (upperIdx == -1) {
            return STOPS.last().palette  // 超出最后锚点，返回黎明色板
        }
        if (upperIdx == 0) {
            return STOPS.first().palette  // 早于第一锚点，返回晨曦色板
        }
        val upper = STOPS[upperIdx]
        val lower = STOPS[upperIdx - 1]
        // 计算在相邻两锚点间的插值比例 [0f, 1f]
        val fraction = (effective - lower.hour) / (upper.hour - lower.hour)

        return SkyPalette(
            bottom = lerp(lower.palette.bottom, upper.palette.bottom, fraction),
            top = lerp(lower.palette.top, upper.palette.top, fraction),
            // grainDensity 同步线性插值，03:00 极夜区间颗粒感自然过渡
            grainDensity = lower.palette.grainDensity +
                (upper.palette.grainDensity - lower.palette.grainDensity) * fraction
        )
    }
}
