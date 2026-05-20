package com.lizhi1026.sleepwhisper.core.visualkit.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.lizhi1026.sleepwhisper.core.visualkit.TimeOfDayPalette
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * Living night sky for the Sleeping screen.
 *
 * 5 layers composited in a single Canvas:
 *   1. Aurora gradient mesh — sky color from current wall-clock time
 *   2. Static breath-twinkle stars (~80)        — added in Task 3
 *   3. Drifting brighter stars (~6)             — added in Task 4
 *   4. Rare meteor showers                       — added in Task 5
 *   5. Atmospheric cloud wisps (deep dark hours) — added in Task 6
 *
 * @param morphProgress fade-in driver from Phase 1's hero morph (0..1).
 * @param timeProvider injectable for tests; defaults to wall-clock.
 */
@Composable
fun NightSkyCanvas(
    modifier: Modifier = Modifier,
    morphProgress: Float = 1f,
    timeProvider: () -> LocalTime = { LocalTime.now() }
) {
    // Repoll the palette every 60 seconds so the sky drifts across hour boundaries
    // without redrawing the gradient brush every frame.
    var palette by remember { mutableStateOf(TimeOfDayPalette.forTime(timeProvider())) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            palette = TimeOfDayPalette.forTime(timeProvider())
        }
    }

    Canvas(
        modifier = modifier
            .alpha(morphProgress.coerceIn(0f, 1f))
            .clearAndSetSemantics { }
    ) {
        // Layer 1: vertical gradient — bottom palette at top of screen, top palette
        // at bottom. The "top" in palette refers to horizon-warm hint near the bottom
        // of the screen at dusk.
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(palette.bottom, palette.top),
                startY = 0f,
                endY = size.height
            ),
            topLeft = Offset.Zero,
            size = size
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PreviewNightSkyCanvasEvening() {
    NightSkyCanvas(
        modifier = Modifier.fillMaxSize(),
        timeProvider = { LocalTime.of(21, 0) }
    )
}

@androidx.compose.ui.tooling.preview.Preview(
    showBackground = true,
    widthDp = 360,
    heightDp = 800
)
@Composable
private fun PreviewNightSkyCanvasDeadNight() {
    NightSkyCanvas(
        modifier = Modifier.fillMaxSize(),
        timeProvider = { LocalTime.of(3, 0) }
    )
}
