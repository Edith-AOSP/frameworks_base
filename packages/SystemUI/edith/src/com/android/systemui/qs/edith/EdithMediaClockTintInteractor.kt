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

package com.android.systemui.qs.edith

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.keyguard.domain.interactor.KeyguardClockInteractor
import com.android.systemui.media.remedia.data.repository.MediaRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Tints the small lock screen clock with the current session's album-art color while the Edith media
 * player is expanded.
 *
 * The color is pushed into [ClockEventController.smallClockTintColor], which routes it through each
 * clock face's normal `updateColor` path (this works for the default `FlexClockViewGroup` clock and
 * the custom Edith clocks, none of which are plain `TextView`s).
 */
@SysUISingleton
class EdithMediaClockTintInteractor
@Inject
constructor(
    @Application applicationScope: CoroutineScope,
    private val mediaRepository: MediaRepository,
    private val expansionInteractor: EdithLockscreenMediaExpansionInteractor,
    keyguardClockInteractor: KeyguardClockInteractor,
) {
    private val clockEventController = keyguardClockInteractor.clockEventController

    init {
        // Recompute whenever the expansion state or the shown card changes. Follow the currently
        // shown carousel card (like the album-art backdrop), not the active session, so swiping to
        // another media player re-tints the clock.
        snapshotFlow {
                if (expansionInteractor.isExpanded) {
                    val media = mediaRepository.currentMedia
                    if (media.isEmpty()) {
                        null
                    } else {
                        val index = mediaRepository.currentCarouselIndex.coerceIn(0, media.size - 1)
                        media[index].colorScheme?.primary
                    }
                } else {
                    null
                }
            }
            .onEach { clockEventController.smallClockTintColor = it }
            .launchIn(applicationScope)
    }

    /** The current tint (exposed for tests / previews). */
    val clockTintColor: Color?
        get() = clockEventController.smallClockTintColor
}
