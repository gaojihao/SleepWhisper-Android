package com.lizhi1026.sleepwhisper.model

// 来源：iOS EventEditSheet.swift 顶部 enum 定义
// 不加 @Serializable（含 sealed class 子类持有具体事件，序列化由 EventEditSheet 上层逻辑接管）

sealed class EventEditTarget {
    data class Feeding(val event: FeedingEvent) : EventEditTarget()
    data class Diaper(val event: DiaperEvent) : EventEditTarget()
}
