/*
 * Copyright 2023 The Android Open Source Project
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

// ProjectExtensions：提供给 buildkit 各插件复用的 Project 扩展属性与方法。
// 主要包括 version catalog 访问与版本号读取辅助。

package com.z1nt.buildkit

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

// Project.libs：访问消费方声明的 version catalog（gradle/libs.versions.toml）
val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

/**
 * Reads an integer version (e.g. `compileSdk` / `minSdk` / `targetSdk`) from the consumer's
 * version catalog, falling back to [default] when the key is not declared there.
 *
 * 从消费方的 version catalog 读取指定版本号（如 compileSdk / minSdk / targetSdk），
 * 缺失时回退到 default。常用于为 SDK 版本提供"可被覆盖的默认值"。
 */
internal fun Project.findVersionOrDefault(name: String, default: Int): Int =
    libs.findVersion(name).map { it.requiredVersion.toInt() }.orElse(default)
