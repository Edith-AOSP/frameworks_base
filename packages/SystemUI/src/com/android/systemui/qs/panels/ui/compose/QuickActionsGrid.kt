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

package com.android.systemui.qs.panels.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.systemui.animation.Expandable
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.grid.ui.compose.VerticalSpannedGrid
import com.android.systemui.qs.edith.QuickActionsTileOverride
import com.android.systemui.qs.panels.ui.compose.infinitegrid.Tile
import com.android.systemui.qs.panels.ui.viewmodel.BounceableTileViewModel
import com.android.systemui.qs.panels.ui.viewmodel.QuickActionsGridViewModel
import com.android.systemui.qs.panels.ui.viewmodel.QuickActionsGridViewModel.Companion.COLUMNS
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.qs.shared.ui.QuickSettings.Elements.toQuickActionElementKey
import com.android.systemui.res.R

/**
 * A fixed, non-resizable 2x2 "Quick Actions" grid (WiFi, Bluetooth, Mobile data, Hotspot).
 *
 * Unlike the regular QS tile grid, the tiles here are always 1x1 and cannot be resized or edited.
 */
@Composable
fun ContentScope.QuickActionsGrid(
    viewModel: QuickActionsGridViewModel,
    modifier: Modifier = Modifier,
    listening: () -> Boolean,
    edithTileStyle: Boolean = false,
    edithColorEnabled: Boolean = false,
    edithQuickActionsOverride: QuickActionsTileOverride? = null,
) {
    val columns = COLUMNS
    val specs by viewModel.specs.collectAsStateWithLifecycle()
    val sizedTiles = viewModel.sizedTiles(specs)
    val tiles = sizedTiles.fastMap { it.tile }
    val squishiness by viewModel.squishinessViewModel.squishiness.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Per-composition Expandable per spec. This grid renders in both QQS and the expanded QS panel
    // at the same time; a shared Expandable would hold two transition sources and the dialog morph
    // could pick the wrong (off-screen) one. Keeping one Expandable per rendering keeps the source
    // unambiguous. Keyed by spec so it is stable across recompositions of this grid.
    val expandables = remember { mutableMapOf<TileSpec, Expandable>() }

    Box(modifier = modifier) {
        val bounceables =
            remember(sizedTiles) { List(sizedTiles.size) { BounceableTileViewModel() } }
        val spans by remember(sizedTiles) { derivedStateOf { sizedTiles.fastMap { it.width } } }
        VerticalSpannedGrid(
            columns = columns,
            columnSpacing = dimensionResource(R.dimen.qs_tile_margin_horizontal),
            rowSpacing = dimensionResource(R.dimen.qs_tile_margin_vertical),
            spans = spans,
            modifier = Modifier.sysuiResTag("quick_actions_grid"),
            keys = { sizedTiles[it].tile.spec },
        ) { spanIndex, column, isFirstInColumn, isLastInColumn ->
            val it = sizedTiles[spanIndex]
            // Use a dedicated element key namespace so these tiles participate in the QQS<->QS
            // shared-element transition without colliding with the regular tile grid.
            Element(it.tile.spec.toQuickActionElementKey(), Modifier) {
                Tile(
                    tile = it.tile,
                    iconOnly = false,
                    squishiness = { squishiness },
                    coroutineScope = scope,
                    bounceableInfo =
                        bounceables.bounceableInfo(
                            it,
                            index = spanIndex,
                            column = column,
                            columns = columns,
                            isFirstInRow = isFirstInColumn,
                            isLastInRow = isLastInColumn,
                        ),
                    expandableOverride = expandables.getOrPut(it.tile.spec) { Expandable() },
                    tileHapticsViewModelFactory = viewModel.tileHapticsViewModelFactory,
                    // There is no details view for the fixed Quick Actions grid.
                    detailsViewModel = null,
                    isVisible = listening,
                    edithTileStyle = edithTileStyle,
                    edithColorEnabled = edithColorEnabled && edithTileStyle,
                    edithQuickActionsTile = edithTileStyle,
                    // Carries both the color overrides and the dual-target shape overrides.
                    edithQuickActionsOverride = edithQuickActionsOverride.takeIf { edithTileStyle },
                )
            }
        }
    }

    TileListener(tiles, listening)
}
