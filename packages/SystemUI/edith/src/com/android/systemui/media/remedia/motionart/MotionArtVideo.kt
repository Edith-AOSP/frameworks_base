/*
 * Copyright (C) 2026 The PenguinOS Project
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

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.IOException
import kotlinx.coroutines.delay

/**
 * How long the player takes to fade in over the static art once it has a frame, and to fade out of
 * it again. The still stays fully drawn underneath for that long, so the two fades form the
 * static <-> animated crossfade in both directions.
 */
const val MOTION_ART_FADE_IN_MS = 400

/**
 * Plays a [MotionArt] video, looping and silent, in a [TextureView].
 *
 * The view stays transparent until the first frame is rendered and then fades in, so the static
 * cover underneath is what the user sees while the decoder warms up (and permanently if playback
 * fails).
 */
@Composable
fun MotionArtVideo(
    file: File,
    modifier: Modifier = Modifier,
    crop: Boolean = false,
    onRendering: () -> Unit = {},
) {
    key(file) {
        var rendering by remember { mutableStateOf(false) }
        val alpha by
            animateFloatAsState(
                targetValue = if (rendering) 1f else 0f,
                animationSpec =
                    tween(durationMillis = MOTION_ART_FADE_IN_MS, easing = LinearEasing),
                label = "motionArt",
            )
        AndroidView(
            factory = { context ->
                TextureView(context).apply {
                    isOpaque = false
                    surfaceTextureListener =
                        LoopingPlayer(this, file, crop) {
                            rendering = true
                            onRendering()
                        }
                }
            },
            onRelease = { (it.surfaceTextureListener as? LoopingPlayer)?.release() },
            modifier = modifier.graphicsLayer { this.alpha = alpha },
        )
    }
}

/**
 * A [MotionArtVideo] that also fades **out** when [file] goes away (or is replaced), so the animated
 * art dissolves back into the still instead of popping off — e.g. a track change to an album whose
 * motion art has not been resolved yet.
 *
 * The outgoing file stays composed for the length of the fade: unmounting the player in the frame
 * the file goes away (as `file?.let { MotionArtVideo(...) }` does) would leave nothing to fade.
 */
@Composable
fun FadingMotionArtVideo(
    file: File?,
    modifier: Modifier = Modifier,
    crop: Boolean = false,
    onRendering: () -> Unit = {},
) {
    var shown by remember { mutableStateOf<File?>(null) }
    LaunchedEffect(file) {
        if (file != null) {
            shown = file
        } else if (shown != null) {
            delay(MOTION_ART_FADE_IN_MS.toLong())
            shown = null
        }
    }
    val alpha by
        animateFloatAsState(
            targetValue = if (file != null) 1f else 0f,
            animationSpec = tween(durationMillis = MOTION_ART_FADE_IN_MS, easing = LinearEasing),
            label = "motionArtLeave",
        )
    shown?.let { current ->
        MotionArtVideo(
            file = current,
            modifier = modifier.graphicsLayer { this.alpha = alpha },
            crop = crop,
            onRendering = onRendering,
        )
    }
}

private class LoopingPlayer(
    private val view: TextureView,
    private val file: File,
    private val crop: Boolean,
    private val onRendering: () -> Unit,
) : TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        release()
        val output = Surface(texture)
        surface = output
        player =
            MediaPlayer().apply {
                setSurface(output)
                isLooping = true
                setVolume(0f, 0f)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) onRendering()
                    false
                }
                setOnErrorListener { _, _, _ -> true }
                setOnPreparedListener { it.start() }
                if (crop) {
                    setOnVideoSizeChangedListener { _, width, height -> cropTo(width, height) }
                }
                try {
                    setDataSource(file.path)
                    prepareAsync()
                } catch (e: IOException) {
                    Log.w("LockscreenMotionArt", "Cannot play $file: $e")
                }
            }
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        release()
        return true
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        val player = player ?: return
        if (crop) cropTo(player.videoWidth, player.videoHeight)
    }

    private fun cropTo(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0 || view.width <= 0 || view.height <= 0) return
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()
        val scale = maxOf(viewWidth / videoWidth, viewHeight / videoHeight)
        view.setTransform(
            Matrix().apply {
                setScale(
                    videoWidth * scale / viewWidth,
                    videoHeight * scale / viewHeight,
                    viewWidth / 2f,
                    viewHeight / 2f,
                )
            }
        )
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}

    fun release() {
        player?.release()
        player = null
        surface?.release()
        surface = null
    }
}
