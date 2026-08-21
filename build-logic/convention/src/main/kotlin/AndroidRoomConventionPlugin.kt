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

// AndroidRoomConventionPlugin：Room 数据库的约定插件。
// 启用 androidx.room3 + KSP，输出 Kotlin schema（generateKotlin=true），并把 schemas 目录指向模块内。

import androidx.room3.gradle.RoomExtension
import com.google.devtools.ksp.gradle.KspExtension
import com.z1nt.buildkit.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room3")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<KspExtension> {
                // 生成 Kotlin schema 而不是 Java schema
                arg("room.generateKotlin", "true")
            }

            extensions.configure<RoomExtension> {
                // The schemas directory contains a schema file for each version of the Room database.
                // This is required to enable Room auto migrations.
                // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
                // 每个版本的数据库 schema 都会落到该目录，用于支持 auto migration
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                // api: consumers call RoomDatabase APIs (withWriteTransaction, clearAllTables).
                // 用 api 暴露 RoomDatabase API，使消费方能直接调用事务 API
                "api"(libs.findLibrary("room-runtime").get())
                "ksp"(libs.findLibrary("room-compiler").get())
            }
        }
    }
}
