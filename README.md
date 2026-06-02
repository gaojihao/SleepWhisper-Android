# SleepWhisper

一款面向婴幼儿照护者的睡眠陪伴 Android 应用：记录睡眠 / 喂养 / 尿布,基于月龄推荐下一个清醒窗口,并提供 12 条精心调校的白噪音预设与可选的哭声检测。

> 单 Activity + Jetpack Compose,Kotlin 100%,~10k 行 / 99 文件。

---

## 主要功能

- **睡眠记录**:小睡 / 夜觉 / 接觉三种类型,长按 CTA 防误触;醒来后弹出今日睡眠总览。
- **清醒窗口推荐**:基于 0–36 月龄查表 + 历史会话的纯函数推荐引擎,每 60 秒刷新。
- **白噪音播放器**:12 条预置音频(白噪/粉噪/棕噪/风扇/雨声/海浪/吹风机/吸尘器/心跳/胎内音/摇篮曲 ×2),Media3 ExoPlayer 驱动,1.5s 淡入、单曲循环、锁屏控制、AudioFocus、耳机拔出暂停。
- **哭声检测(可选)**:`AudioRecord` 16kHz 麦克风,RMS → dBFS,连续 ≥3s 超阈值触发并自动提升音量;省电模式下自动停。
- **趋势**:今日时间轴 + 近 7 天柱状图,长按事件可编辑。
- **智能提醒**:`AlarmManager` 精准闹钟调度睡眠 check-in 与下个推荐窗口,内置静默时段过滤,开机后自动恢复。
- **三态主题**:DAY / DARK / NIGHT 自动按系统外观 + 用户设置的夜间时段 + 当前时刻切换;真实时钟驱动天空色板。
- **PDF 日报导出**:通过 `FileProvider` 分享。
- **中英双语**:206 条字符串,内置 `values-zh-rCN`。

---

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 / JVM | Kotlin 2.0.21 / JVM 17 |
| 构建 | AGP 8.7.3,Gradle Version Catalog |
| UI | Jetpack Compose BOM 2024.12.01 + Material3 |
| DI | Hilt 2.52 |
| 持久化 | Room 2.6.1(version=2) + DataStore Preferences |
| 音频 | Media3 ExoPlayer 1.5.1 + MediaSession |
| 序列化 | kotlinx.serialization JSON |
| 异步 | Kotlin Coroutines + LiveData |
| 最低系统 | minSdk 30 / targetSdk 35 / compileSdk 35 |

---

## 构建与运行

环境要求:JDK 17、Android Studio Ladybug 或更新、Android SDK 35。

```bash
# 克隆后在仓库根目录
./gradlew :app:assembleDebug          # 打 debug APK
./gradlew :app:installDebug           # 安装到已连接设备
./gradlew test                        # 运行 JVM 单测
./gradlew :app:assembleRelease        # 打 release(启用 R8 + 资源压缩)
```

首次启动会进入 Onboarding(宝宝姓名 / 出生日 / 可选性别),随后进入主页 → 今日 → 设置 三 Tab 结构。

### 运行时权限

应用按需在运行时申请以下权限:

- `POST_NOTIFICATIONS` — 启动时申请,用于睡眠 check-in 与窗口提醒。
- `RECORD_AUDIO` — 在设置页开启"哭声检测"时校验申请。

其余清单权限(`FOREGROUND_SERVICE_*` / `SCHEDULE_EXACT_ALARM` / `WAKE_LOCK` / `RECEIVE_BOOT_COMPLETED` / `VIBRATE`)为安装时授予。

---

## 架构概览

```
features/  →  core/  →  model/
              ↑
        app/AppStateContainer  (全局门面 @Singleton)
```

- **单 Activity + Compose**:`MainActivity` 内 `RootRoute` 监听 `AppStateContainer.rootKey`,在 Onboarding / WelcomeRitual / Sleeping / Main(Home·Trends·Settings) 四种根态间切换。
- **AppStateContainer 是项目最重要的类**:聚合所有 repo/service,对 ViewModel 暴露 LiveData,封装所有写操作(`startSleep / endSleep / recordFeeding / recordDiaper / saveBaby / saveSettings / recomputeRecommendation` …)。**改业务逻辑通常从这里入手。**
- **ViewModel 是薄壳**:直接暴露多条 `LiveData`,不使用 `data class UiState` / `StateFlow`。
- **Repository 全部 `suspend`,不返回 Flow**;响应式靠 `AppStateContainer` 在写入后手动 reload。
- **DataStore 单 JSON 键**:整 `UserSettings` 序列化为一条记录,加字段安全。
- **设计系统 visualkit/**:统一 `SW*` 前缀,三态主题(DAY/DARK/NIGHT)× 真实时钟天空色板 × 极光呼吸背景 × 星空层。

详细见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) 与 [`docs/CODE_MAP.md`](docs/CODE_MAP.md)。

---

## 目录结构

```
SleepWhisper-android/
├── app/                              唯一模块
│   ├── build.gradle.kts              compileSdk 35 / minSdk 30 / JVM 17
│   ├── schemas/                      Room 历史 schema(version=2)
│   └── src/main/java/com/lizhi1026/sleepwhisper/
│       ├── app/                      Application / MainActivity / RootRoute / AppStateContainer / DI
│       ├── model/                    领域模型(纯 Kotlin,无 Android 依赖)
│       ├── core/                     基础设施
│       │   ├── persistence/          Repository + Room + DataStore
│       │   ├── audio/                ExoPlayer 播放引擎
│       │   ├── cry/                  哭声检测
│       │   ├── foreground/           前台 Service
│       │   ├── notifications/        AlarmManager 调度 + Boot 恢复
│       │   ├── recommendation/       推荐纯函数
│       │   ├── theme/                ThemeProvider
│       │   ├── visualkit/            设计系统 + components/
│       │   ├── haptics/ toast/ strings/ export/
│       └── features/                 业务屏幕
│           ├── home/  sleeping/  player/  trends/  settings/  onboarding/
├── docs/
│   ├── CODE_MAP.md                   文件 / 包索引,30 秒定位
│   └── ARCHITECTURE.md               分层、数据流、约定、改动指南
├── CLAUDE.md                         协作行为准则
├── gradle/libs.versions.toml         版本目录
└── settings.gradle.kts
```

---

## 测试

JVM 单测覆盖**纯算法 + 纯数据**:

- `RecommendationEngineTest` — 推荐输出
- `WakeWindowRuleTest` — 月龄查表
- `FadeRampTest` — 淡入淡出曲线
- `QuietHoursTest` — 跨午夜静默时段
- `TimeOfDayPaletteTest` / `SWColorTest` / `HeroBackdropControllerTest`
- `AudioPresetAuraTest` / `UserSettingsTest`
- `AppStateContainerMorphTest`

Android 框架代码(Service / Receiver / Compose)未覆盖,改动时需手动验证。

---

## 关键约定(改动前请阅读)

1. Repository 不返回 Flow,响应式靠 `AppStateContainer` 手动 reload。
2. `RoomRepository.safeWrite { … }` 吞 IO/conflict 异常,UI 反馈走 `ToastCenter.show(...)`。
3. 共享元素动画(Home `SleepCTA` ↔ Sleeping 中央按钮)由 `beginSleepMorph / endSleepMorph` 协议位驱动,改一端必同改另一端。
4. `AudioPlayerService` 是普通 Kotlin 单例;真正的 Android Service 是 `PlaybackForegroundService`。
5. Room 升 version 必须写 Migration 并更新 `app/schemas/`。
6. 加新偏好字段:在 `UserSettings` 加字段 + 默认值即可(DataStore 单 JSON 键自动兼容)。
7. 新加动画必须读 `LocalReduceMotion`,系统 `ANIMATOR_DURATION_SCALE == 0` 时静止。
8. 哭声检测入口必须先校验 `RECORD_AUDIO` 运行时权限;省电模式下自动停是有意为之,不要绕过。

更多见 [`docs/ARCHITECTURE.md` §10](docs/ARCHITECTURE.md)。

---

## License

未声明。
