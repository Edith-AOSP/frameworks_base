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
import com.android.systemui.qs.pipeline.shared.TileSpec
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Exposes the user-editable Quick Actions tile list.
 *
 * The list is seeded from [QuickActionsRepository.defaultSpecs] the first time it is read, so it is
 * always a genuine, editable list.
 */
@SysUISingleton
class QuickActionsInteractor
@Inject
constructor(
    @Background scope: CoroutineScope,
    private val repository: QuickActionsRepository,
) {
    /** The ordered list of specs shown in the Quick Actions grid. */
    val specs: StateFlow<List<TileSpec>> =
        repository.specs.stateIn(scope, SharingStarted.Eagerly, QuickActionsRepository.defaultSpecs)

    /** Sets the Quick Actions list to [specs]. */
    fun setSpecs(specs: List<TileSpec>) {
        repository.writeQuickActionSpecs(specs)
    }

    /** Adds [spec] to the end of the Quick Actions list (no-op if already present). */
    fun addSpec(spec: TileSpec) {
        val current = specs.value
        if (spec in current) return
        setSpecs(current + spec)
    }

    /** Inserts [spec] at [position] in the Quick Actions list. */
    fun insertSpec(spec: TileSpec, position: Int) {
        val current = specs.value.toMutableList()
        val existing = current.indexOf(spec)
        if (existing != -1) current.removeAt(existing)
        val index = position.coerceIn(0, current.size)
        current.add(index, spec)
        setSpecs(current)
    }

    /** Adds [spec] to the Quick Actions list, replacing [replaceSpec] if the list is full. */
    fun replaceSpec(replaceSpec: TileSpec, with: TileSpec) {
        val current = specs.value.toMutableList()
        val index = current.indexOf(replaceSpec)
        if (index == -1) return
        current[index] = with
        setSpecs(current)
    }

    /** Removes [spec] from the Quick Actions list. */
    fun removeSpec(spec: TileSpec) {
        setSpecs(specs.value - spec)
    }

    /** Resets the Quick Actions list to the built-in [QuickActionsRepository.defaultSpecs]. */
    fun resetToDefaults() {
        setSpecs(QuickActionsRepository.defaultSpecs)
    }

    /** Moves the tile at [fromIndex] to [toIndex] in the Quick Actions list. */
    fun moveSpec(fromIndex: Int, toIndex: Int) {
        val current = specs.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        if (fromIndex == toIndex) return
        val spec = current.removeAt(fromIndex)
        current.add(toIndex, spec)
        setSpecs(current)
    }
}
