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

// PrintTestApks：为每个带 androidTest 的 variant 注册一个 `${variantName}PrintTestApk` 任务，
// 任务仅在确实存在 androidTest 源码时打印生成的 APK 路径，便于 CI/本地后续安装与运行。

package com.z1nt.buildkit

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.HasAndroidTest
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.work.DisableCachingByDefault

// 为每个含 androidTest 的 variant 注册打印 APK 路径的任务
internal fun Project.configurePrintApksTask(extension: AndroidComponentsExtension<*, *, *>) {
    extension.onVariants { variant ->
        if (variant is HasAndroidTest) {
            val loader = variant.artifacts.getBuiltArtifactsLoader()
            val artifact = variant.androidTest?.artifacts?.get(SingleArtifact.APK)
            val javaSources = variant.androidTest?.sources?.java?.all
            val kotlinSources = variant.androidTest?.sources?.kotlin?.all

            // 合并 java 与 kotlin 源码目录用于后续判断是否存在 androidTest
            val testSources = if (javaSources != null && kotlinSources != null) {
                javaSources.zip(kotlinSources) { javaDirs, kotlinDirs ->
                    javaDirs + kotlinDirs
                }
            } else {
                javaSources ?: kotlinSources
            }

            if (artifact != null && testSources != null) {
                tasks.register(
                    "${variant.name}PrintTestApk",
                    PrintApkLocationTask::class.java
                ) {
                    apkFolder = artifact
                    builtArtifactsLoader = loader
                    variantName = variant.name
                    sources = testSources
                }
            }
        }
    }
}

// 显式禁用缓存，因为该任务只做 println，没有真正的输出
@DisableCachingByDefault(because = "Prints output")
internal abstract class PrintApkLocationTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputDirectory
    // APK 输出目录
    abstract val apkFolder: DirectoryProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    // 源码目录列表，用于判断是否真的存在 androidTest
    abstract val sources: ListProperty<Directory>

    @get:Internal
    // AGP 提供的构建产物加载器
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val variantName: Property<String>

    @TaskAction
    fun taskAction() {
        // 判断是否存在非 build/generated 目录下的实际源文件
        val hasFiles = sources.orNull?.any { directory ->
            directory.asFileTree.files.any {
                it.isFile && "build${File.separator}generated" !in it.parentFile.path
            }
        } ?: throw RuntimeException("Cannot check androidTest sources")

        // Don't print APK location if there are no androidTest source files
        // 没有源码时直接 no-op，避免噪声
        if (!hasFiles) return

        val builtArtifacts = builtArtifactsLoader.get().load(apkFolder.get())
            ?: throw RuntimeException("Cannot load APKs")
        if (builtArtifacts.elements.size != 1) {
            throw RuntimeException("Expected one APK !")
        }
        // 打印唯一的 APK 路径，方便 CI 抓取
        val apk = File(builtArtifacts.elements.single().outputFile).toPath()
        println(apk)
    }
}
