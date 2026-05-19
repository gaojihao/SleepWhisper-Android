package com.lizhi1026.sleepwhisper.core.audio

sealed class PlayerState {
    data object Idle : PlayerState()
    data class Loading(val presetId: String) : PlayerState()
    data class Playing(val presetId: String, val endsAt: Long?) : PlayerState()
    data class Paused(val presetId: String, val remainingMs: Long?) : PlayerState()
    data class FadingOut(val presetId: String) : PlayerState()
    data object Stopped : PlayerState()
    data class Interrupted(val restoreTo: String?) : PlayerState()
    data class Error(val message: String) : PlayerState()
}
