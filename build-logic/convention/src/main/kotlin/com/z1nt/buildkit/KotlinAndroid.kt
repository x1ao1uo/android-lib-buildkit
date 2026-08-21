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

// KotlinAndroid：buildkit 内部共享的 Kotlin/JVM 编译器与 Android 编译选项配置入口。
// 为 Android 模块与非 Android JVM 模块统一 jvmTarget=17、warning-as-errors、coreLibraryDesugaring 等基线。

package com.z1nt.buildkit

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Configure base Kotlin with Android options
 *
 * 为 Android 模块配置 Kotlin 选项：compileSdk、minSdk、Java 17 desugar 等基线。
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = findVersionOrDefault("compileSdk", 37)

        defaultConfig.apply {
            // minSdk 默认 24，可被消费方覆盖
            minSdk = findVersionOrDefault("minSdk", 24)
        }

        compileOptions.apply {
            // Up to Java 17 APIs are available through desugaring
            // https://developer.android.com/studio/write/java17-minimal-support-table
            // 通过 desugar 让旧版本设备也能使用 Java 17 API
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = true
        }
    }

    configureKotlin<KotlinAndroidProjectExtension>()

    // 引入 desugar JDK 库，是 isCoreLibraryDesugaringEnabled=true 的运行时支持
    dependencies {
        "coreLibraryDesugaring"(libs.findLibrary("android.desugarJdkLibs").get())
    }
}

/**
 * Configure base Kotlin options for JVM (non-Android)
 *
 * 纯 JVM 模块（不带 Android）的 Kotlin 配置：Java 17 字节码 + Kotlin 通用选项。
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        // Up to Java 17 APIs are available through desugaring
        // https://developer.android.com/studio/write/java17-minimal-support-table
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

/**
 * Configure base Kotlin options
 *
 * 跨 Android / JVM 的 Kotlin 通用配置：JDK toolchain、warningsAsErrors、协程 opt-in 等。
 */
private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() {
    // Optional JDK toolchain pin, e.g. buildkit.jvmToolchain=25. When unset the
    // toolchain is left to the consumer (Gradle daemon JVM by default).
    // 可通过 -Pbuildkit.jvmToolchain=25 强制使用 JDK 25 工具链；不设置则沿用消费方默认
    providers.gradleProperty("buildkit.jvmToolchain").orNull?.toIntOrNull()?.let { toolchain ->
        configure<T> {
            jvmToolchain(toolchain)
        }
    }
    configure<T> {
        // Treat all Kotlin warnings as errors (disabled by default)
        // Override by setting warningsAsErrors=true in your ~/.gradle/gradle.properties
        // 默认不开启 warning-as-errors，可通过 ~/.gradle/gradle.properties 设置 warningsAsErrors=true 开启
        val warningsAsErrors = providers.gradleProperty("warningsAsErrors").map {
            it.toBoolean()
        }.orElse(false)
        when (this) {
            is KotlinAndroidProjectExtension -> compilerOptions
            is KotlinJvmProjectExtension -> compilerOptions
            else -> TODO("Unsupported project extension $this ${T::class}")
        }.apply {
            // JVM 字节码目标统一为 17
            jvmTarget = JvmTarget.JVM_17
            allWarningsAsErrors = warningsAsErrors
            freeCompilerArgs.add(
                // Enable experimental coroutines APIs, including Flow
                // 全工程默认开启 coroutines 实验 API 与 Flow，避免每个模块单独 opt-in
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
            )
            freeCompilerArgs.add(
            /*
             * Remove this args after Phase 3.
             * https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-consistent-copy-visibility/#deprecation-timeline
             *
             * Deprecation timeline
             * Phase 3. (Supposedly Kotlin 2.2 or Kotlin 2.3).
             * The default changes.
             * Unless ExposedCopyVisibility is used, the generated 'copy' method has the same visibility as the primary constructor.
             * The binary signature changes. The error on the declaration is no longer reported.
             * '-Xconsistent-data-class-copy-visibility' compiler flag and ConsistentCopyVisibility annotation are now unnecessary.
             */
            // Kotlin 2.x 期间临时使用，强制 data class 的 copy() 与构造器可见性一致；Phase 3 之后可移除
                "-Xconsistent-data-class-copy-visibility"
            )
        }
    }
}
