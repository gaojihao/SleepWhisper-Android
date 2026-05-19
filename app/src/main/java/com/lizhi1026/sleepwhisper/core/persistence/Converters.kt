package com.lizhi1026.sleepwhisper.core.persistence

/**
 * No-op holder reserved for future cross-type Room conversions.
 *
 * All enum-driven columns are stored as plain String (serializedName),
 * which Room handles natively.  reasoningKeys is JSON-handled in the mapper,
 * so no @TypeConverter annotation is required anywhere at this stage.
 */
class Converters
