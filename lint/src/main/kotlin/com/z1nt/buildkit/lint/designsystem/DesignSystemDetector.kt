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

// DesignSystemDetector：buildkit 的 design system 统一性 Lint 检查。
// 检测代码中是否直接使用了 Compose Material API 而未走 buildkit 自身的 design system 包装。

package com.z1nt.buildkit.lint.designsystem

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression

/**
 * A detector that checks for incorrect usages of Compose Material APIs over equivalents in
 * the app's own design system module.
 */
class DesignSystemDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UCallExpression::class.java,
        UQualifiedReferenceExpression::class.java,
    )

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                // 函数调用：把 Material 的 composable 名称映射到 buildkit 等价物
                val name = node.methodName ?: return
                val preferredName = METHOD_NAMES[name] ?: return
                reportIssue(context, node, name, preferredName)
            }

            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                // 限定引用：例如 Icons.Default 形式
                val name = node.receiver.asRenderString()
                val preferredName = RECEIVER_NAMES[name] ?: return
                reportIssue(context, node, name, preferredName)
            }
        }

    companion object {
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "DesignSystem",
            briefDescription = "Design system",
            explanation = "This check highlights calls in code that use Compose Material " +
                "composables instead of equivalents from the app's own design system " +
                "module.",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                DesignSystemDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )

        // Unfortunately :lint is a Java module and thus can't depend on the :core-designsystem
        // Android module, so we can't use composable function references (eg. ::Button.name)
        // instead of hardcoded names.
        // :lint 是纯 Java 模块，无法依赖 :core-designsystem 的 composable 引用，
        // 只能维护硬编码的名称映射表
        val METHOD_NAMES = mapOf(
            "MaterialTheme" to "BuildkitTheme",
            "Button" to "BuildkitButton",
            "OutlinedButton" to "BuildkitOutlinedButton",
            "TextButton" to "BuildkitTextButton",
            "FilterChip" to "BuildkitFilterChip",
            "ElevatedFilterChip" to "BuildkitFilterChip",
            "NavigationBar" to "BuildkitNavigationBar",
            "NavigationBarItem" to "BuildkitNavigationBarItem",
            "NavigationRail" to "BuildkitNavigationRail",
            "NavigationRailItem" to "BuildkitNavigationRailItem",
            "TabRow" to "BuildkitTabRow",
            "Tab" to "BuildkitTab",
            "IconToggleButton" to "BuildkitIconToggleButton",
            "FilledIconToggleButton" to "BuildkitIconToggleButton",
            "FilledTonalIconToggleButton" to "BuildkitIconToggleButton",
            "OutlinedIconToggleButton" to "BuildkitIconToggleButton",
            "CenterAlignedTopAppBar" to "BuildkitTopAppBar",
            "SmallTopAppBar" to "BuildkitTopAppBar",
            "MediumTopAppBar" to "BuildkitTopAppBar",
            "LargeTopAppBar" to "BuildkitTopAppBar",
        )
        val RECEIVER_NAMES = mapOf(
            "Icons" to "BuildkitIcons",
        )

        // 触发 Lint 警告的统一入口
        fun reportIssue(
            context: JavaContext,
            node: UElement,
            name: String,
            preferredName: String,
        ) {
            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                "Using $name instead of $preferredName",
            )
        }
    }
}
