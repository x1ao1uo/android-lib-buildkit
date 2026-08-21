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

// AndroidFeatureApiConventionPlugin：feature API 层模块的约定插件。
// 启用 Kotlin Serialization，并通过 api 依赖对外暴露 core:navigation 的接口。

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 基础 library + Kotlin Serialization 插件（API 层通常需要数据类的序列化能力）
            apply(plugin = "buildkit.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                // 使用 api 而非 implementation，让下游模块可以直接拿到 navigation 接口
                "api"(project(":core:navigation"))
            }
        }
    }
}
