# android-lib-buildkit

Android 构建/质量工具链 monorepo：`build-logic` convention plugins（插件 id 前缀 `buildkit.*`，派生自 Now in Android，Apache-2.0，见 `LICENSE`/`NOTICE`）、`:lint` 自定义 Lint 检查（`BuildkitIssueRegistry`）、`:ui-test-hilt-manifest`、spotless 版权头模板（`spotless/`）。插件与开关的完整说明见 `README.md`。

## 消费方式

- `build-logic` 经 `pluginManagement.includeBuild` 被多个仓库消费（android-lib-photo-picker、android-lib-photo-viewer、android-lib-updater 等以绝对路径指向本仓库的 `build-logic`，见各仓库 `settings.gradle.kts`）。
- 仅本地 composite build 消费，**不发布到任何远程仓库**；不要添加远程 publishing repository。
- 消费方 version catalog 的 `[versions]` 段可用 `compileSdk` / `minSdk` / `targetSdk` 覆盖 SDK 级别，键缺失时回退内置缺省值（37 / 24 / 37）；生态基线要求消费方覆盖为 compileSdk=37、targetSdk=37、minSdk=24。

## 工具链基线

- Gradle 9.7.1（wrapper 钉死，含 `distributionSha256Sum`）；JDK 17+（`settings.gradle.kts` 有硬性校验）
- AGP 9.3.1、Kotlin 2.4.10、KSP 2.3.11、ktlint 1.8.0、Spotless 8.10.0

## 验证

```bash
./gradlew build   # CI 使用的完整门禁：spotlessCheck、:lint 单测、:ui-test-hilt-manifest 组装
```

CI 在 `.github/workflows/ci.yml`：本仓库无外部绝对路径 includeBuild 依赖且 `settings.gradle.kts` 通过 `pluginManagement.includeBuild("build-logic")` 包含本地复合构建，build job 必须跑在 self-hosted runner（依赖绝对路径本地 source tree），JDK 25；supply-chain 校验仍用 `ubuntu-latest`。

## 提交规范

- Conventional Commits（`feat:` / `fix:` / `build:` / `docs:` / `chore:` 等，破坏性变更加 `!`）。
- 变更需同步维护 `CHANGELOG.md` 的 `[Unreleased]` 段（Keep a Changelog 格式）。
- 源码版权头遵循 `spotless/` 模板；新增源码文件必须带版权头（spotlessCheck 强制）。
- 禁止提交密钥、Token、`local.properties` 与设备标识。
