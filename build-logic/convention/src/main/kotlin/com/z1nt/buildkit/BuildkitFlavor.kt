/*
 * Copyright 2026 The Android Open Source Project
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

// BuildkitFlavor：buildkit 标准的 flavor 维度与枚举定义。
// 仅一个维度 contentType，两个 flavor：demo（带 .demo 后缀，便于和正式版并存）与 prod。

package com.z1nt.buildkit

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.kotlin.dsl.invoke

// 关闭 EnumEntryName 警告，保留首字母小写的 demo/prod 等枚举名
@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType
}

// The content for the app can either come from local static data which is useful for demo
// purposes, or from a production backend server which supplies up-to-date, real content.
// These two product flavors reflect this behaviour.
// contentType 维度下区分：
// - demo：使用本地静态数据，便于演示；applicationId 加 .demo 后缀与正式版并存
// - prod：连接生产后端，使用实时数据
@Suppress("EnumEntryName")
enum class BuildkitFlavor(val dimension: FlavorDimension, val applicationIdSuffix: String? = null) {
    demo(FlavorDimension.contentType, applicationIdSuffix = ".demo"),
    prod(FlavorDimension.contentType)
}

// 把 BuildkitFlavor 注册到任意 CommonExtension（application/library），并允许外部再追加额外配置
fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: BuildkitFlavor) -> Unit = {}
) {
    commonExtension.apply {
        FlavorDimension.entries.forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        productFlavors {
            BuildkitFlavor.entries.forEach { buildkitFlavor ->
                register(buildkitFlavor.name) {
                    dimension = buildkitFlavor.dimension.name
                    flavorConfigurationBlock(this, buildkitFlavor)
                    // 仅 Application 模块支持 applicationIdSuffix
                    if (commonExtension is ApplicationExtension &&
                        this is ApplicationProductFlavor
                    ) {
                        if (buildkitFlavor.applicationIdSuffix != null) {
                            applicationIdSuffix = buildkitFlavor.applicationIdSuffix
                        }
                    }
                }
            }
        }
    }
}
