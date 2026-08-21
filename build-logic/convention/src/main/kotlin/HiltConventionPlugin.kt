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

// HiltConventionPlugin：buildkit 的 Hilt 依赖注入约定插件。
// 同时支持纯 JVM 模块和 Android 模块，根据宿主应用的具体插件加载不同的 Hilt 变体。

import com.android.build.gradle.api.AndroidBasePlugin
import com.z1nt.buildkit.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // KSP 是 Hilt 注解处理器的运行时
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                "ksp"(libs.findLibrary("hilt.compiler").get())
                "ksp"(libs.findLibrary("kotlin.metadata").get())
            }

            // Add support for Jvm Module, base on org.jetbrains.kotlin.jvm
            // 纯 JVM 模块：只引入 hilt.core
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(libs.findLibrary("hilt.core").get())
                }
            }

            // Add support for Android modules, based on AndroidBasePlugin
            // Android 模块：额外应用 Hilt Android Gradle 插件并引入 hilt.android
            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "dagger.hilt.android.plugin")
                dependencies {
                    "implementation"(libs.findLibrary("hilt.android").get())
                }
            }
        }
    }
}
