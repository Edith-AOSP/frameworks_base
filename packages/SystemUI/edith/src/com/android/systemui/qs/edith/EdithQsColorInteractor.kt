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
import com.android.systemui.qs.edith.EdithQsColorRepository.Companion.UNSET
import com.android.systemui.qs.edith.EdithQsColorRepository.Companion.UNSET_INT
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Exposes whether the Edith Quick Settings color scheme is enabled as a [StateFlow]. */
@SysUISingleton
class EdithQsColorInteractor
@Inject
constructor(
    @Background scope: CoroutineScope,
    repository: EdithQsColorRepository,
) {
    val isEnabled: StateFlow<Boolean> =
        repository.isEnabled.stateIn(scope, SharingStarted.Eagerly, true)

    /**
     * Live per-slot tile color overrides from the color tuner, or `null` when nothing is
     * overridden.
     */
    val tileColorOverride: StateFlow<EdithTileColorOverride?> =
        combine(
                repository.tileColorActiveBg,
                repository.tileColorActiveFg,
                repository.tileColorInactiveBg,
                repository.tileColorInactiveFg,
                repository.tileColorInactiveAlpha,
            ) { activeBg, activeFg, inactiveBg, inactiveFg, inactiveAlpha ->
                val override =
                    EdithTileColorOverride(
                        activeBg = activeBg.takeIf { it != UNSET },
                        activeFg = activeFg.takeIf { it != UNSET },
                        inactiveBg = inactiveBg.takeIf { it != UNSET },
                        inactiveFg = inactiveFg.takeIf { it != UNSET },
                        inactiveAlpha =
                            inactiveAlpha.takeIf { it != UNSET_INT }?.coerceIn(0, 100)?.let {
                                it / 100f
                            },
                    )
                override.takeIf { it.hasAny }
            }
            .stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * Live Quick Actions color + dual-target shape overrides from the tuner, or `null` when nothing
     * is overridden.
     */
    val quickActionsTileOverride: StateFlow<QuickActionsTileOverride?> =
        combine(
                listOf<Flow<Any>>(
                    repository.quickActionsActiveBg,
                    repository.quickActionsActiveFg,
                    repository.quickActionsInactiveBg,
                    repository.quickActionsInactiveFg,
                    repository.quickActionsInactiveAlpha,
                    repository.quickActionsShapeOuter,
                    repository.quickActionsShapeInner,
                )
            ) { values ->
                val activeBg = values[0] as String
                val activeFg = values[1] as String
                val inactiveBg = values[2] as String
                val inactiveFg = values[3] as String
                val inactiveAlpha = values[4] as Int
                val shapeOuter = values[5] as Int
                val shapeInner = values[6] as Int
                val override =
                    QuickActionsTileOverride(
                        activeBg = activeBg.takeIf { it != UNSET },
                        activeFg = activeFg.takeIf { it != UNSET },
                        inactiveBg = inactiveBg.takeIf { it != UNSET },
                        inactiveFg = inactiveFg.takeIf { it != UNSET },
                        inactiveAlpha =
                            inactiveAlpha.takeIf { it != UNSET_INT }?.coerceIn(0, 100)?.let {
                                it / 100f
                            },
                        outerCornerRadiusDp = shapeOuter.takeIf { it != UNSET_INT },
                        innerCornerRadiusDp = shapeInner.takeIf { it != UNSET_INT },
                    )
                override.takeIf { it.hasAny }
            }
            .stateIn(scope, SharingStarted.Eagerly, null)
}
