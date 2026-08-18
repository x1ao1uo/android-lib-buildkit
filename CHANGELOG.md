# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- 从 [android/nowinandroid](https://github.com/android/nowinandroid)（Apache-2.0）抽取构建/质量工具链，建成独立可构建的 Gradle 工程：build-logic convention plugins（插件 id 前缀 `buildkit.*`）、`:lint` 自定义 Lint 检查（`BuildkitIssueRegistry`）、`:ui-test-hilt-manifest`（`HiltComponentActivity`）、spotless 版权头配置，以及仅供参考、未接入构建的 `benchmarks/` 源码。
