# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- 对齐生态统一基线：Gradle wrapper 9.7.0 → 9.7.1（新增 `distributionSha256Sum=acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a`）；CI build job runner 由 `ubuntu-latest` 改为 `self-hosted`（与 `settings.gradle.kts` 本地 `pluginManagement.includeBuild("build-logic")` 一致 — 与 `android-lib-photo-picker` / `android-lib-photo-viewer` / `android-lib-updater` 同基线），JDK 25 不变；`buildkit.android.library` / `buildkit.android.application` 约定的 `minSdk` 缺省值 23 → 24（消费方覆盖机制不变，仍以消费方 `libs.versions.toml` 的 `[versions]` 段为准）。
- CI 统一 JDK 25（与消费方 android-xuandian2 的 `buildkit.jvmToolchain=25` 基线一致）；README 约定插件缺省值表更正为 compileSdk 37 / minSdk 23 / targetSdk 37（与代码 `findVersionOrDefault` 实际值一致）。

### Added

- GitHub Actions CI（`.github/workflows/ci.yml`）：supply-chain job 做 Gradle wrapper 校验，build job 跑 `./gradlew build`（含 spotlessCheck、lint、单元测试）并上传构建报告。
- 从 [android/nowinandroid](https://github.com/android/nowinandroid)（Apache-2.0）抽取构建/质量工具链，建成独立可构建的 Gradle 工程：build-logic convention plugins（插件 id 前缀 `buildkit.*`）、`:lint` 自定义 Lint 检查（`BuildkitIssueRegistry`）、`:ui-test-hilt-manifest`（`HiltComponentActivity`）、spotless 版权头配置，以及仅供参考、未接入构建的 `benchmarks/` 源码。
- `buildkit.android.application.firebase` convention 插件：仅当模块存在 `google-services.json` 时 apply `com.google.gms.google-services`，依赖 Firebase BoM + Performance + Crashlytics（不含 analytics），并默认关闭 Crashlytics mapping 文件上传。
- `HttpsUrlValueSource`：通用 Gradle `ValueSource`，校验 URL 必须为 HTTPS（可通过 `allowedHttpUrl` 放行单个例外）。
- `compileSdk` / `minSdk` / `targetSdk` 可从消费方 version catalog 的 `[versions]` 段读取，键缺失时回退到内置缺省值（36 / 23 / 36）。
- Gradle property 开关：`buildkit.resourcePrefix`（覆盖 library `resourcePrefix`）、`buildkit.jacoco.extraExclusions`（逗号分隔的 Jacoco 覆盖率排除 glob）。
- 新增 opt-out 开关：`buildkit.flavors=false` 时 `buildkit.android.library` 跳过 flavor 注入（`contentType` dimension + `demo`/`prod`）；`buildkit.resourcePrefix` 显式设为 `off`/`false`/空字符串时完全不设置 `resourcePrefix`。便于 android-mkaf 这类无 flavors、资源不在模块目录的项目消费 library 插件。
- `buildkit.jvmToolchain` property：为全部 Kotlin Android/JVM 模块统一钉 JDK toolchain（如 `25`），替代消费方根脚本里的 `subprojects { jvmToolchain(...) }` 补丁。
- `buildkit.spotless.recursive=true`：`buildkit.root` 的 spotless 切换为根级递归 target（`**/src/**/*.kt`、`**/*.kts`、`**/src/**/*.xml`），覆盖源码外挂在根级 `core/`、`feature/` 目录树的项目。
- Robolectric 目录约定：消费方根工程存在 `gradle/robolectric/` 时自动挂为 unit test resources srcDir。
- Jacoco 覆盖率排除新增通用 Dagger/Hilt 生成类规则（`HiltWrapper_*`、`Dagger*`、`*_Factory*`、`*_MembersInjector*`、`*Module_*Factory*`、`*_ComponentTreeDeps*`、`*_Impl*`、`*_GeneratedInjector*`、`_com_*`、`ComposableSingletons*`）；合并覆盖率报告任务现在 `dependsOn("test*UnitTest")`。
- Library 模块 unit tests 增加 `--enable-native-access=ALL-UNNAMED` 与 `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` jvmArgs。

### Changed

- 版本基线对齐生态统一基线：AGP 9.0.0 → 9.3.1（androidTools 32.0.0 → 32.3.1 联动）、Kotlin 2.3.0 → 2.4.10、KSP 2.3.4 → 2.3.11、Hilt 2.59 → 2.60.1、Spotless 8.3.0 → 8.10.0、ktlint 1.4.0 → 1.8.0；Gradle wrapper 9.4.0 → 9.7.0。
- Compose BOM 从 alpha 通道（`androidx.compose:compose-bom-alpha:2025.09.01`）切换到稳定通道 `androidx.compose:compose-bom:2026.08.00`。
- 内置缺省 SDK 从 36 升到 37（`compileSdk` 与 application/library 的 `targetSdk` 缺省值；`minSdk` 缺省 23 不变），消费方 catalog 的 `[versions]` 覆盖机制不变。

### Removed

- 删除未接入构建的 `benchmarks/` 残留目录（`settings.gradle.kts` 从未 include，且引用了 catalog 中不存在的 baselineprofile 插件）。

- Java 编译目标从 11 升级到 17（`sourceCompatibility` / `targetCompatibility` / `JvmTarget`，含 JVM library 约定）。
- Room 升级到 3.0.1 线：插件 id `androidx.room3`，依赖改为 `api(room3-runtime)` + `ksp(room3-compiler)`，移除 `room-ktx`。
- Lint 报告关闭 SARIF 输出（`sarifReport = false`）。
- Compose 稳定性配置文件（消费方根目录 `compose_compiler_config.conf`）改为仅当文件存在时才注入 `stabilityConfigurationFiles`，消费方不再需要全局清空的 workaround。
