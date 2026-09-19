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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.ContentScope
import com.android.systemui.qs.panels.ui.compose.infinitegrid.LargeStaticTile
import com.android.systemui.qs.panels.ui.compose.infinitegrid.SmallStaticTile
import com.android.systemui.qs.panels.ui.viewmodel.TileViewModel
import com.android.systemui.qs.panels.ui.viewmodel.toIconProvider
import com.android.systemui.qs.panels.ui.viewmodel.toUiState

/**
 * Renders [tiles] as the *real* QS tiles (active/inactive styling, rounded shape), wrapped
 * [columns] per row. The tiles are *not* interactive: they are just a preview.
 *
 * When [iconOnly] is `true`, only the icon is shown (used for the small QS tiles preview);
 * otherwise the icon and its label are shown (used for the Quick Actions preview).
 */
@Composable
fun ContentScope.RealTilePreview(
    tiles: List<TileViewModel>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    iconOnly: Boolean = false,
    edithColorEnabled: Boolean = false,
    pillShapes: Boolean = false,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(columns).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (tile in row) {
                    Box(modifier = Modifier.weight(1f)) {
                        StaticTilePreview(tile, iconOnly, edithColorEnabled, pillShapes)
                    }
                }
                // Spacers for an incomplete row.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StaticTilePreview(
    tile: TileViewModel,
    iconOnly: Boolean,
    edithColorEnabled: Boolean,
    pillShapes: Boolean,
) {
    val resources = LocalResources.current
    val uiState by
        produceState(tile.currentState.toUiState(resources), tile, resources) {
            tile.state.collect { value = it.toUiState(resources) }
        }
    val iconProvider by
        produceState(tile.currentState.toIconProvider(), tile) {
            tile.state.collect { value = it.toIconProvider() }
        }
    if (iconOnly) {
        SmallStaticTile(
            uiState = uiState,
            iconProvider = iconProvider,
            modifier = Modifier.fillMaxWidth(),
            edithTileStyle = true,
            edithColorEnabled = edithColorEnabled,
        )
    } else {
        LargeStaticTile(
            uiState = uiState,
            iconProvider = iconProvider,
            modifier = Modifier.fillMaxWidth(),
            // The Quick Actions preview is drawn as a pill for all states, matching the editor.
            shape = if (pillShapes) RoundedCornerShape(percent = 50) else null,
            dualTarget = pillShapes && uiState.handlesToggleClick,
            innerShape = RoundedCornerShape(percent = 50),
        )
    }
}
