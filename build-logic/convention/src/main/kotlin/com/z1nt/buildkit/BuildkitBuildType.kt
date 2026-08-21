/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// BuildkitBuildType：buildkit 内部的 build type 枚举，与 :app / :benchmarks 共用。
// DEBUG 带 .debug 后缀，可与正式版并存；RELEASE 无后缀。

package com.z1nt.buildkit

/**
 * This is shared between :app and :benchmarks module to provide configurations type safety.
 *
 * 与 :app 和 :benchmarks 模块共享的 build type 枚举，保证配置类型安全。
 */
enum class BuildkitBuildType(val applicationIdSuffix: String? = null) {
    DEBUG(".debug"),
    RELEASE
}
