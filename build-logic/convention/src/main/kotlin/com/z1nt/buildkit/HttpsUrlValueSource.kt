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

// HttpsUrlValueSource：一个 Gradle ValueSource，用于校验某个字符串是否为合法 HTTPS URL。
// 允许配置一个特殊的"白名单 HTTP URL"（例如内网仓库），用于版本号下载等场景的安全拦截。

package com.z1nt.buildkit

import java.net.URI
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

// Gradle ValueSource：可在配置阶段同步运行
abstract class HttpsUrlValueSource : ValueSource<String, HttpsUrlValueSource.Parameters> {
    // ValueSource 的入参：被校验 URL、允许的特例 HTTP URL、校验失败时的错误信息
    interface Parameters : ValueSourceParameters {
        val url: Property<String>
        val allowedHttpUrl: Property<String>
        val errorMessage: Property<String>
    }

    override fun obtain(): String {
        val value = parameters.url.get()
        val uri = runCatching { URI(value) }.getOrNull()
        // 必须是 HTTPS 且 host 非空，否则需要命中白名单特例
        val isHttps = uri?.scheme.equals("https", ignoreCase = true) && !uri?.host.isNullOrBlank()
        require(isHttps || value == parameters.allowedHttpUrl.get()) {
            parameters.errorMessage.get()
        }
        return value
    }
}
