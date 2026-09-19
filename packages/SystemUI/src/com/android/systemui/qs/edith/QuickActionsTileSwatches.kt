/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.Context
import androidx.annotation.ColorInt

/**
 * Catalog of the color swatches the (debug) **Quick Actions** tile color tuner can pick from.
 *
 * Separate from [EdithTileSwatches] (which stays the accent3/tertiary ramp for the main QS grid):
 * the Quick Actions palette is intentionally built from Material **role** colors
 * (`com.android.internal.R.color.materialColor*`), so it exposes the primary/secondary/tertiary/
 * surface roles rather than the raw `system_accent*_<tone>` ramps.
 *
 * Tags are stored (not resolved colors) so the tile re-resolves the current theme color at draw
 * time and stays in sync with palette changes.
 *
 * Must stay in sync with Settings' `QuickActionsTileSwatches`.
 */
object QuickActionsTileSwatches {
    /** Sentinel tag meaning "no override for this slot" (use the role default). */
    const val UNSET = ""

    /** Role tags, in display order. */
    val ALL_TAGS: List<String> = listOf(
        "primary",
        "primaryContainer",
        "primaryDim",
        "onPrimary",
        "onPrimaryContainer",
        "secondary",
        "secondaryContainer",
        "onSecondary",
        "onSecondaryContainer",
        "tertiary",
        "tertiaryContainer",
        "tertiaryDim",
        "onTertiary",
        "onTertiaryContainer",
        "surface",
        "surfaceContainer",
        "onSurface",
        "onSurfaceVariant",
        "outline",
        "inverseSurface",
        "inversePrimary",
    )

    /** Human-readable label for a [tag] (used by the tuner UI). */
    fun label(tag: String): String = tag

    /**
     * Resolves the [tag] to its current color for the given [context], or `null` when the tag is
     * [UNSET]/unknown.
     */
    @ColorInt
    fun resolve(context: Context, tag: String?): Int? {
        if (tag.isNullOrEmpty()) return null
        val resId =
            when (tag) {
                "primary" -> com.android.internal.R.color.materialColorPrimary
                "primaryContainer" -> com.android.internal.R.color.materialColorPrimaryContainer
                "primaryDim" -> com.android.internal.R.color.materialColorPrimaryDim
                "onPrimary" -> com.android.internal.R.color.materialColorOnPrimary
                "onPrimaryContainer" ->
                    com.android.internal.R.color.materialColorOnPrimaryContainer
                "secondary" -> com.android.internal.R.color.materialColorSecondary
                "secondaryContainer" ->
                    com.android.internal.R.color.materialColorSecondaryContainer
                "onSecondary" -> com.android.internal.R.color.materialColorOnSecondary
                "onSecondaryContainer" ->
                    com.android.internal.R.color.materialColorOnSecondaryContainer
                "tertiary" -> com.android.internal.R.color.materialColorTertiary
                "tertiaryContainer" -> com.android.internal.R.color.materialColorTertiaryContainer
                "tertiaryDim" -> com.android.internal.R.color.materialColorTertiaryDim
                "onTertiary" -> com.android.internal.R.color.materialColorOnTertiary
                "onTertiaryContainer" ->
                    com.android.internal.R.color.materialColorOnTertiaryContainer
                "surface" -> com.android.internal.R.color.materialColorSurface
                "surfaceContainer" -> com.android.internal.R.color.materialColorSurfaceContainer
                "onSurface" -> com.android.internal.R.color.materialColorOnSurface
                "onSurfaceVariant" -> com.android.internal.R.color.materialColorOnSurfaceVariant
                "outline" -> com.android.internal.R.color.materialColorOutline
                "inverseSurface" -> com.android.internal.R.color.materialColorInverseSurface
                "inversePrimary" -> com.android.internal.R.color.materialColorInversePrimary
                else -> null
            }
        return resId?.let { context.getColor(it) }
    }
}
