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

package com.android.systemui.media.remedia.ui.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.common.shared.model.asImageBitmap
import com.android.systemui.media.remedia.ui.viewmodel.MediaCardViewModel
import com.android.systemui.res.R
import kotlinx.coroutines.launch

/**
 * The Edith expanded lock screen media player.
 *
 * A **single shared album-art layer** morphs, iOS style, between two geometries as [progress] goes
 * `0f..1f`:
 * - collapsed = a full-width rounded **banner** (the media card's art silhouette), and
 * - expanded = a centred rounded **square** (inset by [R.dimen.edith_media_art_side_margin]).
 *
 * The card's own art background is suppressed in this path (the caller sets
 * `drawArtBackground = false`), so this layer is the only art: there is never an art-less frame —
 * which is what used to flicker the wallpaper on tap — and the same art visibly morphs in and out.
 *
 * The media content is composed below the art and is offset by the *animated* art height, so it
 * slides down/up as the art grows/shrinks.
 *
 * Collapsing: tapping the art, or dragging down.
 */
@Composable
fun ExpandedMediaArt(
    card: MediaCardViewModel,
    progress: Animatable<Float, AnimationVector1D>,
    onCollapse: () -> Unit,
    onProgressChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    // Height of the collapsed card's art banner. Matches the active presentation style so the
    // collapsed card keeps its fixed height (e.g. the compact two-row card is shorter).
    collapsedHeight: Dp,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // M3 expressive spatial spring, tuned slower + bouncier for the media morph: a lower damping
    // ratio (more overshoot/bounce) and lower stiffness (longer, slower settle) than the default
    // expressive spatial spec. Captured here because motionScheme is a @Composable read and cannot
    // be accessed inside the drag coroutine lambdas below.
    val spatialSpec =
        spring<Float>(dampingRatio = MEDIA_SPRING_DAMPING, stiffness = MEDIA_SPRING_STIFFNESS)

    // Report progress so the ambient backdrop can track it.
    LaunchedEffect(progress) {
        snapshotFlow { progress.value }.collect(onProgressChanged)
    }

    val sideMargin = dimensionResource(R.dimen.edith_media_art_side_margin)
    val cornerRadius = dimensionResource(R.dimen.edith_media_art_corner_radius)
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val fullWidth = maxWidth
        // Everything the lock screen leaves between the clock/smartspace and the bottom of the upper
        // region. Unbounded in previews, so fall back to the collapsed card there.
        val availableHeight = if (maxHeight.value.isFinite()) maxHeight else collapsedHeight

        // Measured height of the controls (the card content). They are anchored to the bottom and
        // the art takes whatever is left above them, so this is what keeps the player fitting no
        // matter how much room the clock/smartspace above it takes. Keyed on the collapsed height so
        // a presentation-style change re-measures instead of reusing the old card's height.
        var contentHeightPx by remember(collapsedHeight) { mutableIntStateOf(0) }
        val contentHeight =
            if (contentHeightPx > 0) with(density) { contentHeightPx.toDp() } else collapsedHeight

        // Space above the controls that the art can use.
        val artSpace = (availableHeight - contentHeight).coerceAtLeast(0.dp)
        val artGap = dimensionResource(R.dimen.edith_media_art_controls_gap)

        // Raw (unclamped) progress: the M3 expressive spatial spring overshoots its rest, and we let
        // that overshoot drive the geometry so the morph lands with a real, physical bounce.
        val raw = progress.value
        val clamped = raw.coerceIn(0f, 1f)

        // Square art side: one scalar drives both dimensions, so the art can only shrink — it never
        // stretches out of its 1:1 aspect ratio — and it never takes more than the slot leaves after
        // the gap. This is what makes the art shrink instead of pushing the controls off the bottom
        // when the clock/smartspace above the player take more room.
        val squareSide =
            (fullWidth - sideMargin * 2)
                .coerceAtMost((artSpace - artGap).coerceAtLeast(0.dp))
                .coerceAtLeast(0.dp)

        // Animated art geometry. Clamp only to keep sizes non-negative (Compose layout would crash),
        // which still preserves the spring's overshoot in the dimensions that can grow.
        val artHeight = (collapsedHeight + (squareSide - collapsedHeight) * raw).coerceAtLeast(0.dp)
        val artWidth = (fullWidth + (squareSide - fullWidth) * raw).coerceAtLeast(0.dp)

        // The block is content-sized, not slot-sized: the art plus the controls, plus the gap between
        // them. Sizing it to the available height instead would turn any slack into dead space above
        // the art — which is what pushed the whole player to the bottom of the screen when the lock
        // screen was measured with a larger constraint (e.g. right after coming out of AOD).

        // The controls slide down as the player expands: at progress 0 they sit on top of the art
        // (the collapsed card, art as the card backing); at progress 1 they are fully below it,
        // separated by [artGap].
        val contentTop = (artHeight + artGap) * clamped

        Box(Modifier.fillMaxWidth()) {
            // Art layer, drawn first so it is behind the controls (matching the collapsed card).
            MorphingArt(
                image = card.background,
                scrimColor = card.colorScheme?.background,
                progress = progress,
                artWidth = artWidth,
                artHeight = artHeight,
                cornerRadius = cornerRadius,
                modifier = Modifier,
                onCollapse = onCollapse,
                onDrag = { dy ->
                    scope.launch {
                        progress.snapTo((progress.value - dy / DRAG_RANGE_PX).coerceIn(0f, 1f))
                    }
                },
                onDragEnd = {
                    if (progress.value < DRAG_COLLAPSE_THRESHOLD) {
                        scope.launch {
                            progress.animateTo(0f, spatialSpec)
                        }
                        onCollapse()
                    } else {
                        scope.launch {
                            progress.animateTo(1f, spatialSpec)
                        }
                    }
                },
            )

            Box(Modifier.fillMaxWidth().padding(top = contentTop)) {
                // Measured *without* the padding above, so this is the controls' own height.
                Box(Modifier.onSizeChanged { contentHeightPx = it.height }) { content() }
            }
        }
    }
}

/**
 * The shared album-art layer: one art box whose width/height/corner radius interpolate with
 * [progress] from the collapsed banner to the expanded square. Its layout height follows the art, so
 * the content below slides with it.
 */
@Composable
private fun MorphingArt(
    image: Icon?,
    scrimColor: Color?,
    progress: Animatable<Float, AnimationVector1D>,
    artWidth: Dp,
    artHeight: Dp,
    cornerRadius: Dp,
    onCollapse: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bitmap =
        remember(image) {
            (image as? Icon.Loaded)?.let {
                Icon.Loaded(it.drawable, contentDescription = null).asImageBitmap()
            }
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(artHeight)
                // A single gesture detector for both tap (collapse) and vertical drag, so the two
                // don't compete on the same node.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragged = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change =
                                event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.changedToUpIgnoreConsumed()) {
                                break
                            }
                            val dy = change.positionChange().y
                            if (dy != 0f) {
                                dragged = true
                                change.consume()
                                onDrag(dy)
                            }
                        }
                        if (!dragged) {
                            // A tap with no movement -> collapse.
                            onCollapse()
                        } else {
                            onDragEnd()
                        }
                    }
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier.width(artWidth)
                    .height(artHeight)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(if (bitmap == null) Color(0xFF2A2A2A) else Color.Transparent)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier.fillMaxWidth().drawWithContent {
                            scale(
                                scaleX = Media.ART_OVER_CROP_SCALE,
                                scaleY = Media.ART_OVER_CROP_SCALE,
                            ) {
                                this@drawWithContent.drawContent()
                            }
                            // When collapsed, overlay the album-art colour scrim (the same flat
                            // 50% scrim the card background draws); fade it out as the art morphs
                            // into the expanded square, which has no scrim.
                            if (scrimColor != null) {
                                val scrimAlpha = (1f - progress.value).coerceIn(0f, 1f)
                                if (scrimAlpha > 0f) {
                                    drawRect(color = scrimColor.copy(alpha = 0.5f * scrimAlpha))
                                }
                            }
                        },
                )
            }
        }
    }
}

/** Drag distance (px) that corresponds to the full collapsed -> expanded range. */
private const val DRAG_RANGE_PX = 600f

/** Progress below which a released drag collapses instead of springing back open. */
private const val DRAG_COLLAPSE_THRESHOLD = 0.75f

/** Media morph spring: lower damping = bouncier, lower stiffness = slower settle. */
private const val MEDIA_SPRING_DAMPING = 0.45f
private const val MEDIA_SPRING_STIFFNESS = 320f
