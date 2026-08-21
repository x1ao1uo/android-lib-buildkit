/*
 * Copyright 2024 The Android Open Source Project
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

// Jacoco：buildkit 内部的覆盖率任务配置入口。
// 为每个 variant 生成 `create{variant}CombinedCoverageReport`，合并 unitTest + androidTest 数据。

package com.z1nt.buildkit

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.api.variant.SourceDirectories
import java.util.Locale
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

// 覆盖率默认排除的类：Android R 类、BuildConfig、Hilt/Dagger 生成代码等
private val coverageExclusions = listOf(
    // Android
    "**/R.class",
    "**/R\$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*_Hilt*.class",
    "**/Hilt_*.class",
    // Dagger / Hilt generated classes
    "**/HiltWrapper_*.class",
    "**/Dagger*.class",
    "**/*_Factory*.class",
    "**/*_MembersInjector*.class",
    "**/*Module_*Factory*.class",
    "**/*_ComponentTreeDeps*.class",
    "**/*_Impl*.class",
    "**/*_GeneratedInjector*.class",
    "**/_com_*.class",
    "**/*ComposableSingletons*.class"
)

// 首字母大写（Locale 感知），用于构造 create{Variant}CombinedCoverageReport 任务名
private fun String.capitalize() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

/**
 * Creates a new task that generates a combined coverage report with data from local and
 * instrumented tests.
 *
 * `create{variant}CombinedCoverageReport`
 *
 * Note that coverage data must exist before running the task. This allows us to run device
 * tests on CI using a different Github Action or an external device farm.
 *
 * 注意：覆盖率数据需在运行本任务前先准备好（unitTest 与 androidTest 都跑过）。
 * 拆分的方式便于 CI 把设备测试放到外部 device farm 执行后再聚合。
 */
internal fun Project.configureJacoco(
    commonExtension: CommonExtension,
    androidComponentsExtension: AndroidComponentsExtension<*, *, *>
) {
    // Configure only the debug build, otherwise it will force the debuggable flag on release buildTypes as well
    // 只在 debug build type 上启用覆盖率，避免给 release 强制打开 debuggable
    commonExtension.buildTypes.named("debug") {
        enableAndroidTestCoverage = true
        enableUnitTestCoverage = true
    }

    // Jacoco 插件版本取自 version catalog
    configure<JacocoPluginExtension> {
        toolVersion = libs.findVersion("jacoco").get().toString()
    }

    // Consumers can append project-specific exclusion globs via the
    // `buildkit.jacoco.extraExclusions` Gradle property (comma-separated).
    // 消费方可通过 -Pbuildkit.jacoco.extraExclusions=**/X*,**/Y* 追加自定义排除规则
    val extraExclusions = providers.gradleProperty("buildkit.jacoco.extraExclusions")
        .map { value -> value.split(",").map { it.trim() }.filter { it.isNotEmpty() } }
        .orElse(emptyList())
        .get()
    val allExclusions = coverageExclusions + extraExclusions

    androidComponentsExtension.onVariants { variant ->
        val myObjFactory = project.objects
        val buildDir = layout.buildDirectory.get().asFile
        val allJars: ListProperty<RegularFile> = myObjFactory.listProperty(RegularFile::class.java)
        val allDirectories: ListProperty<Directory> =
            myObjFactory.listProperty(Directory::class.java)
        // 注册合并覆盖率报告任务
        val reportTask =
            tasks.register(
                "create${variant.name.capitalize()}CombinedCoverageReport",
                JacocoReport::class
            ) {
                dependsOn("test${variant.name.capitalize()}UnitTest")
                // 配 class 目录：从 jar 与 class 目录收集，并排除生成代码
                classDirectories.setFrom(
                    allJars,
                    allDirectories.map { dirs ->
                        dirs.map { dir ->
                            myObjFactory.fileTree().setDir(dir).exclude(allExclusions)
                        }
                    }
                )
                reports {
                    // CI 通常需要 XML，开发者本地需要 HTML
                    xml.required = true
                    html.required = true
                }

                fun SourceDirectories.Flat?.toFilePaths(): Provider<List<String>> = this
                    ?.all
                    ?.map { directories -> directories.map { it.asFile.path } }
                    ?: provider { emptyList() }
                sourceDirectories.setFrom(
                    files(
                        variant.sources.java.toFilePaths(),
                        variant.sources.kotlin.toFilePaths()
                    )
                )

                // 合并 unitTest (.exec) 与 androidTest (.ec) 的执行数据
                executionData.setFrom(
                    project.fileTree(
                        "$buildDir/outputs/unit_test_code_coverage/${variant.name}UnitTest"
                    )
                        .matching { include("**/*.exec") },

                    project.fileTree("$buildDir/outputs/code_coverage/${variant.name}AndroidTest")
                        .matching { include("**/*.ec") }
                )
            }

        variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT)
            .use(reportTask)
            .toGet(
                ScopedArtifact.CLASSES,
                { _ -> allJars },
                { _ -> allDirectories }
            )
    }

    // 全局 Test 任务配置：让 JaCoCo 与 Robolectric 兼容
    tasks.withType<Test>().configureEach {
        configure<JacocoTaskExtension> {
            // Required for JaCoCo + Robolectric
            // https://github.com/robolectric/robolectric/issues/2230
            // Robolectric 需要的开关，避免无 Location 的类被排除
            isIncludeNoLocationClasses = true

            // Required for JDK 11 with the above
            // https://github.com/gradle/gradle/issues/5184#issuecomment-391982009
            excludes = listOf("jdk.internal.*")
        }
    }
}
