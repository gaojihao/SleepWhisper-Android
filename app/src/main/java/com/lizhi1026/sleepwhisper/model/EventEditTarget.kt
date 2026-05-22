/**
 * 事件编辑目标的密封类（model 领域层，纯 Kotlin，无 Android Framework 依赖）。
 *
 * **职责**：作为"当前正在编辑哪条事件记录"的类型安全容器，
 * 区分不同事件类型（喂奶、换尿布等），由上层状态驱动底部编辑弹窗。
 *
 * **数据来源**：对标 iOS EventEditSheet.swift 顶部 enum 定义。
 *
 * **交互方**：`AppStateContainer.pendingEditTarget`（持有当前实例，null 表示无编辑中）；
 * `EventEditSheet`（观察此字段，非 null 时展开底部弹窗并渲染对应表单）。
 *
 * **序列化说明**：故意不标注 `@Serializable`，子类持有具体事件对象，
 * 序列化生命周期由 `EventEditSheet` 的上层状态管理逻辑负责。
 */
package com.lizhi1026.sleepwhisper.model

// 来源：iOS EventEditSheet.swift 顶部 enum 定义
// 不加 @Serializable（含 sealed class 子类持有具体事件，序列化由 EventEditSheet 上层逻辑接管）

/**
 * 事件编辑目标密封类，每个子类对应一种可编辑的事件类型。
 *
 * ⚠️ 此类由 `AppStateContainer.pendingEditTarget`（`StateFlow<EventEditTarget?>`）持有；
 * 设为 null 表示关闭编辑弹窗，设为具体子类实例表示打开对应事件的编辑表单。
 * 所有子类均为 `data class`，保证等值比较与状态去重正确工作。
 */
sealed class EventEditTarget {
    /**
     * 正在编辑一条喂奶记录。
     *
     * @property event 待编辑的 [FeedingEvent] 实体（来自数据库的已存对象）
     */
    data class Feeding(val event: FeedingEvent) : EventEditTarget()

    /**
     * 正在编辑一条换尿布记录。
     *
     * @property event 待编辑的 [DiaperEvent] 实体（来自数据库的已存对象）
     */
    data class Diaper(val event: DiaperEvent) : EventEditTarget()
}
