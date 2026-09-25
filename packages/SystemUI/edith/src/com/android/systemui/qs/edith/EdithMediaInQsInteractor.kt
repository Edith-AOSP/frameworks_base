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

import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.qs.edith.EdithMediaInQsRepository.Companion.MODE_ALWAYS
import com.android.systemui.qs.edith.EdithMediaInQsRepository.Companion.MODE_DISABLED
import com.android.systemui.qs.edith.EdithMediaInQsRepository.Companion.MODE_DYNAMIC
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Exposes the compact-media-player-in-QS mode and derived flags as [StateFlow]s. */
@SysUISingleton
class EdithMediaInQsInteractor
@Inject
constructor(
    @Background scope: CoroutineScope,
    repository: EdithMediaInQsRepository,
) {
    /** The raw mode; one of the `EdithMediaInQsRepository.MODE_*` values. */
    val mode: StateFlow<Int> =
        repository.mode.stateIn(scope, SharingStarted.Eagerly, MODE_DYNAMIC)

    /** `true` when the compact player is disabled entirely (always three-row). */
    val isDisabled: StateFlow<Boolean> =
        repository.mode.map { it == MODE_DISABLED }.stateIn(scope, SharingStarted.Eagerly, false)

    /** `true` when the compact player is enabled for QQS (either Always or Dynamic mode). */
    val isEnabled: StateFlow<Boolean> =
        repository.mode.map { it != MODE_DISABLED }.stateIn(scope, SharingStarted.Eagerly, false)

    /** `true` when the compact player should be forced in both QQS and expanded QS. */
    val isAlways: StateFlow<Boolean> =
        repository.mode.map { it == MODE_ALWAYS }.stateIn(scope, SharingStarted.Eagerly, false)
}
