/*
 * Copyright (C) 2026 The Edith AOSP Project
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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

/** Style parameters for the Edith QS tiles, passed to the tile composables as one value. */
@Immutable
data class EdithTileStyle(
    /** Whether the Edith square-tile layout is active at all. */
    val enabled: Boolean = false,
    /** Whether the Edith tile colour scheme is active (requires [enabled]). */
    val colorsEnabled: Boolean = false,
    /** Fixed square tile size, or `null` to size from the grid. */
    val squareSize: Dp? = null,
    /** Per-slot QS colour overrides from the tuner. */
    val tileColorOverride: EdithTileColorOverride? = null,
    /** Whether this tile is a Quick Actions tile (uses its own palette and shape). */
    val quickActionsTile: Boolean = false,
    /** Per-slot Quick Actions colour/shape overrides. */
    val quickActionsOverride: QuickActionsTileOverride? = null,
) {
    companion object {
        /** Stock presentation. */
        val Disabled = EdithTileStyle()
    }
}
