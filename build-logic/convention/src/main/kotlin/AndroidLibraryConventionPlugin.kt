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

// AndroidLibraryConventionPlugin：组合 com.android.library + Kotlin/JVM 配置 + Lint + Spotless + 默认依赖。
// 应用到模块后会自动得到 targetSdk、managed devices、flavor、resource prefix 等统一能力。

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.z1nt.buildkit.configureFlavors
import com.z1nt.buildkit.configureGradleManagedDevices
import com.z1nt.buildkit.configureKotlinAndroid
import com.z1nt.buildkit.configurePrintApksTask
import com.z1nt.buildkit.configureSpotlessForAndroid
import com.z1nt.buildkit.disableUnnecessaryAndroidTests
import com.z1nt.buildkit.findVersionOrDefault
import com.z1nt.buildkit.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 加载 AGP library 插件与 buildkit 自定义 Lint 插件
            apply(plugin = "com.android.library")
            apply(plugin = "buildkit.android.lint")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                // 同步 testOptions 与 lint 的 targetSdk，保证 lint 报告的目标 API 与运行时一致
                val targetSdkVersion = findVersionOrDefault("targetSdk", 37)
                testOptions.targetSdk = targetSdkVersion
                lint.targetSdk = targetSdkVersion
                // 单元测试需要的 JVM 参数：开放 native access 与 jdk.internal.access 导出
                testOptions.unitTests.all { test ->
                    test.jvmArgs(
                        "--enable-native-access=ALL-UNNAMED",
                        "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED"
                    )
                }
                // Consumers may provide Robolectric resource overrides (e.g. shadows for SDK
                // levels newer than Robolectric supports) under gradle/robolectric/.
                // 允许消费方在 gradle/robolectric/ 下放 Robolectric 阴影/资源覆写
                val robolectricDir =
                    isolated.rootProject.projectDirectory.dir("gradle/robolectric").asFile
                if (robolectricDir.isDirectory) {
                    sourceSets.getByName("test").resources.srcDir(robolectricDir)
                }
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                // 关闭测试动画，避免 Robolectric/Espresso 截图闪烁
                testOptions.animationsDisabled = true
                // Consumers can skip flavor injection (contentType dimension + demo/prod)
                // with `-Pbuildkit.flavors=false`, e.g. flavorless projects like android-mkaf.
                // 通过 -Pbuildkit.flavors=false 可以关闭默认 flavor 注入，便于像 android-mkaf 这种无 flavor 的工程
                val flavorsEnabled =
                    providers.gradleProperty("buildkit.flavors").orNull
                        ?.equals("false", ignoreCase = true) != true
                if (flavorsEnabled) {
                    configureFlavors(this)
                }
                configureGradleManagedDevices(this)
                // The resource prefix is derived from the module name,
                // so resources inside ":core:module1" must be prefixed with "core_module1_".
                // Consumers can override it with the `buildkit.resourcePrefix` Gradle property,
                // or disable the enforcement entirely by setting it to "off"/"false"/"".
                // resourcePrefix 默认按 Gradle 路径生成，例如 ":core:module1" -> "core_module1_"
                // 可通过 -Pbuildkit.resourcePrefix=xxx 覆盖；off/false/空 则关闭强制
                val resourcePrefixOverride =
                    providers.gradleProperty("buildkit.resourcePrefix").orNull
                if (resourcePrefixOverride == null) {
                    resourcePrefix =
                        path.split("""\W""".toRegex()).drop(1).distinct()
                            .joinToString(separator = "_").lowercase() + "_"
                } else if (
                    !resourcePrefixOverride.equals("off", ignoreCase = true) &&
                    !resourcePrefixOverride.equals("false", ignoreCase = true) &&
                    resourcePrefixOverride.isNotEmpty()
                ) {
                    resourcePrefix = resourcePrefixOverride
                }
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                // 打印生成的 AAR 路径
                configurePrintApksTask(this)
                // 关闭 AGP 默认开启但本仓库不需要的 androidTest（统一通过单元测试）
                disableUnnecessaryAndroidTests(target)
            }
            configureSpotlessForAndroid()
            dependencies {
                // 公共测试依赖：androidTest/test 都可以使用 kotlin.test + junit
                "androidTestImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("junit").get())

                // Tracing 是项目默认引入的轻量追踪库
                "implementation"(libs.findLibrary("androidx.tracing.ktx").get())
            }
        }
    }
}
