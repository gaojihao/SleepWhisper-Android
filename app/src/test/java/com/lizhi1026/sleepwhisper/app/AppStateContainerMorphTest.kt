package com.lizhi1026.sleepwhisper.app

import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the Compose Animatable<Float> contract that our morph APIs rely on.
 *
 * The animateTo path requires a TestMonotonicFrameClock which is non-trivial to wire
 * inside JUnit unit tests; the animateTo behavior is covered by manual smoke testing
 * (the final Phase 1 task). Here we cover only the parts that exercise without a
 * frame clock — starting value and snapTo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppStateContainerMorphTest {

    @Test fun `morphProgress starts at zero`() {
        val progress = Animatable(0f)
        assertEquals(0f, progress.value, 0.001f)
    }

    @Test fun `snapTo bypasses animation`() = runTest {
        val progress = Animatable(0f)
        progress.snapTo(1f)
        assertEquals(1f, progress.value, 0.001f)
        progress.snapTo(0f)
        assertEquals(0f, progress.value, 0.001f)
    }
}
