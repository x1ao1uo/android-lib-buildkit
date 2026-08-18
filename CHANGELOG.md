# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- 从 [android/nowinandroid](https://github.com/android/nowinandroid)（Apache-2.0）抽取构建/质量工具链，建成独立可构建的 Gradle 工程：build-logic convention plugins（插件 id 前缀 `buildkit.*`）、`:lint` 自定义 Lint 检查（`BuildkitIssueRegistry`）、`:ui-test-hilt-manifest`（`HiltComponentActivity`）、spotless 版权头配置，以及仅供参考、未接入构建的 `benchmarks/` 源码。
- `buildkit.android.application.firebase` convention 插件：仅当模块存在 `google-services.json` 时 apply `com.google.gms.google-services`，依赖 Firebase BoM + Performance + Crashlytics（不含 analytics），并默认关闭 Crashlytics mapping 文件上传。
- `HttpsUrlValueSource`：通用 Gradle `ValueSource`，校验 URL 必须为 HTTPS（可通过 `allowedHttpUrl` 放行单个例外）。
- `compileSdk` / `minSdk` / `targetSdk` 可从消费方 version catalog 的 `[versions]` 段读取，键缺失时回退到内置缺省值（36 / 23 / 36）。
- Gradle property 开关：`buildkit.resourcePrefix`（覆盖 library `resourcePrefix`）、`buildkit.jacoco.extraExclusions`（逗号分隔的 Jacoco 覆盖率排除 glob）。
- 新增 opt-out 开关：`buildkit.flavors=false` 时 `buildkit.android.library` 跳过 flavor 注入（`contentType` dimension + `demo`/`prod`）；`buildkit.resourcePrefix` 显式设为 `off`/`false`/空字符串时完全不设置 `resourcePrefix`。便于 android-mkaf 这类无 flavors、资源不在模块目录的项目消费 library 插件。
- Robolectric 目录约定：消费方根工程存在 `gradle/robolectric/` 时自动挂为 unit test resources srcDir。
- Jacoco 覆盖率排除新增通用 Dagger/Hilt 生成类规则（`HiltWrapper_*`、`Dagger*`、`*_Factory*`、`*_MembersInjector*`、`*Module_*Factory*`、`*_ComponentTreeDeps*`、`*_Impl*`、`*_GeneratedInjector*`、`_com_*`、`ComposableSingletons*`）；合并覆盖率报告任务现在 `dependsOn("test*UnitTest")`。
- Library 模块 unit tests 增加 `--enable-native-access=ALL-UNNAMED` 与 `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` jvmArgs。

### Changed

- Java 编译目标从 11 升级到 17（`sourceCompatibility` / `targetCompatibility` / `JvmTarget`，含 JVM library 约定）。
- Room 升级到 3.0.1 线：插件 id `androidx.room3`，依赖改为 `api(room3-runtime)` + `ksp(room3-compiler)`，移除 `room-ktx`。
- Lint 报告关闭 SARIF 输出（`sarifReport = false`）。
