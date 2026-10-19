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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.layout.layout
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
 * The controls are the anchor: they are pinned to the bottom of the space the lock screen leaves
 * for the player, and the art is bottom-aligned just above them, so the art is what shrinks when
 * room runs short and the controls never move or get squashed.
 *
 * Collapsing: tapping the art, or dragging down.
 */
@Composable
fun ExpandedMediaArt(
    card: MediaCardViewModel,
    progress: Animatable<Float, AnimationVector1D>,
    // Drives the block height and the controls' slide at the morph spring's frequency, critically
    // damped. The morph spring's tail crosses its target every half period, and in the block height
    // it wobbled the layout by a few dp just after the morph had settled, shaking the controls and
    // the notification stack below.
    layoutProgress: Animatable<Float, AnimationVector1D>,
    onCollapse: () -> Unit,
    onProgressChanged: (Float) -> Unit = {},
    modifier: Modifier = Modifier,
    // Height of the collapsed card's art banner. Matches the active presentation style so the
    // collapsed card keeps its fixed height (e.g. the compact two-row card is shorter).
    collapsedHeight: Dp,
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    // The media morph spring, shared with the tap-to-expand animation in `Media.kt`, so a drag
    // release and a tap settle identically. Held in a val because the drag coroutine lambdas below
    // are not composable.
    val spatialSpec =
        spring<Float>(dampingRatio = Media.MORPH_SPRING_DAMPING, stiffness = Media.MORPH_SPRING_STIFFNESS)
    // Critically damped at the same frequency, so the layout arrives without crossing its target.
    val layoutSpec = spring<Float>(dampingRatio = 1f, stiffness = Media.MORPH_SPRING_STIFFNESS)

    // The ambient backdrop tracks this progress.
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

        val artGap = dimensionResource(R.dimen.edith_media_art_controls_gap)

        // Two progresses, one per spring: the bouncy morph spring drives the art, the critically
        // damped layout spring drives the block height and the controls' slide.
        val raw = progress.value
        val clamped = layoutProgress.value.coerceIn(0f, 1f)

        // The morph spring overshoots in both directions. The forward overshoot (progress > 1) is
        // the art bouncing past its rest square and is kept; the backward one (below 0 while the
        // collapse settles) would shrink the art under the collapsed card and pop back, so the art
        // geometry stops at the collapsed state.
        val artProgress = raw.coerceAtLeast(0f)

        // The controls are the anchor, not the art: the player is as tall as the space the lock
        // screen leaves for it and no taller, so with slack the block ends right below the controls
        // instead of stretching them to the bottom of the screen.
        val artMaxSide = (fullWidth - sideMargin * 2).coerceAtLeast(0.dp)
        val naturalHeight = artMaxSide + artGap + contentHeight
        val expandedBlockHeight = naturalHeight.coerceAtMost(availableHeight)

        // The controls' top when expanded, measured up from the bottom of the block, so the art
        // never moves them. Height the art does not use stays in the gap above them; when space runs
        // short it is the art that gives way, still 1:1.
        val expandedContentTop = (expandedBlockHeight - contentHeight).coerceAtLeast(0.dp)
        val artSpace = (expandedContentTop - artGap).coerceAtLeast(0.dp)
        val squareSide = artMaxSide.coerceAtMost(artSpace).coerceAtLeast(0.dp)

        // Animated art geometry. The sizes are clamped only to stay non-negative (Compose layout
        // would crash); the spring's forward overshoot is kept in the dimensions that can grow.
        val artHeight = (collapsedHeight + (squareSide - collapsedHeight) * artProgress).coerceAtLeast(0.dp)
        val artWidth = (fullWidth + (squareSide - fullWidth) * artProgress).coerceAtLeast(0.dp)

        // Bottom-anchored art: its *bottom* edge sits [artGap] above the controls, so when the art is
        // capped by height it shrinks towards the controls instead of drifting up, and the spring's
        // overshoot grows it upwards rather than into them.
        val artTop = (artSpace - artHeight) * clamped

        // The block animates from the collapsed card's height to the anchored height above.
        val blockHeight =
            (collapsedHeight + (expandedBlockHeight - collapsedHeight) * clamped)
                .coerceAtLeast(0.dp)

        // The controls slide down as the player expands: at progress 0 they sit on top of the art
        // (the collapsed card, art as the card backing); at progress 1 they are at the bottom of the
        // block, separated from the art by [artGap].
        val contentTop = expandedContentTop * clamped

        // The animated block height is reported without shrinking the constraints the children are
        // measured with. `height(blockHeight)` would cap the controls at the block's current height,
        // so early in the expansion they would measure short, that short height would feed back into
        // [contentHeight], and the art would grow in response.
        Box(
            modifier =
                Modifier.fillMaxWidth().layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, blockHeight.roundToPx()) { placeable.place(0, 0) }
                }
        ) {
            // Art layer, drawn first so it is behind the controls (matching the collapsed card).
            MorphingArt(
                image = card.background,
                scrimColor = card.colorScheme?.background,
                progress = progress,
                artWidth = artWidth,
                artHeight = artHeight,
                cornerRadius = cornerRadius,
                modifier = Modifier.offset(y = artTop),
                onCollapse = onCollapse,
                onDrag = { dy ->
                    val next = (progress.value - dy / DRAG_RANGE_PX).coerceIn(0f, 1f)
                    scope.launch {
                        progress.snapTo(next)
                        layoutProgress.snapTo(next)
                    }
                },
                onDragEnd = {
                    val target = if (progress.value < DRAG_COLLAPSE_THRESHOLD) 0f else 1f
                    scope.launch {
                        launch { progress.animateTo(target, spatialSpec) }
                        launch { layoutProgress.animateTo(target, layoutSpec) }
                    }
                    if (target == 0f) {
                        onCollapse()
                    }
                },
            )

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        // The controls are measured with the block's own constraints and only placed
                        // at [contentTop]. Measuring them inside an animated top padding gave them a
                        // shorter constraint as the art grew, which squashed them and fed that height
                        // back into the art size.
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val top = contentTop.roundToPx()
                            layout(placeable.width, placeable.height) { placeable.place(0, top) }
                        }
            ) {
                Box(Modifier.onSizeChanged { contentHeightPx = it.height }) { content() }
            }
        }
    }
}

/**
 * The shared album-art layer: one art box whose width/height/corner radius interpolate with
 * [progress] from the collapsed banner to the expanded square. It is bottom-aligned in the space the
 * controls leave, so the art shrinks towards the controls rather than pushing them.
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
