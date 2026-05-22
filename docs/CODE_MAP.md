# SleepWhisper Android — Code Map

> 目的：让 Claude Code 在 30 秒内定位到要改的文件。  
> 包根：`com.lizhi1026.sleepwhisper`（路径 `app/src/main/java/com/lizhi1026/sleepwhisper/`）。  
> 全部 Kotlin，~10k 行 / 99 文件，Jetpack Compose + Hilt + Room + Media3。

---

## 0. 顶层目录

```
SleepWhisper-android/
├── app/                          ← 唯一模块
│   ├── build.gradle.kts          应用级构建脚本（compileSdk 35 / minSdk 30 / JVM 17）
│   ├── proguard-rules.pro
│   ├── schemas/.../1.json,2.json Room 历史 schema（version=2）
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/lizhi1026/sleepwhisper/  ← 代码
│       │   └── res/              字符串(206 条) + values-zh-rCN + raw 白噪音 12 条
│       ├── test/                 JVM 单测
│       └── androidTest/          仪器测试（仅脚手架）
├── gradle/libs.versions.toml     版本目录（AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / Hilt 2.52 / Room 2.6.1 / Media3 1.5.1）
├── settings.gradle.kts           单模块 `:app`
└── docs/                         ← 本目录
```

---

## 1. 包结构总览

```
com.lizhi1026.sleepwhisper
├── app/          应用入口、根路由、全局状态、Hilt 模块
├── model/        领域模型（@Serializable Kotlin data class / enum）
├── core/         基础设施（无 feature 依赖，可被任意 feature 复用）
│   ├── persistence/   Repository + Room + DataStore
│   ├── audio/         ExoPlayer 播放引擎
│   ├── cry/           麦克风哭声检测
│   ├── foreground/    前台 Service 生命周期
│   ├── notifications/ AlarmManager 调度 + Channels + Boot 恢复
│   ├── recommendation/ 睡眠窗口推荐纯函数
│   ├── theme/         ThemeProvider（DAY/DARK/NIGHT 切换）
│   ├── visualkit/     设计系统（SW* 命名前缀）+ components/
│   ├── haptics/       触觉反馈
│   ├── toast/         全局 Toast 总线（LiveData）
│   ├── strings/       枚举→StringRes 映射
│   └── export/        PDF 报告导出
└── features/     业务屏幕（每个 feature 一个 Compose Screen + ViewModel + sheets）
    ├── home/          主页（问候、清醒窗口、睡眠 CTA、快捷动作）
    ├── sleeping/      沉浸式睡眠中页面
    ├── player/        白噪音播放器（独立 Tab + 嵌入 Sheet 两种形态）
    ├── trends/        今日时间轴 + 近 7 天柱状图
    ├── settings/      用户偏好
    └── onboarding/    单页宝宝信息表单 + Welcome Ritual
```

依赖方向：**`features → core → model`**，`app` 在最外层组装。`core` 与 `model` 永远不依赖 `features`。

---

## 2. app/ — 应用入口与全局状态

| 文件 | 职责 |
|---|---|
| `app/SleepWhisperApp.kt` | `@HiltAndroidApp` Application；`onCreate` 注册通知渠道 |
| `app/MainActivity.kt` | 唯一 Activity；申请 POST_NOTIFICATIONS；`setContent { RootRoute(app) }`；`onConfigurationChanged` → `themeProvider.onSystemConfigurationChanged()` |
| `app/RootRoute.kt` | 根 Compose；按 `AppStateContainer.rootKey` 用 `AnimatedContent` 切换：Onboarding → WelcomeRitual → Sleeping → MainScaffold（Home/Trends/Settings 三 Tab）；叠加 `SleepSummaryOverlay` / `EventEditSheet` / `ToastOverlay` |
| `app/AppStateContainer.kt` | **★ 全局门面**（`@Singleton`）。聚合所有 repo/service，对 ViewModel 暴露 LiveData，封装命令式动作（`startSleep / endSleep / recordFeeding / recordDiaper / recomputeRecommendation / saveBaby / saveSettings / requestEdit / beginSleepMorph / endSleepMorph`）。**改业务逻辑通常从这里入手。** |
| `app/di/DataModule.kt` | Hilt 两个 `@Module @InstallIn(SingletonComponent)`：`DatabaseModule` 提供 `SleepWhisperDatabase` + 5 个 DAO；`RepositoryModule` 把 `RoomRepository` 绑到 `Repository` 接口 |

---

## 3. model/ — 领域模型

> 全部纯 Kotlin（`@Serializable`），无 Android 依赖。在 Room 中通过 `mapper/Mappers.kt` 转 Entity。

| 文件 | 说明 |
|---|---|
| `Baby.kt` | `Baby(id, name, gender, dateOfBirth, …)`；`BabyGender`、`LifePhase`（<36 月=INFANT）；衍生：`ageInMonths`、`ageParts`、`currentPhase` |
| `SleepSession.kt` | `SleepType(NAP/NIGHT/CONTACT_NAP)`、`SleepQuality`；`endAt==null` 即"进行中" |
| `FeedingEvent.kt` | `FeedingMethod(BREAST_LEFT/RIGHT/BOTTLE/SOLID)` + 同文件内 `DiaperEvent` + `DiaperType(WET/DIRTY/MIXED/DRY)` |
| `AudioPreset.kt` | `AudioCategory` 5 类 + 静态 `bundled` 12 条预设 + `AuraColors(top/mid/bottom)`（驱动播放背景） |
| `UserSettings.kt` | 偏好：`AppearanceMode(AUTO/LIGHT/DARK)`、`defaultTimerMinutes`、`fadeAfterSleepMinutes`、`hapticFeedback`、`cryDetectionEnabled` + `cryDetectionThresholdDb`、`nightModeStartHour/EndHour`、`activeBabyId`；方法 `isInNightTime()` 支持跨午夜 |
| `SleepRecommendation.kt` | `nextWindowStart/End`、`confidence`、`reasoningKeys`、`RecommendationOutcome`；`isActive()`、`hasPassed()` |
| `SleepSummary.kt` | 醒来后弹窗用：`justFinishedDurationSec / todayTotalSec / diffVsYesterdayMin / trendIsPositive` |
| `WakeWindowRule.kt` | 0–36 月 9 档清醒窗口查询表（`rule(ageMonths)`） |
| `EventEditTarget.kt` | `sealed`：`Feeding(FeedingEvent) / Diaper(DiaperEvent)`；驱动 `EventEditSheet` |

---

## 4. core/persistence/ — 数据层

| 文件 | 说明 |
|---|---|
| `Repository.kt` | **抽象契约**。全 `suspend fun`，返回普通类型（**不返回 Flow**）；含 `loadBaby/saveBaby/appendSleep/updateSleep/ongoingSleep/sleepSessions/appendFeeding/updateFeeding/deleteFeeding/appendDiaper/updateDiaper/deleteDiaper/appendRecommendation/latestRecommendation` |
| `RoomRepository.kt` | `@Singleton` 实现；私有 `safeWrite { … }` 吞 conflict/IO 异常（防写入失败破坏 UI 状态） |
| `SleepWhisperDatabase.kt` | `@Database(version=2)`，5 张表：`baby / sleep_session / feeding_event / diaper_event / sleep_recommendation`；DB 文件 `sleepwhisper.db` |
| `SettingsStore.kt` | DataStore Preferences；**单键** `settings_json_v2`，整个 `UserSettings` 序列化为 JSON；暴露 `settings: Flow<UserSettings>` + `update {…}` |
| `Converters.kt` | 当前空（枚举存 String，列表通过 mapper 转 JSON） |
| `dao/BabyDao.kt` | `loadLatest`、`upsert` |
| `dao/SleepDao.kt` | `insert / update / ongoing(endAt IS NULL) / since(from)` |
| `dao/FeedingDao.kt` | `insert / update / deleteById / since` |
| `dao/DiaperDao.kt` | `insert / update / deleteById / since` |
| `dao/RecommendationDao.kt` | 唯一索引 `babyId` 强制每宝宝一行；`insert(REPLACE) / deleteForBaby / latest` |
| `entity/*Entity.kt` | 5 张表 schema；子表 `babyId` 全部 FK CASCADE + 索引 `(babyId, ts)` |
| `mapper/Mappers.kt` | Entity ↔ Domain 双向 `toEntity / toDomain` 扩展函数；枚举未知值容错 fallback |

---

## 5. core/audio + core/cry + core/foreground + core/notifications

| 文件 | 说明 |
|---|---|
| `audio/AudioPlayerService.kt` | **单例 ExoPlayer 引擎**（不是 Android Service，是 Kotlin 类）。暴露 `stateLive: LiveData<PlayerState>` + `mediaSession`。1.5s 淡入、`REPEAT_MODE_ONE`、`WAKE_MODE_LOCAL`、AudioFocus、`handleAudioBecomingNoisy`。`boostVolumeOnCry()` 由哭声检测回调触发 |
| `audio/PlayerState.kt` | `sealed`：`Idle / Loading / Playing(presetId, endsAt) / Paused / FadingOut / Stopped / Interrupted / Error` |
| `cry/CryDetectionService.kt` | **单例**麦克风监听（`AudioRecord` 16kHz Mono PCM_16BIT，0.5s 缓冲）。算法：RMS → dBFS，连续 ≥3s 超 60dB（可调）触发，10s 冷却。监听 `ACTION_POWER_SAVE_MODE_CHANGED` 自动停 |
| `cry/CryDetectionState.kt` | `sealed`：`Disabled / Listening / Triggered / Cooldown(untilMs)` |
| `foreground/PlaybackForegroundService.kt` | **真正的 Service**（`LifecycleService`）。注入上面两个引擎，发布 `MediaStyle` 通知；任一空闲即 `stopSelf()` |
| `notifications/AlarmReceiver.kt` | `AlarmManager` 唤醒入口，按 `kind` 选 channel 发通知 |
| `notifications/BootReceiver.kt` | 监听 `BOOT_COMPLETED` 等，重新调度进行中睡眠的 `scheduleSleepCheckIn` |
| `notifications/NotificationChannels.kt` | 3 个渠道：`CH_PLAYBACK`(LOW) / `CH_SLEEP_CHECKIN`(HIGH) / `CH_REC_WINDOW`(DEFAULT) |
| `notifications/NotificationScheduler.kt` | `setExactAndAllowWhileIdle` 调度 `KIND_CHECKIN`（睡眠后 ~2h）和 `KIND_REC_WINDOW`（下一睡眠窗口起点），含 Quiet Hours 过滤 |

---

## 6. core/ — 其他基础设施

| 文件 | 说明 |
|---|---|
| `recommendation/RecommendationEngine.kt` | **纯函数 `object`**。输入 `Baby + List<SleepSession>` → `SleepRecommendation`。**无 Android 依赖**，是单测重点 |
| `export/PdfExporter.kt` | `android.graphics.pdf.PdfDocument` 生成 A4 日报到 `cacheDir`，通过 `FileProvider` 分享 |
| `haptics/SWHaptics.kt` | API 31+ 用 `VibratorManager`；提供 `light/medium/heavy/success/warning/error` |
| `toast/ToastCenter.kt` | 全局 Toast 事件总线（`LiveData<ToastItem?>`）；`show()` 发出，`consume()` 清除 |
| `strings/EnumDisplayKeys.kt` | 枚举 → `@StringRes`：`BabyGender / FeedingMethod / DiaperType / SleepType / AppearanceMode`；另含 `audioPresetNameKey`、`greetingForHour` |
| `theme/ThemeProvider.kt` | `@Singleton`。按 `UserSettings.appearance + nightModeStartHour/End + 系统 uiMode + 当前时刻` 决定 `SWScheme(DAY/DARK/NIGHT)`，每 60s 自动重算。支持 `setForceNightPreview(true)` |

---

## 7. core/visualkit/ — 设计系统

**统一前缀 `SW*`**。风格 = 极光（Aurora ribbon 漂移） + 星空（80 颗闪烁星） + 呼吸感（4.2s 周期光晕）。

### Tokens & 主题

| 文件 | 说明 |
|---|---|
| `SWScheme.kt` | 枚举 `DAY/DARK/NIGHT` + `LocalSWScheme` CompositionLocal + 根入口 `SWTheme {…}` |
| `SWColor.kt` | 按 scheme 返回具名色（`primary/accent/accentSecondary/surface*/text*/border`）。主轴：dusk orange `#FF8A5E` × starlight purple `#9B85FF` |
| `SWGradient.kt` | `Brush` token：`surface / primary / accent / aurora` |
| `SWFont.kt` | Source Serif 4 + Noto Serif CJK；`display* / title* / body* / label*` + tabular 变体 |
| `SWTokens.kt` | `SWSpacing / SWRadius / SWMotion / SWSpring` 常量表 |
| `SWShadow.kt` | `soft / medium / strong / glow` |
| `TimeOfDayPalette.kt` | **真实时钟驱动天空色**：6 锚点线性插值 → `SkyPalette(bottom, top, grainDensity)` |
| `HeroBackdropController.kt` | 持有"ambient 色"全局状态；`blendAmbient()` |
| `Modifiers.kt` | `Modifier.floatingY(amplitude, duration)`，reduce-motion 安全 |
| `ReduceMotion.kt` | 读 `ANIMATOR_DURATION_SCALE` → `LocalReduceMotion` |

### components/ — 可复用 Composable

> **跨 feature 高复用**：`GlassCard`、`SoftButton`、`SectionLabel`、`Hairline`、`DragHandle`、`ToastOverlay`、`EmptyStateCard`、`RollingNumber`、`SerifMetricRow`、`SWSegmentedPicker`。

| 基础 | 装饰/动效 |
|---|---|
| `GlassCard` 毛玻璃卡 | `BreathingBackground` 极光漂移 |
| `SoftButton` 主 CTA | `NightSkyCanvas` 星空 |
| `SectionLabel` 节标题 | `AuroraTextHero` 扫光标题 |
| `Hairline` 0.5dp 分隔 | `AudioWaveform` 仿 FFT 波形 |
| `DragHandle` sheet 拖把手 | `AuraParticles` Player 漂浮光点 |
| `EmptyStateCard` 空状态 | `PulseRing` 三环脉冲 |
| `RollingNumber` 滚动数字 | `LiquidProgressRing` 液面进度环 |
| `SerifMetricRow` 标签·值行 | `MorphingHeroSurface` 共享元素 Hero |
| `SWSegmentedPicker` 分段选择 | `PresetAuraOrb` 预设气场球 |
| `ChevronTrail` 三点指示 | `InnerHighlight` 内描边渐变 |
| `ToastOverlay` 全局 Toast | |

---

## 8. features/ — 业务屏幕

> 每个 feature 是 `Screen` Composable + `ViewModel` + 若干 sheet。**ViewModel 不持有 UiState data class**，直接暴露多条 `LiveData`，由 `AppStateContainer` 注入数据源。

### features/home/
- `HomeScreen.kt`（接 `SharedTransitionScope`，与 SleepingScreen 做共享元素动画）
- `HomeViewModel.kt`：`baby / cachedWakeWindow(剩余, 总分钟) / playerState / currentPreset / hasSeenHints`；init 每 60s `refreshWakeWindow()`
- 子组件：`GreetingSection / WakeWindowCard / SleepCTA(长按→SleepTypePicker) / NowPlayingCard / QuickActionsGrid / OnboardingHintsCard / BottleAmountSheet / SleepTypePicker / QuickActionTile / PlayerSheet`

### features/sleeping/
- `SleepingScreen.kt`（强制 `SWScheme.DARK`；中央长按按钮才结束睡眠防误触）
- `SleepingViewModel.kt`：`ongoingSleep / playerState / baby`（**状态机完全委托给 `AppStateContainer.endSleep()`**）
- 子组件：`SleepSummaryOverlay`（醒后弹卡）

### features/player/
- `PlayerScreen.kt`（参数 `embedded: Boolean` — 独立 Tab 或嵌在 Sheet 内）
- `PlayerSheet.kt`（嵌入入口）
- `PlayerViewModel.kt`：`playerState / currentPreset / allPresets(=AudioPreset.bundled) / baby / settings`
- `AmbientSync.kt`：监听当前 preset 的 `AuraColors`，写入 `HeroBackdropController`

### features/trends/
- `TrendsScreen.kt`
- `TrendsViewModel.kt`：`weeklyBuckets(过去7日, List<Pair<标签D1..D7, 总秒数>>) / todaySleeps / todayFeedings / recentEvents / hasAnyData(MediatorLiveData) / currentRecommendation`
- `TodayTimeline.kt` / `RecentEventsList.kt`（长按 → `app.requestEdit()`）

### features/settings/
- `SettingsScreen.kt`（五区：Baby / Playback / Cry Detection / Appearance / About）
- `SettingsViewModel.kt`：`baby / settings / forceNightPreview`
- 开启哭声检测时校验 `RECORD_AUDIO` 运行时权限

### features/onboarding/
- `OnboardingScreen.kt`（单页表单：名字 / 系统 DatePickerDialog 出生日 / 性别可选）
- `WelcomeRitualScreen.kt`（独立欢迎仪式动画页）
- `OnboardingViewModel.kt`：`name / dobMs / gender / isValid(MediatorLiveData)`；`submit()` → `app.saveBaby` + `app.saveSettings(activeBabyId=…)`

---

## 9. 资源 (`app/src/main/res/`)

- `values/strings.xml` 206 条，含 `values-zh-rCN/` 中文
- `raw/` 12 条 `.m4a`：`white_001 / pink_001 / brown_001 / fan_001 / rain_001 / ocean_001 / dryer_001 / vacuum_001 / heartbeat_001 / womb_001 / lullaby_001 / lullaby_002`
- `font/` 3 个（Source Serif 4 / Noto Serif CJK）
- `xml/` `backup_rules.xml / data_extraction_rules.xml / file_provider_paths.xml`
- `drawable/` 26 个

---

## 10. 测试

`app/src/test/`（JVM 单测）：
- `core/recommendation/RecommendationEngineTest`
- `core/audio/FadeRampTest`
- `core/visualkit/HeroBackdropControllerTest / SWColorTest / TimeOfDayPaletteTest`
- `core/notifications/QuietHoursTest`
- `app/AppStateContainerMorphTest`
- `model/WakeWindowRuleTest / AudioPresetAuraTest / UserSettingsTest`

`app/src/androidTest/`：仅脚手架 `ExampleInstrumentedTest`。

---

## 11. AndroidManifest 摘要

- **Activity**：`MainActivity`（LAUNCHER）
- **Service**：`PlaybackForegroundService`（`foregroundServiceType=microphone|mediaPlayback`）
- **Receiver**：`AlarmReceiver`、`BootReceiver`（`BOOT_COMPLETED / LOCKED_BOOT_COMPLETED / QUICKBOOT_POWERON / MY_PACKAGE_REPLACED`）
- **Provider**：`FileProvider`
- **权限**：`RECORD_AUDIO` · `POST_NOTIFICATIONS` · `FOREGROUND_SERVICE` · `FOREGROUND_SERVICE_MICROPHONE` · `FOREGROUND_SERVICE_MEDIA_PLAYBACK` · `VIBRATE` · `SCHEDULE_EXACT_ALARM` · `USE_EXACT_ALARM` · `WAKE_LOCK` · `RECEIVE_BOOT_COMPLETED`
