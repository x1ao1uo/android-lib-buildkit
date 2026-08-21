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

// GradleManagedDevices：为 buildkit 注册一组托管虚拟设备（Pixel 4/6/C），并暴露一个 ci 设备组供 CI 使用。

package com.z1nt.buildkit

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke

/**
 * Configure project for Gradle managed devices
 *
 * 注册的设备：
 * - Pixel 4 (API 30, aosp-atd)
 * - Pixel 6 (API 31, aosp)
 * - Pixel C (API 30, aosp-atd)
 *
 * 其中 ci 设备组仅包含 Pixel 4 + Pixel C，用于快速 CI smoke 测试。
 */
internal fun configureGradleManagedDevices(commonExtension: CommonExtension) {
    val pixel4 = DeviceConfig("Pixel 4", 30, "aosp-atd")
    val pixel6 = DeviceConfig("Pixel 6", 31, "aosp")
    val pixelC = DeviceConfig("Pixel C", 30, "aosp-atd")

    val allDevices = listOf(pixel4, pixel6, pixelC)
    val ciDevices = listOf(pixel4, pixelC)

    commonExtension.testOptions.apply {
        @Suppress("UnstableApiUsage")
        managedDevices {
            allDevices {
                // 全量注册 3 台设备
                allDevices.forEach { deviceConfig ->
                    maybeCreate(deviceConfig.taskName, ManagedVirtualDevice::class.java).apply {
                        device = deviceConfig.device
                        apiLevel = deviceConfig.apiLevel
                        systemImageSource = deviceConfig.systemImageSource
                    }
                }
            }
            groups {
                // CI 用的精简设备组
                maybeCreate("ci").apply {
                    ciDevices.forEach { deviceConfig ->
                        targetDevices.add(localDevices[deviceConfig.taskName])
                    }
                }
            }
        }
    }
}

// 设备配置数据类，taskName 由 device + api + systemImageSource 拼接得到
private data class DeviceConfig(
    val device: String,
    val apiLevel: Int,
    val systemImageSource: String
) {
    val taskName = buildString {
        append(device.lowercase().replace(" ", ""))
        append("api")
        append(apiLevel.toString())
        append(systemImageSource.replace("-", ""))
    }
}
