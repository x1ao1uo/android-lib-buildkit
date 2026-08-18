# android-lib-buildkit

从 [Now in Android](https://github.com/android/nowinandroid)（Apache-2.0，见 `LICENSE` / `NOTICE`）抽取并适配的 **Android 构建/质量工具链 monorepo**。包名空间已从 `com.google.samples.apps.nowinandroid` 全局替换为 `com.z1nt.buildkit`，Gradle 插件 id 前缀从 `nowinandroid.*` 改为 `buildkit.*`。

## 仓库结构

| 目录 | 说明 |
|---|---|
| `build-logic/convention/` | Gradle convention plugins（独立 included build），涵盖 application/library/test/compose/hilt/room/jacoco/lint/flavors/spotless/graph 等构建约定 |
| `lint/` | 自定义 Lint 检查（JVM library）：`TestMethodNameDetector`、`DesignSystemDetector`，注册类 `BuildkitIssueRegistry` |
| `ui-test-hilt-manifest/` | 提供 `@AndroidEntryPoint HiltComponentActivity`，用于在 UI 测试中绕过 [google/dagger#3394](https://github.com/google/dagger/issues/3394) |
| `spotless/` | Spotless 版权头模板（`copyright.kt` / `copyright.kts` / `copyright.xml`），被 convention 中的 Spotless 配置引用 |
| `benchmarks/` | Macrobenchmark / Baseline Profile 参考源码。**仅作参考，未接入 `settings.gradle.kts`**，不参与构建 |
| `gradle/libs.versions.toml` | 精简后的 version catalog，只保留上述模块与 convention 插件所需条目 |

## 消费方式

### 1. Convention plugins（`includeBuild`）

在消费方工程的 `settings.gradle.kts` 中：

```kotlin
pluginManagement {
    includeBuild("build-logic") // 或以 submodule/composite build 方式指向本仓库的 build-logic
}
```

然后在模块里按 id 应用，例如：

```kotlin
plugins {
    alias(libs.plugins.buildkit.android.library)
    alias(libs.plugins.buildkit.hilt)
}
```

插件 id 一览：`buildkit.android.application`、`buildkit.android.application.compose`、`buildkit.android.application.flavors`、`buildkit.android.application.jacoco`、`buildkit.android.library`、`buildkit.android.library.compose`、`buildkit.android.library.jacoco`、`buildkit.android.feature.api`、`buildkit.android.feature.impl`、`buildkit.android.test`、`buildkit.android.room`、`buildkit.android.lint`、`buildkit.hilt`、`buildkit.jvm.library`、`buildkit.root`。

### 2. 自定义 Lint（`lintChecks` 依赖）

`:lint` 产物是一个带 `META-INF/services` 注册的 Lint jar。消费方 Android 模块：

```kotlin
dependencies {
    lintChecks("<lint 模块的坐标或 project 依赖>")
}
```

### 3. `ui-test-hilt-manifest`

消费方 androidTest 通过 manifest placeholder / 依赖该模块获得 `com.z1nt.buildkit.uitesthiltmanifest.HiltComponentActivity`，配合 Hilt 测试规则启动一个可注入的 `ComponentActivity`。

## 与上游的差异（裁剪说明）

- 删除了 Firebase 相关 convention plugin（`AndroidApplicationFirebaseConventionPlugin`）及 Crashlytics/Perf/gms 依赖。
- 移除了 `com.dropbox.dependency-guard` 的接入。
- `DesignSystemDetector` 中的推荐组件名改为 `Buildkit*` 前缀（检测逻辑保留，作为自定义 Lint 样例）。
- `benchmarks/` 不接入构建，仅保留源码作参考。

## 构建与验证

需要 JDK 17+ 与 Android SDK（`local.properties` 中的 `sdk.dir`）。

```bash
./gradlew :lint:test                              # lint 单测
./gradlew -p build-logic :convention:build        # convention plugins 构建（included build）
./gradlew :ui-test-hilt-manifest:assembleDebug    # Android library 组装
```
