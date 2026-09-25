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

/**
 * Repository for the Edith "compact media player in Quick Settings" mode.
 *
 * The mode is stored in [Settings.Secure] under [SETTING_NAME] as an int:
 * - [MODE_ALWAYS] = compact (two-row) media player in both QQS and the expanded QS panel.
 * - [MODE_DYNAMIC] = compact media player only in QQS (the stock in-row behavior).
 * - [MODE_DISABLED] = always use the three-row media player.
 */
@SysUISingleton
class EdithMediaInQsRepository
@Inject
constructor(
    @Background private val backgroundDispatcher: CoroutineDispatcher,
    private val secureSettingsRepository: SecureSettingsRepository,
) {
    /** The current mode; [MODE_DYNAMIC] by default. */
    val mode: Flow<Int> =
        secureSettingsRepository
            .intSetting(SETTING_NAME, defaultValue = MODE_DYNAMIC)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    companion object {
        /**
         * [Settings.Secure] key for the compact media player mode. Referenced as a string (rather
         * than a framework constant) so this can ship as a SystemUI-only change.
         */
        const val SETTING_NAME = "edith_media_in_qs_mode"

        const val MODE_ALWAYS = 0
        const val MODE_DYNAMIC = 1
        const val MODE_DISABLED = 2
    }
}
