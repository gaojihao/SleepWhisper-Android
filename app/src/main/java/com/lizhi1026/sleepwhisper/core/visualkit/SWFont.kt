package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography tokens — direct port of iOS Core/Theme/Typography.swift.
 *
 * Tabular variants use OpenType "tnum" feature for monospaced digits — used by timers/countdowns
 * so digit-width jitter doesn't shift the surrounding text on each tick.
 */
object SWFont {
    private val rounded = FontFamily.SansSerif // fallback to platform; replace with custom rounded if needed
    private val plain = FontFamily.SansSerif
    private const val TNUM = "tnum"

    fun displayXL(): TextStyle = TextStyle(fontSize = 96.sp, fontWeight = FontWeight.SemiBold, fontFamily = rounded)
    fun displayLG(): TextStyle = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.SemiBold, fontFamily = rounded)
    fun displayMD(): TextStyle = TextStyle(fontSize = 48.sp, fontWeight = FontWeight.Medium, fontFamily = rounded)

    fun titleXL(): TextStyle = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, fontFamily = plain)
    fun titleLG(): TextStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, fontFamily = plain)
    fun titleMD(): TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = plain)

    fun bodyLG(): TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, fontFamily = plain)
    fun bodyMD(): TextStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, fontFamily = plain)

    fun labelMD(): TextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = plain)
    fun labelSM(): TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = plain)

    fun displayLGTabular(): TextStyle =
        TextStyle(fontSize = 64.sp, fontWeight = FontWeight.SemiBold, fontFamily = rounded, fontFeatureSettings = TNUM)

    fun displayLargeTabular(): TextStyle =
        TextStyle(fontSize = 72.sp, fontWeight = FontWeight.SemiBold, fontFamily = rounded, fontFeatureSettings = TNUM)

    fun displayXLTabular(): TextStyle =
        TextStyle(fontSize = 96.sp, fontWeight = FontWeight.SemiBold, fontFamily = rounded, fontFeatureSettings = TNUM)

    /**
     * Serif italic — port of iOS `.system(size:, .regular, .serif).italic()` used on the
     * Onboarding hero, Welcome greeting, Sleeping title, Trends/Settings headers.
     */
    fun serifItalic(size: Int): TextStyle = TextStyle(
        fontSize = size.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Serif,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
    )
}
