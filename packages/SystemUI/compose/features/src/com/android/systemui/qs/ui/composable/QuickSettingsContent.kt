/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.systemui.qs.ui.composable

import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.PlatformSliderDefaults
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.animateContentFloatAsState
import com.android.compose.gesture.gesturesDisabled
import com.android.compose.modifiers.thenIf
import com.android.compose.theme.PlatformTheme
import com.android.systemui.brightness.ui.compose.BrightnessSliderContainer
import com.android.systemui.brightness.ui.compose.ContainerColors
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.media.remedia.ui.compose.Media
import com.android.systemui.media.remedia.ui.compose.MediaPresentationStyle
import com.android.systemui.qs.composefragment.ui.GridAnchor
import com.android.systemui.qs.panels.ui.compose.QuickActionsGrid
import com.android.systemui.qs.panels.ui.compose.TileGrid
import com.android.systemui.qs.shared.ui.QuickSettings
import com.android.systemui.qs.shared.ui.QuickSettings.Elements
import com.android.systemui.qs.ui.viewmodel.QuickSettingsContainerViewModel
import com.android.systemui.res.R
import com.android.systemui.volume.panel.component.volume.ui.composable.VolumeSlider
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun ContentScope.QuickSettingsContent(
    viewModel: QuickSettingsContainerViewModel,
    mediaInRow: Boolean,
    modifier: Modifier = Modifier,
    mediaSquishiness: () -> Float = { 1f },
) {
    // Media morph progress: 1f here (expanded three-row card), 0f in the Shade (QQS) scene. The
    // framework interpolates it across the Shade<->QS transition so the media card height/rows morph
    // smoothly with the pull.
    val mediaExpansion by
        animateContentFloatAsState(
            value = 1f,
            key = QuickSettings.SharedValues.MediaExpansion,
            canOverflow = false,
        )
    QuickSettingsPanelLayout(
        quickActions =
            @Composable {
                if (viewModel.edithTileStyle.enabled) {
                    var listening by remember { mutableStateOf(false) }
                    LifecycleStartEffect(Unit) {
                        listening = true
                        onStopOrDispose { listening = false }
                    }
                    QuickActionsGrid(
                        viewModel = viewModel.quickActionsGridViewModel,
                        modifier = Modifier.fillMaxWidth(),
                        listening = { listening },
                        edithStyle = viewModel.edithTileStyle,
                    )
                }
            },
        brightness =
            @Composable {
                if (viewModel.isBrightnessSliderVisible) {
                    var isBrightnessSliderInteractable by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        snapshotFlow { Elements.QuickSettingsContent.currentAlpha() }
                            .filterNotNull()
                            .collect { isBrightnessSliderInteractable = it >= .5f }
                    }
                    // The Edith style spaces the QS sections itself (see QuickSettingsPanelLayout),
                    // so drop the stock top margin to keep the gaps equal and tight.
                    val brightnessTopMargin =
                        if (viewModel.edithTileStyle.enabled) {
                            0.dp
                        } else {
                            dimensionResource(id = R.dimen.qs_brightness_margin_top)
                        }
                    Element(modifier = Modifier, key = Elements.BrightnessSlider) {
                        BrightnessSliderContainer(
                            viewModel.brightnessSliderViewModel,
                            containerColors =
                                ContainerColors(
                                    Color.Transparent,
                                    ContainerColors.defaultContainerColor,
                                ),
                            modifier =
                                Modifier.padding(vertical = brightnessTopMargin)
                                    .thenIf(!isBrightnessSliderInteractable) {
                                        Modifier.gesturesDisabled()
                                    },
                            // Zero the slider's own vertical padding in the Edith style: the layout
                            // spaces the QS sections itself, so the gaps stay equal and tight.
                            dimensions =
                                if (viewModel.edithTileStyle.enabled) {
                                    QuickSettingsShade.Dimensions.brightnessSliderDimensions.copy(
                                        verticalPadding = 0.dp
                                    )
                                } else {
                                    QuickSettingsShade.Dimensions.brightnessSliderDimensions
                                },
                        )
                    }
                }
            },
        volume =
            @Composable {
                if (viewModel.edithTileStyle.enabled) {
                    val volumeSliderViewModel = viewModel.volumeSliderViewModel
                    val volumeSliderState by
                        volumeSliderViewModel.slider.collectAsStateWithLifecycle()
                    PlatformTheme {
                        VolumeSlider(
                            modifier = Modifier.fillMaxWidth(),
                            showLabel = false,
                            state = volumeSliderState,
                            onValueChange = { newValue: Float ->
                                volumeSliderViewModel.onValueChanged(volumeSliderState, newValue)
                            },
                            onValueChangeFinished = {
                                volumeSliderViewModel.onValueChangeFinished()
                            },
                            onIconTapped = { volumeSliderViewModel.toggleMuted(volumeSliderState) },
                            sliderColors =
                                PlatformSliderDefaults.defaultPlatformSliderColors(),
                            hapticsViewModelFactory =
                                volumeSliderViewModel.getSliderHapticsViewModelFactory(),
                            // Zero the slider's own vertical padding: the Edith layout spaces the
                            // QS sections itself, so the gaps stay equal and tight.
                            dimensions =
                                QuickSettingsShade.Dimensions.VolumeSliderDimensions.copy(
                                    verticalPadding = 0.dp
                                ),
                            iconAtStart = true,
                        )
                    }
                }
            },
        volume =
            @Composable {
                if (viewModel.edithStyleEnabled) {
                    val volumeSliderViewModel = viewModel.volumeSliderViewModel
                    val volumeSliderState by
                        volumeSliderViewModel.slider.collectAsStateWithLifecycle()
                    PlatformTheme {
                        VolumeSlider(
                            modifier = Modifier.fillMaxWidth(),
                            showLabel = false,
                            state = volumeSliderState,
                            onValueChange = { newValue: Float ->
                                volumeSliderViewModel.onValueChanged(volumeSliderState, newValue)
                            },
                            onValueChangeFinished = {
                                volumeSliderViewModel.onValueChangeFinished()
                            },
                            onIconTapped = { volumeSliderViewModel.toggleMuted(volumeSliderState) },
                            sliderColors =
                                PlatformSliderDefaults.defaultPlatformSliderColors(),
                            hapticsViewModelFactory =
                                volumeSliderViewModel.getSliderHapticsViewModelFactory(),
                            dimensions = QuickSettingsShade.Dimensions.VolumeSliderDimensions,
                        )
                    }
                }
            },
        tiles =
            @Composable {
                var listening by remember { mutableStateOf(false) }
                LifecycleStartEffect(Unit) {
                    listening = true

                    onStopOrDispose { listening = false }
                }

                Box {
                    GridAnchor()
                    TileGrid(
                        viewModel.tileGridViewModel,
                        listening = { listening },
                        modifier = Modifier.element(Elements.QuickSettingsTiles),
                    )
                }
            },
        media =
            @Composable {
                // Only compose (and show) the media element when this scene will display it (same
                // rule as QQS). Composing it unconditionally left a stale card during the close
                // when QQS had no player.
                if (isAlwaysComposedContentVisible() && viewModel.showMedia) {
                    // The card only morphs between the two-row QQS and three-row expanded styles in
                    // the Edith "Dynamic" compact mode (Compressed in QQS, Default here). In
                    // "Always" both sides are Compressed, and the stock in-row media keeps its own
                    // handling.
                    val mediaMorphs =
                        viewModel.compactMediaInQs &&
                            !viewModel.compactMediaInQsInExpanded &&
                            !mediaInRow
                    // Shared element key with the QQS media, so the card slides from its QQS
                    // position to its expanded position with the pull.
                    Element(key = Elements.QuickSettingsMedia, modifier = Modifier) {
                        Media(
                            viewModelFactory = viewModel.mediaViewModelFactory,
                            presentationStyle =
                                if (viewModel.compactMediaInQsInExpanded || mediaInRow) {
                                    MediaPresentationStyle.Compressed
                                } else {
                                    MediaPresentationStyle.Default
                                },
                            behavior = QuickSettingsContainerViewModel.mediaUiBehavior,
                            onDismissed = viewModel::onMediaSwipeToDismiss,
                            mediaSquishiness = mediaSquishiness,
                            visible = { viewModel.showMedia },
                            location = Media.Location.QS,
                            // Static compact card unless this is the stock in-row media.
                            animateWithExpansion = mediaInRow,
                            // Morph the card height/rows with the Shade<->QS pull.
                            morphProgress = if (mediaMorphs) {
                                { mediaExpansion }
                            } else {
                                null
                            },
                        )
                    }
                } else {
                    // Add an empty box when QS content is not visible to keep the same number of
                    // elements.
                    Box(modifier = Modifier)
                }
            },
        mediaInRow = mediaInRow,
        edith = viewModel.edithTileStyle.enabled,
        modifier =
            modifier
                .padding(horizontal = dimensionResource(id = R.dimen.qs_horizontal_margin))
                .sysuiResTag("quick_settings_panel"),
    )
}

@Composable
private fun ContentScope.QuickSettingsPanelLayout(
    quickActions: @Composable () -> Unit,
    brightness: @Composable () -> Unit,
    volume: @Composable () -> Unit,
    tiles: @Composable () -> Unit,
    media: @Composable () -> Unit,
    mediaInRow: Boolean,
    edith: Boolean,
    modifier: Modifier = Modifier,
) {
    // The Edith style uses a single, tighter gap between its sections (Quick Actions, brightness,
    // volume and the tile grid) so they read as one evenly-spaced stack. The stock layout keeps the
    // wider default padding.
    val sectionGap =
        if (edith) EdithSectionGap else QuickSettingsShade.Dimensions.VerticalPadding
    if (mediaInRow) {
        Column(
            verticalArrangement = spacedBy(sectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.element(Elements.QuickSettingsContent),
        ) {
            quickActions()
            brightness()
            volume()
            Row(
                horizontalArrangement = spacedBy(QuickSettingsShade.Dimensions.HorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) { tiles() }
                Box(modifier = Modifier.weight(1f)) { media() }
            }
        }
    } else {
        Column(
            verticalArrangement = spacedBy(sectionGap),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier,
        ) {
            // The media card is a sibling of the (fade-gated) QS content so it can cross-fade in
            // place with the QQS card during the Shade<->QS transition instead of being gated by
            // the late QuickSettingsContent fade.
            Column(
                verticalArrangement = spacedBy(sectionGap),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.element(Elements.QuickSettingsContent),
            ) {
                quickActions()
                brightness()
                volume()
                tiles()
            }
            media()
        }
    }
}

/**
 * Vertical gap between the sections of the Edith Quick Settings style (Quick Actions, brightness,
 * volume and the tile grid). Tighter than the stock padding so the stack reads as one group; the
 * sliders' own vertical padding and the brightness top margin are zeroed in this style so this is
 * the exact gap between every section.
 */
private val EdithSectionGap = 12.dp
