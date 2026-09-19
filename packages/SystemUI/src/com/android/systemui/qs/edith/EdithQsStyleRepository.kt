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
 * Repository for the Edith Quick Settings style toggle.
 *
 * The style is stored in [Settings.Secure] under [SETTING_NAME]. When enabled, Quick Settings uses
 * the Edith layout (fixed 1x1 tiles, a Quick Actions grid, and a volume slider). When disabled, the
 * stock AOSP layout is used.
 */
@SysUISingleton
class EdithQsStyleRepository
@Inject
constructor(
    @Background private val backgroundDispatcher: CoroutineDispatcher,
    secureSettingsRepository: SecureSettingsRepository,
) {
    /** Whether the Edith Quick Settings style is enabled. Disabled by default. */
    val isEnabled: Flow<Boolean> =
        secureSettingsRepository
            .boolSetting(SETTING_NAME, defaultValue = false)
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    companion object {
        /**
         * [Settings.Secure] key for the Edith Quick Settings style. Referenced as a string (rather
         * than a framework constant) so this can ship as a SystemUI-only change.
         */
        const val SETTING_NAME = "edith_qs_style"
    }
}
