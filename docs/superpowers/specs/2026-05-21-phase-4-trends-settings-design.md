# Phase 4 — Trends + Settings 视觉打磨 设计文档

**日期：** 2026-05-21
**状态：** 已批准（待最终用户审阅）
**父规约：** [Aurora Redesign Master](2026-05-20-aurora-redesign-master-design.md) §5.4–5.5

Aurora 重设计 Phase 4。把 Phase 0 设计令牌应用到 Trends 与 Settings 两个数据/配置屏，引入 Phase 0 的共享组件（`SectionLabel`、`Hairline`、`SerifMetricRow`、`ChevronTrail`），把 Trends 的 weekly chart 改用极光渐变。

不是 hero phase — 没有新的 Hero 时刻，纯粹打磨。但是仍然要紧贴 Aurora 设计语言：serif headlines、hairline 分隔、极光配色、moonlit-glass 卡片表面（这些 Phase 0 / 1 / 2 已经默认应用了）。

---

## 1. 范围

### 包含

- **Trends 屏**：
  - Weekly chart 柱形改用 `SWGradient.auroraGlow` 渐变（替换 `SWColor.accent / primary`）
  - Today timeline / Recent events 重新检视：应用 `SectionLabel` 替换内联 uppercase BasicText
  - WeeklyCard 大数字（avg hours）升级到 `SWFont.titleXL()` 或 `displayMD()` 让其成为视觉锚点
  - 各 card 的 hairline 分隔
- **Settings 屏**：
  - 将 `LabelRow` / 类似行式布局逐项替换为 Phase 0 共享 `SerifMetricRow`（保留现有的 `StepperRow` 与 `SegmentedPicker` 业务行）
  - 各 section 头部用 `SectionLabel` 替换内联 BasicText
  - 各 SectionCard 内行间用 `Hairline` 替换现有的 `Divider`（如果有）
- Trends/Settings header 都微调字号到 `serifItalic(28)` → 保留现状（已经是 28），但加上 `Hairline` flourish 模仿 Home 的"书章规则"风格
- 不修改 ViewModel 业务逻辑

### 不包含（延后到 Phase 5）

| 项 | 去向 |
|---|---|
| Onboarding / WelcomeRitual 视觉 | Phase 5 |
| `Starfield.kt` 删除 | Phase 5（Onboarding 仍引用） |
| 任何文案变化 | 全 phase 项目原则：不动 strings.xml |
| 添加新的 chart 类型 / 数据维度 | 不动（业务功能不改） |
| Settings 的权限请求逻辑 | 不动 |

### 继承约束

- NIGHT 配色保持无障碍调优
- AuroraBackdrop 已经在 Trends/Settings 渲染（Phase 0 落地），不动
- HeroBackdropController.ambient（Phase 3 推送）会让 Trends/Settings 的 backdrop 跟色 —— 这已经是 Phase 3 自动行为，本 phase 不增加 Trends/Settings 自身的 ambient 推送

---

## 2. Trends 屏

### Header

```
Trends                                          ← serifItalic(28)
───                                              ← Hairline 60dp 宽（与 Home 同款）
TRENDS FOR {babyName}                            ← labelMD uppercase tracked
```

`Header` 改造：
- 现有 `BasicText(...trends_title, serifItalic(28))` 保留
- 在 title 与 subtitle 之间插入 60dp 宽 `Hairline`
- subtitle 已经是 uppercase labelMD，保留

### TodayCard

```
┌──────────────────────────────────────────┐
│ TODAY                                    │ ← SectionLabel 替换 BasicText titleMD
│                                          │
│ TodayTimeline(sleeps, feedings)          │ ← 内部不动
│                                          │
└──────────────────────────────────────────┘
```

`stringResource(R.string.trends_today)` 现在用 `titleMD`，改用 `SectionLabel(...)` 共享组件 —— 自动 uppercase + tracked + textSecondary。这是 Trends 内部小幅一致性整理。

GlassCard 自身已经是 moonlit-glass treatment（Phase 0），不动。

### WeeklyCard

```
┌──────────────────────────────────────────┐
│ THIS WEEK              avg 11.4h         │ ← SectionLabel + 右侧 RollingNumber serif tabular
│ ───                                       │ ← Hairline
│                                          │
│  ╭─╮ ╭─╮ ╭─╮ ╭─╮ ╭─╮ ╭─╮ ╭─╮             │ ← WeeklyBars
│  │░│ │░│ │▓│ │▓│ │▓│ │░│ │▓│             │   今日柱：auroraGlow 渐变
│  ╰─╯ ╰─╯ ╰─╯ ╰─╯ ╰─╯ ╰─╯ ╰─╯             │   其他柱：primary @ 70%
│   M   T   W   T   F   S   S              │
└──────────────────────────────────────────┘
```

Changes:
- Title `"THIS WEEK"` 现 `titleMD textPrimary`，改用 `SectionLabel`（auto-uppercase + tracked + textSecondary 风格 —— 与 Aurora 设计语言一致）
- avg hours `"avg 11.4h"` 当前是 `bodyMD textSecondary`，升级到 `SWFont.titleXL()` 并放大到 serif `displayMD` 级别 —— 用 `SerifMetricRow` 把 "AVG" label 与 "11.4h" 数值分两列展示，让数字成为视觉锚点
- 加 `Hairline` 在 title 行与 chart 之间
- WeeklyBars 中每根柱子的 fill：今日 `SWGradient.auroraGlow(scheme)` (径向)、其他 `SWGradient.accent(scheme)` 加 70% alpha
  - 由于 `drawRoundRect` 需要 brush 而不是单色，要用 Compose `Brush.linearGradient` 或 `Brush.verticalGradient` 把 auroraGlow 的色彩取出来手工组装一条垂直渐变 —— 现实约束：Canvas 的 `drawRoundRect` 接受 `Brush` 参数，直接传 `SWGradient.auroraGlow(scheme)`（它是 `Brush.radialGradient`）会产生圆形的 fill，需要用 `Brush.verticalGradient(listOf(top=accent, bottom=accentSecondary))` 手工构造
  - 决策：新建一个 `chartBarBrush(scheme, isHighlighted)` helper 局部于 `TrendsScreen.kt`，封装这个细节

### TodayTimeline / RecentEventsList

这两个子组件已经存在于独立文件（`TodayTimeline.kt` / `RecentEventsList.kt`）。本 phase 仅做最小改动：
- 检查它们内部是否有 uppercase BasicText section labels，如果有则替换为共享 `SectionLabel`
- 检查它们是否使用 `Color.White` / `SWColor.accent` 等硬编码 —— 应该全部走 SWColor 令牌（Phase 0 应该已经走通了，本任务复检）
- 各 event row 间加 `Hairline`（如果还没有）

### EmptyStateCard

Phase 0 已经升级了 EmptyStateCard 的 surface 治理，本 phase 不动。

---

## 3. Settings 屏

### Header

```
Settings                                         ← serifItalic(28) 保留
───                                              ← Hairline flourish 60dp 宽
```

加 Hairline 与 Home/Trends 一致。

### SectionCard 头部

每个 section 当前用一个 `BasicText(... title ...)` 顶部 + 内容。例如：
- "BABY" / "PLAYBACK" / "NOTIFICATIONS" / "MICROPHONE" / "ABOUT" 等等

现在 SectionCard 的 title 字段已经传字符串进去，内部用 `titleMD`。改造：title 改用 `SectionLabel(title)` 共享组件渲染（自动 uppercase + tracked + textSecondary）。

### LabelRow → SerifMetricRow

Settings 中存在 `LabelRow(label, value)` 形如：

```
Name              Emma
Age               142 days
```

这是 Phase 0 `SerifMetricRow` 的精确用例：左 label / 右 value / 可选 chevron。批量替换：所有 `LabelRow(label, value)` 调用点改为 `SerifMetricRow(label, value, showChevron = false)`。

`LabelRow` 的私有定义如果不再使用，删除。

### StepperRow / SegmentedPicker / Toggle 行

这些是带交互的行，**不替换为 SerifMetricRow**。保留现状，但确保字体走 SWFont（应该已经是）。

### Divider → Hairline

Settings 中如果有自定义的 `Divider()` 私有组件用作行间分隔，替换为 Phase 0 `Hairline`。

---

## 4. 风险与缓解

| ID | 风险 | 缓解 |
|---|---|---|
| R1 | WeeklyBars 渐变 brush 实现错误，柱子全黑或单色 | 新建 `chartBarBrush` helper 局部到 `TrendsScreen.kt`，单元测试不强求（视觉验证够了） |
| R2 | SerifMetricRow 替换 LabelRow 时，原 LabelRow 有些字段是空 / 多行 / 含 Composable trailing | 逐个 LabelRow 调用点检视；任何不符合 `(label, value)` 简单模式的保留原样，不强行套 SerifMetricRow |
| R3 | SectionLabel 替换 SectionCard 内 title 时，原 title 已经全大写 + uppercase 字符串，会变成 double-uppercase | `SectionLabel` 内部对入参做 `.uppercase()` 是幂等操作（已经大写的字符串再 uppercase 不变）。安全 |
| R4 | Settings 各 section 文案中的 emoji 或 Unicode 字符 | 不动文案 ✓ |
| R5 | Phase 4 完成后，Trends 屏在有 ambient 时（用户在播放音乐）的柱状图配色变怪 | WeeklyBars 用 auroraGlow / accent —— 这两个 token 本身 ambient-agnostic（它们是预设色，不消费 ambient）。AuroraBackdrop 会跟 ambient 漂移，但前景柱子保持 Aurora 极光色不变。视觉上"屋子换了色，画框里的图保持不变"，可接受 |
| R6 | Settings 中 `LabelRow` 私有删除可能破坏其他不可见调用点 | grep "fun LabelRow" 确认是否只有一个定义点，replace_all 安全 |

---

## 5. 实现 Phase 化 —— 6 个 task

| Task | 输出 | 文件 |
|---|---|---|
| 1 | TrendsScreen header 加 Hairline + WeeklyCard title 改用 SectionLabel + avg hours 升级 | `TrendsScreen.kt` |
| 2 | WeeklyBars 柱形改用 chartBarBrush helper（verticalGradient 双色） | `TrendsScreen.kt` |
| 3 | TodayTimeline + RecentEventsList 整理（如有 inline label 替换为 SectionLabel；如有 Color.White 硬编码替换；行间加 Hairline） | `TodayTimeline.kt`, `RecentEventsList.kt` |
| 4 | SettingsScreen header 加 Hairline + SectionCard title 改用 SectionLabel | `SettingsScreen.kt` |
| 5 | SettingsScreen 所有 LabelRow → SerifMetricRow + 删除本地 LabelRow + Divider→Hairline | `SettingsScreen.kt` |
| 6 | 编译 + 手动冒烟 + 截图 | (验证) |

排序：1-2 独立，3 独立，4-5 独立。1-3 并行可，4-5 串行。6 是收尾。

---

## 6. 验证

### 单元测试

无新增 —— Phase 4 是纯视觉打磨，没有新的纯逻辑需要 TDD 保护。

### 仪表化测试

无强求。

### 手动冒烟（Task 6）

1. 进入 Trends 屏：
   - Header "Trends" 下出现 60dp Hairline；subtitle uppercase tracked
   - WeeklyCard 标题 "THIS WEEK" 是 labelMD style（uppercase, tracked, textSecondary）
   - avg hours 在右上角，用 SerifMetricRow 风格展示
   - WeeklyBars：今日柱有 auroraGlow 双色垂直渐变；其他柱是 accent 单色（@70% alpha）
   - 加 Hairline 在 chart 上方
2. 进入 Settings 屏：
   - Header "Settings" 下出现 60dp Hairline
   - 每个 section 的 title (BABY / PLAYBACK / NOTIFICATIONS 等) 用 SectionLabel 风格
   - BABY section 内 "Name / Emma" "Age / 142 days" 走 SerifMetricRow
   - 行间分隔用 Hairline（如果原本有 Divider）

### 截图

`docs/superpowers/screenshots/2026-05-21-phase-4/`：
- Trends 屏
- Settings 屏（baby section + playback section 可见）

---

## 7. 锁定决策

| 决策 | 值 |
|---|---|
| WeeklyBars 今日柱 | verticalGradient(top=accent, bottom=accentSecondary) |
| WeeklyBars 其他柱 | accent @ 70% alpha |
| SectionCard title | SectionLabel 共享组件 |
| LabelRow 替换策略 | SerifMetricRow，保留 StepperRow/SegmentedPicker/Toggle |
| Header Hairline | 60dp 宽，与 Home 同款 |
| 文案变化 | 无 |
| 业务逻辑变化 | 无 |
| 新增组件 | 无（只用 Phase 0 共享件） |
| Ambient 行为 | 不在 Trends/Settings 推 ambient（继承 Phase 3 跨屏持久化） |
| chart 颜色 vs ambient | chart 颜色独立于 ambient（保留 Aurora 极光色，不跟 preset 漂移） |
