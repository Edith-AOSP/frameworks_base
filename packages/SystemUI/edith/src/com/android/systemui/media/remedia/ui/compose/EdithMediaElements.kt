/*
 * Copyright (C) 2026 The Edith AOSP Project
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf

import android.view.ViewConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.compose.animation.Expandable
import com.android.compose.animation.rememberExpandableController
import com.android.compose.animation.scene.ContentScope
import com.android.compose.modifiers.height
import com.android.compose.modifiers.thenIf
import com.android.systemui.animation.Expandable
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.common.ui.compose.Icon
import com.android.systemui.media.remedia.ui.viewmodel.MediaDeviceChipViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaNavigationViewModel
import com.android.systemui.res.R
import kotlin.math.abs


/**
 * A bare seek bar (no navigation buttons on the sides), used by the Edith expanded media layout so
 * the slider can span the full width between the two timestamps. Behavior is identical to the seek
 * bar inside [Navigation].
 */
@Composable
internal fun ContentScope.EdithSeekBar(
    viewModel: MediaNavigationViewModel.Showing,
    modifier: Modifier = Modifier,
) {
    val colors =
        colors(
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            thumbColor = Color.White,
        )
    if (viewModel.isIndeterminate) {
        // No known duration (e.g. a livestream): a full-width, non-interactive track. It animates
        // (squiggly) while playing and is static when paused. No thumb, no scrubbing.
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .semantics { stateDescription = viewModel.contentDescription }
        ) {
            SeekBarTrack(
                progress = { 1f },
                isSquiggly = viewModel.isSquiggly,
                colors = colors,
                isThumbEnabled = false,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            )
        }
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    val sliderDragDelta = remember {
        // Not a mutableStateOf - this is never accessed in composition and using an anonymous
        // object avoids generics boxing of inline Offset.
        object {
            var value = Offset.Zero
        }
    }
    val isEnabled = viewModel.onScrubChange != null
    val velocityTracker = remember { VelocityTracker() }
    val context = LocalContext.current
    val flingVelocity =
        remember(context) { ViewConfiguration.get(context).scaledMinimumFlingVelocity * 10 }
    var isDrag by remember { mutableStateOf(false) }
    var isDragStartedOnThumb by remember { mutableStateOf(false) }
    val currentProgress by rememberUpdatedState(viewModel.progress)
    Slider(
        interactionSource = interactionSource,
        value = viewModel.progress,
        enabled = isEnabled,
        onValueChange = { progress ->
            if (!isDrag || isDragStartedOnThumb) {
                viewModel.onScrubChange?.invoke(progress)
            }
        },
        onValueChangeFinished = {
            val velocity = velocityTracker.calculateVelocity().x
            if (!isDrag || isDragStartedOnThumb) {
                viewModel.onScrubFinished?.invoke(
                    sliderDragDelta.value,
                    abs(velocity) < abs(flingVelocity),
                )
            }

            isDragStartedOnThumb = false
            isDrag = false
        },
        colors = colors,
        thumb = {
            if (isEnabled) {
                SeekBarThumb(interactionSource = interactionSource, colors = colors)
            }
        },
        track = { sliderState ->
            SeekBarTrack(
                sliderState = sliderState,
                isSquiggly = viewModel.isSquiggly,
                colors = colors,
                isThumbEnabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .semantics { stateDescription = viewModel.contentDescription }
                .pointerInput(Unit) {
                    // Track and report the drag delta to the view-model so it can decide whether to
                    // accept the next onValueChangeFinished or reject it if the drag was overly
                    // vertical.
                    awaitPointerEventScope {
                        var down: PointerInputChange? = null
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)

                            event.changes.forEach { change ->
                                // Feed the velocity tracker to detect flings
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                            }
                            when (event.type) {
                                PointerEventType.Press -> {
                                    down = event.changes.last()
                                    isDrag = false
                                }

                                PointerEventType.Move -> {
                                    val change = event.changes.last()

                                    if (change.id == down?.id) {
                                        if (!isDrag) {
                                            val thumbX = size.width * currentProgress
                                            // Add some forgiveness to hit target by 20dp radius
                                            isDragStartedOnThumb =
                                                abs(down.position.x - thumbX) < 20.dp.toPx()
                                        }
                                        isDrag = true
                                        if (isDragStartedOnThumb) {
                                            sliderDragDelta.value =
                                                change.position - down.position
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
    )
}


/**
 * The app icon + "App on" part of the three-row context header. The output device control
 * (`EdithOutputControl`) is rendered separately as a sibling [Media.Elements.OutputControl] element
 * so it can morph (slide) between the two-row and three-row headers.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ContentScope.EdithContextHeader(
    icon: Icon,
    iconTint: Color,
    appName: String,
    deviceName: String,
    modifier: Modifier = Modifier,
    onAppIconClick: (() -> Unit)? = null,
) {
    val headerDescription =
        stringResource(R.string.edith_media_context_header, appName, deviceName)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier.clearAndSetSemantics { contentDescription = headerDescription },
    ) {
        Icon(
            icon = icon,
            tint = iconTint,
            // Match the compressed (two-row) header icon: same size + circular clip, so the app
            // icon doesn't jump when the card cross-fades between the two scenes.
            modifier =
                Modifier.size(24.dp)
                    .clip(CircleShape)
                    .thenIf(onAppIconClick != null) {
                        Modifier.clickable(onClick = onAppIconClick!!)
                    },
        )
        // Gap after the app icon.
        Spacer(Modifier.width(6.dp))
        Text(
            text = appName,
            color = Color.White,
            style = MaterialTheme.typography.titleSmallEmphasized,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // The context header shares the row with the device name. Let the app name shrink
            // (with an ellipsis) instead of pushing the output control off the card.
            modifier = Modifier.weight(1f, fill = false),
        )
        // Balanced seams around the "on" connector so it reads as one phrase without looking
        // over-spaced (uniform spacedBy) or cramped.
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.edith_media_on),
            color = Color.White,
            // The emphasized title-small family is baked at weight 600, so the connector uses the
            // regular (400) Google Sans text/Flex face to read as a light connector.
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}


/** The output device name (semibold, text only) + chevron, clickable to open the output switcher. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ContentScope.EdithOutputControl(
    viewModel: MediaDeviceChipViewModel,
    modifier: Modifier = Modifier,
) {
    val deviceName = deviceNameFor(viewModel)
    val clickInteractionSource = remember { MutableInteractionSource() }
    val expandable = remember { Expandable() }
    Expandable(
        expandable = expandable,
        controller =
            rememberExpandableController(
                color = Color.Transparent,
                // The controller shape drives the output-switcher dialog's expand/collapse morph;
                // a rounded shape (matching DeviceChip) avoids closing into a rectangle.
                shape = RoundedCornerShape(12.dp),
            ),
        useModifierBasedImplementation = true,
        defaultMinSize = false,
        modifier = Modifier.wrapContentSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                modifier
                    .clickable(
                        interactionSource = clickInteractionSource,
                        indication = null,
                    ) {
                        viewModel.onClick(expandable)
                    }
                    .clearAndSetSemantics { contentDescription = deviceName },
        ) {
            Text(
                text = deviceName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmallEmphasized,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Reserve the chevron: the name may ellipsize, but the affordance stays visible.
                modifier = Modifier.weight(1f, fill = false),
            )
            // Trailing affordance: indicates the device name is clickable (opens the output
            // switcher). Decorative — the row's content description is the device name.
            Spacer(Modifier.width(2.dp))
            MediaOutputChevron()
        }
    }
}


/** The trailing chevron of the output control (decorative; the parent provides the semantics). */
@Composable
internal fun MediaOutputChevron(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 18.dp,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}
