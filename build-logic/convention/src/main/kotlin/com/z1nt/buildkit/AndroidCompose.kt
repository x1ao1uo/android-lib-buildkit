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

// AndroidCompose：为 Compose 模块统一打开 buildFeatures.compose、引入 BOM 与 Tooling 依赖，
// 并按 Gradle 属性开启 Compose Compiler Metrics / Reports / stability 配置。

package com.z1nt.buildkit

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Configure Compose-specific options
 *
 * - 启用 buildFeatures.compose；
 * - 引入 androidx.compose BOM，统一管理 Compose 各组件版本；
 * - 仅当 -PenableComposeCompilerMetrics=true 时打开 compiler metrics；
 * - 仅当 -PenableComposeCompilerReports=true 时打开 compiler reports；
 * - 若根工程存在 compose_compiler_config.conf，则作为 stability 配置文件加载。
 */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.apply {
        buildFeatures.apply {
            compose = true
        }

        dependencies {
            val bom = libs.findLibrary("androidx-compose-bom").get()
            // BOM 平台坐标，统一管理 Compose 各组件版本
            "implementation"(platform(bom))
            "androidTestImplementation"(platform(bom))
            "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            // tooling 仅 debug，避免给 release 打包带来多余代码
            "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // 仅当 Gradle 属性为 true 时才返回值，否则返回 null（即不开启）
        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }
        fun Provider<*>.relativeToRootProject(dir: String) = map {
            @Suppress("UnstableApiUsage")
            isolated.rootProject.projectDirectory
                .dir("build")
                .dir(projectDir.toRelativeString(rootDir))
        }.map { it.dir(dir) }

        // 可选：打开 Compose Compiler Metrics 输出到 <root>/build/.../compose-metrics
        project.providers.gradleProperty("enableComposeCompilerMetrics").onlyIfTrue()
            .relativeToRootProject("compose-metrics")
            .let(metricsDestination::set)

        // 可选：打开 Compose Compiler Reports 输出到 <root>/build/.../compose-reports
        project.providers.gradleProperty("enableComposeCompilerReports").onlyIfTrue()
            .relativeToRootProject("compose-reports")
            .let(reportsDestination::set)

        // Only point Compose at the root stability configuration file when the
        // consumer actually ships one; otherwise every Compose module would log
        // a missing-file warning.
        // 只有当根工程确实存在 compose_compiler_config.conf 时才加载，避免空文件告警
        @Suppress("UnstableApiUsage")
        isolated.rootProject.projectDirectory.file("compose_compiler_config.conf")
            .takeIf { it.asFile.exists() }
            ?.let(stabilityConfigurationFiles::add)
    }
}
