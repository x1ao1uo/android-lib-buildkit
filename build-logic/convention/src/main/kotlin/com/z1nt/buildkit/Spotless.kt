/*
 * Copyright 2026 The Android Open Source Project
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

// Spotless：buildkit 内部的代码格式化与版权头校验逻辑。
// 按 root / Android 子项目 / JVM 子项目三种形态分别注册 Spotless 任务。

package com.z1nt.buildkit

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

// Android 子项目：复用通用 Kotlin/Gradle 配置 + 注册 XML 格式校验
internal fun Project.configureSpotlessForAndroid() {
    configureSpotlessCommon()
    extensions.configure<SpotlessExtension> {
        format("xml") {
            target("src/**/*.xml")
            // Look for the first XML tag that isn't a comment (<!--) or the xml declaration (<?xml)
            // 匹配首个非注释/声明的 XML 标签，识别版权头插入位置
            licenseHeaderFile(rootDir.resolve("spotless/copyright.xml"), "(<[^!?])")
            endWithNewline()
        }
    }
}

// 纯 JVM 子项目：只启用通用 Kotlin/Gradle 配置
internal fun Project.configureSpotlessForJvm() {
    configureSpotlessCommon()
}

// 根项目的 Spotless 配置：根据 -Pbuildkit.spotless.recursive 决定扫描范围
internal fun Project.configureSpotlessForRootProject() {
    apply(plugin = "com.diffplug.spotless")
    // Consumers whose sources live outside module directories (e.g. source sets
    // pointing at root-level core/ or feature/ trees) set
    // `buildkit.spotless.recursive=true` to lint the whole tree from the root
    // instead of the default NIA build-logic layout.
    // recursive=true 适用于源码不在标准模块目录（如统一放在 core/、feature/）的仓库
    val recursive = providers.gradleProperty("buildkit.spotless.recursive")
        .map { it.equals("true", ignoreCase = true) }
        .getOrElse(false)
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
    extensions.configure<SpotlessExtension> {
        kotlin {
            if (recursive) {
                target("**/src/**/*.kt")
                targetExclude("**/build/**")
            } else {
                target("build-logic/convention/src/**/*.kt")
            }
            ktlint(ktlintVersion).editorConfigOverride(
                // ktlint 1.8 removed the legacy `android` boolean property;
                // android_studio is the documented replacement code style.
                // ktlint 1.8 起使用 android_studio 作为 Android 代码风格
                mapOf("ktlint_code_style" to "android_studio")
            )
            licenseHeaderFile(rootDir.resolve("spotless/copyright.kt"))
            endWithNewline()
        }
        if (recursive) {
            kotlinGradle {
                target("**/*.kts")
                targetExclude("**/build/**", ".gradle/**", "spotless/**")
                ktlint(ktlintVersion).editorConfigOverride(
                    // ktlint 1.8 removed the legacy `android` boolean property;
                    // android_studio is the documented replacement code style.
                    mapOf("ktlint_code_style" to "android_studio")
                )
                // Look for the first line that doesn't have a block comment (assumed to be the license)
                // 匹配首个非块注释行作为版权头锚点
                licenseHeaderFile(rootDir.resolve("spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
                endWithNewline()
            }
        } else {
            format("kts") {
                target("*.kts")
                target("build-logic/*.kts")
                target("build-logic/convention/*.kts")
                // Look for the first line that doesn't have a block comment (assumed to be the license)
                licenseHeaderFile(rootDir.resolve("spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
                endWithNewline()
            }
        }
        if (recursive) {
            format("xml") {
                target("**/src/**/*.xml")
                targetExclude("**/build/**")
                licenseHeaderFile(rootDir.resolve("spotless/copyright.xml"), "(<[^!?])")
                endWithNewline()
            }
        }
    }
}

// 子项目通用的 Spotless 配置：kotlin（ktlint + 版权头）+ kts 版权头
private fun Project.configureSpotlessCommon() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(libs.findVersion("ktlint").get().requiredVersion).editorConfigOverride(
                // ktlint 1.8 removed the legacy `android` boolean property;
                // android_studio is the documented replacement code style.
                mapOf("ktlint_code_style" to "android_studio")
            )
            licenseHeaderFile(rootDir.resolve("spotless/copyright.kt"))
            endWithNewline()
        }
        format("kts") {
            target("*.kts")
            // Look for the first line that doesn't have a block comment (assumed to be the license)
            licenseHeaderFile(rootDir.resolve("spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
            endWithNewline()
        }
    }
}
