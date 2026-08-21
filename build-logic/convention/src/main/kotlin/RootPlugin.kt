/*
 * Copyright 2025 The Android Open Source Project
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

// RootPlugin：apply 在 root project 的插件。
// - 在开启 Isolated Projects 的工程里跳过跨子项目任务图配置，避免违反 IP 约束；
// - 其它情况下统一配置任务图与 Spotless 版权头校验。

import com.z1nt.buildkit.configureGraphTasks
import com.z1nt.buildkit.configureSpotlessForRootProject
import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures

abstract class RootPlugin : Plugin<Project> {
    // 通过 Gradle 注入的 BuildFeatures，用于检查是否启用了 Isolated Projects
    @get:Inject abstract val buildFeatures: BuildFeatures

    override fun apply(target: Project) {
        // 该插件只能应用到 root project，避免在子 project 上重复配置
        require(target.path == ":")
        // Isolated Projects 下禁止跨项目执行任意任务，因此跳过 configureGraphTasks
        if (!buildFeatures.isIsolatedProjectsEnabled()) {
            target.subprojects { configureGraphTasks() }
        }
        // 给根项目统一打 Spotless 版权头
        target.configureSpotlessForRootProject()
    }
}

// Gradle 的 isolatedProjects 是 Provider<Boolean>，getOrElse(false) 安全地拿到默认值
private fun BuildFeatures.isIsolatedProjectsEnabled(): Boolean =
    isolatedProjects.active.getOrElse(false)
