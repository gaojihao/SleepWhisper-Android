package com.lizhi1026.sleepwhisper.core.persistence

/**
 * Room @TypeConverter 占位类，为未来跨类型转换预留扩展点，属于 data / persistence 层。
 *
 * 所在层：core.persistence，通过 `@TypeConverters(Converters::class)` 注册到数据库（如需时）。
 *
 * 关键约定：
 * - 所有枚举列均以 `serializedName`（字符串）形式直接存储，Room 原生支持，**无需 TypeConverter**。
 * - `reasoningKeysJson`（`List<String>`）通过 Mapper 层手动序列化为 JSON 字符串，
 *   也**无需 TypeConverter**。
 * - 当前此类为空实现（no-op）；若日后引入自定义类型（如 `LocalDate`、枚举集合等），
 *   请在此类中添加带 `@TypeConverter` 注解的转换方法。
 */
class Converters
