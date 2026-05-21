package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.ui.graphics.Color
import com.lizhi1026.sleepwhisper.core.visualkit.HeroBackdropController
import com.lizhi1026.sleepwhisper.model.AudioPreset

/**
 * Push the active preset's mid-aura color to the backdrop. If no preset is
 * active, fall back to the caller's contextual color (e.g. Home's evening
 * time-of-day hint). Pass null for [fallback] when there is no fallback
 * (e.g. on the Player screen itself).
 *
 * Priority (highest first):
 *   1. currentPreset.auraColors.mid
 *   2. fallback
 *   3. null (backdrop uses default Aurora palette)
 */
fun HeroBackdropController.syncToPlayer(
    currentPreset: AudioPreset?,
    fallback: Color?
) {
    setAmbient(currentPreset?.auraColors?.mid ?: fallback)
}
