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

// AndroidApplicationFirebaseConventionPlugin：集成 Firebase Performance + Crashlytics。
// 仅当存在 google-services.json 时才启用 google-services 插件，并默认禁用 Crashlytics mapping 上传。

import com.android.build.api.dsl.ApplicationExtension
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.z1nt.buildkit.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.exclude

class AndroidApplicationFirebaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // 仅在提供了 google-services.json 时才启用 google-services，避免缺配置报错
            if (file("google-services.json").isFile) {
                apply(plugin = "com.google.gms.google-services")
            }
            apply(plugin = "com.google.firebase.firebase-perf")
            apply(plugin = "com.google.firebase.crashlytics")

            dependencies {
                // Firebase BOM 平台坐标，统一 Firebase 组件版本
                val bom = libs.findLibrary("firebase-bom").get()
                "implementation"(platform(bom))
                "implementation"(libs.findLibrary("firebase.performance").get()) {
                    /*
                    Exclusion of protobuf / protolite dependencies is necessary as the
                    datastore-proto brings in protobuf dependencies. These are the source of truth
                    for Now in Android.
                    That's why the duplicate classes from below dependencies are excluded.
                    显式排除 protobuf-javalite / protolite-well-known-types，避免 datastore-proto
                    引入的 protobuf 依赖造成类冲突
                     */
                    exclude(group = "com.google.protobuf", module = "protobuf-javalite")
                    exclude(group = "com.google.firebase", module = "protolite-well-known-types")
                }
                "implementation"(libs.findLibrary("firebase.crashlytics").get())
            }

            extensions.configure<ApplicationExtension> {
                buildTypes.configureEach {
                    // Disable the Crashlytics mapping file upload. This feature should only be
                    // enabled if a Firebase backend is available and configured in
                    // google-services.json.
                    // 默认禁用 Crashlytics mapping 上传，需要后端实际可用并配好 google-services.json 时再开启
                    configure<CrashlyticsExtension> {
                        mappingFileUploadEnabled = false
                    }
                }
            }
        }
    }
}
