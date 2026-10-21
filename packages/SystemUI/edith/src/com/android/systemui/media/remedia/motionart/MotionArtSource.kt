/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.media.remedia.motionart

import android.content.Context
import java.io.File

/**
 * Resolves the motion (video) album art of a track.
 *
 * Bindings are collected into a set and the highest [priority] wins, so a build that ships its own
 * implementation (the Edith SystemUI APK) replaces the default one without a second binding for the
 * same type. The signature is deliberately free of the in-tree resolver's types so an implementation
 * can live in a different package (and a different APK) without depending on it.
 */
interface MotionArtSource {
    /** Higher wins when several sources are bound. */
    val priority: Int

    /**
     * The loop for [artist]/[title], or `null` when the album has none. [tall] selects the backdrop
     * shape (`true`) or the square one the expanded player draws (`false`).
     */
    suspend fun find(context: Context, artist: String, title: String, tall: Boolean): File?
}
