package com.lizhi1026.sleepwhisper.app

import androidx.compose.animation.core.Animatable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateContainerMorphTest {

    @Test fun `morphProgress starts at zero`() {
        val progress = Animatable(0f)
        assertEquals(0f, progress.value, 0.001f)
    }

    @Test fun `animateTo 1f reaches target`() = runTest {
        val progress = Animatable(0f)
        progress.animateTo(targetValue = 1f)
        assertEquals(1f, progress.value, 0.001f)
    }

    @Test fun `snapTo bypasses animation`() = runTest {
        val progress = Animatable(0f)
        progress.snapTo(1f)
        assertEquals(1f, progress.value, 0.001f)
        progress.snapTo(0f)
        assertEquals(0f, progress.value, 0.001f)
    }

    @Test fun `animateTo reverse from 1f to 0f works`() = runTest {
        val progress = Animatable(1f)
        progress.animateTo(targetValue = 0f)
        assertEquals(0f, progress.value, 0.001f)
    }
}
