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

package com.android.systemui.qs.ui.viewmodel

import android.media.AudioManager
import android.view.Display
import androidx.compose.runtime.getValue
import com.android.settingslib.volume.shared.model.AudioStream
import com.android.systemui.brightness.ui.viewmodel.BrightnessSliderViewModel
import com.android.systemui.dagger.qualifiers.MainImmediate
import com.android.systemui.display.data.repository.DisplayTypeRepository
import com.android.systemui.lifecycle.HydratedActivatable
import com.android.systemui.media.controls.domain.pipeline.interactor.MediaCarouselInteractor
import com.android.systemui.media.controls.ui.controller.MediaHierarchyManager.Companion.LOCATION_QS
import com.android.systemui.media.remedia.ui.compose.MediaUiBehavior
import com.android.systemui.media.remedia.ui.viewmodel.MediaCarouselVisibility
import com.android.systemui.media.remedia.ui.viewmodel.MediaViewModel
import com.android.systemui.qs.edith.EdithMediaInQsInteractor
import com.android.systemui.qs.edith.EdithQsColorInteractor
import com.android.systemui.qs.edith.EdithQsStyleInteractor
import com.android.systemui.qs.edith.QuickActionsEditViewModel
import com.android.systemui.qs.panels.ui.viewmodel.DetailsViewModel
import com.android.systemui.qs.panels.ui.viewmodel.EditModeViewModel
import com.android.systemui.qs.panels.ui.viewmodel.MediaInRowInLandscapeViewModel
import com.android.systemui.qs.panels.ui.viewmodel.QuickActionsGridViewModel
import com.android.systemui.qs.panels.ui.viewmodel.TileGridViewModel
import com.android.systemui.shade.ShadeDisplayAware
import com.android.systemui.shade.ui.viewmodel.ShadeHeaderViewModel
import com.android.systemui.volume.panel.component.volume.domain.model.SliderType
import com.android.systemui.volume.panel.component.volume.slider.ui.viewmodel.AudioStreamSliderViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class QuickSettingsContainerViewModel
@AssistedInject
constructor(
    brightnessSliderViewModelFactory: BrightnessSliderViewModel.Factory,
    shadeHeaderViewModelFactory: ShadeHeaderViewModel.Factory,
    tileGridViewModelFactory: TileGridViewModel.Factory,
    @Assisted private val supportsBrightnessMirroring: Boolean,
    val editModeViewModel: EditModeViewModel,
    val detailsViewModel: DetailsViewModel,
    private val mediaCarouselInteractor: MediaCarouselInteractor,
    val mediaViewModelFactory: MediaViewModel.Factory,
    mediaInRowInLandscapeViewModelFactory: MediaInRowInLandscapeViewModel.Factory,
    @ShadeDisplayAware shadeDisplayTypeRepository: DisplayTypeRepository,
    private val edithQsStyleInteractor: EdithQsStyleInteractor,
    private val edithQsColorInteractor: EdithQsColorInteractor,
    private val edithMediaInQsInteractor: EdithMediaInQsInteractor,
    val quickActionsGridViewModel: QuickActionsGridViewModel,
    val quickActionsEditViewModel: QuickActionsEditViewModel,
    audioStreamSliderViewModelFactory: AudioStreamSliderViewModel.Factory,
    @MainImmediate private val applicationScope: CoroutineScope,
) : HydratedActivatable() {

    val isBrightnessSliderVisible by
        shadeDisplayTypeRepository.displayType
            // The shade could be on an external display: in that case the slider shouldn't
            // be visible.
            .map { it == Display.TYPE_INTERNAL }
            .hydratedStateOf(
                initialValue = shadeDisplayTypeRepository.displayType.value == Display.TYPE_INTERNAL
            )

    val isEditing by editModeViewModel.isEditing.hydratedStateOf()

    val brightnessSliderViewModel =
        brightnessSliderViewModelFactory.create(supportsBrightnessMirroring)

    val shadeHeaderViewModel = shadeHeaderViewModelFactory.create()

    val tileGridViewModel = tileGridViewModelFactory.create()

    val showMedia: Boolean by mediaCarouselInteractor.hasAnyMedia.hydratedStateOf()

    val showMediaInRow: Boolean
        get() = qsMediaInRowViewModel.shouldMediaShowInRow

    /**
     * Volume (media) slider shown in expanded QS, directly below the brightness slider. Reuses the
     * same view model/composable as the volume panel and the QS shade overlay.
     */
    val volumeSliderViewModel: AudioStreamSliderViewModel =
        audioStreamSliderViewModelFactory.create(
            AudioStreamSliderViewModel.FactoryAudioStreamWrapper(
                SliderType.Stream(AudioStream(AudioManager.STREAM_MUSIC)).stream
            ),
            applicationScope,
        )

    /** Whether the Edith Quick Settings style is enabled (Quick Actions grid, volume, 1x1 tiles). */
    val edithStyleEnabled by
        edithQsStyleInteractor.isEnabled.hydratedStateOf(
            traceName = "edithStyleEnabled",
            initialValue = false,
        )

    /** Whether the Edith QS color scheme (tertiary tiles) is enabled. */
    val edithColorEnabled by
        edithQsColorInteractor.isEnabled.hydratedStateOf(
            traceName = "edithColorEnabled",
            initialValue = true,
        )

    /** Per-slot tile color overrides from the (debug) tuner, or `null` for the defaults. */
    val edithTileColorOverride by
        edithQsColorInteractor.tileColorOverride.hydratedStateOf(
            traceName = "edithTileColorOverride",
        )

    /**
     * Quick Actions color + dual-target shape overrides from the (debug) tuner, or `null` for the
     * defaults.
     */
    val edithQuickActionsOverride by
        edithQsColorInteractor.quickActionsTileOverride.hydratedStateOf(
            traceName = "edithQuickActionsOverride",
        )

    /**
     * Whether the compact (two-row) media player is enabled for Quick Quick Settings (the
     * "Enable compact media player in QQS and Expanded QS" or "... in QQS" Edith modes). When
     * `false`, QQS keeps the stock media style.
     */
    val compactMediaInQs by
        edithMediaInQsInteractor.isEnabled.hydratedStateOf(
            traceName = "compactMediaInQs",
            initialValue = false,
        )

    /**
     * Whether the compact (two-row) media player should also be forced in the expanded QS panel
     * (only the "Enable compact media player in QQS and Expanded QS" Edith mode). When `false`, the
     * expanded panel keeps the stock media style.
     */
    val compactMediaInQsInExpanded by
        edithMediaInQsInteractor.isAlways.hydratedStateOf(
            traceName = "compactMediaInQsInExpanded",
            initialValue = false,
        )

    fun onMediaSwipeToDismiss() = mediaCarouselInteractor.onSwipeToDismiss()

    private val qsMediaInRowViewModel =
        mediaInRowInLandscapeViewModelFactory.create(LOCATION_QS, mediaUiBehavior)

    override suspend fun onActivated() {
        coroutineScope {
            launch { brightnessSliderViewModel.activate() }
            launch { shadeHeaderViewModel.activate() }
            launch { tileGridViewModel.activate() }
            launch { qsMediaInRowViewModel.activate() }
        }
    }

    companion object {

        /** Behavior of the media carousel in quick settings */
        val mediaUiBehavior =
            MediaUiBehavior(
                isCarouselDismissible = false,
                carouselVisibility = MediaCarouselVisibility.WhenNotEmpty,
            )
    }

    @AssistedFactory
    interface Factory {
        fun create(supportsBrightnessMirroring: Boolean): QuickSettingsContainerViewModel
    }
}
