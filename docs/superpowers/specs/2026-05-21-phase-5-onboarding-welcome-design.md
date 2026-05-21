# Phase 5 — Onboarding + Welcome Ritual 设计文档

**日期：** 2026-05-21
**状态：** 已批准（待最终用户审阅）
**父规约：** [Aurora Redesign Master](2026-05-20-aurora-redesign-master-design.md) §5.6

Aurora 重设计的最后一个 phase。给 Onboarding（第一次启动应用、用户输入宝宝信息）和 WelcomeRitual（第一次保存宝宝后的"相遇时刻"动画）两个屏幕应用 Aurora 设计语言。最重要的视觉举动：把宝宝名字在 WelcomeRitual 中做成 96sp DM Serif Display —— 整个 app 字号最大的瞬间 —— 与 master spec 的 §5.6 "The moment of meeting" 一致。

同时，**最终删除 `Starfield.kt`** —— 替换 Onboarding/Welcome 中的 `Starfield()` 调用为 `NightSkyCanvas`。

---

## 1. 范围

### 包含

- **OnboardingScreen**:
  - 替换 `Starfield(density = ...)` → `NightSkyCanvas(morphProgress = 1f)`
  - Hero 区 solid 圆背景从 `SWGradient.accent(scheme)` 改用 `SWGradient.auroraGlow(scheme)` + `Modifier.innerHighlight(cornerRadius = 44.dp)`
  - 表单 `Field(label, content)` 私有 composable 中的 inline uppercase BasicText 改用 Phase 0 共享 `SectionLabel`
  - 删除内联私有 `Divider` → 调用点改用 `Hairline`
  - 删除内联私有 `Double.sp()` 扩展函数（不再被引用）
- **WelcomeRitualScreen**:
  - 替换 `Starfield(density = 60)` → `NightSkyCanvas(morphProgress = 1f)`
  - 宝宝名字字号从 44sp Sans-Serif 升级到 96sp DM Serif Display via `SWFont.displayXL()` + `SWFont.cjkAware(...)`
  - greeting 字号从 17 升级到 22 与 Home/Trends 对齐
  - hero solid 圆用 `SWGradient.auroraGlow(scheme)` + `Modifier.innerHighlight(cornerRadius = 44.dp)`
  - ring stroke 颜色统一为 `SWColor.accentSecondary(scheme)` (starlight purple)，去除 isPurpleHero 条件
  - 名字下方加 `Hairline` 60dp
- **Starfield 删除**:
  - `app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt` 物理删除
  - grep 验证 `app/src/` 全部无残留引用

### 不包含

| 项 | 原因 |
|---|---|
| Onboarding 业务逻辑（DatePicker / SegmentedPicker / 验证） | 不动 |
| 文案变化 | 全 phase 项目原则 |
| 新增 onboarding step / 新字段 | 不动 |
| ritual 时序变化（4-stage timing 700/600/800/1200ms 等） | 现有时序已经足够 |
| 应用图标 / 启动屏 | 留给未来 Phase 6 |

---

## 2. OnboardingScreen 改造

### Hero 区 — 88dp solid 圆 + 3-ring PulseRing

```kotlin
// 当前
Box(modifier = Modifier.size(88.dp).clip(CircleShape).background(SWGradient.accent(scheme)))

// 改造后
Box(
    modifier = Modifier
        .size(88.dp)
        .clip(CircleShape)
        .background(SWGradient.auroraGlow(scheme))
        .innerHighlight(cornerRadius = 44.dp)
)
```

PulseRing 现有 radius=60dp / intensity 不动 —— Phase 0 已经升级为 3-ring offset。

### 表单 Field 容器

`Field(label, content)` 保留为 layout 容器，但内部的 inline label 改用 SectionLabel：

```kotlin
@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(SWSpacing.xs)) {
        SectionLabel(label)  // 替换 BasicText(text = label.uppercase(), style = SWFont.labelMD()...)
        content()
    }
}
```

### 删除私有定义

- `private fun Divider() { ... }` 删除；2 处调用点（line 136, 171）改用 `Hairline()`
- `private fun Double.sp()` 扩展函数 删除（之前给 Field 的 letterSpacing 用，现在 SectionLabel 内部处理）

### 背景

`Starfield(... density = if (scheme == SWScheme.DAY) 30 else 70)` → `NightSkyCanvas(modifier = Modifier.fillMaxSize(), morphProgress = 1f)`。

---

## 3. WelcomeRitualScreen 改造

### Hero 圆（统一 cross-scheme）

```kotlin
// 当前 conditional
val isPurpleHero = scheme == SWScheme.DAY
val ringStrokeColor = if (isPurpleHero) Color(0.45f, 0.36f, 0.85f) else SWColor.accent(scheme)
val heroSolidColor = if (isPurpleHero) SWColor.softLilac(scheme) else SWColor.accent(scheme)
// hero solid 圆
Box(modifier = Modifier.size(88.dp).background(heroSolidColor, CircleShape))

// 改造后 unified
val ringStrokeColor = SWColor.accentSecondary(scheme)  // starlight purple, all schemes
// hero solid 圆
Box(
    modifier = Modifier
        .size(88.dp)
        .clip(CircleShape)
        .background(SWGradient.auroraGlow(scheme))
        .innerHighlight(cornerRadius = 44.dp)
)
```

`heroSolidColor` 变量删除（替换为 inline auroraGlow）。`isPurpleHero` 变量删除。

### Stage 2 — 相遇时刻

```kotlin
// 当前 greeting
BasicText(
    text = stringResource(R.string.welcome_greeting),
    style = SWFont.serifItalic(17).copy(...)
)

// 改造后
BasicText(
    text = stringResource(R.string.welcome_greeting),
    style = SWFont.serifItalic(22).copy(color = SWColor.textPrimary(scheme).copy(alpha = 0.85f))
)
```

宝宝名字 —— 整个 app 字号最大的瞬间：

```kotlin
// 当前 44sp Sans-Serif SemiBold
BasicText(
    text = baby?.name ?: "",
    style = androidx.compose.ui.text.TextStyle(
        fontSize = 44.sp,
        fontWeight = FontWeight.SemiBold,
        color = SWColor.primary(scheme)
    )
)

// 改造后 96sp DM Serif Display + CJK fallback
BasicText(
    text = baby?.name ?: "",
    style = SWFont.cjkAware(
        SWFont.displayXL().copy(color = SWColor.textPrimary(scheme))
    ),
    maxLines = 1,
    overflow = androidx.compose.ui.text.style.TextOverflow.Visible
)
Spacer(Modifier.height(SWSpacing.xs))
Hairline(modifier = Modifier.width(60.dp))
```

`maxLines = 1` 加 `overflow = Visible`：在极小屏 / 长名字时让它溢出可视区也比断行好（与 spec R1 缓解一致）。

### Stage 3

不动。已经是 bodyMD + labelSM Phase 0 风格。

### 背景

`Starfield(modifier = Modifier.fillMaxSize(), density = 60)` → `NightSkyCanvas(modifier = Modifier.fillMaxSize(), morphProgress = 1f)`。

---

## 4. Starfield 删除（最终）

迁移完两个屏幕后：

```bash
grep -rn "Starfield" app/src/main/java/ app/src/test/java/ app/src/androidTest/java/ 2>/dev/null
# 应该返回 empty
rm app/src/main/java/com/lizhi1026/sleepwhisper/core/visualkit/components/Starfield.kt
./gradlew :app:compileDebugKotlin
# BUILD SUCCESSFUL
```

---

## 5. 风险

| ID | 风险 | 缓解 |
|---|---|---|
| R1 | 96sp DM Serif 在长名字 / 小屏溢出 | `maxLines = 1` + `overflow = Visible`，让它优雅地超出布局而非断行 |
| R2 | NightSkyCanvas 在 Onboarding (DAY scheme, 白天打开) 显示有点深的天空 | TimeOfDayPalette 在白天会落到 morning hand-back stop（深蓝-暖橙），与 hero auroraGlow 颜色搭配自然。可接受 |
| R3 | 中文宝宝名字 fallback 到 Noto Serif CJK SC，首次显示可能闪 | Phase 0 cjkAware 路径已经测试通过；Welcome 屏停留 ~2.6 秒可吸收 fallback delay |
| R4 | 删除 Starfield 但漏了某个隐藏调用点 | grep 三个 src 树（main/test/androidTest），删除前/后双确认；编译验证 |
| R5 | OnboardingScreen 私有 `Double.sp()` 删除后某处仍在用 | grep 文件内 `\.sp()` 模式，应该只有 `1.4.sp()` 那一处用 |

---

## 6. 实现 Phase 化 —— 5 个 task

| Task | 输出 | 文件 |
|---|---|---|
| 1 | OnboardingScreen Starfield→NightSkyCanvas + Hero auroraGlow + innerHighlight | `OnboardingScreen.kt` |
| 2 | OnboardingScreen Field→SectionLabel + 删 Divider/Double.sp 私有 | `OnboardingScreen.kt` |
| 3 | WelcomeRitualScreen Starfield→NightSkyCanvas + 统一 hero 圆 auroraGlow + ring accentSecondary | `WelcomeRitualScreen.kt` |
| 4 | WelcomeRitualScreen 名字 96sp DM Serif + cjkAware + Hairline + greeting 22sp | `WelcomeRitualScreen.kt` |
| 5 | 删除 Starfield.kt + 编译 + 自动截图 | delete + verify |

排序：1-2 合并 commit；3-4 合并 commit；5 收尾。

---

## 7. 锁定决策

| 决策 | 值 |
|---|---|
| Starfield 删除 | 本 phase 末尾 |
| OnboardingScreen 背景 | NightSkyCanvas |
| WelcomeRitualScreen 背景 | NightSkyCanvas |
| Hero 圆背景（两屏） | SWGradient.auroraGlow + innerHighlight(cornerRadius = 44.dp) |
| Welcome ring stroke | SWColor.accentSecondary（starlight-purple，跨 scheme 统一） |
| Welcome 宝宝名字 | SWFont.displayXL() (96sp DM Serif Display) + cjkAware + maxLines=1 |
| Welcome greeting | 17 → 22 sp |
| Welcome 名字下方 Hairline | 60dp 宽 |
| Onboarding Field label | 共享 SectionLabel |
| Onboarding 私有 Divider 删除 | 是，调用点改 Hairline |
| Onboarding 私有 Double.sp() 删除 | 是 |
| 文案变化 | 无 |
| ritual 时序变化 | 无 |
| 业务逻辑变化 | 无 |
