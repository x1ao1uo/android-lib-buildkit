# android-lib-buildkit — Code Wiki

> 自动生成于 2026-09-07,基于仓库快照;运行命令均以项目根目录为 cwd。

## 1. 项目概览

android-lib-buildkit 是一套**面向 Android monorepo 的 Gradle 构建 / 质量工具链**,从 Google 的 [Now in Android](https://github.com/android/nowinandroid) 派生并适配:

- **包名空间** 由 `com.google.samples.apps.nowinandroid` 全局替换为 `com.z1nt.buildkit`,Gradle 插件 id 前缀由 `nowinandroid.*` 改为 `buildkit.*`,整体沿用 Apache-2.0 许可 (`LICENSE` / `NOTICE`)。
- **核心交付物** 是 [`build-logic/convention`](build-logic) 下的 16 个 Convention Plugin,以及 `:lint` 自定义 Lint 检查、`:ui-test-hilt-manifest` 测试 Hilt Activity、`spotless/` 版权头模板。
- **消费方式** 为 composite build —— 消费方通过 `pluginManagement { includeBuild("build-logic") }` 把本仓库作为复合构建引入,然后在模块里按 id `alias(libs.plugins.buildkit.android.library)` 等方式应用。
- **技术栈**:Gradle Wrapper 9.7.1(含 `distributionSha256Sum` 校验)、JDK 17+(CI 锁 25)、AGP 9.3.2(androidTools 32.3.2)、Kotlin 2.4.10、KSP 2.3.11、Hilt 2.60.1、Room 3.0.1、Spotless 8.10.0、ktlint 1.8.0、Jacoco 0.8.15、Firebase BoM / Crashlytics / Performance。
- **仓库入口**:`settings.gradle.kts` 包含 `:lint` 与 `:ui-test-hilt-manifest`,并通过 `pluginManagement.includeBuild("build-logic")` 把 `build-logic` 作为复合构建加入。

## 2. 整体架构

仓库是**多 Gradle 工程的复合构建**,整体结构如下:

```
android-lib-buildkit/                            # 根 Gradle 工程
├── settings.gradle.kts                          # includeBuild("build-logic"),include :lint/:ui-test-hilt-manifest
├── build.gradle.kts                             # 声明顶层 apply false 插件 + apply buildkit.root
├── gradle/libs.versions.toml                    # 单一 version catalog
├── build-logic/                                 # included build(独立 settings.gradle.kts)
│   └── convention/                              # kotlin-dsl 项目,导出 16 个 buildkit.* 插件 id
│       └── src/main/kotlin/...                  # 共享工具函数 + 16 个 ConventionPlugin 类
├── lint/                                        # JVM library,产出 lintChecks jar
│   └── src/main/kotlin/...                      # BuildkitIssueRegistry + 2 个 Detector
├── ui-test-hilt-manifest/                       # Android library,提供 HiltComponentActivity
└── spotless/                                    # copyright.kt / .kts / .xml 模板
```

- **复合构建数据流**:消费方 `settings.gradle.kts` 触发 `includeBuild("build-logic")`,Gradle 把 `build-logic` 视为独立 root,通过 `dependencyResolutionManagement.versionCatalogs` 共享根工程的 `libs.versions.toml`;消费方应用 `alias(libs.plugins.buildkit.*)` 时,Gradle 在 `build-logic/convention/build.gradle.kts` 的 `gradlePlugin { plugins { register(...) } }` 中找到对应实现类并实例化。
- **进程 / 线程模型**:Convention Plugin 在 Gradle 的**配置阶段**串行执行;Lint Detector 在 lint 任务里对源文件并行扫描;Jacoco 报告任务(`create{variant}CombinedCoverageReport`)消费 unit + android 测试的 exec/ec 文件生成 XML/HTML 报告,本身是 Gradle 任务图中的标准节点。
- **配置可见性**:`build-logic` 是独立 included build,Gradle 不会自动把根工程的 `gradle.properties` 透传给它,因此 `build-logic/gradle.properties` 独立维护并行/缓存/配置缓存开关。

## 3. 主要模块

### 3.1 根工程 `android-lib-buildkit/`

- **路径**:`/Volumes/LVLIAN_1T/code/android-lib-buildkit`
- **职责**:作为复合构建宿主,声明顶级 `apply false` 插件以统一 classpath,并通过 `buildkit.root` 在根工程统一注册 Spotless 版权头;为消费方提供 `gradle/libs.versions.toml` 作为共享 version catalog。
- **关键文件与类/函数**:
  - `settings.gradle.kts` —— `pluginManagement.includeBuild("build-logic")`、`include(":lint")` / `include(":ui-test-hilt-manifest")`、`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`、`check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_17))`。
  - `build.gradle.kts` —— `plugins { alias(libs.plugins.*) apply false; alias(libs.plugins.buildkit.root) }`。
  - `gradle/libs.versions.toml` —— `[versions]` / `[libraries]` / `[plugins]` 三段,后两段末尾的 `# Plugins defined by this project (build-logic/convention)` 列出所有 `buildkit-*` 插件 id(无 `version.ref`,只声明 id)。
  - `gradle.properties` —— `org.gradle.configuration-cache=true`、`org.gradle.caching=true`、`ksp.project.isolation.enabled=true` 等构建期开关。
- **依赖关系**:被消费方以 includeBuild / composite build 方式引用;反向依赖 `:lint` / `:ui-test-hilt-manifest` / `build-logic`(通过 `pluginManagement`)。

### 3.2 `build-logic` (included build)

- **路径**:`/Volumes/LVLIAN_1T/code/android-lib-buildkit/build-logic`(独立 `settings.gradle.kts`、`gradle.properties`),实现入口为 `build-logic/convention/build.gradle.kts`。
- **职责**:作为复合构建里的"插件工厂",注册 16 个 `buildkit.*` 插件 id,并提供共享工具(`com.z1nt.buildkit.*` package),供消费方 Gradle 脚本复用。
- **关键类与函数**(目录 `build-logic/convention/src/main/kotlin/com/z1nt/buildkit/`):
  - `AndroidCompose.kt` —— `Project.configureAndroidCompose(CommonExtension)`:启用 `buildFeatures.compose`、引入 Compose BOM、可选 metrics/reports、只在 `compose_compiler_config.conf` 存在时挂稳定性配置。
  - `AndroidInstrumentedTests.kt` —— `LibraryAndroidComponentsExtension.disableUnnecessaryAndroidTests(Project)`:仅当 `src/androidTest` 目录存在时启用 instrumented test。
  - `Badging.kt` —— `GenerateBadgingTask`(调 aapt2 dump badging)、`CheckBadgingTask`(与 golden 文件 diff)、`Project.configureBadgingTasks(ApplicationAndroidComponentsExtension)`。
  - `BuildkitBuildType.kt` —— `enum class BuildkitBuildType { DEBUG(".debug"), RELEASE }`,application 与 benchmarks 共享的 build type。
  - `BuildkitFlavor.kt` —— `enum class FlavorDimension { contentType }`、`enum class BuildkitFlavor`、`configureFlavors(CommonExtension, ...)`。
  - `GradleManagedDevices.kt` —— `configureGradleManagedDevices(CommonExtension)`:注册 Pixel 4 / Pixel 6 / Pixel C,以及精简的 `ci` 设备组。
  - `Graph.kt` —— `Project.configureGraphTasks()`、`GraphDumpTask`、`GraphUpdateTask`,把工程依赖渲染成 mermaid 图并写入 `README.md`。
  - `HttpsUrlValueSource.kt` —— `abstract class HttpsUrlValueSource : ValueSource<String, Parameters>`,通用 HTTPS URL 校验,可放行单个 `allowedHttpUrl` 例外。
  - `Jacoco.kt` —— `Project.configureJacoco(CommonExtension, AndroidComponentsExtension)`:内置 `coverageExclusions` + `buildkit.jacoco.extraExclusions` 合并,生成 `create{Variant}CombinedCoverageReport`。
  - `KotlinAndroid.kt` —— `configureKotlinAndroid`(设 `compileSdk` / `minSdk` 缺省 37 / 24)、`configureKotlinJvm`、`Project.findVersionOrDefault("compileSdk" / "minSdk", 37 / 24)`;`AndroidLibraryConventionPlugin` 再单独 `findVersionOrDefault("targetSdk", 37)` 同步到 `testOptions.targetSdk` / `lint.targetSdk`;统一 JVM 17 字节码、`-opt-in=ExperimentalCoroutinesApi`、`-Xconsistent-data-class-copy-visibility`、`-Pbuildkit.jvmToolchain` 可选钉 toolchain;`compileSdk` 默认 37、`minSdk` 默认 24(application 模块才设 `defaultConfig.targetSdk`,library 模块由 `AndroidLibraryConventionPlugin` 同步 `testOptions` / `lint`)。
  - `PrintTestApks.kt` —— `Project.configurePrintApksTask(...)` 与 `PrintApkLocationTask`:为含 androidTest 的 variant 注册打印 APK 路径的任务。
  - `ProjectExtensions.kt` —— `val Project.libs`(访问消费方 `libs` catalog)与 `Project.findVersionOrDefault`。
  - `Spotless.kt` —— `Project.configureSpotlessForAndroid / ForJvm / ForRootProject`;根工程依据 `-Pbuildkit.spotless.recursive` 切换递归 vs NIA 布局;`configureSpotlessCommon` 提供 `kotlin`(ktlint 1.8 `android_studio` code style + 版权头)、`kts`(版权头);Android 多一项 `xml`(找首个非注释/声明 tag,锚定 `copyright.xml`)。
- **16 个 Convention Plugin**(`build-logic/convention/src/main/kotlin/` 下实际存在 16 个 `*ConventionPlugin.kt` / `RootPlugin.kt`,`convention/build.gradle.kts` 的 `gradlePlugin.register` 也只有 16 项;§1/§3.2 标题已据此更正):
  - `RootPlugin` —— 只能 apply 在根 project,启用 `configureGraphTasks`(Isolated Projects 下跳过)与 `configureSpotlessForRootProject`。
  - `AndroidApplicationConventionPlugin` —— 链 `com.android.application` + `buildkit.android.lint`,配置 SDK / 测试动画 / Robolectric 资源目录 / Managed Devices / `printApksTask` / `badgingTasks` / Spotless。
  - `AndroidApplicationComposeConventionPlugin` —— 链 `com.android.application` + `org.jetbrains.kotlin.plugin.compose`。
  - `AndroidApplicationFirebaseConventionPlugin` —— 链 `com.google.gms.google-services`(仅当 `google-services.json` 存在)+ Firebase Perf + Crashlytics,排除 protobuf-javalite / protolite-well-known-types,关闭 Crashlytics mapping 上传。
  - `AndroidApplicationFlavorsConventionPlugin` —— 在 application 上注册 `contentType` 维度 + demo/prod flavor。
  - `AndroidApplicationJacocoConventionPlugin` —— 套用 Jacoco + `configureJacoco(...)`。
  - `AndroidLibraryConventionPlugin` —— 链 `com.android.library` + `buildkit.android.lint`,按模块路径派生 `resourcePrefix`(可被 `-Pbuildkit.resourcePrefix` 覆盖 / 关闭)、同步 `testOptions.targetSdk = lint.targetSdk`、注入 native access JVM args、视 `-Pbuildkit.flavors=false` 决定是否注入 `contentType` dimension、默认依赖 `androidx.tracing.ktx`。
  - `AndroidLibraryComposeConventionPlugin` —— library + Compose 编译器插件。
  - `AndroidLibraryJacocoConventionPlugin` —— library + Jacoco。
  - `AndroidLintConventionPlugin` —— 按宿主插件类型走 `ApplicationExtension` / `LibraryExtension`,统一打开 XML 报告、关闭 SARIF、关闭 `GradleDependency`。
  - `AndroidRoomConventionPlugin` —— 套用 `androidx.room3` + KSP,`room.generateKotlin=true`,schema 写入 `$projectDir/schemas`,依赖 `room-runtime`(api)+ `room-compiler`(ksp)。
  - `AndroidTestConventionPlugin` —— 套用 `com.android.test`,`targetSdk=37`。
  - `AndroidFeatureApiConventionPlugin` —— `buildkit.android.library` + `kotlin.serialization`,`api(project(":core:navigation"))`。
  - `AndroidFeatureImplConventionPlugin` —— `buildkit.android.library` + `buildkit.hilt`,引入 `:core:ui` / `:core:designsystem` 与 lifecycle / navigation3 / tracing 等。
  - `HiltConventionPlugin` —— KSP + `hilt.compiler` + `kotlin-metadata`;纯 JVM 时只引入 `hilt-core`,Android 额外应用 `dagger.hilt.android.plugin` + `hilt-android`。
  - `JvmLibraryConventionPlugin` —— Kotlin JVM + `buildkit.android.lint` + JVM Spotless + `kotlin.test`。
- **依赖关系**:消费方以 `pluginManagement.includeBuild` 拉入;`convention/build.gradle.kts` 通过 `compileOnly(...)` 拿到 `com.android.tools.build:gradle-api` / `compose-compiler-gradle-plugin` / `kotlin-gradle-plugin` / `ksp` / `room` / `spotless` / Firebase 插件以使用其 API;`implementation(libs.truth)` 仅用于 Badging 测试断言;`lintChecks(libs.androidx.lint.gradle)` 自检。

### 3.3 `:lint` 自定义 Lint 检查

- **路径**:`/Volumes/LVLIAN_1T/code/android-lib-buildkit/lint`(JVM library,`group = "com.z1nt"`,`version = "0.1.0"`,`archivesName = "android-lib-buildkit-lint"`,JDK 17 字节码)。
- **职责**:把 buildkit 的自定义 Lint 规则打成可被 `lintChecks(...)` 消费的 jar;`META-INF/services/com.android.tools.lint.client.api.IssueRegistry` 注册入口 `com.z1nt.buildkit.lint.BuildkitIssueRegistry`。
- **关键类与函数**:
  - `BuildkitIssueRegistry` —— 暴露 `DesignSystemDetector.ISSUE`、`TestMethodNameDetector.FORMAT`、`TestMethodNameDetector.PREFIX`,`api = CURRENT_API`,`minApi = 12`。
  - `DesignSystemDetector`(目录 `lint/.../designsystem/`)—— `UastScanner`,检查调用与限定引用,把 Compose Material 的 `MaterialTheme` / `Button` / `NavigationBar` / `TopAppBar` 等映射到 `Buildkit*` 等价物;`Icons` 限定引用映射到 `BuildkitIcons`。
  - `TestMethodNameDetector` —— `SourceCodeScanner`,检测 `@Test` 方法名:`PREFIX` 警告并提供 `autoFix` 去掉 `test[\s_]*` 前缀;`FORMAT` 在 `androidTest` 源码路径下校验方法名匹配 `given_when_then` / `when_then` 格式。
  - 测试:`TestMethodNameDetectorTest` 用 `TestLintTask.lint()` 覆盖两种 issue 的正反例;`DesignSystemDetectorTest` 同理。
- **依赖关系**:`compileOnly(libs.kotlin.stdlib)`、`compileOnly(libs.lint.api)`;`testImplementation(libs.kotlin.test / lint.checks / lint.tests)`。被消费方在 Android 模块用 `lintChecks("<该模块坐标>")` 引入。

### 3.4 `:ui-test-hilt-manifest`

- **路径**:`/Volumes/LVLIAN_1T/code/android-lib-buildkit/ui-test-hilt-manifest`,`namespace = "com.z1nt.buildkit.uitesthiltmanifest"`,由 `buildkit.android.library` + `buildkit.hilt` 构成。
- **职责**:在 androidTest 下提供一个 `@AndroidEntryPoint` 的 `ComponentActivity`,绕过 [google/dagger#3394](https://github.com/google/dagger/issues/3394)(Hilt 测试在 `ComponentActivity` 上注入受限)。
- **关键类与文件**:
  - `HiltComponentActivity`(`@AndroidEntryPoint class HiltComponentActivity : ComponentActivity()`)。
  - `AndroidManifest.xml` —— 注册该 Activity,主题 `@android:style/Theme.Material.Light.NoActionBar`,与生产 app 基线一致,避免 action bar 与测试控件重叠。
- **依赖关系**:仅依赖 Hilt(`buildkit.hilt` 透传 `hilt-android` / KSP),不引入业务依赖。

### 3.5 `spotless/` 版权头模板

- **路径**:`/Volumes/LVLIAN_1T/code/android-lib-buildkit/spotless`。
- **职责**:为 Spotless 在 Kotlin / Kotlin Gradle Script / XML 三类文件中提供版权头插入模板。
- **关键文件**:`copyright.kt`(Kotlin 文件版权头)、`copyright.kts`(Gradle Kotlin DSL 版权头,锚点 `^(?![\\/ ]\\*).*$`)、`copyright.xml`(XML 版权头,锚点 `(<[^!?])`,匹配首个非注释 / 声明 tag)。
- **依赖关系**:被 `build-logic/convention/src/main/kotlin/com/z1nt/buildkit/Spotless.kt` 通过 `rootDir.resolve("spotless/copyright.*")` 引用。

## 4. 关键类与函数

- **插件注册表**:`build-logic/convention/build.gradle.kts` 的 `gradlePlugin { plugins { register(...) } }`,把 16 个 `buildkit.*` id 映射到 `com.z1nt.buildkit.*` 实现类。
- **协议 / 契约**:
  - `BuildkitFlavor(contentType, demo/prod, applicationIdSuffix)` —— flavor 注册契约。
  - `BuildkitBuildType(DEBUG, RELEASE)` —— build type 契约,applicationIdSuffix = `.debug`。
  - `PluginType`(`AndroidApplication / AndroidFeature / AndroidLibrary / AndroidTest / Jvm / Unknown`)—— Graph mermaid 节点着色契约。
- **状态机 / 任务**:
  - `GenerateBadgingTask` → `update{Variant}Badging`(Copy)→ `check{Variant}Badging`(Truth 断言)。
  - `GraphDumpTask` → `GraphUpdateTask`(把 mermaid 写回 `README.md` 的 `<!--region graph-->` 块)。
  - `create{Variant}CombinedCoverageReport`(JacocoReport,`dependsOn("test{Variant}UnitTest")`)。
  - `print{Variant}TestApk`(仅当存在非 `build/generated` 的 androidTest 源)。
  - `configurePrintApksTask` / `configureBadgingTasks` 都是对 `onVariants` 回调的封装。
- **常量 / 枚举 / 默认值**:
  - SDK 默认值(`build-logic/convention/src/main/kotlin/com/z1nt/buildkit/KotlinAndroid.kt`):`compileSdk=37`、`minSdk=24`、`targetSdk=37`;`Jacoco.kt` 内置覆盖排除 `R.class / BuildConfig / Hilt_* / Dagger* / *_Factory* / *_MembersInjector* / *_Impl* / ComposableSingletons*` 等;`Badging.kt` 用 `Locale.getDefault()` 不感知大小写;`HttpsUrlValueSource` 强制 HTTPS,可白名单单条 URL。
  - `Project.findVersionOrDefault("compileSdk" / "minSdk" / "targetSdk", 37 / 24 / 37)` 让消费方 `[versions]` 段可覆盖。
- **Lint 入口 / Issue**:
  - `BuildkitIssueRegistry`(META-INF/services 注册)。
  - `DesignSystemDetector.ISSUE(id = "DesignSystem")`、`TestMethodNameDetector.PREFIX(id = "TestMethodPrefix")`、`TestMethodNameDetector.FORMAT(id = "TestMethodFormat")`。

## 5. 依赖关系

直接版本约束来自 `gradle/libs.versions.toml`:

- **构建工具**:
  - `androidGradlePlugin = "9.3.2"`(`com.android.tools.build:gradle-api` 等)、`androidTools = "32.3.2"`(`lint-api` / `lint-checks` / `lint-tests`)。
  - `kotlin = "2.4.10"`(`kotlin-stdlib-jdk8` / `kotlin-test` / `kotlin-metadata-jvm` / `kotlin-gradle-plugin` / `compose-compiler-gradle-plugin`)。
  - `ksp = "2.3.11"`、`room = "3.0.1"`、`hilt = "2.60.1"`、`spotless = "8.10.0"`、`ktlint = "1.8.0"`、`jacoco = "0.8.15"`、`truth = "1.4.5"`、`junit4 = "4.13.2"`、`androidDesugarJdkLibs = "2.1.5"`。
  - Firebase:`firebaseCrashlyticsPlugin = "3.0.8"`、`firebasePerfPlugin = "2.0.2"`、`gmsPlugin = "4.5.0"`。
- **Compose / AndroidX**:`androidxComposeBom = "2026.08.00"`(`androidx.compose:compose-bom`,稳定通道)、`androidxHiltLifecycleViewModelCompose = "1.4.0"`、`androidxLifecycle = "2.11.0"`、`androidxLintGradle = "1.0.0"`、`androidxNavigation3 = "1.1.6"`、`androidxTracing = "2.0.0"`。
- **平台 / 系统依赖**:JDK 17+(`settings.gradle.kts` 硬性校验,CI 锁 JDK 25),Android SDK(由消费方 `local.properties` 提供,`local.properties` 不入版本控制);本仓库自身无 SDK 路径写入。
- **插件 id**:`com.android.application / .library / .lint / .test`、`org.jetbrains.kotlin.plugin.compose`、`com.google.dagger.hilt.android`、`org.jetbrains.kotlin.jvm`、`org.jetbrains.kotlin.plugin.serialization`、`com.google.devtools.ksp`、`androidx.room3`、`com.diffplug.spotless`、`com.google.gms.google-services`、`com.google.firebase.firebase-perf`、`com.google.firebase.crashlytics`、`dagger.hilt.android.plugin`。
- **本仓库自研插件 id**:`buildkit.android.application / .application.compose / .application.firebase / .application.flavors / .application.jacoco / .library / .library.compose / .library.jacoco / .feature.api / .feature.impl / .test / .room / .lint`、`buildkit.hilt`、`buildkit.jvm.library`、`buildkit.root`(无 `version.ref`,只声明 id)。

## 6. 项目运行方式

> 以下命令直接来自本仓库的 `README.md` / `CLAUDE.md` / `.github/workflows/ci.yml`,未经仓库验证的命令未列出。

- **构建门禁(本地 / CI)**:`./gradlew build`(覆盖 spotlessCheck、`:lint` 单测、`:ui-test-hilt-manifest` 组装)。
- **:lint 单测**:`./gradlew :lint:test`。
- **convention plugin 构建(included build)**:`./gradlew -p build-logic :convention:build`。
- **:ui-test-hilt-manifest 组装**:`./gradlew :ui-test-hilt-manifest:assembleDebug`。
- **CI**:GitHub Actions `.github/workflows/ci.yml`
  - `supply-chain` job(`ubuntu-latest`,10 分钟):`actions/checkout@v7.0.1` + `gradle/actions/wrapper-validation@v6.3.0`。
  - `build` job(`[self-hosted]`,30 分钟):`actions/setup-java@v5.7.0`(Temurin,JDK 25)+ `android-actions/setup-android@v4.0.1` + `gradle/actions/setup-gradle@v6.3.0` → `./gradlew build --stacktrace`,`always()` 上传 `**/build/reports/**`。
- **依赖自动更新**:`.github/dependabot.yml` —— `gradle` 与 `github-actions` 生态每周一 04:00(Asia/Shanghai)开 PR,group 聚合 minor/patch,major `org.jetbrains.kotlin*` 忽略(需要联动 Compose / KSP)。
- **关键配置**:
  - `local.properties` 提供 `sdk.dir`(不入版本控制)。
  - 消费方可通过 `buildkit.resourcePrefix` / `buildkit.flavors` / `buildkit.jacoco.extraExclusions` / `buildkit.jvmToolchain` / `buildkit.spotless.recursive` 覆盖默认行为(详见 README §"Gradle property 开关")。
  - 消费方 `gradle/libs.versions.toml` 的 `[versions]` 段中 `compileSdk` / `minSdk` / `targetSdk` 可覆盖默认值。

## 7. 约定与备注

- **命名规范**:
  - Java / Kotlin 包名:`com.z1nt.buildkit.*`;plugin id 前缀 `buildkit.*`;group `com.z1nt.buildkit.buildlogic`(convention)、`com.z1nt`(lint)。
  - Buildkit 自研 Design System 的 Compose 组件前缀:`Buildkit*`(由 `DesignSystemDetector.METHOD_NAMES / RECEIVER_NAMES` 维护)。
  - 测试方法命名:单元测试不应以 `test` 开头(`TestMethodNameDetector.PREFIX`),`androidTest` 必须遵循 `given_when_then` 或 `when_then`(`TestMethodNameDetector.FORMAT`)。
- **重要约束**:
  - Convention Plugin 默认 `compileSdk=37`、`minSdk=24`、`targetSdk=37`(代码实际值);`README.md` 的 "Version catalog 键表" 仍写 `minSdk` 缺省 `23`,与 `KotlinAndroid.kt` 实际 `findVersionOrDefault("minSdk", 24)` 不一致,代码侧为准。Java 字节码统一 17(`sourceCompatibility` / `targetCompatibility` / `JvmTarget` 全栈一致)。
  - `LibraryConventionPlugin` 把 `lint.targetSdk = testOptions.targetSdk = findVersionOrDefault("targetSdk", 37)` 同步,保证 lint 报告的目标 API 与运行时一致;单元测试固定追加 `--enable-native-access=ALL-UNNAMED` 与 `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED`。
  - `LibraryConventionPlugin` 的 `resourcePrefix` 默认从模块 Gradle 路径派生(`:` 替换为 `_`、小写),可通过 `-Pbuildkit.resourcePrefix` 覆盖;`off` / `false` / 空字符串时强制关闭。
  - `LibraryConventionPlugin` 在 `-Pbuildkit.flavors=false` 时跳过 `contentType` dimension + demo/prod 注入(便于 android-mkaf 这类无 flavor 项目)。
  - Firebase convention 仅在存在 `google-services.json` 时 apply `com.google.gms.google-services`,并默认禁用 Crashlytics mapping 上传。
  - `RootPlugin` 仅能 apply 在根 project(`require(target.path == ":")`),并会在 `Isolated Projects` 启用时跳过 `configureGraphTasks`。
  - `:lint` 是纯 Java 模块,无法依赖 `:core-designsystem` 的 composable 引用,因此 DesignSystemDetector 维护**硬编码**名称映射表;新增/改名 design system 组件必须同步更新该表。
  - `:lint` 自身在 lint-api 32.x 上构建,要求 JVM 17;`minApi = 12`、`api = CURRENT_API`。
- **已知风险 / 遗留事项**:
  - `CHANGELOG.md` "Unreleased" 已记录 JDK 25 / Gradle 9.7.1 / AGP 9.3.1 / Kotlin 2.4.10 等基线,以及 Room 3.0.1(`androidx.room3`)、Hilt 2.60.1、Compose BOM 切稳定通道(2026.08.00)等迁移;`gradle/libs.versions.toml` 当前值(AGP 9.3.2、androidTools 32.3.2)与 README/CHANGELOG 中描述的 AGP 9.3.1 存在偏差,可能尚未合入文档;CHANGELOG 内文也写"minSdk 缺省 23",但代码 `findVersionOrDefault("minSdk", 24)` 与 `KotlinAndroid.kt` 实际取值以 24 为准。
  - `benchmarks/` 目录在 README 中被标注为"仅作参考,未接入 `settings.gradle.kts`",且 CHANGELOG 的 Removed 段提到 `benchmarks/` 残留目录已删除,实际仓库根目录目前**不包含** `benchmarks/`,与 README 描述存在文档漂移。
  - `AndroidLibraryConventionPlugin` 在 `ConfigureLibraryExtension` 上读取 `findVersionOrDefault("targetSdk", 37)` 用于 `testOptions.targetSdk` 与 `lint.targetSdk`;消费方覆盖 `targetSdk` 时,这两个值会同步更新,但 `defaultConfig.targetSdk` 不在此处赋值(application 模块才设),library 模块需要消费方自行处理 `defaultConfig.targetSdk` 或保留 lint/test 自同步。
  - `Jacoco.kt` 的 `create{Variant}CombinedCoverageReport` 仅 `dependsOn("test{Variant}UnitTest")`,androidTest 数据需消费方在 CI 外部 device farm 提前准备好(注释明确说明)。
  - `gradle.properties` 同时启用 `org.gradle.configuration-cache=true` 与 `org.gradle.configuration-cache.parallel=true`,且 `org.gradle.configuration-cache.problems=warn`,存在不兼容任务时不会 fail。