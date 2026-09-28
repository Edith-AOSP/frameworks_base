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

package com.android.systemui.qs.panels.ui.compose.infinitegrid

import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.android.systemui.qs.edith.EdithTileColorOverride
import com.android.systemui.qs.edith.EdithTileSwatches
import com.android.systemui.qs.edith.QuickActionsTileOverride
import com.android.systemui.qs.edith.QuickActionsTileSwatches
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.EdithInactiveTileAlpha
import com.android.systemui.qs.panels.ui.viewmodel.TileUiState
import com.android.systemui.res.R

/**
 * Default Edith QS tertiary tile colors, set per-theme via the `edith_qs_tile_*` color
 * resources (different accent3 tones in `values`/`values-night`):
 *
 *  - Dark:  active bg=a3_200, active icon=a3_800, inactive bg=a3_800, inactive icon=a3_100
 *  - Light: active bg=a3_600, active icon=a3_50,  inactive bg=a3_10,  inactive icon=a3_700
 *
 * They alias `@android:color/system_accent3_*`, which are dynamic (theme-aware), so the tiles
 * follow palette changes (wallpaper / ThemePicker). The inactive background is applied with
 * [EdithInactiveTileAlpha], matching AOSP's surfaceEffect1, so the wallpaper shows through.
 * Unavailable tiles use the stock dimmed look.
 *
 * @param edithTileColorOverride optional per-slot color overrides from the (debug) color tuner
 *   in Settings; unset slots fall back to the resource defaults above.
 */
@Composable
fun edithTertiaryTileColors(
    visualState: Int,
    edithTileColorOverride: EdithTileColorOverride? = null,
): TileColors {
    if (visualState != STATE_INACTIVE && visualState != STATE_ACTIVE) {
        // Unavailable (e.g. STATE_UNAVAILABLE): a dimmed neutral surface with a still-visible
        // glyph, matching the default unavailable tile look.
        return TileDefaults.unavailableTileColors()
    }
    val context = LocalContext.current
    // Read the configuration (assetsSeq matches PlatformTheme) and the dark/light state so the
    // color resolution below is re-run when the theme changes: a dark<->light toggle changes the
    // values/values-night resources without necessarily changing assetsSeq, and picking new
    // colors in ThemePicker changes assetsSeq. Without both keys the tile keeps the previous
    // mode's color until something else forces a recomposition.
    val assetsSeq = LocalConfiguration.current.assetsSeq
    val isDark = isSystemInDarkTheme()
    val active = visualState == STATE_ACTIVE
    val inactiveAlpha = edithTileColorOverride?.inactiveAlpha ?: EdithInactiveTileAlpha

    val defaultBgRes =
        if (active) R.color.edith_qs_tile_active_bg else R.color.edith_qs_tile_inactive_bg
    val defaultIconRes =
        if (active) R.color.edith_qs_tile_active_icon else R.color.edith_qs_tile_inactive_icon

    // The override holds a swatch tag; resolve it against the current theme so the tile still
    // follows palette changes (wallpaper / ThemePicker / dark mode). Keyed on assetsSeq and the
    // dark/light state so the resolved colors are recomputed when the theme changes.
    val bgTag =
        if (active) edithTileColorOverride?.activeBg else edithTileColorOverride?.inactiveBg
    val iconTag =
        if (active) edithTileColorOverride?.activeFg else edithTileColorOverride?.inactiveFg
    val resolved =
        remember(context, isDark, assetsSeq, bgTag, iconTag, defaultBgRes, defaultIconRes) {
            val bgArgb =
                EdithTileSwatches.resolve(context, bgTag) ?: context.getColor(defaultBgRes)
            val fgArgb =
                EdithTileSwatches.resolve(context, iconTag) ?: context.getColor(defaultIconRes)
            bgArgb to fgArgb
        }
    val bg = Color(resolved.first).copy(alpha = if (active) 1f else inactiveAlpha)
    val fg = Color(resolved.second)
    return TileColors(
        background = bg,
        iconBackground = bg,
        label = fg,
        secondaryLabel = fg,
        icon = fg,
    )
}

/**
 * Colors for the Quick Actions tiles: the stock look, with any tuner overrides layered on top
 * (resolved through [QuickActionsTileSwatches]). Slots without an override keep their stock
 * color.
 */
@Composable
fun edithQuickActionsTileColors(
    uiState: TileUiState,
    iconOnly: Boolean,
    override: QuickActionsTileOverride? = null,
): TileColors {
    val stock = TileDefaults.getColorForState(uiState, iconOnly)
    if (override == null || !override.hasAnyColor) {
        return stock
    }
    if (uiState.visualState != STATE_INACTIVE && uiState.visualState != STATE_ACTIVE) {
        return stock
    }
    val context = LocalContext.current
    // Keyed on assetsSeq and the dark/light state so the resolved colors are recomputed when the
    // theme changes (a dark<->light toggle may not change assetsSeq).
    val assetsSeq = LocalConfiguration.current.assetsSeq
    val isDark = isSystemInDarkTheme()
    val active = uiState.visualState == STATE_ACTIVE
    val inactiveAlpha = override.inactiveAlpha ?: EdithInactiveTileAlpha

    val bgTag = if (active) override.activeBg else override.inactiveBg
    val iconTag = if (active) override.activeFg else override.inactiveFg
    val resolved =
        remember(context, isDark, assetsSeq, bgTag, iconTag) {
            QuickActionsTileSwatches.resolve(context, bgTag) to
                QuickActionsTileSwatches.resolve(context, iconTag)
        }
    // Anything the tuner didn't set keeps its stock color.
    val background =
        resolved.first?.let { Color(it).copy(alpha = if (active) 1f else inactiveAlpha) }
            ?: stock.background
    val fg = resolved.second?.let { Color(it) }
    return TileColors(
        background = background,
        iconBackground = background,
        label = fg ?: stock.label,
        secondaryLabel = fg ?: stock.secondaryLabel,
        icon = fg ?: stock.icon,
    )
}

/**
 * Edith QS color scheme for the editor tiles. The editor uses a single (circle) shape, so it
 * uses the inactive tile colors (per-theme `edith_qs_tile_inactive_*` resources), with the
 * background kept semi-transparent ([EdithInactiveTileAlpha]) to match the QS tiles.
 */
@Composable
fun edithTertiaryEditTileColors(): TileColors {
    val context = LocalContext.current
    // Read the config so the colors are re-resolved when the theme changes (ThemePicker).
    val assetsSeq = LocalConfiguration.current.assetsSeq
    val resolved =
        remember(context, assetsSeq) {
            context.getColor(R.color.edith_qs_tile_inactive_bg) to
                context.getColor(R.color.edith_qs_tile_inactive_icon)
        }
    val bg = Color(resolved.first).copy(alpha = EdithInactiveTileAlpha)
    val fg = Color(resolved.second)
    return TileColors(
        background = bg,
        iconBackground = bg,
        label = fg,
        secondaryLabel = fg,
        icon = fg,
    )
}
