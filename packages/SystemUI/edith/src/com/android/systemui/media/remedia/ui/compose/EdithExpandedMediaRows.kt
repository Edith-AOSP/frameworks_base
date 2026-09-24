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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.systemui.animation.Expandable
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.media.remedia.shared.model.MediaCardActionButtonLayout
import com.android.systemui.media.remedia.ui.viewmodel.MediaCardViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaNavigationViewModel

/** The expanded media card: context header, metadata, scrubber and controls. */
@Composable
internal fun ContentScope.EdithExpandedMediaRows(
    viewModel: MediaCardViewModel,
    colorScheme: AnimatedColorScheme,
    expandable: Expandable,
) {
    //   1. Context header: (app icon) [App] on (output icon) [Device]
    //   2. Metadata (title + artist), left aligned
    //   3. Scrubber row: [elapsed] (seekbar) [duration]
    //   4. Control bar: [left action] [prev] (pill play/pause) [next]
    val areActionsVisible =
        viewModel.actionButtonLayout == MediaCardActionButtonLayout.WithPlayPause

    // 1. Context header: (app icon) App on (output device name >).
    // The "(app icon) App on" part is keyed as an element that cross-fades (it exists only
    // in this three-row scene), while the output control is a SEPARATE shared element
    // (sibling, not nested) that morphs/slides between the two scenes.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
    ) {
        Element(key = Media.Elements.ContextHeader, modifier = Modifier) {
            EdithContextHeader(
                icon = viewModel.icon,
                iconTint = colorScheme.primary,
                appName = viewModel.appName,
                deviceName = deviceNameFor(viewModel.outputSwitcherChip),
                modifier = Modifier,
                // The card body tap expands the lock screen player, so the app is opened
                // from the header app icon instead (unchanged elsewhere — this is the same
                // click intent the card body used to run).
                onAppIconClick = { viewModel.onClick(expandable) },
            )
        }
        Spacer(Modifier.width(4.dp))
        Element(key = Media.Elements.OutputControl, modifier = Modifier) {
            EdithOutputControl(viewModel = viewModel.outputSwitcherChip)
        }
    }

    // 2. Metadata.
    Metadata(
        title = viewModel.title,
        subtitle = viewModel.subtitle,
        isExplicit = viewModel.isExplicit,
        color = Color.White,
        modifier =
            Modifier.fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .animateContentSize(),
    )

    // 3. Scrubber row. The timestamps sit flush against the same 16dp insets as the
    // metadata above, so the row is horizontally balanced.
    // Keyed as an element so the whole row (timestamps + seek bar) cross-fades during the
    // morph; the row exists only in this scene.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val navigationViewModel = viewModel.navigation
        Element(
            key = Media.Elements.ScrubberRow,
            modifier =
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (navigationViewModel is MediaNavigationViewModel.Showing) {
                    TimestampText(navigationViewModel.progressText)
                    EdithSeekBar(
                        viewModel = navigationViewModel,
                        modifier = Modifier.weight(1f),
                    )
                    TimestampText(navigationViewModel.durationText)
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }

    // 4. Control bar: [additional0] [prev] (play) [next] [additional1], with the play/pause
    // button centered. The far slots reserve equal width so the middle stays centered for
    // 0/1/2 additional actions.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        val navigationViewModel = viewModel.navigation as? MediaNavigationViewModel.Showing
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
        ) {
            // Far-left slot: additional action #0 (or reserved space).
            SecondarySlot(viewModel = viewModel.additionalActions.getOrNull(0), elementIndex = 0)

            Spacer(Modifier.weight(1f))

            // Center group: skip back, play/pause, skip forward.
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationViewModel?.left?.let {
                    SecondaryAction(
                        viewModel = it,
                        modifier = Modifier.sysuiResTag(MediaRes.PREV_BTN),
                        element = Media.Elements.PrevButton,
                    )
                }

                if (areActionsVisible) {
                    AnimatedVisibility(visible = viewModel.playPauseAction != null) {
                        PlayPauseAction(
                            viewModel = viewModel.playPauseAction,
                            buttonColor = colorScheme.primary,
                            iconColor = colorScheme.onPrimary,
                            buttonCornerRadius = { isPlaying ->
                                if (isPlaying) 22.dp else 44.dp
                            },
                        )
                    }
                }

                navigationViewModel?.right?.let {
                    SecondaryAction(
                        viewModel = it,
                        modifier = Modifier.sysuiResTag(MediaRes.NEXT_BTN),
                        element = Media.Elements.NextButton,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Far-right slot: additional action #1 (or reserved space).
            SecondarySlot(viewModel = viewModel.additionalActions.getOrNull(1), elementIndex = 1)
        }
    }
}
