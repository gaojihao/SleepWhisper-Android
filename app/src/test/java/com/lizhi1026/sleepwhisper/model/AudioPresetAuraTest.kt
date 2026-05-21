package com.lizhi1026.sleepwhisper.model

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPresetAuraTest {

    @Test fun `every bundled preset has a non-default AuraColors`() {
        AudioPreset.bundled.forEach { preset ->
            assertNotEquals(
                "Preset ${preset.id} is still using the default AuraColors — populate its auraColors field.",
                AuraColors.DEFAULT,
                preset.auraColors
            )
        }
    }

    @Test fun `all bundled preset mid colors are unique`() {
        val mids = AudioPreset.bundled.map { it.auraColors.mid }
        val uniqueMids = mids.toSet()
        assertTrue(
            "Found duplicate mid colors in AudioPreset.bundled: ${mids.size} total but only ${uniqueMids.size} unique. " +
                "Each preset's mid color drives the cross-screen ambient — they must be visually distinct.",
            mids.size == uniqueMids.size
        )
    }

    @Test fun `every preset's top color is brighter than its bottom color`() {
        AudioPreset.bundled.forEach { preset ->
            val top = preset.auraColors.top
            val bottom = preset.auraColors.bottom
            val topLuma = top.red + top.green + top.blue
            val bottomLuma = bottom.red + bottom.green + bottom.blue
            assertTrue(
                "Preset ${preset.id}: top color (luma=$topLuma) is not brighter than bottom (luma=$bottomLuma). " +
                    "auraColors swap detected — top should be the brightest highlight, bottom the darkest room color.",
                topLuma > bottomLuma
            )
        }
    }
}
