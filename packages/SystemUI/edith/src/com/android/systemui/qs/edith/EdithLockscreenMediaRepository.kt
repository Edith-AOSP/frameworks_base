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

package com.android.systemui.qs.edith

import android.provider.Settings
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.shared.settings.data.repository.SecureSettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * Repository for the Edith lock screen media player toggles.
 *
 * - [SETTING_NAME] (`edith_lockscreen_media_compact`): `true` = compact (two-row) media player,
 *   `false` (default) = three-row.
 * - [EXPANDABLE_SETTING_NAME] (`edith_lockscreen_media_expandable`): `true` (default) = tapping
 *   the lock screen media card expands it into the large square-art view; `false` = the card body
 *   tap launches the playing app (stock behaviour).
 * - [MOTION_ART_MODE_SETTING_NAME] (`edith_lockscreen_motion_art_mode`): how the expanded player
 *   fetches dynamic (video) album art — always (default), only on unmetered networks, or never.
 */
@SysUISingleton
class EdithLockscreenMediaRepository
@Inject
constructor(
    @Background private val backgroundDispatcher: CoroutineDispatcher,
    secureSettingsRepository: SecureSettingsRepository,
) {
    /** Whether the compact lock screen media player is enabled. Disabled by default. */
    val isCompact: Flow<Boolean> =
        secureSettingsRepository
            .boolSetting(SETTING_NAME, defaultValue = false)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    /** Whether tapping the lock screen media card expands it. Enabled by default. */
    val isExpandable: Flow<Boolean> =
        secureSettingsRepository
            .boolSetting(EXPANDABLE_SETTING_NAME, defaultValue = true)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    /**
     * Mode for dynamic (video) album art on the expanded lock screen player, one of
     * [MOTION_ART_ON], [MOTION_ART_WIFI_ONLY] or [MOTION_ART_OFF]. Defaults to on.
     *
     * Older builds stored the same choice as a boolean under [MOTION_ART_SETTING_NAME]; that value
     * is still honoured until the mode key is written, because a legacy `true` (`1`) would otherwise
     * read as [MOTION_ART_WIFI_ONLY].
     */
    val motionArtMode: Flow<Int> =
        combine(
                secureSettingsRepository.stringSetting(MOTION_ART_MODE_SETTING_NAME, null),
                secureSettingsRepository.stringSetting(MOTION_ART_SETTING_NAME, null),
            ) { mode, legacy ->
                mode?.toIntOrNull()?.takeIf { it in MOTION_ART_ON..MOTION_ART_OFF }
                    ?: when (legacy?.lowercase()) {
                        "1",
                        "true" -> MOTION_ART_ON
                        "0",
                        "false" -> MOTION_ART_OFF
                        else -> MOTION_ART_ON
                    }
            }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    companion object {
        /**
         * [Settings.Secure] key for the compact lock screen media player. Referenced as a string
         * (rather than a framework constant) so this can ship as a SystemUI-only change.
         */
        const val SETTING_NAME = "edith_lockscreen_media_compact"

        /**
         * [Settings.Secure] key for the expandable lock screen media player. Referenced as a
         * string (rather than a framework constant) so this can ship as a SystemUI-only change.
         */
        const val EXPANDABLE_SETTING_NAME = "edith_lockscreen_media_expandable"

        /**
         * [Settings.Secure] key for the mode of dynamic (video) album art on the lock screen media
         * player. Referenced as a string (rather than a framework constant) so this can ship as a
         * SystemUI-only change.
         */
        const val MOTION_ART_MODE_SETTING_NAME = "edith_lockscreen_motion_art_mode"

        /** [MOTION_ART_MODE_SETTING_NAME] fallback used by builds that stored a boolean. */
        const val MOTION_ART_SETTING_NAME = "edith_lockscreen_motion_art"

        /** Fetch and play motion art on any network. */
        const val MOTION_ART_ON = 0

        /** Fetch motion art only on unmetered networks; cached loops still play. */
        const val MOTION_ART_WIFI_ONLY = 1

        /** Never fetch or play motion art; the static cover is used. */
        const val MOTION_ART_OFF = 2
    }
}
