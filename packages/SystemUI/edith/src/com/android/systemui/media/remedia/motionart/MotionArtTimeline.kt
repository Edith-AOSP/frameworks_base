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

import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Moves a motion art segment's timeline to zero without touching its media data.
 *
 * Apple's fragmented MP4 starts its fragments at an arbitrary decode time (10 s on the albums
 * checked) rather than at zero. The boxes are rewritten in place — every `tfdt` loses the first
 * one's base decode time — while the samples, their composition offsets, and their flags stay
 * exactly as encoded.
 *
 * Re-muxing the samples instead (extractor → muxer) is not an option: the loops use B-frames, so
 * the extractor reports presentation times out of order, and the muxer re-derives a decode timeline
 * from them. That flattens the frame cadence into an uneven one and is what makes the loop look
 * broken.
 */
object MotionArtTimeline {
    private const val TAG = "LockscreenMotionArt"

    /** Copies [source] to [target] with the timeline rebased. Returns false if there is none. */
    fun rewrite(source: File, target: File): Boolean {
        val bytes =
            try {
                source.readBytes()
            } catch (e: IOException) {
                Log.w(TAG, "Cannot read $source: $e")
                return false
            }
        if (!hasFtyp(bytes) || !shiftToZero(bytes)) return false
        return try {
            target.writeBytes(bytes)
            true
        } catch (e: IOException) {
            Log.w(TAG, "Cannot write $target: $e")
            false
        }
    }

    private fun hasFtyp(bytes: ByteArray): Boolean =
        bytes.size >= 12 && String(bytes, 4, 4, Charsets.US_ASCII) == "ftyp"

    /**
     * Subtracts the first `tfdt` base decode time from every `tfdt` in place. Returns whether a
     * `tfdt` was found, so a file this does not understand is left untouched.
     */
    private fun shiftToZero(bytes: ByteArray): Boolean {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        var origin = -1L
        var found = false

        fun walk(start: Int, end: Int) {
            var position = start
            while (position + 8 <= end) {
                val size32 = buffer.getInt(position).toLong() and 0xffffffffL
                val type = String(bytes, position + 4, 4, Charsets.US_ASCII)
                val header = if (size32 == 1L) 16 else 8
                val size =
                    when {
                        size32 == 0L -> (end - position).toLong()
                        size32 == 1L -> buffer.getLong(position + 8)
                        else -> size32
                    }
                if (size < header || position + size > end) return
                val payload = position + header
                when (type) {
                    "moof",
                    "traf" -> walk(payload, (position + size).toInt())
                    "tfdt" -> {
                        if (bytes[payload].toInt() == 1) {
                            val base = buffer.getLong(payload + 4)
                            if (origin < 0) origin = base
                            buffer.putLong(payload + 4, (base - origin).coerceAtLeast(0))
                        } else {
                            val base = buffer.getInt(payload + 4).toLong() and 0xffffffffL
                            if (origin < 0) origin = base
                            buffer.putInt(payload + 4, (base - origin).coerceAtLeast(0).toInt())
                        }
                        found = true
                    }
                }
                position = (position + size).toInt()
            }
        }

        walk(0, bytes.size)
        return found
    }
}
