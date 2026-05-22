package com.lizhi1026.sleepwhisper.core.visualkit

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GoogleFontVariant
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.lizhi1026.sleepwhisper.R

/**
 * visualkit 字体 token 层（Aurora 重设计 Phase 0）。
 *
 * 西文字体：Source Serif 4（本地内嵌，字重 Regular / SemiBold / Bold）。
 * 中文字体：Noto Serif CJK SC（通过 Google Play Services 按需下载）。
 *
 * 层级约定：
 *  - `display*` — 超大展示标题，SemiBold，负字间距
 *  - `title*`   — 页面/卡片标题，SemiBold
 *  - `body*`    — 正文内容，Regular，行高 1.55
 *  - `label*`   — 标签/徽章，SemiBold，正字间距（大写跟踪）
 *  - `*Tabular` — 计时器/数字展示，启用 OpenType `tnum` 等宽数字
 *
 * 需要中文渲染时，用 [cjkAware] 包裹任意 TextStyle 即可获得 CJK 回退。
 *
 * 调用方：SWTheme、HomeScreen、SleepingScreen、PlayerScreen、TrendsScreen。
 */
object SWFont {

    /** OpenType 等宽数字特性键，防止计时数字抖动偏移。 */
    private const val TNUM = "tnum"

    // ── Google Fonts provider & CJK face ─────────────────────────────────────

    /** Google Fonts 字体提供者，通过 GMS 按需下载 CJK 字体。 */
    private val GoogleProvider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    /** Noto Serif SC 字体描述符，从 Google Fonts 拉取简体中文衬线字体。 */
    private val NotoSerifCjkSc = GoogleFont("Noto Serif SC")

    /** CJK Regular 下载字重——对应正文/标题的中文部分。 */
    private val CjkRegularFont  = GoogleFontVariant(googleFont = NotoSerifCjkSc, fontProvider = GoogleProvider, weight = FontWeight.Normal)
    /** CJK SemiBold 下载字重——对应标题/强调的中文部分。 */
    private val CjkSemiBoldFont = GoogleFontVariant(googleFont = NotoSerifCjkSc, fontProvider = GoogleProvider, weight = FontWeight.SemiBold)

    // These plain-Font arrays are the single source of truth for every public
    // FontFamily in this object and for the CJK merged families in cjkAware.
    /** Source Serif 4 本地字体数组，是所有公开 FontFamily 的唯一来源。 */
    private val BodyFonts: Array<Font> = arrayOf(
        Font(R.font.source_serif_4_regular,  FontWeight.Normal),
        Font(R.font.source_serif_4_semibold, FontWeight.SemiBold),
        Font(R.font.source_serif_4_bold,     FontWeight.Bold)
    )
    /** CJK 下载字体数组，合并进 cjkAware 回退链。 */
    private val CjkFonts: Array<Font> = arrayOf(CjkRegularFont, CjkSemiBoldFont)

    // ── Public stable family (used directly by token functions) ───────────────

    /** 西文主字族 Source Serif 4，内部 token 函数直接引用。 */
    private val BodySerif = FontFamily(*BodyFonts)

    /**
     * 单独暴露的 CJK 回退字族（Noto Serif SC）。
     * 通过 Google Play Services 一次性加载，全局共享。
     */
    val CjkFallback: FontFamily = FontFamily(*CjkFonts)

    /**
     * 将任意 [TextStyle] 包装为 CJK 感知版本：西文字形优先走 Source Serif 4，
     * 西文字库缺失的汉字自动回退到 Noto Serif CJK SC。
     *
     * 适用场景：用户昵称、预设名称、本地化文案等可能含中文的文本。
     * Compose 字体解析按列表顺序查找，西文在前（本地，更快），CJK 在后（网络）。
     */
    fun cjkAware(style: TextStyle): TextStyle =
        style.copy(fontFamily = buildCjkFamily(style.fontFamily))

    /**
     * 构造 CJK 合并字族：将 [base] 的字体列表与 [CjkFonts] 拼接。
     * [FontListFontFamily] 才能展开取子列表；其他类型直接退化为纯 CJK 字族。
     */
    private fun buildCjkFamily(base: FontFamily?): FontFamily = when {
        base === null || base === BodySerif ->
            FontFamily(*BodyFonts, *CjkFonts)

        base is FontListFontFamily && base.fonts.isNotEmpty() ->
            FontFamily(*base.fonts.toTypedArray(), *CjkFonts)

        else ->
            FontFamily(*CjkFonts)
    }

    // ─── Display (Source Serif 4 SemiBold) ───────────────────────────────────

    /** 超大展示标题——96sp，负字间距 -2sp，用于欢迎屏/Hero 区域大数字。 */
    fun displayXL(): TextStyle = TextStyle(
        fontSize = 96.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-2).sp, lineHeight = 96.sp
    )
    /** 大展示标题——64sp，负字间距 -1sp，用于睡眠时长主数字。 */
    fun displayLG(): TextStyle = TextStyle(
        fontSize = 64.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1).sp, lineHeight = 67.sp
    )
    /** 中展示标题——48sp，用于统计卡片核心数值。 */
    fun displayMD(): TextStyle = TextStyle(
        fontSize = 48.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp, lineHeight = 53.sp
    )

    // ─── Title (Source Serif 4 SemiBold) ─────────────────────────────────────

    /** 超大标题——34sp，用于屏幕主标题（如首页问候语）。 */
    fun titleXL(): TextStyle = TextStyle(
        fontSize = 34.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.5).sp, lineHeight = 37.sp
    )
    /** 大标题——24sp，用于卡片标题、Section Header。 */
    fun titleLG(): TextStyle = TextStyle(
        fontSize = 24.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        lineHeight = 29.sp
    )
    /** 中标题——18sp，用于列表项标题、Modal 标题栏。 */
    fun titleMD(): TextStyle = TextStyle(
        fontSize = 18.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        lineHeight = 23.sp
    )

    // ─── Body (Source Serif 4 Regular) ───────────────────────────────────────

    /** 正文大——17sp，行高 1.55，用于主要阅读内容、声音描述文案。 */
    fun bodyLG(): TextStyle = TextStyle(
        fontSize = 17.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        lineHeight = 26.sp  // 1.55
    )
    /** 正文中——15sp，行高 1.55，用于次要说明、辅助描述。 */
    fun bodyMD(): TextStyle = TextStyle(
        fontSize = 15.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        lineHeight = 23.sp  // 1.55
    )

    // ─── Label (Source Serif 4 SemiBold, expanded tracking) ──────────────────

    /** 标签中——12sp，字间距 +1.6sp，用于标签芯片、分类徽章。 */
    fun labelMD(): TextStyle = TextStyle(
        fontSize = 12.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.6.sp, lineHeight = 19.sp
    )
    /** 标签小——10sp，字间距 +1.4sp，用于极小提示文字、时间戳角标。 */
    fun labelSM(): TextStyle = TextStyle(
        fontSize = 10.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp, lineHeight = 15.sp
    )

    // ─── Tabular (timers / numbers in cards) ─────────────────────────────────

    /** 64sp 等宽数字——计时器/睡眠时长展示，OpenType tnum 防数字抖动。 */
    fun displayLGTabular(): TextStyle = TextStyle(
        fontSize = 64.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, lineHeight = 64.sp
    )
    /** 72sp 等宽数字——大型数字仪表盘使用。 */
    fun displayLargeTabular(): TextStyle = TextStyle(
        fontSize = 72.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, lineHeight = 72.sp
    )
    /** 96sp 等宽数字——超大闹钟/计时器全屏展示。 */
    fun displayXLTabular(): TextStyle = TextStyle(
        fontSize = 96.sp, fontFamily = BodySerif, fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = TNUM, letterSpacing = (-2).sp, lineHeight = 96.sp
    )

    /**
     * 自定义尺寸衬线装饰字——用于问候语、睡眠字幕、Hero 副标题。
     * 行高固定为字号 × 1.4，保证多行时节奏自然。
     */
    fun serif(size: Int): TextStyle = TextStyle(
        fontSize = size.sp, fontFamily = BodySerif, fontWeight = FontWeight.Normal,
        lineHeight = (size * 1.4f).sp
    )
}
