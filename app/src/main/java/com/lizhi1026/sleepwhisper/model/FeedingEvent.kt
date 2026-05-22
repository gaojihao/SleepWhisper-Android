/**
 * 喂养事件与换尿布事件实体模型。
 *
 * **职责**：记录宝宝的一次喂养（母乳、奶瓶、辅食）或换尿布事件，供主界面日志和趋势分析使用。
 *
 * **所在层**：model 层（领域模型），对应 iOS `FeedingEvent.swift` 和 `DiaperEvent.swift`。
 *
 * **与谁交互**：
 * - [AppStateContainer]（app 层）— 通过 `recordFeeding` / `recordDiaper` 写入，支持撤销和编辑。
 * - [Repository]（core/persistence）— 通过 `appendFeeding` / `appendDiaper` 等方法读写数据库。
 * - [EventEditTarget]（model 层）— 编辑操作时将事件包装为 `EventEditTarget.Feeding` 或
 *   `EventEditTarget.Diaper` 传入编辑面板。
 *
 * **关键约定**：
 * - [DiaperEvent] 故意不含 `isEdited` 字段，与 iOS 保持一致。
 * - [FeedingEvent.isValid] 用于服务端/存储前的完整性校验，UI 层也可用于表单验证。
 */
package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

// ─── 喂养事件 ──────────────────────────────────────────────────────────────────

/**
 * 单次喂养事件数据类。
 *
 * @property id              唯一标识符（UUID 字符串）。
 * @property babyId          关联的宝宝 ID。
 * @property method          喂养方式（母乳左/右、奶瓶、辅食）。
 * @property amountMl        奶瓶喂养时的奶量（毫升），非奶瓶方式可为 null。
 * @property durationSeconds 喂养持续时间（秒），可为 null（未记录）。
 * @property startedAt       喂养开始时间，UTC epoch 毫秒。
 * @property isEdited        是否经过用户手动编辑，默认 false。
 */
@Serializable
data class FeedingEvent(
    val id: String,
    val babyId: String,
    val method: FeedingMethod,
    val amountMl: Int? = null,
    val durationSeconds: Int? = null,
    val startedAt: Long, // epoch-millis UTC
    val isEdited: Boolean = false
) {
    enum class FeedingMethod(val serializedName: String) {
        BREAST_LEFT("breast_left"),
        BREAST_RIGHT("breast_right"),
        BOTTLE("bottle"),
        SOLID("solid")
    }

    val isValid: Boolean
        get() {
            if (method == FeedingMethod.BOTTLE && (amountMl ?: 0) < 0) return false
            if ((durationSeconds ?: 0) < 0) return false
            if (startedAt > System.currentTimeMillis()) return false
            return true
        }
}

// ─── 换尿布事件 ────────────────────────────────────────────────────────────────

/**
 * 单次换尿布事件数据类。
 *
 * ⚠️ 故意不含 `isEdited` 字段，与 iOS `DiaperEvent` 保持一致。
 *
 * @property id         唯一标识符（UUID 字符串）。
 * @property babyId     关联的宝宝 ID。
 * @property type       尿布类型（湿尿布、脏尿布、混合、干净）。
 * @property occurredAt 换尿布时间，UTC epoch 毫秒。
 */
@Serializable
data class DiaperEvent(
    val id: String,
    val babyId: String,
    val type: DiaperType,
    val occurredAt: Long // epoch-millis UTC
    // 故意不加 isEdited，与 DiaperEvent iOS 一致
) {
    enum class DiaperType(val serializedName: String) {
        WET("wet"),
        DIRTY("dirty"),
        MIXED("mixed"),
        DRY("dry")
    }
}
