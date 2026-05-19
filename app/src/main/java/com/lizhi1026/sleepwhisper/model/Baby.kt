package com.lizhi1026.sleepwhisper.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset

@Serializable
data class Baby(
    val id: String,
    val name: String,
    val gender: BabyGender,
    val dateOfBirth: Long, // epoch-millis UTC
    val avatarLocalPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    enum class BabyGender(val serializedName: String) {
        FEMALE("female"),
        MALE("male"),
        UNKNOWN("unknown")
    }

    enum class LifePhase {
        INFANT,
        TODDLER_PLUS
    }

    companion object {
        fun isValidName(name: String): Boolean =
            name.trim().isNotEmpty() && name.trim().length <= 20
    }

    /** 整月龄。 */
    fun ageInMonths(now: Long = System.currentTimeMillis()): Int {
        val birth = LocalDate.ofEpochDay(dateOfBirth / 86_400_000L)
        val today = LocalDate.ofEpochDay(now / 86_400_000L)
        return Period.between(birth, today).years * 12 + Period.between(birth, today).months
    }

    /** 与 iOS `currentPhase` 等价的函数式版本，依赖 [ageInMonths]。 */
    fun currentPhase(now: Long = System.currentTimeMillis()): LifePhase =
        if (ageInMonths(now) < 36) LifePhase.INFANT else LifePhase.TODDLER_PLUS
}
