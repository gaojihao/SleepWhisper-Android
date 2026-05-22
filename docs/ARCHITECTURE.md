# SleepWhisper Android — 应用架构

> 一份"足够 Claude Code 做出正确改动"的架构说明。  
> 配合 [`CODE_MAP.md`](./CODE_MAP.md) 阅读：地图告诉你**文件在哪**，本文告诉你**层与层之间怎么连**。

---

## 1. 一句话定位

**单 Activity + Compose** 的婴儿睡眠陪伴应用。技术栈 Hilt + Room(v2) + DataStore + Media3 ExoPlayer + Compose BOM。架构介于 MVVM 与"全局门面 + LiveData"之间——**所有跨页面状态都收敛到 `AppStateContainer`**。

---

## 2. 分层

```
                      ┌─────────────────────────────────────────┐
                      │  Compose UI (features/*)                │
                      │   Screens · sheets · visualkit/         │
                      └────────────────┬────────────────────────┘
                                       │ observes LiveData
                                       │ calls suspend actions
                      ┌────────────────▼────────────────────────┐
                      │  ViewModel (features/*ViewModel)        │
                      │   薄壳：暴露 LiveData，转发命令         │
                      └────────────────┬────────────────────────┘
                                       │ injects via @Inject
                      ┌────────────────▼────────────────────────┐
                      │  AppStateContainer  (@Singleton)        │
                      │  ★ 全局门面 / 跨页协调器                 │
                      │  - 聚合 baby / settings / sleep / 推荐   │
                      │  - 控制 rootKey 决定根路由              │
                      │  - 所有写操作的入口（startSleep…）       │
                      └─┬─────────┬──────────┬──────────┬───────┘
                        │         │          │          │
                  ┌─────▼──┐  ┌───▼────┐ ┌───▼────┐ ┌───▼─────────┐
                  │Repo    │  │Settings│ │Audio + │ │Notifications│
                  │(Room)  │  │Store   │ │Cry +FG │ │Scheduler    │
                  │5 张表  │  │Data-   │ │Media3  │ │AlarmManager │
                  │        │  │Store   │ │+ MIC   │ │             │
                  └────────┘  └────────┘ └────────┘ └─────────────┘
                        ▲                                ▲
                        │                                │
                  ┌─────┴────────────────────────────────┴─────┐
                  │  model/  纯 Kotlin domain（无 Android 依赖）│
                  └────────────────────────────────────────────┘
```

依赖规则：
- `features → core → model`，反向不允许。
- `core/recommendation`、`model/` 完全无 Android 依赖（保证 JVM 单测）。
- `AppStateContainer` 在 `app/` 里，不在 `core/`，因为它需要协调 feature 间状态。

---

## 3. 启动流程

```
SleepWhisperApp.onCreate()
   └─ NotificationChannels.ensure()  (3 个渠道)

MainActivity.onCreate()
   ├─ 注入 AppStateContainer + ThemeProvider
   ├─ 申请 POST_NOTIFICATIONS 运行时权限
   └─ setContent { RootRoute(app) }

RootRoute  (Compose)
   └─ 监听 AppStateContainer.rootKey: LiveData<RootKey>
        ├─ ONBOARDING       → OnboardingScreen
        ├─ WELCOME_RITUAL   → WelcomeRitualScreen
        ├─ SLEEPING         → SleepingScreen   (有进行中睡眠)
        └─ MAIN             → MainScaffold ── Tabs(Home / Trends / Settings)
```

`rootKey` 是 `MediatorLiveData`，由 `baby + ongoingSleep + 引导完成标志` 推导。**改导航逻辑 → 改 `AppStateContainer.rootKey` 的 source**，不要在 `RootRoute` 内塞条件。

---

## 4. AppStateContainer ★

**所有 ViewModel 都注入它**，这是项目最重要的类。

### 暴露的 LiveData
| 字段 | 含义 |
|---|---|
| `baby` | 当前 active 宝宝（DataStore 里 `activeBabyId` → DB 查） |
| `settings` | `UserSettings`（DataStore Flow → asLiveData） |
| `ongoingSleep` | `endAt IS NULL` 的睡眠会话 |
| `currentRecommendation` | 最近一次推荐 |
| `lastSleepSummary` | 醒来后展示给 `SleepSummaryOverlay` |
| `pendingEditTarget` | `EventEditTarget?`，驱动 `EventEditSheet` |
| `rootKey` | MediatorLiveData，决定根路由 |
| `audioPlayer.stateLive` / `currentPreset` | 转发 |
| `cryDetection.stateLive` | 转发 |
| `themeProvider.scheme` | 转发 |

### 关键命令式 API
- `saveBaby(baby)` / `saveSettings(s)`
- `startSleep(type)` / `endSleep()` — 写 DB + 触发 `NotificationScheduler.scheduleSleepCheckIn()` + 触发 `recomputeRecommendation()`
- `recordFeeding(...)` / `recordDiaper(...)`
- `requestEdit(target)` / `commitEdit(...)`
- `recomputeRecommendation()` — 调 `RecommendationEngine` 纯函数 + 持久化
- `beginSleepMorph() / endSleepMorph()` — 共享元素动画的协议位
- `refreshWakeWindow(force)` — Home 每 60s 调用

**改业务规则**：从 `AppStateContainer` 的对应方法入手。**改纯算法**（推荐窗口、清醒窗口）：去 `core/recommendation/` 或 `model/WakeWindowRule.kt`。

---

## 5. 数据持久化

### Room（结构化数据）
- DB：`sleepwhisper.db`，**version = 2**，`schemas/` 中已固化 1.json / 2.json。**改表结构 → 必须写 Migration 并升 version**。
- 5 张表：`baby / sleep_session / feeding_event / diaper_event / sleep_recommendation`，子表全部 `babyId` FK CASCADE。
- `Repository` 接口**全部 `suspend`，不返回 Flow**。响应式来自 `AppStateContainer` 在写入后手动刷新 LiveData。
- `RoomRepository.safeWrite { … }` 吞 conflict/IO 异常——**写入失败不会冒泡到 UI**。需要 UI 反馈时用 `ToastCenter.show(...)`。

### DataStore（用户偏好）
- 单文件 `sw_settings_v2`，**单键** `settings_json_v2`，整 `UserSettings` 序列化为 JSON。
- 优点：加新字段只需在 `UserSettings` 上加默认值，`ignoreUnknownKeys = true` + `encodeDefaults = true` 自动兼容。
- **不要拆成多个键**——会与现有读写逻辑冲突。

---

## 6. 音频与前台服务

```
        AppStateContainer.audioPlayer
                 │
        AudioPlayerService (单例 Kotlin 类，非 Service)
        ├─ ExoPlayer (1.5s 淡入 / REPEAT_ONE / WAKE_MODE_LOCAL)
        ├─ MediaSession  ────────────► 锁屏 / Auto / 快捷面板
        └─ AudioFocus + AudioBecomingNoisy

        AppStateContainer.cryDetection
                 │
        CryDetectionService (单例 Kotlin 类)
        └─ AudioRecord (16kHz Mono PCM_16BIT, 0.5s 缓冲)
            算法：RMS → dBFS → 连续 ≥3s 超阈值（默认 60dB）→ Triggered → 10s 冷却
            副作用：触发后回调 audioPlayer.boostVolumeOnCry()

        ┌─────────────────────────────────────────────────┐
        │ PlaybackForegroundService (LifecycleService)    │
        │  注入上面两个引擎 → 任一活跃则保活、双双空闲则 stopSelf │
        │  通知用 MediaStyleNotificationHelper 接入媒体中心 │
        └─────────────────────────────────────────────────┘
```

权限：`RECORD_AUDIO`（运行时申请）+ `FOREGROUND_SERVICE_MICROPHONE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK`。

**改播放行为** → `AudioPlayerService`；**改哭声阈值/算法** → `CryDetectionService` + `UserSettings.cryDetectionThresholdDb`；**改前台 Service 生命周期** → `PlaybackForegroundService`。

---

## 7. 通知与定时

```
NotificationScheduler.scheduleSleepCheckIn(sleepId, atMs)
        │
        ▼ AlarmManager.setExactAndAllowWhileIdle()
        │
        ▼ broadcast → AlarmReceiver
        │
        ▼ NotificationManager.notify( CH_SLEEP_CHECKIN | CH_REC_WINDOW )
```

- 两类提醒：`KIND_CHECKIN`（睡眠后 ~2h） / `KIND_REC_WINDOW`（下一推荐窗口起点）。
- **Quiet Hours**（默认 22:00–07:00，可配置）在调度时过滤；最小提前量 5min 保护。
- `BootReceiver` 在 `BOOT_COMPLETED / LOCKED_BOOT_COMPLETED / QUICKBOOT_POWERON / MY_PACKAGE_REPLACED` 时重新调度进行中睡眠的 check-in（通过 `Repository.ongoingSleep()`）。
- 三个渠道在 `NotificationChannels.ensure()` 中注册：`CH_PLAYBACK`(LOW) / `CH_SLEEP_CHECKIN`(HIGH) / `CH_REC_WINDOW`(DEFAULT)。

权限：`SCHEDULE_EXACT_ALARM` + `USE_EXACT_ALARM` + `RECEIVE_BOOT_COMPLETED` + `WAKE_LOCK` + `POST_NOTIFICATIONS`。

---

## 8. 设计系统（visualkit）

```
SWTheme(scheme = ThemeProvider.scheme.value) {
    CompositionLocalProvider(
        LocalSWScheme provides …,
        LocalReduceMotion provides …,
    ) {
        // SWColor.primary / SWGradient.aurora / SWFont.titleLg …
    }
}
```

- 三态主题：`DAY / DARK / NIGHT`（NIGHT 是夜间模式区间内的更深色态）。
- `ThemeProvider`（`@Singleton`）每 60s 重算 scheme，输入：`UserSettings.appearance(AUTO/LIGHT/DARK)` × `nightModeStartHour/End` × `Configuration.uiMode` × 当前时刻。`SettingsScreen` 的 "Force Night Preview" 直接调 `setForceNightPreview(true)`。
- `TimeOfDayPalette` 在背景层独立工作：按真实时钟在 6 个时间锚点之间插值产生 `SkyPalette`，给 `NightSkyCanvas` 上色（与上面三态主题正交）。
- `BreathingBackground` + `NightSkyCanvas` 通常嵌在屏幕最底层；`HeroBackdropController` 让 Player 选中预设后把 `AuraColors` 传染到全局背景。
- 动效合规：所有动画都通过 `LocalReduceMotion` 检查（系统 `ANIMATOR_DURATION_SCALE == 0` → 静止）。**新加动画必须读这个 Local**。

---

## 9. ViewModel 范式

项目里**没有 `data class UiState`**，每个 ViewModel 直接暴露多条 `LiveData`。常见模式：

```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(
    private val app: AppStateContainer
) : ViewModel() {
    val baby: LiveData<Baby?> = app.baby
    val settings: LiveData<UserSettings> = app.settings

    fun doThing() = viewModelScope.launch {
        app.recordFeeding(...)
    }
}
```

**新加 feature**：复用此模板，不要引入 `StateFlow / UiState` 让风格分裂。**真要引入** → 先在 `AppStateContainer` 加好数据源再说。

---

## 10. 关键约定与坑

1. **Repository 不返回 Flow**——查询都是 `suspend` 一次性。响应式靠 `AppStateContainer` 在写入后手动 reload。
2. **写入失败被 `safeWrite` 吞掉**——业务上若需告知用户失败，调 `ToastCenter.show(...)`；不要假设异常会冒泡。
3. **共享元素动画**：Home 的 `SleepCTA` ↔ Sleeping 中央按钮，靠 `MorphingHeroSurface` + `beginSleepMorph()/endSleepMorph()` 协议位驱动。改这两个屏幕之一时务必两边同改。
4. **Sleeping 屏强制 DARK**：内部 `SWTheme(scheme = SWScheme.DARK)`，与全局 ThemeProvider 解耦。
5. **AudioPlayerService 不是 Android Service**，是普通 Kotlin 单例。真正的 Service 是 `PlaybackForegroundService`。两者职责分离勿混淆。
6. **DataStore 单 JSON 键**：加字段安全；改字段名/语义不安全（旧 JSON 可能 fallback 到默认值）。
7. **Room version 升级**：必须写 Migration + 更新 `schemas/` 中的 JSON。
8. **`hasSeenHints / activeBabyId` 等"轻状态"** 都在 `UserSettings` 里——不要新建 SharedPreferences。
9. **触发哭声检测前必须确认 `RECORD_AUDIO` 权限**（`SettingsScreen` 已实现该校验，新入口也要做）。
10. **省电模式**会让 `CryDetectionService` 拒启动并自动停（监听 `ACTION_POWER_SAVE_MODE_CHANGED`）。这是有意为之，不要绕过。

---

## 11. 测试策略

JVM 单测覆盖**纯算法 + 纯数据**：
- `RecommendationEngineTest` — 推荐输出
- `WakeWindowRuleTest` — 月龄查表
- `FadeRampTest` — 音量淡入淡出曲线
- `QuietHoursTest` — 跨午夜判断
- `TimeOfDayPaletteTest` / `SWColorTest` / `HeroBackdropControllerTest`
- `AudioPresetAuraTest` / `UserSettingsTest`
- `AppStateContainerMorphTest` — 动画状态位转换

仪器测试目前只有脚手架。Android 框架代码（Service、Receiver、Compose）**未单测覆盖**，改动时格外小心或手动验证。

---

## 12. 如何快速完成常见改动

| 任务 | 入口 |
|---|---|
| 加睡眠相关业务规则 | `AppStateContainer` 的对应方法 |
| 调推荐算法 | `core/recommendation/RecommendationEngine.kt`（无 Android 依赖，单测先行） |
| 加新表 / 新字段 | `core/persistence/entity/` + `dao/` + `Mappers.kt` + 升级 `SleepWhisperDatabase.version` + 写 Migration |
| 加新偏好 | `model/UserSettings.kt` 加字段 + 默认值；`SettingsScreen` 加控件 |
| 加新音频预设 | `model/AudioPreset.kt` 的 `bundled` 列表 + `res/raw/` 放音频 |
| 改 UI 主题/色彩 | `core/visualkit/SWColor.kt` / `SWScheme.kt` |
| 加新屏幕 | `features/<name>/<Name>Screen.kt` + `<Name>ViewModel.kt`；在 `RootRoute` 或 `MainScaffold` 接入 |
| 加新通知 | 加 channel id 到 `NotificationChannels`；调度逻辑放 `NotificationScheduler`；接收逻辑放 `AlarmReceiver`（按 `kind` 分支） |
| 加触觉反馈 | `core/haptics/SWHaptics`，已有 6 种预设 |
| 触发全局 Toast | `app.toastCenter.show(R.string.xxx)` |
