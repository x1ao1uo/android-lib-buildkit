/*
 * Copyright 2023 The Android Open Source Project
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

// Badging：通过 aapt2 dump badging 生成与校验应用 badging 信息（图标、版本等元数据）。
// 为每个 variant 注册 generate / update / check 三个任务，用于 CI 校验 manifest 元数据未意外变化。

package com.z1nt.buildkit

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.Aapt2
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.common.truth.Truth.assertWithMessage
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.ExecOperations

// 调用 aapt2 dump badging 生成元数据文件
@CacheableTask
abstract class GenerateBadgingTask : DefaultTask() {

    @get:OutputFile
    abstract val badging: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val apk: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val aapt2Executable: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun taskAction() {
        // 调用 aapt2 dump badging 把 APK 的元数据写入输出文件
        execOperations.exec {
            commandLine(
                aapt2Executable.get().asFile.absolutePath,
                "dump",
                "badging",
                apk.get().asFile.absolutePath
            )
            standardOutput = badging.asFile.get().outputStream()
        }
    }
}

// 比较生成的 badging 与 golden 文件，发现漂移即断言失败
@CacheableTask
abstract class CheckBadgingTask : DefaultTask() {

    // In order for the task to be up-to-date when the inputs have not changed,
    // the task must declare an output, even if it's not used. Tasks with no
    // output are always run regardless of whether the inputs changed
    // 必须声明 output 才能让 Gradle 正确做 up-to-date 检查
    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val goldenBadging: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val generatedBadging: RegularFileProperty

    @get:Input
    abstract val updateBadgingTaskName: Property<String>

    override fun getGroup(): String = LifecycleBasePlugin.VERIFICATION_GROUP

    @TaskAction
    fun taskAction() {
        assertWithMessage(
            "Generated badging is different from golden badging! " +
                "If this change is intended, run ./gradlew ${updateBadgingTaskName.get()}"
        )
            .that(generatedBadging.get().asFile.readText())
            .isEqualTo(goldenBadging.get().asFile.readText())
    }
}

// Locale 无关的首字母大写工具方法
private fun String.capitalized() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

// 在 Project 上注册一组 badging 相关任务
fun Project.configureBadgingTasks(componentsExtension: ApplicationAndroidComponentsExtension) {
    // Registers a callback to be called, when a new variant is configured
    componentsExtension.onVariants { variant ->
        // Registers a new task to verify the app bundle.
        val capitalizedVariantName = variant.name.capitalized()
        val generateBadgingTaskName = "generate${capitalizedVariantName}Badging"
        val generateBadging =
            tasks.register<GenerateBadgingTask>(generateBadgingTaskName) {
                apk = variant.artifacts.get(SingleArtifact.APK_FROM_BUNDLE)
                aapt2Executable = componentsExtension.sdkComponents.aapt2.flatMap(Aapt2::executable)
                badging = project.layout.buildDirectory.file(
                    "outputs/apk_from_bundle/${variant.name}/${variant.name}-badging.txt"
                )
            }

        // 把最新生成的 badging 复制回项目根目录，便于提交为 golden
        val updateBadgingTaskName = "update${capitalizedVariantName}Badging"
        tasks.register<Copy>(updateBadgingTaskName) {
            from(generateBadging.map(GenerateBadgingTask::badging))
            into(project.layout.projectDirectory)
        }

        // 比对 golden 与新生成 badging，发现差异即失败
        val checkBadgingTaskName = "check${capitalizedVariantName}Badging"
        tasks.register<CheckBadgingTask>(checkBadgingTaskName) {
            goldenBadging = project.layout.projectDirectory.file("${variant.name}-badging.txt")

            generatedBadging.set(generateBadging.flatMap(GenerateBadgingTask::badging))

            this.updateBadgingTaskName = updateBadgingTaskName

            output = project.layout.buildDirectory.dir("intermediates/$checkBadgingTaskName")
        }
    }
}
