package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.SWMotion

/**
 * Shared-element key for the Sleep CTA → Sleeping timer halo morph (Hero #1).
 * The same value is used by both the Home origin and the Sleeping destination.
 */
private const val SLEEP_HERO_KEY = "sleep-hero"

/**
 * Tag this composable as the Home end of the Sleep hero morph.
 * Bounds animate to the Sleeping halo's position/size during route change.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sleepHeroOrigin(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
): Modifier = with(sharedScope) {
    this@sleepHeroOrigin.sharedBounds(
        sharedContentState = rememberSharedContentState(key = SLEEP_HERO_KEY),
        animatedVisibilityScope = animScope,
        boundsTransform = { _, _ -> tween(SWMotion.heroMorphMs, easing = LinearEasing) },
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
    )
}

/**
 * Tag this composable as the Sleeping end of the Sleep hero morph.
 * Same key as `sleepHeroOrigin` — Compose matches them during route change.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sleepHeroDestination(
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope
): Modifier = with(sharedScope) {
    this@sleepHeroDestination.sharedBounds(
        sharedContentState = rememberSharedContentState(key = SLEEP_HERO_KEY),
        animatedVisibilityScope = animScope,
        boundsTransform = { _, _ -> tween(SWMotion.heroMorphMs, easing = LinearEasing) },
        resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds
    )
}
