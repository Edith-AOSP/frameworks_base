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
package com.android.systemui.clocks.edithclockliterata

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.android.systemui.customization.clocks.R
import com.android.systemui.customization.clocks.utils.ViewUtils.measuredSize
import com.android.systemui.customization.clocks.view.DigitalClockViewGroup
import com.android.systemui.plugins.keyguard.VMeasurePoint
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import java.util.Locale

@SuppressLint("ViewConstructor")
class EdithClockLiterataViewGroup(clockCtx: EdithClockLiterataContext) :
    DigitalClockViewGroup<EdithClockLiterataTextView>(clockCtx) {

    override val children: Sequence<EdithClockLiterataTextView>
        get() = (this as ViewGroup).children.filterIsInstance<EdithClockLiterataTextView>()

    init {
        setWillNotDraw(false)
    }

    override fun calculateSize(measureSpec: VMeasurePoint): VPointF {
        val yBuffer = context.resources.getDimensionPixelSize(R.dimen.clock_vertical_digit_buffer)
        return maxChildSize * VPointF(2f, 2f) + VPointF(0f, yBuffer)
    }

    override fun getChildFrame(child: EdithClockLiterataTextView): VRectF {
        val yBuffer = context.resources.getDimensionPixelSize(R.dimen.clock_vertical_digit_buffer)
        var offset =
            maxChildSize.run {
                when (child.id) {
                    ClockViewIds.HOUR_FIRST_DIGIT -> VPointF.ZERO
                    ClockViewIds.HOUR_SECOND_DIGIT -> VPointF(x, 0f)
                    ClockViewIds.MINUTE_FIRST_DIGIT -> VPointF(0f, y + yBuffer)
                    ClockViewIds.MINUTE_SECOND_DIGIT -> VPointF(x, y + yBuffer)
                    else -> VPointF.ZERO
                }
            }

        val childSize = child.measuredSize
        val midY = (maxChildSize.y - childSize.y) / 2f
        val midX = measuredWidth / 4f
        offset += VPointF(midX - childSize.x / 2f, midY)

        return VRectF.fromTopLeft(offset, childSize)
    }

    override fun refreshTime() {
        children.forEach { it.refreshText() }
    }

    override fun onLocaleChanged(locale: Locale) {
        requestLayout()
    }

    override fun updateColor(lockscreenColor: Int, aodColor: Int) {
        children.forEach { it.updateColor(lockscreenColor, aodColor) }
        invalidate()
    }

    override fun updateAxes(axes: ClockAxisStyle, isAnimated: Boolean) {
        children.forEach { it.updateAxes(axes, isAnimated) }
        requestLayout()
    }

    override fun onFontSettingChanged(fontSizePx: Float) {
        children.forEach { it.applyTextSize(fontSizePx, constrainedByHeight = false) }
    }

    override fun animateDoze(isDozing: Boolean, isAnimated: Boolean) {
        children.forEach { it.animateDoze(isDozing, isAnimated) }
    }

    override fun animateCharge() {
        children.forEach { it.animateCharge() }
    }

    override fun animateFidget(pt: VPointF, enforceBounds: Boolean): Boolean {
        if (enforceBounds) {
            if (visibility != View.VISIBLE) return false
            val bounds = VRectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
            if (!bounds.contains(pt)) return false
        }
        children.forEach { it.animateFidget(pt, enforceBounds = false) }
        return true
    }
}
