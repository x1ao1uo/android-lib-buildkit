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

// HiltComponentActivity：测试专用、标注了 @AndroidEntryPoint 的空 ComponentActivity。
// 作为 Robolectric / Compose UI 测试的宿主，规避 https://github.com/google/dagger/issues/3394 的限制。

package com.z1nt.buildkit.uitesthiltmanifest

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * A [ComponentActivity] annotated with [AndroidEntryPoint] for use in tests, as a workaround
 * for https://github.com/google/dagger/issues/3394
 */
@AndroidEntryPoint
class HiltComponentActivity : ComponentActivity()
