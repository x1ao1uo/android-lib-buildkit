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
            apply(plugin = "com.android.application")
            apply(plugin = "buildkit.android.lint")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = findVersionOrDefault("targetSdk", 36)
                testOptions.animationsDisabled = true
                // Consumers may provide Robolectric resource overrides (e.g. shadows for SDK
                // levels newer than Robolectric supports) under gradle/robolectric/.
                val robolectricDir =
                    isolated.rootProject.projectDirectory.dir("gradle/robolectric").asFile
                if (robolectricDir.isDirectory) {
                    sourceSets.getByName("test").resources.srcDir(robolectricDir)
                }
                configureGradleManagedDevices(this)
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                configurePrintApksTask(this)
                configureBadgingTasks(this)
            }
            configureSpotlessForAndroid()
        }
    }
}
