# Usage

## Prerequisites

- JDK 26
- Android SDK 37,NDK 29.0.14206865
- Gradle Wrapper 9.x

## Build

本仓库只产出约定插件与构建工具,直接消费它的兄弟仓库通过 `includeBuild` 引入:

```bash
./gradlew :build-logic:assemble
```

构建消费方的示例(在 `android-lib-photo-picker`、`android-lib-updater` 等):

```bash
cd ../android-lib-photo-picker
./gradlew :library:assembleRelease
```

## Test

```bash
./gradlew test
```

## Lint

```bash
./gradlew lint
```

## Tooling

- 版本目录:`gradle/libs.versions.toml`(全仓版本单一来源)。
- Spotless:`./gradlew spotlessCheck` / `spotlessApply`。
- 本地 SDK 路径:`local.properties`(`sdk.dir=...`);不入库。

更多见 [docs/architecture.md](architecture.md)。
