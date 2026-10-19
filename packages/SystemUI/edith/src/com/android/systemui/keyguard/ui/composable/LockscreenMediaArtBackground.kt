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

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.common.shared.model.asImageBitmap
import com.android.systemui.keyguard.ui.viewmodel.LockscreenMediaArtBackgroundViewModel

/**
 * A full-screen blurred album-art backdrop, drawn behind the lock screen while the media player is
 * expanded. The art is blurred and dimmed so the clock and controls stay legible, and fades out
 * toward the bottom (matching the ColorOS "ambient cover" treatment).
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
                                .blur(BLUR_RADIUS)
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
    }
}

private val BLUR_RADIUS = 24.dp

/** Extra dimming on top of the blur, so the clock stays legible over bright album art. */
private const val DIM_ALPHA = 0.35f

/** Fraction of the height at which the bottom fade begins. */
private const val BOTTOM_FADE_START = 0.55f
