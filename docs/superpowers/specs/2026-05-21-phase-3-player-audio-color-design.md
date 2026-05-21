# Phase 3 — Player 音色同步 设计文档

**日期：** 2026-05-21
**状态：** 已批准（待最终用户审阅）
**父规约：** [Aurora Redesign Master](2026-05-20-aurora-redesign-master-design.md) §6.3

Aurora 重设计的 Phase 3。实现 **Hero #3**：在 Player 屏选择一个白噪音预设时，整个 app 的环境色（AuroraBackdrop 的 ambient）跟着这个预设的"光环色"漂移，并在退出 Player 后继续在 Home / Sleeping 持久存在 —— 选音乐就是"换了一间房"。

---

## 1. 范围

### 包含

- 给 `AudioPreset` 模型加 `auraColors: List<Color>` 字段（12 个预设，每个 3 色：top → mid → bottom）
- 新组件 **`PresetAuraOrb`** —— 64dp 玻璃光环球，内部漂浮粒子（基于该预设 mid-aura 色）
- Player 屏重设计：
  - Header 升级 serif（已 Phase 0 落地，本 phase 仅微调）
  - `PresetRow` 重做：左侧是 `PresetAuraOrb` 而不是单色 icon 圆；活跃行的 orb 发光更强；相邻 ±2 行的 surface tint 混入 12% 选中行的 mid-aura（"反光"）
  - 24 个浮动粒子（`AuraParticles`）覆盖在 Player 屏上层，颜色来自当前 ambient
- 当用户在 Player 屏选定一个预设时，将其 mid-aura 色推给 `LocalHeroBackdropController.setAmbient(...)`，AuroraBackdrop（Phase 0 已支持 ambient 漂移）自动在 600ms 内将极光带颜色向新 ambient 偏移 20%
- ambient **跨屏持久化**：退出 Player 抽屉后，Home / Trends / Settings 上的 AuroraBackdrop 仍带这个色调；停止播放或切换预设时更新；播放器自然结束（FadingOut 完成）时清空回 null
- Phase 1 已存在的 Home `LaunchedEffect(hour)` 时间感知 ambient 在 Player 设置 ambient 时让位，只有当 ambient 为 null 时才被使用

### 不包含（延后到后续 phase）

| 项 | 去向 |
|---|---|
| Trends 重设计 | Phase 4 |
| Settings 重设计 | Phase 4 |
| Onboarding / Welcome 重设计 | Phase 5 |
| `Starfield.kt` 删除 | Phase 5（OnboardingScreen / WelcomeRitualScreen 仍引用） |
| 真正的 FFT 音频分析 | 不做 —— `AudioWaveform` 已经是合成的正弦扰动条形 |
| AudioPreset 数据库 / 远程下载 | 不动 —— 12 个 bundled 预设维持现状 |

### 继承的约束

- Sleeping 屏强制 `LocalSWScheme = DARK`（不动）
- NIGHT 配色保持无障碍调优，本 phase 不改 NIGHT 行为
- Phase 1 morphProgress + Phase 2 NightSkyCanvas 都消费 `LocalHeroBackdropController.ambient` —— 本 phase 提供更密集的更新源，但不改它们的消费逻辑
- 现有 `PlayerScreen(embedded: Boolean = false)` 参数保留（Phase 2 添加，给 Sleeping 屏的迷你播放器抽屉使用）

---

## 2. 每个预设的光环色（auraColors）

12 个 bundled 预设各自的 3 色调色板。**top → mid → bottom**：top 是最亮的高光、mid 是主调（推到 ambient 的就是这一色）、bottom 是最深的房间地色（决定整屏背景偏移方向）。

| 预设 | top | mid | bottom |
|---|---|---|---|
| Womb (子宫) | `#C46B8C` | `#5B4880` | `#1A0F1E` |
| Heartbeat (心跳) | `#C44E5C` | `#4E1A22` | `#1A0A0E` |
| Dryer (烘干机) | `#C99B5A` | `#6B4A20` | `#1A1108` |
| Vacuum (吸尘器) | `#6E6A66` | `#38362F` | `#14130F` |
| White Noise (白噪) | `#FAFAFA` | `#ECE9E2` | `#1F222A` |
| Brown Noise (褐噪) | `#8A6850` | `#3E2A1F` | `#1A100A` |
| Pink Noise (粉噪) | `#E6B6B8` | `#B85F77` | `#1A0F12` |
| Rain (雨) | `#4A5A6E` | `#B7C3CC` | `#0F1620` |
| Ocean (海) | `#1F4F73` | `#2E7A86` | `#051A26` |
| Fan (电扇) | `#B5BFC8` | `#4F5C68` | `#0E1418` |
| Lullaby 1 | `#B8A4FF` | `#4D3C7A` | `#0F0C1E` |
| Lullaby 2 | `#FFB088` | `#8C4A26` | `#1A0E08` |

色彩选择原则（来自 master spec）：每个预设的 mid 色与极光色彩家族（dusk-orange / starlight-purple）有家族联系，但都偏移到能区分听感的方向。Rain 是雨灰偏冷蓝，Womb 是体液暖紫红，Ocean 是深海冷青，Heartbeat 是肌肉红 —— 听觉直觉对得上色觉直觉。

---

## 3. AudioPreset 模型扩展

`AudioPreset.kt` 加一个字段。考虑两种实现方式：

**方案 A（推荐）：** 在 model 层直接加 `auraColors: List<Color>`。简单、聚合、直观。代价是 `model` 包引入了 `androidx.compose.ui.graphics.Color`，本来 model 层应该是纯 Kotlin。

**方案 B：** 在 `core/visualkit/PresetAura.kt` 新建一个伴生 lookup 表 `PresetAura.byId(presetId): AuraColors`。model 保持纯净。代价是要维护两份"id → 数据"的映射，未来加预设时容易漏一个。

**取 A**，理由：项目目前的 `AudioPreset` 已经有 `iconName: String`（Android drawable 名）这种 Android-flavored 字段，model 层不是纯净的。加 Compose Color 不破坏现有约定，反而消除了"两份 id 表必须同步"的隐藏风险。

实现：

```kotlin
@Serializable
data class AudioPreset(
    val id: String,
    val nameKey: String,
    val category: AudioCategory,
    val recommendedAgeMinMonths: Int,
    val recommendedAgeMaxMonths: Int,
    val fileBundleName: String,
    val defaultDurationSeconds: Int = 1800,
    val loop: Boolean = true,
    val license: String = "CC0",
    val iconName: String,
    // 新增。@Transient 防止 kotlinx-serialization 序列化（Color 非 @Serializable）
    @kotlinx.serialization.Transient
    val auraColors: AuraColors = AuraColors.DEFAULT
)

@kotlinx.serialization.Serializable
data class AuraColors(
    @kotlinx.serialization.Transient val top: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFB8A4FF),
    @kotlinx.serialization.Transient val mid: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF4D3C7A),
    @kotlinx.serialization.Transient val bottom: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFF0F0C1E)
) {
    companion object {
        val DEFAULT = AuraColors()
    }
}
```

`bundled` 列表里每个预设的构造调用补一个 `auraColors = AuraColors(top = ..., mid = ..., bottom = ...)`。

**序列化考量：** 模型 `AudioPreset` 是 `@Serializable`（kotlinx-serialization）。它实际被序列化的地方只有 Settings 的 `currentPlayingPresetId` 之类字段；预设 catalog 本身是硬编码常量，从不写入磁盘。给 `auraColors` 加 `@Transient` 之后序列化器跳过它，反序列化时回到 `AuraColors.DEFAULT`（一个紫调默认），但这条路径在产品运行中不应被走到 —— 加 `@Transient` 是防御性的，避免未来某天有人把整个 `AudioPreset` 写入 DataStore 时崩溃。

---

## 4. `PresetAuraOrb` 组件

新文件：`app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/PresetAuraOrb.kt`

```kotlin
@Composable
fun PresetAuraOrb(
    aura: AuraColors,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp
)
```

构造（z-order 从底到顶）：
1. **基础圆盘**：径向渐变 `radialGradient(0% → aura.top, 70% → aura.mid, 100% → aura.bottom)`，clip 到 CircleShape
2. **InnerHighlight**：使用 Phase 0 的 `Modifier.innerHighlight(cornerRadius = sizeDp/2)` —— 顶部白光弧
3. **粒子层**：4 个小光点（1-2dp）在 orb 内部缓慢漂移，颜色为 `aura.top` @ 60% 透明度。基于 `withInfiniteAnimationFrameMillis` 帧时钟（与 NightSkyCanvas 同款 30fps 节流模式）
4. **active 状态**：`isActive = true` 时外圈加一层 24dp 阴影 `Modifier.shadow(24.dp, ambient/spot = aura.mid @ 40%)`，呼吸节律 `breathCycleMs = 4200ms` 在 30%↔60% alpha 间脉动

**reduce-motion：** 粒子静止在初始位置，呼吸阴影静止在 45% alpha 中位值。

**low-RAM：** 粒子层完全跳过，只渲染基础圆盘 + InnerHighlight + active 阴影。

公开 API 仅一个函数 + 默认 sizeDp 参数。所有内部状态（粒子位置数组、呼吸时钟）由 `remember { ... }` 私有持有。

---

## 5. AmbientSync —— 推 ambient 的桥

新文件：`app/src/main/java/com/lizhi1026/sleepwhisper/features/player/AmbientSync.kt`

不是 composable，而是一组 helper：

```kotlin
/**
 * 当 player state 变化时把 active preset 的 mid-aura 色推到 HeroBackdropController。
 * 在 PlayerScreen / SleepingScreen / Home 顶部 LaunchedEffect 中调用。
 */
fun HeroBackdropController.syncToPlayer(
    currentPreset: AudioPreset?,
    timeOfDayAmbient: Color? = null
) {
    setAmbient(currentPreset?.auraColors?.mid ?: timeOfDayAmbient)
}
```

调用规则：
- **PlayerScreen**: `LaunchedEffect(currentPreset)` 调用 `syncToPlayer(currentPreset, null)`，因为 Player 屏不参与 time-of-day 调色
- **SleepingScreen**: 不主动 push（Sleeping 屏走自己的 time-of-day 颜色逻辑，Hero #3 的 ambient 来自其他屏推过来的）
- **HomeScreen**: Phase 1 已有的 `LaunchedEffect(hour)` 重写为 `LaunchedEffect(hour, currentPreset)`，参数：`syncToPlayer(currentPreset, eveningHintColor)`

这样优先级是 **Player 预设 mid 色 > Home time-of-day 色 > null**，与 master spec 一致。

**清理：** 当 player 进入 `Stopped` 或 `Idle` 状态（FadingOut 完成或手动停止），`currentPreset` 变为 null，`syncToPlayer(null, ...)` 自动 fallback 到 time-of-day 色（如果在 Home）或 null（其他屏）。无需手动 clear。

---

## 6. Player 屏布局更新

```
┌─────────────────────────────────────────┐
│ ── DragHandle ──                         │
│                                          │
│  Tonight's room                          │ ← serifItalic(22) Phase 0 已落地，不动
│  RECOMMENDED FOR 4 MONTHS                │ ← labelMD tracked，已落地
│                                          │
│  ┌─ TIMER ──────────────────────────┐   │ ← DurationPickerCard，不动结构，
│  │ 15  30  45  60  90               │   │   chip 选中态改用 SWGradient.auroraGlow
│  └──────────────────────────────────┘   │
│                                          │
│  ── RECOMMENDED FOR 4 MONTHS ──          │ ← SectionLabel（Phase 0 共享组件）
│  ┌──────────────────────────────────┐   │
│  │ ◉ Womb                  ≋≋≋  ↵  │   │ ← PresetAuraOrb (active) + name + waveform
│  │   0-3m · Pink & deep            │   │   metadata 行新增（age range + descriptor）
│  └──────────────────────────────────┘   │
│  ┌──────────────────────────────────┐   │
│  │ ◯ Heartbeat                  ›   │   │ ← PresetAuraOrb (idle)
│  │   0-3m                           │   │
│  └──────────────────────────────────┘   │
│                                          │
│  ── ALL SOUNDS ──                        │
│  ┌──────────────────────────────────┐   │
│  │ ◯ Rain                       ›   │   │ ← surface tint 混 12% 当前 active aura
│  │   6-36m                          │   │   （"反光"效果）
│  └──────────────────────────────────┘   │
│  …                                       │
│                                          │
│  · ⋅  ⋅  ⋅    ⋅  ·    ⋅  ⋅              │ ← AuraParticles 全屏 24 粒子覆盖层
│                                          │
└─────────────────────────────────────────┘
```

### 子组件变化

**`Header` (line 115)** — 不变。Phase 0 已经把它升级到 `serifItalic(22) + labelMD`。

**`SectionLabel` (line 133)** — 删掉文件内的私有版本，引入 Phase 0 的共享 `com.lizhi1026.sleepwhisper.core.visualkit.components.SectionLabel`。

**`DurationPickerCard` (line 145)** — chip 的 active 态背景由 `SWGradient.primary(scheme)` 改为 `SWGradient.auroraGlow(scheme)`。inactive 态加 `Modifier.innerHighlight(cornerRadius = SWRadius.pill 一半)`。

**`PresetRow` (line 217)** — 重做：
1. 左侧 `Box(size = 48dp).clip(CircleShape).background(...)` 整段替换为 `PresetAuraOrb(aura = preset.auraColors, isActive = isPlaying, sizeDp = 48.dp)`
2. 中间 Column 加一行 metadata：`SWFont.labelSM().copy(color = textSecondary)` 显示 `"{ageMin}-{ageMax}m · {descriptor}"`，descriptor 字符串来自 `R.string.player_preset_descriptor_{nameKey}` —— **不在 Phase 3 添加新文案**（Aurora 重设计是视觉项目，避免文案蔓延），改为暂时用现有的 `R.string.player_preset_agerange`，等用户/产品决定 descriptor 文案
3. 右侧 `if (isPlaying)` 分支 `AudioWaveform` 颜色改用 `preset.auraColors.top`（而不是 `SWColor.accent(scheme)`），让波形与 orb 同色
4. 右侧 `else` 分支 `BasicText("›")` 改用 `ChevronTrail()`（Phase 0 共享）
5. 整个 `GlassCard` 的 surface tint 加"反光"调制：在父级用 `derivedStateOf` 算出 `surfaceTint = ambient ?: SWColor.surfaceElevated(scheme)`，把当前 ambient 以 12% 混入每个非-active 行的 GlassCard 背景 —— 通过给 `GlassCard.kt` 添加可选 `surfaceTintOverride: Color? = null` 参数（背向兼容，默认 null 时走原逻辑）

**`AuraParticles`** —— 新增 Player 屏顶层 24 个浮动粒子。当且仅当 `currentPreset != null` 时显示。粒子位置随机但确定性 seed，每个粒子 0.2 dp/sec 缓速漂移 + 正弦扰动。粒子色来自 `currentPreset.auraColors.top @ 30% alpha`。在 Player Box 内放在 LazyColumn 上方（最顶层）但不消费触摸事件（`Modifier.clearAndSetSemantics { }` + 无 pointerInput）。reduce-motion 静止；low-RAM 完全跳过。

---

## 7. GlassCard 微改

`GlassCard` 当前签名（Phase 0 + Phase 1 落地）：

```kotlin
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    hero: Boolean = false,
    content: @Composable BoxScope.() -> Unit
)
```

加一个可选参数：

```kotlin
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = SWRadius.lg,
    contentPadding: PaddingValues = PaddingValues(SWSpacing.md),
    elevation: ElevationLevel = ElevationLevel.SOFT,
    hero: Boolean = false,
    surfaceTintOverride: Color? = null,  // 新增
    content: @Composable BoxScope.() -> Unit
)
```

当 `surfaceTintOverride != null` 时，内部 `.background(SWColor.surfaceElevated(scheme))` 改为 `.background(surfaceTintOverride)`。**默认值 null 保留现有行为**，所有 Phase 0/1/2 的 GlassCard 调用点零改动。

只有 Player 屏的 PresetRow 利用这个新参数 —— 把 12% ambient 混入 elevated surface 后传进来。其他 GlassCard 调用点继续走默认。

---

## 8. 选中动画时序

用户点击一个 PresetRow：

```
t=0      触觉反馈：HapticFeedbackType.LongPress
         vm.playPreset(presetId, minutes) — 现有逻辑不变

t=0      AmbientSync.syncToPlayer 触发：
         HeroBackdropController.setAmbient(newPreset.auraColors.mid)
         AuroraBackdrop 在 600ms 内将三条极光带 lerp 至新 ambient（Phase 0 已实现）

t=0      PresetAuraOrb 的 isActive 状态变化：
         先前的 active orb 的 24dp 阴影 fade out (~300ms)
         新的 active orb 的 24dp 阴影 fade in (~300ms)，呼吸开始

t=120ms  AudioWaveform 出现，颜色从默认 accent 过渡到 newPreset.auraColors.top
         AudioWaveform 自身的 sine envelope 不变

t=180ms  邻近行 (±2 from selected) 的 surfaceTint 开始向 12% ambient 漂移 (RGB lerp)
         实现：每个 PresetRow 监听父级的 derivedStateOf<Color?> ambient，自己计算最终 tint

t=400ms  AuraParticles 透明度从 0 ramp 到 1（如果先前 currentPreset 是 null）

t=600ms  所有过渡完成
```

**取消选中**（用户再次点击当前 active preset → vm.stop()）：
- 反向：ambient 渐变到 null（如果在 Home tab 则 fallback 到 time-of-day 色，其他屏直接 null）
- particles fade out
- 邻近行的 surface tint 复原
- 所有过渡总时长 600ms

---

## 9. ambient 跨屏持久化

`HeroBackdropController.ambient` 是 `AppStateContainer.heroBackdrop` 的属性（Phase 1 已建立），活在 Hilt singleton 上 → 跨 screen 切换不丢失。

**生命周期：**
- Player 屏存在 + 有 active preset → ambient = preset.mid
- Player 屏被关闭，切回 Home tab → ambient 不变，但 Home 的 `LaunchedEffect(hour, currentPreset)` 重新评估，因为 currentPreset 还有值 → 仍是 preset.mid
- 切到 Trends / Settings → 那些屏的 AuroraBackdrop 也读 ambient，自动跟色
- 用户在 Home 主动按 Sleep CTA → 进入 Sleeping 屏，Sleeping 的 NightSkyCanvas 自己用 TimeOfDayPalette 选色，**不消费 ambient**（Phase 2 决定，避免 audio aura 污染时间叙事）。但当用户从 Sleeping 退回 Home，preset 仍在播 → Home 仍有 ambient
- 播放器 timer 到点 stop / 用户手动 stop → preset 变 null → ambient 自动回到 time-of-day 或 null

这条机制完全使用 Phase 1 已有的 CompositionLocal 通道，不引入新 plumbing。

---

## 10. 风险与缓解

| ID | 风险 | 缓解 |
|---|---|---|
| R1 | 12 个预设的 auraColors 是单点真值；未来加预设时容易漏配 | model 层加单元测试 `AudioPresetAuraTest`：遍历 `AudioPreset.bundled`，断言每个的 `auraColors != AuraColors.DEFAULT`。漏配立即红 |
| R2 | AuraColors 含 Compose Color，`@Transient` 让序列化路径丢失数据。如果未来某个新的功能把整个 AudioPreset 写入 DataStore，反序列化拿到 DEFAULT 而非原色 | 在 `AuraColors.DEFAULT` 同文件加 KDoc 强警告。短期不构成实际 bug（catalog 是常量） |
| R3 | 同时把 ambient 推到 AuroraBackdrop + PresetAuraOrb + AudioWaveform 颜色，3 处都读 `auraColors`，但 ambient state 更新 frame-by-frame，UI 容易抖动 | `auraColors` 是 preset 的不变属性，不随 ambient 变。ambient 只驱动 AuroraBackdrop 内部的极光带 lerp（已经在 Phase 0 的 600ms tween 内吸收）。3 处不会同步抖动 |
| R4 | 24 个 AuraParticles + 4 个 orb-internal 粒子 + 80 stars (Sleeping) 同屏不会发生，但 Player 屏 12 个 PresetAuraOrb × 4 内部粒子 = 48 粒子常驻 + 24 全屏 AuraParticles = 72 粒子 | 全部走同一帧时钟 (30fps withInfiniteAnimationFrameMillis)。每帧 72 个 `drawCircle` 是 Compose Canvas 量级里廉价的。Pixel 10 实测无压力。低端机用 `isLowRamDevice()` 跳过 AuraParticles（24 个），保留 orb 自身的 4×12=48 |
| R5 | RGB lerp 在 Compose lerp(Color, Color, Float) 上 Phase 1 发现量化问题；surfaceTint 12% 混色可能输出"脏"颜色 | 12% 是很弱的混色，量化误差被肉眼忽略。如果实测有问题，改用 manual `Color(r*(1-f) + r2*f, g*(1-f)+g2*f, ...)` —— Phase 1 的 `blendAmbient` 就是这么做的，可直接复用 |
| R6 | mini-Player-sheet (Phase 2) 模式下也会推 ambient 给 backdrop；但 mini-Player-sheet 显示时 Sleeping 屏背景是 NightSkyCanvas，**不读 ambient** | 当前设计：mini-Player-sheet 里点选预设时 ambient 会被推，但 Sleeping 屏的 NightSkyCanvas 不消费，所以视觉上无效果。一旦 sheet 关闭，回到 Home/Trends/Settings 时 ambient 已就位 → 自动跟色。可接受 |
| R7 | "邻近行 ±2 行 surface tint 混 12% ambient" 列表性能：12 行 LazyColumn，每行都要读 ambient state 并 derivedStateOf 计算 tint | LazyColumn 的 `items` lambda 自然只重组可见行；ambient 变更触发的 recomposition 范围是"屏上可见的 PresetRow" ≤ 4-5 个。可接受 |

---

## 11. 实现 Phase 化 —— 9 个 task

| Task | 输出 | 文件 |
|---|---|---|
| 1 | `AuraColors` 数据类 + `AudioPreset.auraColors` 字段 + 12 个 bundled 预设各自的颜色 + `AudioPresetAuraTest` 单元测试遍历断言 | `AudioPreset.kt`, new test |
| 2 | `PresetAuraOrb.kt` 新组件（基础圆盘 + innerHighlight + 粒子 + active 呼吸阴影），含 `@Preview` | new |
| 3 | `AmbientSync.kt` 新文件，`syncToPlayer` 扩展函数 | new |
| 4 | `GlassCard.kt` 加 `surfaceTintOverride: Color? = null` 参数（背向兼容） | modify |
| 5 | `PlayerScreen.kt` 把 `PresetRow` 的左侧圆替换为 `PresetAuraOrb`，加 `surfaceTintOverride` 反光，AudioWaveform 用 `preset.auraColors.top`，"›" 改 `ChevronTrail`；导入共享 `SectionLabel` 删本地版本；DurationChip active 态用 `auroraGlow` | modify |
| 6 | `PlayerScreen.kt` 内嵌 `LaunchedEffect(currentPreset)` 调用 `heroBackdrop.syncToPlayer(currentPreset, null)`；不影响 `embedded` mode | modify |
| 7 | `HomeScreen.kt` 把 `LaunchedEffect(hour)` 扩展为 `LaunchedEffect(hour, currentPreset)`，调用 `syncToPlayer(currentPreset, eveningHint)` | modify |
| 8 | `AuraParticles.kt` 新组件 + Player 屏顶层覆盖；24 粒子；reduce-motion 静止；low-RAM 跳过 | new + modify PlayerScreen |
| 9 | 手动冒烟 + 截图 | (无代码) |

**排序：**
- Task 1 是基础（auraColors 数据源）
- Task 2, 3, 4 可并行（数据使用方）
- Task 5 用前面 4 个
- Task 6 把 PlayerScreen 与 ambient 系统接上
- Task 7 拓展到 HomeScreen
- Task 8 是 Player 屏顶层装饰
- Task 9 验证全部串联

---

## 12. 验证

### 单元测试

- `AudioPresetAuraTest.kt` ——
  - 遍历 `AudioPreset.bundled`，断言每个的 `auraColors != AuraColors.DEFAULT`
  - 断言 12 个的 `mid` 色两两不同（无重复阻塞调色板视觉）
  - 断言每个的 `top.red + top.green + top.blue > bottom.red + bottom.green + bottom.blue`（top 比 bottom 亮，避免误录）

### 仪表化测试（可选，本 phase 不强求）

- `PlayerScreenAmbientSyncTest` —— composeTestRule，点击一个 PresetRow，断言 `app.heroBackdrop.ambient` 不再为 null

### 手动冒烟（Task 9）

1. 打开 Player → 点 Womb → backdrop 在 ~600ms 内变粉紫
2. 退回 Home tab → backdrop 仍粉紫
3. 切到 Trends → backdrop 仍粉紫
4. 切回 Player → 点 Ocean → backdrop 变深青
5. Player timer 到点结束播放 → backdrop 渐变回 time-of-day 色 / null
6. 在 Sleeping 屏（迷你播放器抽屉中）选 Rain → 关闭抽屉退回 Home → backdrop 变蓝灰
7. reduce-motion 开 → AuraParticles 静止、orb 呼吸阴影静止；ambient 漂移变成瞬切（Phase 0 的 AuroraBackdrop 已尊重 reduce-motion）

### 截图

`docs/superpowers/screenshots/2026-05-21-phase-3/` 收录：
- Player 屏全貌（Womb active）
- Home 屏 ambient 持久化态（Ocean 推过来的深青）
- PresetAuraOrb 单组件多状态预览（Android Studio @Preview 截图）

---

## 13. 锁定的决策

| 决策 | 值 |
|---|---|
| 每预设色数 | 3（top / mid / bottom）|
| 推到 ambient 的色 | mid |
| ambient 漂移时长 | 600ms（Phase 0 既定）|
| 邻近行反光比例 | 12% |
| 反光行数 | 全部非-active 行（不是只 ±2，因为 LazyColumn 可见行天然就是邻近的）|
| AuraParticles 数量 | 24（仅 Player 屏顶层）|
| Orb 内部粒子数 | 4（每个 orb）|
| Orb 默认尺寸 | 48dp（Player 屏 row）/ 64dp（其他场景 default）|
| Orb 呼吸节律 | `SWMotion.breathCycleMs = 4200ms`（与 GlassCard hero 同步）|
| auraColors 序列化 | `@Transient`（kotlinx-serialization 跳过）|
| AudioPreset 不变性 | 12 个 bundled 维持，本 phase 不加/不减预设 |
| ambient 跨屏 | Phase 1 的 `LocalHeroBackdropController` 通道复用 |
| Sleeping 屏 | **不消费 ambient**（Phase 2 已定，时间叙事独立）|
| 文案变化 | 无（preset descriptor 暂用现有 age range 文案）|
| `PlayerScreen.embedded` | 已存在（Phase 2），本 phase 不动；mini-player-sheet 推 ambient 仍生效，sheet 关闭后跨屏可见 |
| low-RAM | AuraParticles + orb 内部粒子跳过，orb 主体保留 |
| reduce-motion | 全部粒子静止，呼吸阴影静止，ambient 漂移瞬切 |
