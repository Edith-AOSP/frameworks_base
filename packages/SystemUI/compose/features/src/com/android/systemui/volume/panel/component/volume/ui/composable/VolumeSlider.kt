/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.systemui.volume.panel.component.volume.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState as MaterialSliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.compose.PlatformSliderColors
import com.android.systemui.common.shared.colors.SystemUISliderColors
import com.android.systemui.common.shared.model.Icon as IconModel
import com.android.systemui.common.ui.compose.Icon
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.haptics.slider.compose.ui.SliderHapticsViewModel
import com.android.systemui.qs.ui.compose.borderOnFocus
import com.android.systemui.res.R
import com.android.systemui.volume.dialog.sliders.ui.compose.SliderIconsState
import com.android.systemui.volume.dialog.sliders.ui.compose.SliderTrack
import com.android.systemui.volume.haptics.ui.VolumeHapticsConfigsProvider
import com.android.systemui.volume.panel.component.volume.slider.ui.viewmodel.SliderState
import com.android.systemui.volume.panel.component.volume.ui.composable.InternalDimensions.SliderTrackRoundedCorner
import com.android.systemui.volume.ui.compose.slider.AccessibilityParams
import com.android.systemui.volume.ui.compose.slider.Haptics
import com.android.systemui.volume.ui.compose.slider.Slider
import com.android.systemui.volume.ui.compose.slider.SliderIcon
import com.google.common.annotations.VisibleForTesting

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VolumeSlider(
    state: SliderState,
    onValueChange: (newValue: Float) -> Unit,
    onIconTapped: () -> Unit,
    sliderColors: PlatformSliderColors,
    modifier: Modifier = Modifier,
    hapticsViewModelFactory: SliderHapticsViewModel.Factory?,
    onValueChangeFinished: (() -> Unit)? = null,
    button: (@Composable RowScope.() -> Unit)? = null,
    showLabel: Boolean = true,
    dimensions: VolumeSliderDimensions = VolumeSliderDimensions.Defaults,
    materialSliderColors: SliderColors = SystemUISliderColors.Defaults,
    /**
     * Whether the track icon is drawn at the start of the track (the left edge in LTR, mirrored to
     * the right edge in RTL) instead of the end. Quick Settings places it at the start to match the
     * brightness slider; the volume dialog keeps it at the end.
     */
    iconAtStart: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .borderOnFocus(
                    color = MaterialTheme.colorScheme.secondary,
                    cornerSize = CornerSize(SliderTrackRoundedCorner),
                )
                .animateContentSize()
    ) {
        if (showLabel) {
            Text(
                text = state.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = dimensions.verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state is SliderState.Empty) {
                // reserve the space for the slider to avoid excess resizing
                Spacer(modifier = Modifier.weight(1f).height(dimensions.thumbHeight))
            } else {
                Slider(
                    value = state.value,
                    valueRange = state.valueRange,
                    onValueChanged = onValueChange,
                    onValueChangeFinished = { onValueChangeFinished?.invoke() },
                    colors = materialSliderColors,
                    isEnabled = state.isEnabled,
                    stepDistance = state.step,
                    accessibilityParams =
                        AccessibilityParams(
                            contentDescription = state.a11yContentDescription,
                            stateDescription = state.a11yStateDescription,
                        ),
                    track = { sliderState ->
                        // Quick Settings places the icon at the start of the track (solid when the
                        // fill can contain it, dimmed just after the thumb otherwise), matching the
                        // brightness slider. The shared SliderTrack start-icon slots hug the very
                        // edge and don't inset, so draw the icon ourselves. The volume dialog keeps
                        // the shared end icons.
                        if (iconAtStart) {
                            BoxWithConstraints(
                                Modifier.fillMaxWidth().height(dimensions.trackHeight)
                            ) {
                                SliderTrack(
                                    sliderState = sliderState,
                                    colors = materialSliderColors,
                                    isEnabled = state.isEnabled,
                                    trackSize = dimensions.trackHeight,
                                    trackCornerSize = SliderTrackRoundedCorner,
                                )
                                val icon = state.icon
                                if (icon != null) {
                                    StartTrackIcon(
                                        icon = icon,
                                        state = sliderState,
                                        trackWidth = maxWidth,
                                        iconSize = dimensions.iconSize,
                                        colors = materialSliderColors,
                                        isEnabled = state.isEnabled,
                                    )
                                }
                            }
                        } else {
                            fun endIconSlot(
                                tag: String,
                                isVisible: (SliderIconsState) -> Boolean,
                            ): (@Composable BoxScope.(SliderIconsState) -> Unit)? =
                                state.icon?.let { icon ->
                                    { iconsState ->
                                        SliderIcon(
                                            icon = {
                                                Icon(
                                                    icon = icon,
                                                    tint = null,
                                                    modifier =
                                                        Modifier.size(dimensions.iconSize)
                                                            .testTag(tag),
                                                )
                                            },
                                            isVisible = isVisible(iconsState),
                                        )
                                    }
                                }
                            SliderTrack(
                                sliderState = sliderState,
                                colors = materialSliderColors,
                                isEnabled = state.isEnabled,
                                trackSize = dimensions.trackHeight,
                                activeTrackEndIcon =
                                    endIconSlot(VolumeSlidersMotionTestKeys.ACTIVE_ICON_TAG) {
                                        !it.isInactiveTrackEndIconVisible
                                    },
                                inactiveTrackEndIcon =
                                    endIconSlot(VolumeSlidersMotionTestKeys.INACTIVE_ICON_TAG) {
                                        it.isInactiveTrackEndIconVisible
                                    },
                                trackCornerSize = SliderTrackRoundedCorner,
                            )
                        }
                    },
                    thumb = { sliderState, interactionSource ->
                        SliderDefaults.Thumb(
                            sliderState = sliderState,
                            interactionSource = interactionSource,
                            enabled = state.isEnabled,
                            colors = materialSliderColors,
                            thumbSize = DpSize(dimensions.thumbWidth, dimensions.thumbHeight),
                        )
                    },
                    haptics =
                        hapticsViewModelFactory?.let {
                            Haptics.Enabled(
                                hapticsViewModelFactory = it,
                                hapticConfigs =
                                    VolumeHapticsConfigsProvider.continuousConfigs(
                                        state.hapticFilter
                                    ),
                                orientation = Orientation.Horizontal,
                            )
                        } ?: Haptics.Disabled,
                    modifier =
                        Modifier.weight(1f).height(dimensions.thumbHeight).sysuiResTag(state.label),
                )
            }
            button?.invoke(this)
        }
        state.disabledMessage?.let { disabledMessage ->
            AnimatedVisibility(visible = !state.isEnabled) {
                Row(
                    modifier =
                        Modifier.padding(bottom = 12.dp)
                            .testTag(VolumeSlidersMotionTestKeys.DISABLED_MESSAGE_TAG),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MaterialIcon(
                        painter = painterResource(R.drawable.ic_error_outline),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = disabledMessage,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.basicMarquee().clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

@VisibleForTesting
object VolumeSlidersMotionTestKeys {
    const val ACTIVE_ICON_TAG = "Volume_Slider_activeStartIcon"
    const val INACTIVE_ICON_TAG = "Volume_Slider_inactiveStartIcon"
    const val DISABLED_MESSAGE_TAG = "disabledMessage"
}

private object InternalDimensions {
    val SliderTrackRoundedCorner = 12.dp
    val IconPadding = 6.dp
    val ThumbTrackGapSize = 6.dp
}

/**
 * Draws the Quick Settings volume icon using the same placement rules as the brightness slider: the
 * icon's left edge is pinned at [InternalDimensions.IconPadding] (the start; mirrored to the right
 * edge in RTL) with the active tint when the filled portion is wide enough to contain it, and just
 * after the thumb with the inactive tint otherwise.
 */
@Composable
private fun BoxWithConstraintsScope.StartTrackIcon(
    icon: IconModel,
    state: MaterialSliderState,
    trackWidth: Dp,
    iconSize: Dp,
    colors: SliderColors,
    isEnabled: Boolean,
) {
    val fraction = state.coercedValueAsFraction
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The track shares the brightness slider's logical (LTR) coordinate space: the thumb travels
    // from the start edge, so its x is trackWidth * fraction. The icon is mirrored for RTL the same
    // way the brightness icon is (around the track centre), not by mirroring the thumb position.
    val thumbX = trackWidth * fraction
    val iconFitsInFill =
        thumbX - InternalDimensions.ThumbTrackGapSize - InternalDimensions.IconPadding * 2 >
            iconSize

    val atStart = iconFitsInFill
    // Left edge of the icon in logical coordinates (start = left).
    val xLogical =
        if (atStart) {
            InternalDimensions.IconPadding
        } else {
            // Just after the thumb, toward the end.
            thumbX + InternalDimensions.ThumbTrackGapSize + InternalDimensions.IconPadding
        }
    // Signed translation from the track centre to the icon's centre. The icon is anchored at
    // Alignment.Center. Use absoluteOffset (not offset) so the sign is applied literally:
    // Modifier.offset mirrors the horizontal offset for RTL, which would cancel this mirror. The
    // logical "start" must land on the visual right in RTL, matching the brightness icon.
    val centerOffset = xLogical + iconSize / 2 - trackWidth / 2

    val tint =
        when {
            !isEnabled -> colors.disabledInactiveTickColor
            atStart -> colors.activeTickColor
            else -> colors.inactiveTickColor
        }

    Icon(
        icon = icon,
        tint = tint,
        modifier =
            Modifier.align(Alignment.Center)
                .absoluteOffset(x = if (rtl) -centerOffset else centerOffset)
                .size(iconSize)
                .testTag(
                    if (atStart) {
                        VolumeSlidersMotionTestKeys.ACTIVE_ICON_TAG
                    } else {
                        VolumeSlidersMotionTestKeys.INACTIVE_ICON_TAG
                    }
                ),
    )
}

data class VolumeSliderDimensions(
    val iconSize: Dp,
    val thumbHeight: Dp,
    val thumbWidth: Dp,
    val trackHeight: Dp,
    val verticalPadding: Dp,
) {
    companion object {
        val Defaults =
            VolumeSliderDimensions(
                iconSize = 24.dp,
                thumbHeight = 52.dp,
                thumbWidth = 4.dp,
                trackHeight = 40.dp,
                verticalPadding = 4.dp,
            )
    }
}

object VolumeSliderColors {
    val Defaults: SliderColors
        @Composable
        get() =
            SliderDefaults.colors()
                .copy(
                    activeTickColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledActiveTickColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
}
