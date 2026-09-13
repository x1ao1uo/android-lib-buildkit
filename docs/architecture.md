# Architecture

`android-lib-buildkit` 是 Android 库/应用项目的构建工具底座,通过 composite build 暴露 `buildkit.*` 约定插件,统一版本目录、签名、SDK 配置、Spotless 等。

## Top-level layout

- `build-logic/` — 约定插件(`buildkit.android.library`、`buildkit.android.application`、`buildkit.android.compose` 等)的复合构建源;其他仓库通过 `includeBuild("/.../android-lib-buildkit/build-logic")` 引入。
- `build/` — 本地构建产物。
- `lint/` — 自定义 lint 检查与规则。
- `spotless/` — Spotless 模板与版权头。
- `compose_compiler_config.conf` — Compose 编译器参数。
- `ui-test-hilt-manifest` — Hilt UI 测试用的 manifest 工具。
- `gradle/libs.versions.toml` — 全仓版本目录(第三方依赖统一来源)。
- `settings.gradle.kts` — `build-logic` 包含与仓库声明。
- `NOTICE` — 第三方致谢。
