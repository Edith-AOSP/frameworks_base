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

import android.icu.util.TimeZone
import android.view.View
import com.android.app.animation.Interpolators
import com.android.systemui.customization.clocks.AnimationState
import com.android.systemui.customization.clocks.DigitalTimeFormatter
import com.android.systemui.customization.clocks.DigitalTimespec
import com.android.systemui.customization.clocks.DigitalTimespecHandler
import com.android.systemui.customization.clocks.FontTextStyleImpl
import com.android.systemui.customization.clocks.view.DefaultClockFaceLayout
import com.android.systemui.customization.clocks.view.HorizontalAlignment
import com.android.systemui.customization.clocks.view.VerticalAlignment
import com.android.systemui.plugins.keyguard.VPointF
import com.android.systemui.plugins.keyguard.VRect
import com.android.systemui.plugins.keyguard.VRectF
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAnimations
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFaceLayout
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPositionAnimationArgs
import com.android.systemui.plugins.keyguard.ui.clocks.ClockViewIds
import com.android.systemui.plugins.keyguard.ui.clocks.ThemeConfig
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.util.Locale

class EdithClockLiterataFaceController(
    private val clockCtx: EdithClockLiterataContext,
    private val isLargeClock: Boolean,
) : ClockFaceController {

    private val smallFormatter =
        DigitalTimeFormatter("h:mm", clockCtx.timeKeeper, enableContentDescription = true)
    private val smallHandler =
        DigitalTimespecHandler(DigitalTimespec.TIME_FULL_FORMAT, smallFormatter)

    private val hourFormatter = DigitalTimeFormatter("hh", clockCtx.timeKeeper)
    private val minuteFormatter = DigitalTimeFormatter("mm", clockCtx.timeKeeper)
    private val digitViews =
        mutableListOf<Pair<EdithClockLiterataTextView, DigitalTimespecHandler>>()

    private val smallView: EdithClockLiterataTextView?
    private val largeGroup: EdithClockLiterataViewGroup?

    override val view: View

    var boundsListener: ((VRectF) -> Unit)? = null
    var maxSizeListener: ((VPointF) -> Unit)? = null

    private val dozeState = AnimationState(1F)
    private var hasFontAxes = false

    private var availableWidthPx: Int = clockCtx.resources.displayMetrics.widthPixels
    private var requestedFontSizePx: Float = 0f

    init {
        if (isLargeClock) {
            smallView = null
            val group =
                EdithClockLiterataViewGroup(clockCtx).apply {
                    addView(createDigitView(DigitalTimespec.FIRST_DIGIT, hourFormatter))
                    addView(createDigitView(DigitalTimespec.SECOND_DIGIT, hourFormatter))
                    addView(createDigitView(DigitalTimespec.FIRST_DIGIT, minuteFormatter))
                    addView(createDigitView(DigitalTimespec.SECOND_DIGIT, minuteFormatter))
                }
            group.onViewBoundsChanged = { boundsListener?.invoke(it) }
            group.onViewMaxSizeChanged = { maxSizeListener?.invoke(it) }
            largeGroup = group
            view = group
        } else {
            largeGroup = null
            val textView =
                EdithClockLiterataTextView(clockCtx).apply {
                    id = smallHandler.getViewId()
                    horizontalAlignment = HorizontalAlignment.START
                    verticalAlignment = VerticalAlignment.CENTER
                    applyStyles(SMALL_STYLE, SMALL_AOD_STYLE)
                    text = smallHandler.getText()
                }
            textView.onViewBoundsChanged = { boundsListener?.invoke(it) }
            textView.onViewMaxSizeChanged = { maxSizeListener?.invoke(it) }
            smallView = textView
            view = textView
        }
    }

    private fun createDigitView(
        timespec: DigitalTimespec,
        formatter: DigitalTimeFormatter,
    ): EdithClockLiterataTextView {
        val handler = DigitalTimespecHandler(timespec, formatter)
        return EdithClockLiterataTextView(clockCtx)
            .apply {
                id = handler.getViewId()
                horizontalAlignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                applyStyles(LARGE_STYLE, LARGE_AOD_STYLE)
                text = handler.getText()
            }
            .also { digitViews.add(it to handler) }
    }

    private fun applyFittedTextSize(fontSizePx: Float) {
        requestedFontSizePx = fontSizePx
        val small = smallView ?: return
        if (availableWidthPx <= 0) {
            small.applyTextSize(fontSizePx, constrainedByHeight = false)
            return
        }
        val maxWidthPx = availableWidthPx * FIT_WIDTH_FRACTION
        val paint = small.lockscreenPaint
        paint.textSize = fontSizePx
        val naturalWidth = paint.measureText(WIDEST_TIME_SAMPLE)
        val fittedSize =
            if (naturalWidth > maxWidthPx && naturalWidth > 0f) {
                fontSizePx * (maxWidthPx / naturalWidth)
            } else {
                fontSizePx
            }
        small.applyTextSize(fittedSize, constrainedByHeight = false)
    }

    override val config = ClockFaceConfig()

    override var theme = ThemeConfig(true, clockCtx.settings.seedColor)

    override val layout: ClockFaceLayout =
        DefaultClockFaceLayout(view).apply {
            view.id =
                if (isLargeClock) ClockViewIds.LOCKSCREEN_CLOCK_VIEW_LARGE
                else ClockViewIds.LOCKSCREEN_CLOCK_VIEW_SMALL
        }

    private fun refreshTime() {
        if (isLargeClock) {
            digitViews.forEach { (digit, handler) -> digit.text = handler.getText() }
            largeGroup?.refreshTime()
        } else {
            smallView?.let { small ->
                small.text = smallHandler.getText()
                small.refreshTime()
            }
        }
    }

    override val events =
        object : ClockFaceEvents {
            override fun onTimeTick() {
                clockCtx.timeKeeper.updateTime()
                if (isLargeClock) {
                    largeGroup?.contentDescription = null
                } else {
                    smallView?.contentDescription = smallHandler.getContentDescription()
                }
                refreshTime()
            }

            override fun onThemeChanged(theme: ThemeConfig) {
                this@EdithClockLiterataFaceController.theme = theme
                val lockscreenColor = theme.getDefaultColor(clockCtx.context)
                val aodColor = theme.getAodColor(clockCtx.context)
                if (isLargeClock) {
                    largeGroup?.updateColor(lockscreenColor, aodColor)
                } else {
                    smallView?.updateColor(lockscreenColor, aodColor)
                }
            }

            override fun onFontSettingChanged(fontSizePx: Float) {
                if (isLargeClock) {
                    largeGroup?.onFontSettingChanged(fontSizePx)
                } else {
                    applyFittedTextSize(fontSizePx)
                }
            }

            override fun onTargetRegionChanged(targetRegion: VRect) {
                if (!isLargeClock && targetRegion.width > 0) {
                    availableWidthPx = targetRegion.width
                    if (requestedFontSizePx > 0f) applyFittedTextSize(requestedFontSizePx)
                }
            }

            override fun onSecondaryDisplayChanged(onSecondaryDisplay: Boolean) {}
        }

    override val animations =
        object : ClockAnimations {
            override fun enter() {
                refreshTime()
            }

            override fun doze(fraction: Float) {
                val (hasChanged, hasJumped) = dozeState.update(fraction)
                if (hasChanged) {
                    if (isLargeClock) {
                        largeGroup?.animateDoze(dozeState.isActive, !hasJumped)
                    } else {
                        smallView?.animateDoze(dozeState.isActive, !hasJumped)
                    }
                }
                if (isLargeClock) {
                    largeGroup?.dozeFraction = fraction
                    largeGroup?.invalidate()
                } else {
                    smallView?.dozeFraction = fraction
                    smallView?.invalidate()
                }
            }

            override fun fold(fraction: Float) {
                refreshTime()
            }

            override fun charge() {
                if (isLargeClock) largeGroup?.animateCharge() else smallView?.animateCharge()
            }

            override fun onPositionAnimated(anim: ClockPositionAnimationArgs) {}

            override fun onPickerCarouselSwiping(swipingFraction: Float) {}

            override fun onFidgetTap(x: Float, y: Float) {
                if (isLargeClock) {
                    largeGroup?.animateFidget(VPointF(x, y), enforceBounds = true)
                } else {
                    smallView?.animateFidget(VPointF(x, y), enforceBounds = true)
                }
            }

            override fun onFontAxesChanged(style: ClockAxisStyle) {
                if (isLargeClock) {
                    largeGroup?.updateAxes(style, isAnimated = hasFontAxes)
                } else {
                    smallView?.updateAxes(style, isAnimated = hasFontAxes)
                }
                hasFontAxes = true
            }
        }

    fun onTimeZoneChanged(timeZone: TimeZone) {
        clockCtx.timeKeeper.timeZone = timeZone
        refreshTime()
    }

    fun onTimeFormatChanged(formatKind: TimeFormatKind) {
        smallFormatter.formatKind = formatKind
        hourFormatter.formatKind = formatKind
        minuteFormatter.formatKind = formatKind
        refreshTime()
    }

    fun onLocaleChanged(locale: Locale) {
        smallFormatter.locale = locale
        hourFormatter.locale = locale
        minuteFormatter.locale = locale
        if (isLargeClock) largeGroup?.onLocaleChanged(locale) else smallView?.invalidate()
        refreshTime()
    }

    companion object {
        private const val AOD_TRANSITION_DURATION = 800L

        private const val FIT_WIDTH_FRACTION = 0.9f

        private const val WIDEST_TIME_SAMPLE = "00:00"

        private val SMALL_STYLE =
            FontTextStyleImpl(fontSizeScale = 0.98f, fontFeatureSettings = "pnum")
        private val SMALL_AOD_STYLE =
            FontTextStyleImpl(
                transitionInterpolator = Interpolators.EMPHASIZED,
                transitionDuration = AOD_TRANSITION_DURATION,
                fontFeatureSettings = "pnum",
            )

        private val LARGE_STYLE = FontTextStyleImpl(lineHeight = 147.25f)
        private val LARGE_AOD_STYLE =
            FontTextStyleImpl(
                transitionInterpolator = Interpolators.EMPHASIZED,
                transitionDuration = AOD_TRANSITION_DURATION,
            )
    }
}
