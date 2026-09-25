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

import com.android.systemui.kosmos.Kosmos
import com.android.systemui.kosmos.backgroundScope
import com.android.systemui.qs.edith.EdithMediaInQsRepository.Companion.MODE_DYNAMIC
import com.android.systemui.shared.settings.data.repository.secureSettingsRepository
import kotlinx.coroutines.runBlocking

val Kosmos.edithMediaInQsRepository by
    Kosmos.Fixture { EdithMediaInQsRepository(backgroundScope, secureSettingsRepository) }

val Kosmos.edithMediaInQsInteractor by
    Kosmos.Fixture { EdithMediaInQsInteractor(backgroundScope, edithMediaInQsRepository) }

val Kosmos.edithLockscreenMediaRepository by
    Kosmos.Fixture { EdithLockscreenMediaRepository(backgroundScope, secureSettingsRepository) }

val Kosmos.edithLockscreenMediaInteractor by
    Kosmos.Fixture {
        EdithLockscreenMediaInteractor(backgroundScope, edithLockscreenMediaRepository)
    }

/** Sets the Edith compact-media-player mode (one of the `EdithMediaInQsRepository.MODE_*` values). */
fun Kosmos.setEdithMediaInQsMode(mode: Int) {
    runBlocking { secureSettingsRepository.setInt(EdithMediaInQsRepository.SETTING_NAME, mode) }
}

/** Resets the Edith compact-media-player mode to the default ([MODE_DYNAMIC]). */
fun Kosmos.resetEdithMediaInQsMode() = setEdithMediaInQsMode(MODE_DYNAMIC)
