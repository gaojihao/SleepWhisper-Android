/**
 * 宝宝信息实体模型。
 *
 * **职责**：表示一个宝宝的基础档案信息，是绝大多数业务数据（睡眠、喂养等）的父实体。
 *
 * **所在层**：model 层（领域模型），与 iOS `Baby.swift` 字段一一对应。
 *
 * **与谁交互**：
 * - [AppStateContainer]（app 层）— 持有 `_baby` LiveData，驱动 `rootKey` 导航。
 * - [Repository]（core/persistence）— 通过 `saveBaby` / `loadBaby` 读写数据库。
 * - [WakeWindowRule]（model 层）— 通过 [ageInMonths] 查表获取对应的唤醒窗口规则。
 *
 * **关键约定**：
 * - `dateOfBirth` 存储为 UTC epoch 毫秒；[ageInMonths] 和 [ageParts] 均基于本地日期计算。
 * - ⚠️ [LifePhase] 当前只使用 `INFANT`（月龄 < 36），`TODDLER_PLUS` 为预留枚举值，
 *   相关功能尚未实现。
 * - [isValidName] 是 UI 层输入校验的唯一依据，不超过 20 个字符且不为空。
 */
package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

/**
 * 宝宝基础档案数据类。
 *
 * @property id              唯一标识符（UUID 字符串）。
 * @property name            宝宝昵称，须满足 [isValidName] 校验（非空且不超过 20 字符）。
 * @property gender          性别枚举 [BabyGender]。
 * @property dateOfBirth     出生日期，UTC epoch 毫秒。
 * @property avatarLocalPath 头像本地文件路径，可为 null（未设置头像时）。
 * @property createdAt       档案创建时间，UTC epoch 毫秒。
 * @property updatedAt       档案最后更新时间，UTC epoch 毫秒。
 */
@Serializable
data class Baby(
    val id: String,
    val name: String,
    val gender: BabyGender,
    val dateOfBirth: Long, // epoch-millis UTC
    val avatarLocalPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    enum class BabyGender(val serializedName: String) {
        FEMALE("female"),
        MALE("male"),
        UNKNOWN("unknown")
    }

    enum class LifePhase {
        INFANT,
        TODDLER_PLUS
    }

    companion object {
        fun isValidName(name: String): Boolean =
            name.trim().isNotEmpty() && name.trim().length <= 20
    }

    /**
     * 计算从出生日期到 [now] 的完整月龄（整数向下取整）。
     *
     * @param now 当前时刻，UTC epoch 毫秒，默认为系统当前时间。
     * @return 整月龄，0 表示不足一个月。
     */
    fun ageInMonths(now: Long = System.currentTimeMillis()): Int {
        val birth = LocalDate.ofEpochDay(dateOfBirth / 86_400_000L)
        val today = LocalDate.ofEpochDay(now / 86_400_000L)
        return Period.between(birth, today).years * 12 + Period.between(birth, today).months
    }

    /**
     * 三段式年龄分量：年 / 月 / 日，与 iOS `Baby.ageDisplay` 中的拆分方式一致。
     *
     * 供 UI 层使用复数感知字符串资源渲染成 "X岁Y个月Z天" 等格式。
     */
    data class AgeParts(val years: Int, val months: Int, val days: Int)

    /**
     * 返回从出生日期到 [now] 的三段式年龄分量（年/月/日），各分量均 ≥ 0。
     *
     * @param now 当前时刻，UTC epoch 毫秒，默认为系统当前时间。
     * @return [AgeParts] 实例，各字段已通过 `coerceAtLeast(0)` 保证非负。
     */
    fun ageParts(now: Long = System.currentTimeMillis()): AgeParts {
        val birth = LocalDate.ofEpochDay(dateOfBirth / 86_400_000L)
        val today = LocalDate.ofEpochDay(now / 86_400_000L)
        val p = Period.between(birth, today)
        return AgeParts(p.years.coerceAtLeast(0), p.months.coerceAtLeast(0), p.days.coerceAtLeast(0))
    }

    /**
     * 返回当前成长阶段，与 iOS `currentPhase` 计算逻辑等价。
     *
     * ⚠️ 当前业务逻辑仅使用 [LifePhase.INFANT]（月龄 < 36），[LifePhase.TODDLER_PLUS]
     * 为预留枚举值，尚未实现对应功能。
     *
     * @param now 当前时刻，UTC epoch 毫秒，默认为系统当前时间。
     * @return 月龄 < 36 返回 [LifePhase.INFANT]，否则返回 [LifePhase.TODDLER_PLUS]。
     */
    fun currentPhase(now: Long = System.currentTimeMillis()): LifePhase =
        if (ageInMonths(now) < 36) LifePhase.INFANT else LifePhase.TODDLER_PLUS
}
