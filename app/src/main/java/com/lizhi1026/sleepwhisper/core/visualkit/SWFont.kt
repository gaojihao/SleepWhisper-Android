package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontVariant
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.lizhi1026.sleepwhisper.R

/**
 * Aurora typography tokens. Public function names unchanged from earlier; the
 * underlying font families and metrics have been rebuilt:
 *
 *   - DM Serif Display drives all `display*()` styles (large headlines only).
 *   - Source Serif 4 drives `title*` / `body*` / `label*` and tabular numerals.
 *   - Source Serif 4 Italic is the emotional accent (`serifItalic`).
 *   - Noto Serif CJK SC (downloadable via Google Fonts Compose) is the fallback
 *     for any text containing Chinese characters — see [cjkAware].
 *
 * Tabular variants use OpenType "tnum" so digit-width jitter doesn't shift the
 * surrounding text on each tick.
 */
object SWFont {

    private const val TNUM = "tnum"

    // ── Google Fonts provider & CJK face ─────────────────────────────────────
    private val GoogleProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    private val NotoSerifCjkSc = GoogleFont("Noto Serif SC")

    private val CjkRegularFont  = GoogleFontVariant(googleFont = NotoSerifCjkSc, fontProvider = GoogleProvider, weight = FontWeight.Normal)
    private val CjkSemiBoldFont = GoogleFontVariant(googleFont = NotoSerifCjkSc, fontProvider = GoogleProvider, weight = FontWeight.SemiBold)

    // These plain-Font arrays are the single source of truth for every public
    // FontFamily in this object and for the CJK merged families in cjkAware.
    private val DisplayFonts: Array<Font> = arrayOf(
        Font(R.font.dm_serif_display_regular, FontWeight.Normal)
    )
    private val BodyFonts: Array<Font> = arrayOf(
        Font(R.font.source_serif_4_regular,  FontWeight.Normal),
        Font(R.font.source_serif_4_italic,   FontWeight.Normal, FontStyle.Italic),
        Font(R.font.source_serif_4_semibold, FontWeight.SemiBold),
        Font(R.font.source_serif_4_bold,     FontWeight.Bold)
    )
    private val CjkFonts: Array<Font> = arrayOf(CjkRegularFont, CjkSemiBoldFont)

    // ── Public stable families (used directly by token functions) ─────────────
    private val DisplaySerif = FontFamily(*DisplayFonts)
    private val BodySerif    = FontFamily(*BodyFonts)

    /** Single-weight CJK fallback family. Loaded once via Google Play Services. */
    val CjkFallback: FontFamily = FontFamily(*CjkFonts)

    /**
     * Wraps a TextStyle so that any unresolved glyph in the Latin family falls
     * through to the CJK serif. Use for any text that may render baby names,
     * Chinese greetings, localized preset names, etc.
     *
     * Compose's font resolution walks the list of families in order until a glyph
     * is found, so the Latin family is tried first (faster, no network) and CJK
     * resolves anything Latin doesn't have.
     */
    fun cjkAware(style: TextStyle): TextStyle =
        style.copy(fontFamily = buildCjkFamily(style.fontFamily))

    /**
     * Returns a new [FontFamily] whose fonts are the fonts from [base] followed
     * by the CJK fallback fonts. When [base] is a [FontListFontFamily] we flatten
     * its list so that the CJK faces can participate in the same fallback chain.
     */
    private fun buildCjkFamily(base: FontFamily?): FontFamily = when {
        base === null || base === BodySerif ->
            FontFamily(*BodyFonts,    *CjkFonts)

        base === DisplaySerif ->
            FontFamily(*DisplayFonts, *BodyFonts, *CjkFonts)  // Display has no CJK face of its own

        base is FontListFontFamily && base.fonts.isNotEmpty() ->
            FontFamily(*base.fonts.toTypedArray(), *CjkFonts)

        else ->
            FontFamily(*CjkFonts)
    }

    // ─── Display (DM Serif Display) ──────────────────────────────────────────
    fun displayXL(): TextStyle = TextStyle(
        fontSize = 96.sp, fontFamily = DisplaySerif, fontWeight = FontWeight.Normal,
        letterSpacing = (-2).sp, lineHeight = 96.sp
    )
    fun displayLG(): TextStyle = TextStyle(
        fontSize = 64.sp, fontFamily = DisplaySerif, fontWeight = FontWeight.Normal,
        letterSpacing = (-1).sp, lineHeight = 67.sp
    )
    fun displayMD(): TextStyle = TextStyle(
        fontSize = 48.sp, fontFamily = DisplaySerif, fontWeight = FontWeight.Normal,
        letterSpacing = (-0.5).sp, lineHeight = 53.sp
    )

    // ─── Title (Source Serif 4 SemiBold) ─────────────────────────────────────
    fun titleXL(): TextStyle = TextStyle(
        fontSize = 34.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp, lineHeight = 37.sp
    )
    fun titleLG(): TextStyle = TextStyle(
        fontSize = 24.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        lineHeight = 29.sp
    )
    fun titleMD(): TextStyle = TextStyle(
        fontSize = 18.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp
    )

    // ─── Body (Source Serif 4 Regular) ───────────────────────────────────────
    fun bodyLG(): TextStyle = TextStyle(
        fontSize = 17.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        lineHeight = 26.sp  // 1.55
    )
    fun bodyMD(): TextStyle = TextStyle(
        fontSize = 15.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        lineHeight = 23.sp  // 1.55
    )

    // ─── Label (Source Serif 4 SemiBold, expanded tracking) ──────────────────
    fun labelMD(): TextStyle = TextStyle(
        fontSize = 12.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.6.sp, lineHeight = 19.sp
    )
    fun labelSM(): TextStyle = TextStyle(
        fontSize = 10.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp, lineHeight = 15.sp
    )

    // ─── Tabular (timers / numbers in cards) ─────────────────────────────────
    fun displayLGTabular(): TextStyle = TextStyle(
        fontSize = 64.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, lineHeight = 64.sp
    )
    fun displayLargeTabular(): TextStyle = TextStyle(
        fontSize = 72.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, lineHeight = 72.sp
    )
    fun displayXLTabular(): TextStyle = TextStyle(
        fontSize = 96.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, letterSpacing = (-2).sp, lineHeight = 96.sp
    )

    /** Emotional italic accent — used on greetings, sleeping captions, hero subtitles. */
    fun serifItalic(size: Int): TextStyle = TextStyle(
        fontSize = size.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Italic, lineHeight = (size * 1.4f).sp
    )
}
