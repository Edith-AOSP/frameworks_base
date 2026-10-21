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

package com.android.systemui.keyguard.ui.viewmodel

import com.android.systemui.common.shared.model.Icon
import com.android.systemui.deviceentry.domain.interactor.DeviceEntryInteractor
import com.android.systemui.keyguard.domain.interactor.KeyguardInteractor
import com.android.systemui.lifecycle.HydratedActivatable
import com.android.systemui.media.remedia.data.repository.MediaRepository
import android.content.Context
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.media.remedia.motionart.MotionArtNetwork
import com.android.systemui.media.remedia.motionart.MotionArtSource
import com.android.systemui.qs.edith.EdithLockscreenMediaExpansionInteractor
import com.android.systemui.qs.edith.EdithLockscreenMediaRepository
import com.android.systemui.wallpapers.data.repository.WallpaperRepository
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.io.File
import kotlinx.coroutines.flow.combine

/**
 * Models the full-screen blurred album-art backdrop drawn behind the lock screen while the media
 * player is expanded (the Edith "expandable media player").
 */
class LockscreenMediaArtBackgroundViewModel
@AssistedInject
constructor(
    private val mediaRepository: MediaRepository,
    private val expansionInteractor: EdithLockscreenMediaExpansionInteractor,
    lockscreenMediaRepository: EdithLockscreenMediaRepository,
    motionArtNetwork: MotionArtNetwork,
    private val motionArtSources: Set<@JvmSuppressWildcards MotionArtSource>,
    @Application private val appContext: Context,
    keyguardInteractor: KeyguardInteractor,
    deviceEntryInteractor: DeviceEntryInteractor,
    wallpaperRepository: WallpaperRepository,
) : HydratedActivatable() {
    /**
     * The album art of the currently-**shown** session (the carousel's current card), or `null`
     * when there is no art. Follows the same index as the expanded media card, so prev/next switch
     * the backdrop together with the square art. Read from [MediaRepository.currentMedia] (a plain
     * snapshot list) rather than `MediaInteractor.sessions`, whose getter has side effects and must
     * not be read from composition.
     */
    val artwork: Icon?
        get() {
            val media = mediaRepository.currentMedia
            if (media.isEmpty()) return null
            val index = mediaRepository.currentCarouselIndex.coerceIn(0, media.size - 1)
            return media[index].background
        }

    /**
     * Whether the backdrop is suppressed entirely: after the device is unlocked (entered) so the
     * art does not bleed onto the home screen, and on AOD (dozing) unless wallpaper-on-AOD is
     * supported and enabled. Not gated by the current scene — `isDeviceEntered`/`isDozing` are
     * stable across the lockscreen <-> shade/QS drag, so this never toggles mid-transition (a scene
     * gate made the alpha jump to 0 mid-drag and broke the QQS -> expanded QS transition).
     */
    private val suppressed: Boolean by
        combine(
                deviceEntryInteractor.isDeviceEntered,
                keyguardInteractor.isDozing,
                wallpaperRepository.wallpaperSupportsAmbientMode,
            ) { deviceEntered, dozing, wallpaperOnAod ->
                deviceEntered || (dozing && !wallpaperOnAod)
            }
            .hydratedStateOf(initialValue = true)

    /**
     * Expansion progress `0f..1f` (drives the backdrop alpha), so the backdrop fades in/out with
     * the player's transition rather than snapping. Reads the live interactor progress (Compose
     * state), so it updates every frame during the drag.
     */
    val progress: Float
        get() = if (suppressed) 0f else expansionInteractor.progress

    /** Whether the backdrop should currently render. */
    val isExpanded: Boolean
        get() = progress > 0f

    /** Whether dynamic (video) album art may be fetched and played. */
    private val motionArtMode: Int by
        lockscreenMediaRepository.motionArtMode.hydratedStateOf(
            initialValue = EdithLockscreenMediaRepository.MOTION_ART_OFF
        )

    /** Whether the default network is unmetered; only consulted in "Wi-Fi only" mode. */
    private val unmetered: Boolean by
        motionArtNetwork.isUnmetered.hydratedStateOf(initialValue = false)

    /** Art mode, for the backdrop to tell "off" apart from "no art on this album". */
    val isMotionArtEnabled: Boolean
        get() = motionArtMode != EdithLockscreenMediaRepository.MOTION_ART_OFF

    /** Whether a loop may be downloaded right now (cached art plays regardless of this). */
    val canDownloadMotionArt: Boolean
        get() =
            when (motionArtMode) {
                EdithLockscreenMediaRepository.MOTION_ART_ON -> true
                EdithLockscreenMediaRepository.MOTION_ART_WIFI_ONLY -> unmetered
                else -> false
            }

    /** Artist and title of the shown session, used to look up its motion art. */
    val track: Pair<String, String>?
        get() {
            val media = mediaRepository.currentMedia
            if (media.isEmpty()) return null
            val index = mediaRepository.currentCarouselIndex.coerceIn(0, media.size - 1)
            val shown = media[index]
            return shown.subtitle to shown.title
        }

    /**
     * Resolves the loop for the shown track through the highest-priority [MotionArtSource]. The
     * default implementation is the in-tree one; a build that ships its own (the Edith SystemUI APK)
     * replaces it by binding a source with a higher priority.
     *
     * @param tall `true` for the backdrop shape, `false` for the square one the player draws.
     */
    suspend fun findMotionArt(tall: Boolean): File? {
        val (artist, title) = track ?: return null
        val source = motionArtSources.maxByOrNull { it.priority } ?: return null
        return source.find(appContext, artist, title, tall)
    }

    /** Hands the resolved square motion art to the expanded player (it draws it in the art square). */
    fun setSquareMotionArt(file: File?) {
        expansionInteractor.setSquareMotionArt(file)
    }

    @AssistedFactory
    interface Factory {
        fun create(): LockscreenMediaArtBackgroundViewModel
    }
}
