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

package com.android.systemui.keyguard.ui.composable

import android.os.Handler
import android.os.Looper
import android.view.CrossWindowBlurListeners
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.common.shared.model.asImageBitmap
import com.android.systemui.keyguard.ui.viewmodel.LockscreenMediaArtBackgroundViewModel
import com.android.systemui.media.remedia.motionart.MotionArt
import com.android.systemui.media.remedia.motionart.FadingMotionArtVideo
import java.io.File
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * A full-screen blurred album-art backdrop, drawn behind the lock screen while the media player is
 * expanded. The art is blurred and dimmed so the clock and controls stay legible, and fades out
 * toward the bottom (matching the ColorOS "ambient cover" treatment).
 *
 * When the album has motion art, its tall (3:4) loop plays over the top of the backdrop instead of
 * the still image — full width, blurred and dimmed exactly like that still, and masked out toward
 * its bottom edge so it blends into the blur.
 *
 * It never shows a blank/wallpaper gap: an opaque base is always drawn once visible, the **last
 * known art** is kept while a new session's art is still being decoded (next/prev), and the art
 * cross-fades in when it arrives (ColorOS keeps the previous cover + a default cover placeholder).
 */
@Composable
fun LockscreenMediaArtBackground(
    viewModel: LockscreenMediaArtBackgroundViewModel,
    modifier: Modifier = Modifier,
) {
    // The art is blurred only while window blur is allowed. The still and the motion art share the
    // same setting (`Settings.Global.DISABLE_WINDOW_BLURS`, the accessibility "reduce transparency
    // and blur" toggle), so turning it off leaves both sharp.
    val blurEnabled = rememberBlurEnabled()

    // While expanding/expanded the backdrop fades in/out with the transition progress (like the
    // ColorOS panel alpha), rather than popping. The element is *always composed* (never removed
    // when the progress reaches 0): it is a scene element that the shade <-> QS transitions
    // reference, so removing it mid-gesture makes the shade vanish. Alpha carries the visibility.
    val alpha = viewModel.progress.coerceIn(0f, 1f)

    val artwork = viewModel.artwork
    val bitmap =
        remember(artwork) {
            (artwork as? Icon.Loaded)?.let {
                Icon.Loaded(it.drawable, contentDescription = null).asImageBitmap()
            }
        }

    // Hold the last decoded art so switching tracks (whose art is decoded asynchronously) keeps
    // showing the previous art instead of falling back to the wallpaper for a frame.
    var lastBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(bitmap) { if (bitmap != null) lastBitmap = bitmap }
    val shownBitmap = bitmap ?: lastBitmap

    // Motion art, resolved only while the player is expanded so no lookup or download runs for a
    // compact card. Both variants are fetched here: the tall one plays on this backdrop, the square
    // one is handed to the expanded player, which plays it inside the art square.
    val track = viewModel.track
    val expanded = viewModel.isExpanded
    var tallArt by remember { mutableStateOf<File?>(null) }
    val context = LocalContext.current
    LaunchedEffect(viewModel.isMotionArtEnabled, track, expanded) {
        tallArt = null
        viewModel.setSquareMotionArt(null)
        if (!viewModel.isMotionArtEnabled || !expanded || track == null) return@LaunchedEffect
        val (artist, title) = track
        tallArt = MotionArt.find(context, artist, title, MotionArt.Kind.TALL)
        viewModel.setSquareMotionArt(
            MotionArt.find(context, artist, title, MotionArt.Kind.SQUARE)
        )
    }

    // Opaque base so the wallpaper never shows through while there is no art at all; the whole
    // layer is alpha-ramped by the transition progress.
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
                .background(Color.Black)
    ) {
        if (shownBitmap != null) {
            Crossfade(targetState = shownBitmap, label = "MediaArtBackdrop") { fadeBitmap ->
                if (fadeBitmap != null) {
                    Image(
                        bitmap = fadeBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier.fillMaxSize()
                                // Blur so the clock and controls stay legible.
                                .artBlur(blurEnabled)
                                .drawWithContent {
                                    drawContent()
                                    // Dim.
                                    drawRect(color = Color.Black.copy(alpha = DIM_ALPHA))
                                    // Fade out toward the bottom.
                                    drawRect(
                                        brush =
                                            Brush.verticalGradient(
                                                0f to Color.Transparent,
                                                BOTTOM_FADE_START to Color.Transparent,
                                                1f to Color.Black,
                                            )
                                    )
                                },
                    )
                }
            }
        }

        // The album's tall 3:4 loop, full width from the top, with the same blur and dim as the
        // still above (it is the same art, moving) and masked out toward its bottom edge so it
        // blends into the blurred still underneath. It fades back out over the still when the track
        // changes, alongside the still fading in.
        if (expanded) {
            FadingMotionArtVideo(
                file = tallArt,
                crop = true,
                modifier =
                    Modifier.align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .aspectRatio(TALL_ART_ASPECT)
                        .artBlur(blurEnabled)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            // Dim, exactly like the still.
                            drawRect(color = Color.Black.copy(alpha = DIM_ALPHA))
                            drawRect(
                                brush =
                                    Brush.verticalGradient(
                                        MOTION_ART_FADE_START to Color.Black,
                                        1f to Color.Transparent,
                                    ),
                                blendMode = BlendMode.DstIn,
                            )
                        },
            )
        }
    }
}

private val BLUR_RADIUS = 24.dp

/** Blurs the art unless the system blur toggle is off (see [rememberBlurEnabled]). */
private fun Modifier.artBlur(enabled: Boolean): Modifier =
    if (enabled) blur(BLUR_RADIUS) else this

/**
 * Whether the system allows blur, following the live accessibility toggle
 * (`Settings.Global.DISABLE_WINDOW_BLURS`, surfaced by `CrossWindowBlurListeners`) rather than a
 * snapshot, so turning it on/off takes effect without leaving the lock screen.
 */
@Composable
private fun rememberBlurEnabled(): Boolean {
    val listeners = remember { CrossWindowBlurListeners.getInstance() }
    // The listener is called on a binder thread; Compose state is only touched on the main thread.
    val handler = remember { Handler(Looper.getMainLooper()) }
    var enabled by remember { mutableStateOf(listeners.isCrossWindowBlurEnabled) }
    DisposableEffect(listeners) {
        val executor = Executor { handler.post(it) }
        val listener = Consumer<Boolean> { enabled = it }
        listeners.addListener(executor, listener)
        onDispose { listeners.removeListener(listener) }
    }
    return enabled
}

/** Extra dimming on top of the blur, so the clock stays legible over bright album art. */
private const val DIM_ALPHA = 0.35f

/** Fraction of the height at which the bottom fade begins. */
private const val BOTTOM_FADE_START = 0.55f

/** Aspect ratio of the tall motion art. Apple Music ships 3:4 vertical loops. */
private const val TALL_ART_ASPECT = 3f / 4f

/** Where the motion art starts fading into the backdrop below it. */
private const val MOTION_ART_FADE_START = 0.6f
