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

package com.android.systemui.qs.panels.ui.viewmodel

import com.android.systemui.animation.Expandable
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.haptics.msdl.qs.TileHapticsViewModel
import com.android.systemui.plugins.qs.QSTile
import com.android.systemui.qs.edith.QuickActionsInteractor
import com.android.systemui.qs.panels.shared.model.SizedTileImpl
import com.android.systemui.qs.pipeline.domain.interactor.CurrentTilesInteractor
import com.android.systemui.qs.pipeline.shared.TileSpec
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * View model backing the "Quick Actions" grid.
 *
 * The grid shows up to [TILE_COUNT] (4) fixed 1x1 tiles in a 2x2 layout, in the order given by
 * [specs] (the user-editable list, defaulting to the connectivity tiles). Tiles that are unavailable
 * on the current device are dropped.
 *
 * Tiles are resolved through [CurrentTilesInteractor.createTileSync]; they are not resizable and are
 * not necessarily part of the user's selectable QS tile list.
 */
@SysUISingleton
class QuickActionsGridViewModel
@Inject
constructor(
    private val tilesInteractor: CurrentTilesInteractor,
    private val quickActionsInteractor: QuickActionsInteractor,
    val squishinessViewModel: TileSquishinessViewModel,
    val tileHapticsViewModelFactory: TileHapticsViewModel.Factory,
) {

    /** The user-editable list of specs, collected reactively by the UI. */
    val specs: StateFlow<List<TileSpec>> = quickActionsInteractor.specs

    // Cache of created tiles, keyed by spec. Tiles are expensive and listener-bound, so they are
    // created at most once per spec and reused across list edits.
    private val tileCache = mutableMapOf<TileSpec, QSTile?>()

    /**
     * Resolves the [specs] into display tiles, in order, capped at [TILE_COUNT].
     *
     * Call this with the latest collected value of [specs] so the UI recomposes when the list
     * changes.
     */
    fun tileViewModels(specs: List<TileSpec>, max: Int = TILE_COUNT): List<TileViewModel> {
        val resolved = mutableListOf<TileViewModel>()
        for (spec in specs) {
            if (resolved.size == max) break
            if (resolved.any { it.spec == spec }) continue
            val tile = getOrCreateTile(spec) ?: continue
            resolved += TileViewModel(tile, spec, Expandable())
        }
        return resolved
    }

    /** The tiles as [SizedTile]s, all with a fixed width of 1 (non-resizable). */
    fun sizedTiles(specs: List<TileSpec>, max: Int = TILE_COUNT): List<SizedTileImpl<TileViewModel>> =
        tileViewModels(specs, max).map { SizedTileImpl(it, width = 1) }

    private fun getOrCreateTile(spec: TileSpec): QSTile? {
        if (tileCache.containsKey(spec)) {
            return tileCache[spec]
        }
        // The user explicitly chose this tile for Quick Actions, so render it whenever the pipeline
        // can create it. We intentionally do not gate on `isAvailable`, which can be transiently
        // false (e.g. before the tile's controllers are ready) and would otherwise make explicitly
        // added tiles disappear.
        val tile = tilesInteractor.createTileSync(spec)
        tileCache[spec] = tile
        return tile
    }

    companion object {
        /** Number of columns of the Quick Actions grid. */
        const val COLUMNS = 2

        /** Total number of tiles in the grid (2x2). */
        const val TILE_COUNT = COLUMNS * 2
    }
}
