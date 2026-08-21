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

// BuildkitIssueRegistry：buildkit 自定义 Lint IssueRegistry，集中暴露给 Lint。
// 当前包含 DesignSystem 强制走 design system 的检查与 TestMethodName 命名规范检查。

package com.z1nt.buildkit.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.z1nt.buildkit.lint.designsystem.DesignSystemDetector

class BuildkitIssueRegistry : IssueRegistry() {

    override val issues = listOf(
        // Design system 统一性检查
        DesignSystemDetector.ISSUE,
        // 测试方法命名格式（androidTest 下 given_when_then）
        TestMethodNameDetector.FORMAT,
        // 测试方法不应有 test 前缀
        TestMethodNameDetector.PREFIX,
    )

    override val api: Int = CURRENT_API

    override val minApi: Int = 12

    override val vendor: Vendor = Vendor(
        vendorName = "android-lib-buildkit",
        feedbackUrl = "https://github.com/android/nowinandroid/issues",
        contact = "https://github.com/android/nowinandroid",
    )
}
