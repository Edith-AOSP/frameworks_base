/*
 * Copyright (C) 2026 Edith AOSP
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

package com.android.systemui.statusbar.edith

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
 * The status bar's notification-icon presentation.
 *
 * [mode] is written by Settings > Notifications > General and observed here so the change is live:
 *
 * - [MODE_ICONS] keeps the stock AOSP notification icons.
 * - [MODE_NUMBER] hides the icons and shows a count badge inside the clock pill.
 * - [MODE_NONE] hides the icons and shows no count.
 */
@SysUISingleton
class EdithStatusBarNotificationStyleRepository
@Inject
constructor(
    @Background private val backgroundDispatcher: CoroutineDispatcher,
    secureSettingsRepository: SecureSettingsRepository,
) {
    val mode: Flow<Int> =
        secureSettingsRepository
            .intSetting(SETTING_NAME, MODE_ICONS)
            .map { it.coerceIn(MODE_ICONS, MODE_NONE) }
            .distinctUntilChanged()
            .flowOn(backgroundDispatcher)

    companion object {
        /** [android.provider.Settings.Secure] key shared with the Settings preference. */
        const val SETTING_NAME = "edith_status_bar_notification_style"

        const val MODE_ICONS = 0
        const val MODE_NUMBER = 1
        const val MODE_NONE = 2
    }
}
