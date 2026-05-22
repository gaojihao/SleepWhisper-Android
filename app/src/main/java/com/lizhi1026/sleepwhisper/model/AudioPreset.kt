/**
 * 音频预设与氛围色彩实体模型。
 *
 * **职责**：
 * - [AuraColors]：定义一个音频预设的三层氛围色（高光色、主色调、深底色），
 *   用于驱动 AuroraBackdrop 渐变和音频波形颜色。
 * - [AudioPreset]：描述一个内置音频预设的完整元数据，包含名称键、分类、适用月龄范围、
 *   资源文件名及氛围色。
 *
 * **所在层**：model 层（领域模型），与 iOS `AudioPreset.swift` 字段一一对应。
 *
 * **与谁交互**：
 * - [AudioPlayerService]（core/audio）— 读取 `fileBundleName` 和 `loop` 等字段控制播放。
 * - [HeroBackdropController]（core/visualkit）— 读取 `auraColors.mid` 更新氛围背景色。
 * - [AppStateContainer]（app 层）— 通过 `audioPlayer` 间接使用预设。
 *
 * **关键约定**：
 * - 所有预设均为硬编码内置列表（[AudioPreset.bundled]），运行时不从网络或数据库加载。
 * - `@Transient` 标注的 Compose `Color` 字段不参与序列化，防止未来误将整个预设写入 DataStore。
 * - 使用 `AudioPreset.byId(id)` 反查预设；找不到时返回 null（调用方需处理缺失情况）。
 */
package com.lizhi1026.sleepwhisper.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 3-color aura for an audio preset:
 *  - [top]    brightest highlight; used as AudioWaveform color and orb core
 *  - [mid]    primary tone; the one pushed to HeroBackdropController.ambient
 *             so the AuroraBackdrop drifts toward this color across screens
 *  - [bottom] deepest "room" color; the orb's outer ring
 *
 * Marked @Transient because Compose `Color` is not @Serializable. The
 * bundled catalog is a hardcoded list and is never serialized at runtime,
 * but @Transient defends against future regressions if a feature tries
 * to write a whole AudioPreset to DataStore.
 */
@Serializable
data class AuraColors(
    @Transient val top: Color = Color(0xFFB8A4FF),
    @Transient val mid: Color = Color(0xFF4D3C7A),
    @Transient val bottom: Color = Color(0xFF0F0C1E)
) {
    companion object {
        /** WARNING: returned only if a preset forgot to populate auraColors —
         *  catch via AudioPresetAuraTest before merging. */
        val DEFAULT = AuraColors()
    }
}

@Serializable
data class AudioPreset(
    val id: String,
    val nameKey: String,
    val category: AudioCategory,
    val recommendedAgeMinMonths: Int,
    val recommendedAgeMaxMonths: Int,
    val fileBundleName: String, // res/raw 资源名（不带扩展名）
    val defaultDurationSeconds: Int = 1800,
    val loop: Boolean = true,
    val license: String = "CC0",
    val iconName: String,
    @Transient
    val auraColors: AuraColors = AuraColors.DEFAULT
) {
    enum class AudioCategory(val serializedName: String) {
        WOMB("womb"),
        WHITE_NOISE("white_noise"),
        BROWN_NOISE("brown_noise"),
        LULLABY("lullaby"),
        AMBIENT("ambient")
    }

    companion object {
        /** 与 iOS `AudioPreset.bundled` 字段逐一对应；iconName 已映射为 Android drawable 资源名。 */
        val bundled: List<AudioPreset> = listOf(
            AudioPreset(
                id = "preset_womb_001",
                nameKey = "audio.womb",
                category = AudioCategory.WOMB,
                recommendedAgeMinMonths = 0,
                recommendedAgeMaxMonths = 3,
                fileBundleName = "womb_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_womb",
                auraColors = AuraColors(top = Color(0xFFC46B8C), mid = Color(0xFF5B4880), bottom = Color(0xFF1A0F1E))
            ),
            AudioPreset(
                id = "preset_heartbeat_001",
                nameKey = "audio.heartbeat",
                category = AudioCategory.WOMB,
                recommendedAgeMinMonths = 0,
                recommendedAgeMaxMonths = 3,
                fileBundleName = "heartbeat_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_heartbeat",
                auraColors = AuraColors(top = Color(0xFFC44E5C), mid = Color(0xFF4E1A22), bottom = Color(0xFF1A0A0E))
            ),
            AudioPreset(
                id = "preset_dryer_001",
                nameKey = "audio.dryer",
                category = AudioCategory.WHITE_NOISE,
                recommendedAgeMinMonths = 0,
                recommendedAgeMaxMonths = 6,
                fileBundleName = "dryer_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_dryer",
                auraColors = AuraColors(top = Color(0xFFC99B5A), mid = Color(0xFF6B4A20), bottom = Color(0xFF1A1108))
            ),
            AudioPreset(
                id = "preset_vacuum_001",
                nameKey = "audio.vacuum",
                category = AudioCategory.WHITE_NOISE,
                recommendedAgeMinMonths = 0,
                recommendedAgeMaxMonths = 6,
                fileBundleName = "vacuum_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_vacuum",
                auraColors = AuraColors(top = Color(0xFF6E6A66), mid = Color(0xFF38362F), bottom = Color(0xFF14130F))
            ),
            AudioPreset(
                id = "preset_white_001",
                nameKey = "audio.white",
                category = AudioCategory.WHITE_NOISE,
                recommendedAgeMinMonths = 0,
                recommendedAgeMaxMonths = 12,
                fileBundleName = "white_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_white",
                auraColors = AuraColors(top = Color(0xFFFAFAFA), mid = Color(0xFFECE9E2), bottom = Color(0xFF1F222A))
            ),
            AudioPreset(
                id = "preset_brown_001",
                nameKey = "audio.brown",
                category = AudioCategory.BROWN_NOISE,
                recommendedAgeMinMonths = 3,
                recommendedAgeMaxMonths = 24,
                fileBundleName = "brown_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_brown",
                auraColors = AuraColors(top = Color(0xFF8A6850), mid = Color(0xFF3E2A1F), bottom = Color(0xFF1A100A))
            ),
            AudioPreset(
                id = "preset_pink_001",
                nameKey = "audio.pink",
                category = AudioCategory.BROWN_NOISE,
                recommendedAgeMinMonths = 3,
                recommendedAgeMaxMonths = 24,
                fileBundleName = "pink_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_pink",
                auraColors = AuraColors(top = Color(0xFFE6B6B8), mid = Color(0xFFB85F77), bottom = Color(0xFF1A0F12))
            ),
            AudioPreset(
                id = "preset_rain_001",
                nameKey = "audio.rain",
                category = AudioCategory.AMBIENT,
                recommendedAgeMinMonths = 6,
                recommendedAgeMaxMonths = 36,
                fileBundleName = "rain_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_rain",
                auraColors = AuraColors(top = Color(0xFFB7C3CC), mid = Color(0xFF4A5A6E), bottom = Color(0xFF0F1620))
            ),
            AudioPreset(
                id = "preset_ocean_001",
                nameKey = "audio.ocean",
                category = AudioCategory.AMBIENT,
                recommendedAgeMinMonths = 6,
                recommendedAgeMaxMonths = 36,
                fileBundleName = "ocean_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_ocean",
                auraColors = AuraColors(top = Color(0xFF2E7A86), mid = Color(0xFF1F4F73), bottom = Color(0xFF051A26))
            ),
            AudioPreset(
                id = "preset_fan_001",
                nameKey = "audio.fan",
                category = AudioCategory.WHITE_NOISE,
                recommendedAgeMinMonths = 6,
                recommendedAgeMaxMonths = 36,
                fileBundleName = "fan_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_fan",
                auraColors = AuraColors(top = Color(0xFFB5BFC8), mid = Color(0xFF4F5C68), bottom = Color(0xFF0E1418))
            ),
            AudioPreset(
                id = "preset_lullaby_001",
                nameKey = "audio.lullaby1",
                category = AudioCategory.LULLABY,
                recommendedAgeMinMonths = 12,
                recommendedAgeMaxMonths = 36,
                fileBundleName = "lullaby_001",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_lullaby_1",
                auraColors = AuraColors(top = Color(0xFFAD5CDC), mid = Color(0xFF6B3FA0), bottom = Color(0xFF0D0C1F))
            ),
            AudioPreset(
                id = "preset_lullaby_002",
                nameKey = "audio.lullaby2",
                category = AudioCategory.LULLABY,
                recommendedAgeMinMonths = 12,
                recommendedAgeMaxMonths = 36,
                fileBundleName = "lullaby_002",
                defaultDurationSeconds = 1800,
                loop = true,
                license = "CC0",
                iconName = "ic_preset_lullaby_2",
                auraColors = AuraColors(top = Color(0xFFFFB088), mid = Color(0xFF8C4A26), bottom = Color(0xFF1A0E08))
            )
        )

        /** 按月龄过滤推荐预设。 */
        fun recommended(forAgeMonths: Int): List<AudioPreset> =
            bundled.filter { it.recommendedAgeMinMonths <= forAgeMonths && it.recommendedAgeMaxMonths >= forAgeMonths }

        /** 通过 id 反查预设，找不到返回 null。 */
        fun byId(id: String): AudioPreset? = bundled.firstOrNull { it.id == id }
    }
}
