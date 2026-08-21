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

// AndroidLintConventionPlugin：为任意 Android 模块（application / library）或纯 Lint 模块统一应用 buildkit 的 Lint 配置。

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

class AndroidLintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 根据宿主已应用的具体 Android 插件类型选用对应的 Extension
            when {
                pluginManager.hasPlugin("com.android.application") ->
                    configure<ApplicationExtension> { lint(Lint::configure) }

                pluginManager.hasPlugin("com.android.library") ->
                    configure<LibraryExtension> { lint(Lint::configure) }

                else -> {
                    // 兜底：直接应用 com.android.lint 插件
                    apply(plugin = "com.android.lint")
                    configure<Lint>(Lint::configure)
                }
            }
        }
    }
}

// buildkit 统一的 Lint 基线配置
private fun Lint.configure() {
    xmlReport = true  // 输出 XML 报告
    sarifReport = false  // 暂不输出 SARIF
    checkDependencies = true  // 同时检查依赖项
    // GradleDependency 在版本目录模式下经常误报，按 buildkit 基线禁用
    disable += "GradleDependency"
}
