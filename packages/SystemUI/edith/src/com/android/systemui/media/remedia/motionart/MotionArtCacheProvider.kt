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

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle

/**
 * Lets Settings report and clear the motion art cache.
 *
 * The cache lives in this process's device-protected storage, which Settings cannot read, so the
 * settings screen talks to it through [call] instead of touching the files. Protected by
 * [android.Manifest.permission.STATUS_BAR_SERVICE]: the only caller is the platform-signed Settings
 * app, and the provider is not part of SystemUI's public surface.
 */
class MotionArtCacheProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? =
        when (method) {
            METHOD_STATS -> stats()
            METHOD_CLEAR -> {
                MotionArt.clearCache(attachedContext())
                stats()
            }
            else -> super.call(method, arg, extras)
        }

    private fun stats(): Bundle {
        val context = attachedContext()
        return Bundle().apply {
            putLong(KEY_BYTES, MotionArt.cacheBytes(context))
            putInt(KEY_FILES, MotionArt.cacheFiles(context))
        }
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private fun attachedContext() = requireNotNull(context) { "Provider is not attached" }

    companion object {
        const val AUTHORITY = "com.android.systemui.motionart"

        /** Returns [KEY_BYTES] and [KEY_FILES] for the cache. */
        const val METHOD_STATS = "stats"

        /** Deletes the cache and returns the fresh stats. */
        const val METHOD_CLEAR = "clear"

        const val KEY_BYTES = "bytes"
        const val KEY_FILES = "files"
    }
}
