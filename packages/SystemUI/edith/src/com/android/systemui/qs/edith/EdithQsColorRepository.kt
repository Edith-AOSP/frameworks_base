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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Repository for the Edith Quick Settings color scheme toggle.
 *
 * When enabled (and the Edith QS style is on), the Edith tiles use the tertiary color scheme
 * (tertiaryContainer for the inactive circle, tertiary for the active squircle) instead of the
 * default colors. Stored in [Settings.Secure] under [SETTING_NAME], defaulting to enabled.
 */
@SysUISingleton
class EdithQsColorRepository
@Inject
constructor(
    @Background private val backgroundDispatcher: CoroutineDispatcher,
    secureSettingsRepository: SecureSettingsRepository,
) {
    /** Whether the Edith Quick Settings color scheme is enabled. Enabled by default. */
    val isEnabled: Flow<Boolean> =
        secureSettingsRepository
            .boolSetting(SETTING_NAME, defaultValue = true)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    /**
     * Per-slot color overrides for the Edith tertiary tile scheme, set from the (debug) color tuner
     * in Settings. Each value is a swatch tag (see [EdithTileSwatches]), or [UNSET] when the slot is
     * not overridden. Storing a tag (not a resolved color) lets the tile re-resolve the theme color.
     */
    val tileColorActiveBg: Flow<String> =
        secureSettingsRepository
            .stringSetting(COLOR_ACTIVE_BG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    val tileColorActiveFg: Flow<String> =
        secureSettingsRepository
            .stringSetting(COLOR_ACTIVE_FG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    val tileColorInactiveBg: Flow<String> =
        secureSettingsRepository
            .stringSetting(COLOR_INACTIVE_BG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    val tileColorInactiveFg: Flow<String> =
        secureSettingsRepository
            .stringSetting(COLOR_INACTIVE_FG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    /** Alpha (0-100) for the inactive background, or [UNSET_INT]. */
    val tileColorInactiveAlpha: Flow<Int> =
        secureSettingsRepository
            .intSetting(COLOR_INACTIVE_ALPHA, defaultValue = UNSET_INT)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    // --- Quick Actions scope -------------------------------------------------

    /** Quick Actions per-slot color overrides (swatch tags), or [UNSET]. */
    val quickActionsActiveBg: Flow<String> =
        secureSettingsRepository
            .stringSetting(QA_COLOR_ACTIVE_BG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    val quickActionsActiveFg: Flow<String> =
        secureSettingsRepository
            .stringSetting(QA_COLOR_ACTIVE_FG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    val quickActionsInactiveBg: Flow<String> =
        secureSettingsRepository
            .stringSetting(QA_COLOR_INACTIVE_BG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    val quickActionsInactiveFg: Flow<String> =
        secureSettingsRepository
            .stringSetting(QA_COLOR_INACTIVE_FG, defaultValue = UNSET)
            .map { it ?: UNSET }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    /** Alpha (0-100) for the Quick Actions inactive background, or [UNSET_INT]. */
    val quickActionsInactiveAlpha: Flow<Int> =
        secureSettingsRepository
            .intSetting(QA_COLOR_INACTIVE_ALPHA, defaultValue = UNSET_INT)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    /** Dual-target outer box corner radius (dp), or [UNSET_INT]. */
    val quickActionsShapeOuter: Flow<Int> =
        secureSettingsRepository
            .intSetting(QA_SHAPE_OUTER, defaultValue = UNSET_INT)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    /** Dual-target inner toggle-target box corner radius (dp), or [UNSET_INT]. */
    val quickActionsShapeInner: Flow<Int> =
        secureSettingsRepository
            .intSetting(QA_SHAPE_INNER, defaultValue = UNSET_INT)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    companion object {
        /**
         * [Settings.Secure] key for the Edith QS color scheme. Referenced as a string (rather
         * than a framework constant) so this can ship as a SystemUI-only change.
         */
        const val SETTING_NAME = "edith_use_qs_color_scheme"

        /** Sentinel meaning "this color slot is not overridden" (string slot). */
        const val UNSET = ""

        /** Sentinel meaning "this numeric slot is not overridden". */
        const val UNSET_INT = -1

        /** [Settings.Secure] keys for the (debug) Edith tile color tuner overrides. */
        const val COLOR_ACTIVE_BG = "edith_qs_tile_color_active_bg"

        const val COLOR_ACTIVE_FG = "edith_qs_tile_color_active_fg"
        const val COLOR_INACTIVE_BG = "edith_qs_tile_color_inactive_bg"
        const val COLOR_INACTIVE_FG = "edith_qs_tile_color_inactive_fg"
        const val COLOR_INACTIVE_ALPHA = "edith_qs_tile_color_inactive_alpha"

        /** [Settings.Secure] keys for the Quick Actions scope (colors + dual-target shapes). */
        const val QA_COLOR_ACTIVE_BG = "edith_qa_tile_color_active_bg"

        const val QA_COLOR_ACTIVE_FG = "edith_qa_tile_color_active_fg"
        const val QA_COLOR_INACTIVE_BG = "edith_qa_tile_color_inactive_bg"
        const val QA_COLOR_INACTIVE_FG = "edith_qa_tile_color_inactive_fg"
        const val QA_COLOR_INACTIVE_ALPHA = "edith_qa_tile_color_inactive_alpha"
        const val QA_SHAPE_OUTER = "edith_qa_tile_shape_outer"
        const val QA_SHAPE_INNER = "edith_qa_tile_shape_inner"
    }
}
