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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.compose.ui.graphics.painter.rememberDrawablePainter
import com.android.systemui.qs.edith.EdithNotificationChipState
import com.android.systemui.res.R

/**
 * The lock screen notification summary shown while the expanded media player is up.
 *
 * It replaces the notification shelf (hidden while the player is expanded) with one pill: the
 * original-colour app icon of the first notification on a filled circle, its count, and the next few
 * app icons underneath. Tapping it opens the locked shade, like the shelf did.
 *
 * The pill rides in the shortcut row, so it shares the same bottom edge as the camera and
 * flashlight circles.
 */
@Composable
fun LockscreenNotificationChip(
    count: Int,
    icons: List<Drawable>,
    containerColor: Color,
    contentColor: Color,
    circleColor: Color?,
    interactive: Boolean = true,
    chipState: EdithNotificationChipState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                // No draw offset and no scale: the pill is placed purely by layout. A spring
                // translation here left the pill ~18dp below its layout slot when the animation
                // stopped short, which no amount of layout padding could correct.
                .requiredHeight(ChipHeight)
                .clip(RoundedCornerShape(ChipHeight / 2))
                .background(containerColor)
                .clickable(enabled = interactive, onClick = onClick)
                .padding(start = 8.dp, end = 14.dp)
                .onGloballyPositioned {
                    chipState.centre = it.boundsInRoot().center
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FirstIcon(icons.firstOrNull(), contentColor, circleColor)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text =
                    pluralStringResource(
                        R.plurals.edith_lockscreen_notification_count,
                        count,
                        count,
                    ),
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
            if (icons.size > 1) {
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    icons.drop(1).forEach { icon ->
                        // A faint disc behind each face keeps dark or mostly-transparent app icons
                        // from blending into the pill.
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

@Composable
private fun FirstIcon(icon: Drawable?, contentColor: Color, circleColor: Color?) {
    Box(
        modifier =
            Modifier.size(FirstIconCircle)
                .background(
                    circleColor ?: contrastCircleFor(icon, contentColor.copy(alpha = 0.16f)),
                    CircleShape,
                )
                .padding(5.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            AppIcon(icon, Modifier.fillMaxSize())
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
private fun AppIcon(drawable: Drawable, modifier: Modifier) {
    Image(
        painter = rememberDrawablePainter(drawable),
        contentDescription = null,
        colorFilter = null,
        modifier = modifier,
    )
}

// The same height as the quick affordances either side of it, so the pill and the shortcut circles
// share one band.
private val ChipHeight = 48.dp
private val FirstIconCircle = 32.dp
private val SmallIconSize = 12.dp
