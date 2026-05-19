package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable

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
    val iconName: String
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
                iconName = "ic_preset_womb"
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
                iconName = "ic_preset_heartbeat"
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
                iconName = "ic_preset_dryer"
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
                iconName = "ic_preset_vacuum"
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
                iconName = "ic_preset_white"
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
                iconName = "ic_preset_brown"
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
                iconName = "ic_preset_pink"
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
                iconName = "ic_preset_rain"
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
                iconName = "ic_preset_ocean"
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
                iconName = "ic_preset_fan"
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
                iconName = "ic_preset_lullaby_1"
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
                iconName = "ic_preset_lullaby_2"
            )
        )

        /** 按月龄过滤推荐预设。 */
        fun recommended(forAgeMonths: Int): List<AudioPreset> =
            bundled.filter { it.recommendedAgeMinMonths <= forAgeMonths && it.recommendedAgeMaxMonths >= forAgeMonths }

        /** 通过 id 反查预设，找不到返回 null。 */
        fun byId(id: String): AudioPreset? = bundled.firstOrNull { it.id == id }
    }
}
