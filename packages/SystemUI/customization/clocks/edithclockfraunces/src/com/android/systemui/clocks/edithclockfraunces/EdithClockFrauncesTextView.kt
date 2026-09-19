/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.systemui.clocks.edithclockfraunces

import android.annotation.SuppressLint
import com.android.systemui.animation.AxisDefinition
import com.android.systemui.animation.GSFAxes
import com.android.systemui.customization.clocks.utils.FontUtils.put
import com.android.systemui.customization.clocks.view.DigitalClockTextView
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle

@SuppressLint("ViewConstructor")
class EdithClockFrauncesTextView(private val clockCtx: EdithClockFrauncesContext) :
    DigitalClockTextView(clockCtx) {

    override val typefaceCache = clockCtx.typefaceCache.getVariantCache(Unit)

    override var fontVariations: FontVariations = buildFontVariations(DEFAULT_AXES)

    override fun updateFontVariations(lsAxes: ClockAxisStyle): FontVariations {
        return buildFontVariations(DEFAULT_AXES.copyWith(lsAxes))
    }

    private fun buildFontVariations(lsAxes: ClockAxisStyle): FontVariations {
        val aodAxes =
            lsAxes.copyWith(
                ClockAxisStyle {
                    put(WEIGHT_AXIS, AOD_WEIGHT)
                    put(OPTICAL_SIZE_AXIS, AOD_OPSZ)
                }
            )

        return FontVariations(
            lockscreen = lsAxes.toFVar(),
            doze = aodAxes.toFVar(),
            fidget = buildAnimationTargetVariation(lsAxes, FIDGET_DISTS).toFVar(),
            chargeLockscreen = buildAnimationTargetVariation(lsAxes, CHARGE_DISTS).toFVar(),
            chargeDoze = buildAnimationTargetVariation(aodAxes, CHARGE_DISTS).toFVar(),
        )
    }

    companion object {
        val WEIGHT_AXIS: AxisDefinition = GSFAxes.WEIGHT
        val OPTICAL_SIZE_AXIS: AxisDefinition = GSFAxes.OPTICAL_SIZE

        val SOFT_AXIS = AxisDefinition("SOFT", 0f, 0f, 100f, 1f)
        val WONK_AXIS = AxisDefinition("WONK", 0f, 1f, 1f, 1f)

        private const val WEIGHT = 900f
        private const val OPSZ = 144f
        private const val WONK = 1f

        private const val AOD_WEIGHT = 400f
        private const val AOD_OPSZ = 144f

        val DEFAULT_AXES: ClockAxisStyle =
            ClockAxisStyle {
                put(WEIGHT_AXIS, WEIGHT)
                put(OPTICAL_SIZE_AXIS, OPSZ)
                put(SOFT_AXIS, SOFT_AXIS.defaultValue)
                put(WONK_AXIS, WONK)
            }

        private val CHARGE_DISTS =
            listOf(
                DigitalClockTextView.AxisAnimation(WEIGHT_AXIS, 200f),
            )

        private val FIDGET_DISTS =
            listOf(
                DigitalClockTextView.AxisAnimation(WEIGHT_AXIS, 200f),
            )
    }
}
