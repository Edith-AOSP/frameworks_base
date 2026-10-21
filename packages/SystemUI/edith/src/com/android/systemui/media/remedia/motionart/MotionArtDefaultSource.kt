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
import javax.inject.Inject

/**
 * The default [MotionArtSource]: the in-tree resolver, used when nothing else is bound.
 *
 * A build that ships its own resolver (the Edith SystemUI APK) binds a source with a higher
 * [priority] and this one is never consulted.
 */
class MotionArtDefaultSource @Inject constructor() : MotionArtSource {
    override val priority: Int = 0

    override suspend fun find(
        context: Context,
        artist: String,
        title: String,
        tall: Boolean,
    ): File? =
        MotionArt.find(
            context,
            artist,
            title,
            if (tall) MotionArt.Kind.TALL else MotionArt.Kind.SQUARE,
        )
}
