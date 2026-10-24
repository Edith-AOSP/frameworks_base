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

package com.android.systemui.keyguard.ui.composable.elements

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.core.graphics.drawable.toBitmap
import com.android.compose.ui.graphics.painter.rememberDrawablePainter
import com.android.systemui.media.remedia.ui.compose.Media
import com.android.systemui.qs.edith.EdithNotificationChipState
import com.android.systemui.res.R

/**
 * The lock screen notification summary shown while the expanded media player is up.
 *
 * It replaces the notification shelf (hidden while the player is expanded) with one pill: the
 * original-colour app icon of the first notification on a filled circle, its count, and the next
 * few app icons underneath. Tapping it opens the locked shade, like the shelf did.
 *
 * The pill rides in the shortcut row, so it shares the same bottom edge as the camera and
 * flashlight circles.
 *
 * @param compact whether the pill has shrunk to the icon and the bare count so the charging chip
 *   can have the room beside it.
 * @param connected how joined this pill is to the charging chip, `0f` = apart, `1f` = the facing
 *   corner squared off and the gap closed. Pressing the pill tightens that corner further, the way
 *   an M3 button group morphs against its neighbour.
 * @param growFast use the charging chip's faster close curve for a grow-back, so the pill opens on
 *   the same beat as the chip folding away instead of trailing it.
 */
@Composable
fun LockscreenNotificationChip(
    count: Int,
    icons: List<Drawable>,
    containerColor: Color,
    contentColor: Color,
    interactive: Boolean = true,
    compact: Boolean = false,
    connected: Float = 0f,
    growFast: Boolean = false,
    dimAmount: Float = 0f,
    chipState: EdithNotificationChipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val countDescription =
        pluralStringResource(R.plurals.edith_lockscreen_notification_count, count, count)
    val joined = connected.coerceIn(0f, 1f)
    val pressedAmount by
        animateFloatAsState(
            targetValue = if (pressed && joined > 0.5f) 1f else 0f,
            animationSpec = ShapeSpring,
            label = "notificationChipPressed",
        )

    val pillRadius = ChipHeight / 2
    // The pill sits after the charging chip in the row, so its facing side is always its start side
    // — in RTL the row order and the start/end corners both mirror together.
    val facingRadius = lerp(lerp(pillRadius, JoinedRadius, joined), PressedRadius, pressedAmount)
    val shape =
        RoundedCornerShape(
            topStart = facingRadius,
            bottomStart = facingRadius,
            topEnd = pillRadius,
            bottomEnd = pillRadius,
        )

    Row(
        modifier =
            modifier
                // No draw offset and no scale: the pill is placed purely by layout. A spring
                // translation here left the pill ~18dp below its layout slot when the animation
                // stopped short, which no amount of layout padding could correct.
                .requiredHeight(ChipHeight)
                .clip(shape)
                .background(containerColor)
                // Inside the clip and the background, so the pill itself grows with the animation
                // instead of snapping to the new width while only the space around it moves.
                .animateContentSize(if (growFast) CloseContentSizeSpring else ContentSizeSpring)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = interactive,
                    onClick = onClick,
                )
                .padding(start = 8.dp, end = if (compact) 12.dp else 14.dp)
                .onGloballyPositioned { chipState.centre = it.boundsInRoot().center },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FirstIcon(icons.firstOrNull(), dimAmount)
        Spacer(Modifier.width(10.dp))
        if (compact) {
            // The number is the whole content here, so it is what reacts to a notification:
            // a quick hop up when one arrives, down when one goes away, then it settles.
            val nudge = remember { Animatable(0f) }
            var previousCount by remember { mutableIntStateOf(count) }
            val nudgePx = with(LocalDensity.current) { 4.dp.toPx() }
            LaunchedEffect(count) {
                if (count == previousCount) return@LaunchedEffect
                val direction = if (count > previousCount) -1f else 1f
                previousCount = count
                nudge.snapTo(0f)
                nudge.animateTo(
                    targetValue = direction * nudgePx,
                    animationSpec = tween(90, easing = LinearOutSlowInEasing),
                )
                nudge.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
                )
            }
            Text(
                text = count.toString(),
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                // The word is gone, but not for TalkBack: announce the full count as before.
                modifier =
                    Modifier.clearAndSetSemantics { contentDescription = countDescription }
                        .graphicsLayer { translationY = nudge.value },
            )
        } else {
            Column {
                Text(
                    text = countDescription,
                    color = contentColor,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                )
                if (icons.size > 1) {
                    Spacer(Modifier.height(3.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        icons.drop(1).forEach { icon ->
                            // A faint disc behind each face keeps dark or mostly-transparent app
                            // icons from blending into the pill.
                            Box(
                                Modifier.size(SmallIconSize + 8.dp)
                                    .background(
                                        contrastCircleFor(icon, contentColor.copy(alpha = 0.12f)),
                                        CircleShape,
                                    )
                                    .padding(4.dp)
                            ) {
                                AppIcon(icon, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FirstIcon(icon: Drawable?, dimAmount: Float) {
    val dim = dimAmount.coerceIn(0f, 1f)
    val circleColor = lerpColor(MaterialTheme.colorScheme.primary, Color.Black, dim)
    val glyphColor = lerpColor(MaterialTheme.colorScheme.onPrimary, Color.Black, dim)
    Box(
        modifier =
            Modifier.size(FirstIconCircle)
                // Same treatment as the charging badge: the cookie's primary fill with the
                // glyph in onPrimary, so the two chips read as one pair.
                .background(circleColor, CircleShape)
                .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            AppIcon(
                icon,
                Modifier.fillMaxSize(),
                tint = glyphColor,
            )
        }
    }
}

/**
 * A circle that contrasts with the icon drawn on it. App icons and notification glyphs can be
 * almost white or almost black, and a fixed translucent tint made some of them disappear.
 */
@Composable
private fun contrastCircleFor(drawable: Drawable?, fallback: Color): Color =
    if (drawable == null) {
        fallback
    } else {
        remember(drawable) {
            val luminance = drawable.averageLuminance()
            if (luminance > 0.55f) {
                Color.Black.copy(alpha = 0.45f)
            } else {
                Color.White.copy(alpha = 0.9f)
            }
        }
    }

private fun Drawable.averageLuminance(): Float {
    val bitmap =
        try {
            toBitmap(width = 16, height = 16)
        } catch (e: Exception) {
            return 0.5f
        }
    var sum = 0f
    var count = 0
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel ushr 24) / 255f
            if (alpha < 0.1f) continue
            val red = (pixel shr 16 and 0xff) / 255f
            val green = (pixel shr 8 and 0xff) / 255f
            val blue = (pixel and 0xff) / 255f
            sum += (0.299f * red + 0.587f * green + 0.114f * blue) * alpha
            count++
        }
    }
    return if (count == 0) 0.5f else sum / count
}

@Composable
private fun AppIcon(drawable: Drawable, modifier: Modifier, tint: Color? = null) {
    Image(
        painter = rememberDrawablePainter(drawable),
        contentDescription = null,
        colorFilter = tint?.let { ColorFilter.tint(it) },
        modifier = modifier,
    )
}

// The same height as the quick affordances either side of it, so the pill and the shortcut circles
// share one band.
private val ChipHeight = 48.dp
private val FirstIconCircle = 32.dp
private val SmallIconSize = 12.dp

/** Facing corner radius while joined with the charging chip, M3's "small" corner. */
private val JoinedRadius = 8.dp

/** The same corner while the pill is pressed: a nudge, not a bounce. */
private val PressedRadius = 4.dp

private val ContentSizeSpring =
    spring<IntSize>(
        dampingRatio = Media.MORPH_SPRING_DAMPING,
        stiffness = Media.MORPH_SPRING_STIFFNESS,
    )

/** The grow-back while the charging chip folds away: the same beat as that close. */
private val CloseContentSizeSpring =
    spring<IntSize>(dampingRatio = 0.65f, stiffness = 170f)

/** The corner morph rides the same spring as the chips' entrance. */
private val ShapeSpring =
    spring<Float>(
        dampingRatio = Media.MORPH_SPRING_DAMPING,
        stiffness = Media.MORPH_SPRING_STIFFNESS,
    )
