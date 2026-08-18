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
            apply(plugin = "com.android.library")
            apply(plugin = "buildkit.android.lint")

            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                val targetSdkVersion = findVersionOrDefault("targetSdk", 36)
                testOptions.targetSdk = targetSdkVersion
                lint.targetSdk = targetSdkVersion
                testOptions.unitTests.all { test ->
                    test.jvmArgs(
                        "--enable-native-access=ALL-UNNAMED",
                        "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    )
                }
                // Consumers may provide Robolectric resource overrides (e.g. shadows for SDK
                // levels newer than Robolectric supports) under gradle/robolectric/.
                val robolectricDir =
                    isolated.rootProject.projectDirectory.dir("gradle/robolectric").asFile
                if (robolectricDir.isDirectory) {
                    sourceSets.getByName("test").resources.srcDir(robolectricDir)
                }
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true
                configureFlavors(this)
                configureGradleManagedDevices(this)
                // The resource prefix is derived from the module name,
                // so resources inside ":core:module1" must be prefixed with "core_module1_".
                // Consumers can override it with the `buildkit.resourcePrefix` Gradle property.
                resourcePrefix =
                    providers.gradleProperty("buildkit.resourcePrefix").orNull
                        ?: path.split("""\W""".toRegex()).drop(1).distinct()
                            .joinToString(separator = "_").lowercase() + "_"
            }
            extensions.configure<LibraryAndroidComponentsExtension> {
                configurePrintApksTask(this)
                disableUnnecessaryAndroidTests(target)
            }
            configureSpotlessForAndroid()
            dependencies {
                "androidTestImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("junit").get())

                "implementation"(libs.findLibrary("androidx.tracing.ktx").get())
            }
        }
    }
}
