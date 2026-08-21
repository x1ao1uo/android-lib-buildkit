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

// AndroidApplicationConventionPlugin：组合 com.android.application + Kotlin/JVM 配置 + Lint + Spotless。
// 应用到模块后会自动得到 targetSdk、managed devices、apk 打印等统一能力。

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.z1nt.buildkit.configureBadgingTasks
import com.z1nt.buildkit.configureGradleManagedDevices
import com.z1nt.buildkit.configureKotlinAndroid
import com.z1nt.buildkit.configurePrintApksTask
import com.z1nt.buildkit.configureSpotlessForAndroid
import com.z1nt.buildkit.findVersionOrDefault
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 1) 加载 AGP 的 application 插件
            apply(plugin = "com.android.application")
            // 2) 加载 buildkit 内部的 Lint 插件（自定义 IssueRegistry）
            apply(plugin = "buildkit.android.lint")

            extensions.configure<ApplicationExtension> {
                // Kotlin/JVM 选项（jvmTarget、freeCompilerArgs 等）
                configureKotlinAndroid(this)
                // 默认 SDK 37，可被消费方 libs.versions.toml 的 [versions] 段覆盖
                defaultConfig.targetSdk = findVersionOrDefault("targetSdk", 37)
                // 关闭测试动画，避免 Robolectric/Espresso 截图闪烁
                testOptions.animationsDisabled = true
                // Consumers may provide Robolectric resource overrides (e.g. shadows for SDK
                // levels newer than Robolectric supports) under gradle/robolectric/.
                // 允许消费方在 gradle/robolectric/ 下放 Robolectric 阴影/资源覆写
                val robolectricDir =
                    isolated.rootProject.projectDirectory.dir("gradle/robolectric").asFile
                if (robolectricDir.isDirectory) {
                    sourceSets.getByName("test").resources.srcDir(robolectricDir)
                }
                // 注册 managed devices，方便跑 instrumented test
                configureGradleManagedDevices(this)
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                // 任务：构建完成后打印生成的 APK 路径
                configurePrintApksTask(this)
                // 任务：生成 Android badging 信息（图标、版本等）
                configureBadgingTasks(this)
            }
            // Spotless 版权头与代码格式化（仅 Android 子集）
            configureSpotlessForAndroid()
        }
    }
}
