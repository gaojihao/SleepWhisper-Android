package com.lizhi1026.sleepwhisper.core.cry

sealed class CryDetectionState {
    data object Disabled : CryDetectionState()
    data object Listening : CryDetectionState()
    data object Triggered : CryDetectionState()
    data class Cooldown(val untilMs: Long) : CryDetectionState()
}
