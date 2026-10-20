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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.internal.logging.InstanceId
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.media.controls.domain.pipeline.interactor.MediaCarouselInteractor
import com.android.systemui.media.remedia.data.repository.MediaRepository
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * Holds the expanded state of the Edith lock screen media player.
 *
 * The lock screen media card can be expanded into the large square-art presentation (see the
 * "expandable lock screen media player"). This is UI state only, so it lives in a process-wide
 * singleton and is backed by Compose snapshot state (like [com.android.systemui.media.remedia.
 * domain.interactor.MediaInteractor]'s session list) so composables read it without a flow.
 *
 * The state is kept in sync with the media player by observing the media state directly (rather than
 * relying on a composable, which is removed from the composition when the media disappears): the
 * player collapses when there is no active media / media is not allowed on the lock screen, and when
 * the shown session changes.
 */
@SysUISingleton
class EdithLockscreenMediaExpansionInteractor
@Inject
constructor(
    @Application applicationScope: CoroutineScope,
    mediaCarouselInteractor: MediaCarouselInteractor,
    mediaRepository: MediaRepository,
    lockscreenMediaInteractor: EdithLockscreenMediaInteractor,
) {

    /** Whether the lock screen media player is currently expanded. */
    private var _isExpanded: Boolean by mutableStateOf(false)

    /**
     * Drag progress of the expansion gesture, `0f` = fully collapsed, `1f` = fully expanded. While
     * the user drags, this tracks the finger and [isExpanded] is `true` (the gesture is active);
     * on release the element snaps to the nearest end.
     */
    private var _progress: Float by mutableFloatStateOf(0f)

    /**
     * Square (1:1) motion art of the shown track, if the album has one and motion art is enabled.
     * Resolved by the lock screen art backdrop — which knows the track and is composed whenever the
     * player is — and played inside the expanded art square; `null` while it is unknown, when the
     * album has none, or when motion art is turned off.
     */
    private var _squareMotionArt: File? by mutableStateOf(null)

    /** The session the current expansion belongs to, so a session change resets the state. */
    private var sessionKey: InstanceId? = null

    val isExpanded: Boolean
        get() = _isExpanded

    val squareMotionArt: File?
        get() = _squareMotionArt

    val progress: Float
        get() = _progress

    /** Snapshot-backed flow of [isExpanded] (re-emits on change), for combining into other flows. */
    val isExpandedFlow: kotlinx.coroutines.flow.Flow<Boolean> =
        androidx.compose.runtime.snapshotFlow { _isExpanded }

    init {
        // Collapse whenever the media stops being usable (no active media / not allowed on the lock
        // screen), so the backdrop never lingers after the session detaches.
        combine(
                mediaCarouselInteractor.hasActiveMedia,
                mediaCarouselInteractor.allowMediaOnLockscreen,
            ) { hasActiveMedia, allowMediaOnLockscreen ->
                hasActiveMedia && allowMediaOnLockscreen
            }
            .distinctUntilChanged()
            .onEach { mediaUsable -> if (!mediaUsable) collapseAndReset() }
            .launchIn(applicationScope)

        // Collapse whenever the shown session changes or detaches, so an expansion never carries
        // over to a different track and never lingers after the player is cleared.
        mediaRepository
            .currentMediaFlow()
            .map { models -> models.firstOrNull { it.isActive }?.instanceId }
            .distinctUntilChanged()
            .onEach { key ->
                if (key != sessionKey) {
                    sessionKey = key
                    // The resolved file belongs to one album, so a session change discards it.
                    _squareMotionArt = null
                    if (key == null) collapseAndReset()
                }
            }
            .launchIn(applicationScope)

        // Turning the expandable player off collapses it: the card falls back to opening the
        // playing app, and neither the backdrop nor the clock tint may linger from an expansion
        // that is no longer allowed.
        lockscreenMediaInteractor.isExpandable
            .onEach { expandable -> if (!expandable) collapseAndReset() }
            .launchIn(applicationScope)

        // The expansion is preserved when the shade/QQS is opened on the lockscreen: the player
        // stays expanded and the notification stack is shown over it (the stack fade and the shade
        // gate only hide it while the shade is collapsed).
    }

    /** Expands the media player. The animation reports [progress] as it runs. */
    fun expand() {
        _isExpanded = true
    }

    /** Collapses the media player. The animation reports [progress] as it runs. */
    fun collapse() {
        _isExpanded = false
    }

    /**
     * Collapses and immediately resets [progress] to 0. Used when the collapse is not driven by the
     * card's own collapse animation (media cleared / session detached / shade pulled), where the
     * media element may already be gone and would otherwise leave the ambient art backdrop stuck
     * visible at its last progress.
     */
    private fun collapseAndReset() {
        _isExpanded = false
        _progress = 0f
        _squareMotionArt = null
    }

    /** Sets the resolved square motion art of the shown track (see [squareMotionArt]). */
    fun setSquareMotionArt(file: File?) {
        if (_squareMotionArt != file) _squareMotionArt = file
    }

    /** Toggles the expanded state. */
    fun toggle() {
        if (_isExpanded) collapse() else expand()
    }

    /** Sets the raw animation/drag progress in `[0, 1]` (used by the ambient backdrop). */
    fun setProgress(progress: Float) {
        _progress = progress.coerceIn(0f, 1f)
    }
}

/** Snapshot-backed view of [MediaRepository.currentMedia] as a cold flow (re-emits on change). */
private fun MediaRepository.currentMediaFlow() =
    androidx.compose.runtime.snapshotFlow { currentMedia.toList() }
