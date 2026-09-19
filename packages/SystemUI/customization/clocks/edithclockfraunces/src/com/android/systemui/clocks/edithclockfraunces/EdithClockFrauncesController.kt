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

import android.icu.util.TimeZone
import com.android.systemui.plugins.keyguard.data.model.AlarmData
import com.android.systemui.plugins.keyguard.data.model.WeatherData
import com.android.systemui.plugins.keyguard.data.model.ZenData
import com.android.systemui.plugins.keyguard.ui.clocks.ClockConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEventListeners
import com.android.systemui.plugins.keyguard.ui.clocks.ClockEvents
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.TimeFormatKind
import java.io.PrintWriter
import java.util.Locale

class EdithClockFrauncesController(
    private val clockCtx: EdithClockFrauncesContext,
    messageBuffers: ClockMessageBuffers,
) : ClockController {

    override val smallClock =
        EdithClockFrauncesFaceController(
            clockCtx.copy(messageBuffer = messageBuffers.smallClockMessageBuffer),
            isLargeClock = false,
        )

    override val largeClock =
        EdithClockFrauncesFaceController(
            clockCtx.copy(messageBuffer = messageBuffers.largeClockMessageBuffer),
            isLargeClock = true,
        )

    override val config =
        ClockConfig(
            EdithClockFrauncesProvider.CLOCK_ID,
            clockCtx.resources.getString(R.string.edith_clock_name),
            clockCtx.resources.getString(R.string.edith_clock_description),
        )

    override val events =
        object : ClockEvents {
            override fun onTimeZoneChanged(timeZone: TimeZone) {
                smallClock.onTimeZoneChanged(timeZone)
                largeClock.onTimeZoneChanged(timeZone)
            }

            override fun onTimeFormatChanged(formatKind: TimeFormatKind) {
                smallClock.onTimeFormatChanged(formatKind)
                largeClock.onTimeFormatChanged(formatKind)
            }

            override fun onLocaleChanged(locale: Locale) {
                smallClock.onLocaleChanged(locale)
                largeClock.onLocaleChanged(locale)
            }

            override fun onWeatherDataChanged(data: WeatherData) {}

            override fun onAlarmDataChanged(data: AlarmData) {}

            override fun onZenDataChanged(data: ZenData) {}
        }

    override val eventListeners = ClockEventListeners()

    override fun initialize(isDarkTheme: Boolean, dozeFraction: Float, foldFraction: Float) {
        smallClock.run {
            boundsListener = { eventListeners.fire { onBoundsChanged(it, isLargeClock = false) } }
            maxSizeListener = {
                eventListeners.fire { onMaxSizeChanged(it, isLargeClock = false) }
            }
            events.onThemeChanged(theme.copy(isDarkTheme = isDarkTheme))
            animations.onFontAxesChanged(clockCtx.settings.axes)
            animations.doze(dozeFraction)
            animations.fold(foldFraction)
            events.onTimeTick()
        }

        largeClock.run {
            boundsListener = { eventListeners.fire { onBoundsChanged(it, isLargeClock = true) } }
            maxSizeListener = {
                eventListeners.fire { onMaxSizeChanged(it, isLargeClock = true) }
            }
            events.onThemeChanged(theme.copy(isDarkTheme = isDarkTheme))
            animations.onFontAxesChanged(clockCtx.settings.axes)
            animations.doze(dozeFraction)
            animations.fold(foldFraction)
            events.onTimeTick()
        }
    }

    override fun dump(pw: PrintWriter) {}
}
