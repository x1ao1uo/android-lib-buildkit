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

插件 id 一览：`buildkit.android.application`、`buildkit.android.application.compose`、`buildkit.android.application.firebase`、`buildkit.android.application.flavors`、`buildkit.android.application.jacoco`、`buildkit.android.library`、`buildkit.android.library.compose`、`buildkit.android.library.jacoco`、`buildkit.android.feature.api`、`buildkit.android.feature.impl`、`buildkit.android.test`、`buildkit.android.room`、`buildkit.android.lint`、`buildkit.hilt`、`buildkit.jvm.library`、`buildkit.root`。

## 消费方配置

### Version catalog 键（均可选）

Convention 插件从消费方 `gradle/libs.versions.toml` 的 `[versions]` 段读取 SDK 版本；键缺失时回退到内置默认值，不报错：

| 键 | 用途 | 缺省值 |
|---|---|---|
| `compileSdk` | `compileSdk` | 36 |
| `minSdk` | `defaultConfig.minSdk` | 23 |
| `targetSdk` | `defaultConfig.targetSdk`、`lint.targetSdk`、`testOptions.targetSdk` | 36 |

`buildkit.android.application.firebase` 额外要求消费方 catalog 提供 `firebase-bom`、`firebase-performance`、`firebase-crashlytics` 三个 library 键，且 `com.google.gms.google-services` / `com.google.firebase.firebase-perf` / `com.google.firebase.crashlytics` 插件在消费方 classpath 可用（`com.google.gms.google-services` 仅当模块存在 `google-services.json` 时才会被 apply）。

### Gradle property 开关

| Property | 作用 |
|---|---|
| `buildkit.resourcePrefix` | 覆盖 library 模块的 `resourcePrefix`；缺省按模块路径派生（如 `:core:module1` → `core_module1_`）。显式设为 `off` / `false` / 空字符串时完全不设置 `resourcePrefix`（不强制资源前缀检查），适合资源不在模块目录内的项目 |
| `buildkit.flavors` | 设为 `false`（不区分大小写）时跳过 `buildkit.android.library` 的 flavor 注入（`contentType` dimension + `demo`/`prod`）；缺省保持注入 |
| `buildkit.jacoco.extraExclusions` | 追加 Jacoco 覆盖率排除规则，逗号分隔的 class glob（并入内置的 Android/Dagger/Hilt 生成类排除列表） |
| `buildkit.jvmToolchain` | 设为整数（如 `25`）时给所有 Kotlin Android/JVM 模块钉 `jvmToolchain`；缺省不设置（跟随 Gradle daemon JVM） |
| `buildkit.spotless.recursive` | 设为 `true` 时 `buildkit.root` 的 spotless 改为根级递归 target（`**/src/**/*.kt`、`**/*.kts`、`**/src/**/*.xml`），适合源码外挂在根目录 `core/`、`feature/` 树、模块目录内无源码的项目；缺省保持 NIA build-logic 布局 |

无 flavors、资源不在模块目录的项目（如 android-mkaf）在 `gradle.properties` 中：

```properties
buildkit.flavors=false
buildkit.resourcePrefix=off
buildkit.spotless.recursive=true
```

注意：关闭 flavors 后不要再 apply `buildkit.android.application.flavors`，也不要引用 `BuildType`/`Flavor` 相关的 demo/prod sourceSet。

### Robolectric 目录约定

当消费方根工程存在 `gradle/robolectric/` 目录时，application/library convention 插件会把它挂为 `test` sourceSet 的 resources srcDir（用于 Robolectric 资源/shadow 覆盖）；目录不存在则完全跳过。

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

- Firebase convention plugin（`buildkit.android.application.firebase`）按改进版恢复：仅当模块存在 `google-services.json` 才 apply `com.google.gms.google-services`，且不含 analytics 依赖。
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
