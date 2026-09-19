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

import android.content.Context
import android.graphics.Typeface
import com.android.internal.annotations.Keep
import com.android.systemui.animation.AxisDefinition
import com.android.systemui.customization.clocks.ClockContextImpl
import com.android.systemui.customization.clocks.ClockLogger
import com.android.systemui.customization.clocks.TimeKeeperImpl
import com.android.systemui.customization.clocks.TypefaceCache
import com.android.systemui.customization.clocks.utils.FontUtils.put
import com.android.systemui.customization.clocks.utils.FontUtils.toClockAxis
import com.android.systemui.log.LogcatOnlyMessageBuffer
import com.android.systemui.log.core.LogLevel
import com.android.systemui.plugins.annotations.Requires
import com.android.systemui.plugins.keyguard.ui.clocks.AxisPresetConfig
import com.android.systemui.plugins.keyguard.ui.clocks.AxisType
import com.android.systemui.plugins.keyguard.ui.clocks.ClockAxisStyle
import com.android.systemui.plugins.keyguard.ui.clocks.ClockController
import com.android.systemui.plugins.keyguard.ui.clocks.ClockFontAxis.Companion.merge
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMessageBuffers
import com.android.systemui.plugins.keyguard.ui.clocks.ClockMetadata
import com.android.systemui.plugins.keyguard.ui.clocks.ClockPickerConfig
import com.android.systemui.plugins.keyguard.ui.clocks.ClockProviderPlugin
import com.android.systemui.plugins.keyguard.ui.clocks.ClockSettings

@Keep
@Requires(target = ClockProviderPlugin::class, version = ClockProviderPlugin.VERSION)
class EdithClockLiterataProvider : ClockProviderPlugin {
    private lateinit var hostCtx: Context
    private lateinit var pluginCtx: Context
    private lateinit var messageBuffers: ClockMessageBuffers

    override fun onCreate(hostCtx: Context, pluginCtx: Context) {
        this.hostCtx = hostCtx
        this.pluginCtx = pluginCtx
    }

    override fun initialize(buffers: ClockMessageBuffers?) {
        this.messageBuffers =
            buffers ?: ClockMessageBuffers(LogcatOnlyMessageBuffer(LogLevel.DEBUG))
    }

    override fun getClocks(): List<ClockMetadata> {
        return listOf(ClockMetadata(CLOCK_ID))
    }

    override fun createClock(ctx: Context, settings: ClockSettings): ClockController {
        if (settings.clockId != CLOCK_ID)
            throw IllegalArgumentException("${settings.clockId} unsupported by this provider")

        val buffers = messageBuffers ?: ClockMessageBuffers(ClockLogger.DEFAULT_MESSAGE_BUFFER)
        val literataTypeface =
            try {
                pluginCtx.resources.getFont(R.font.literata_variable)
            } catch (ex: Exception) {
                Typeface.create(LITERATA_FAMILY, Typeface.NORMAL)
            }
        val typefaceCache =
            TypefaceCache<Unit>(buffers.infraMessageBuffer, NUM_CLOCK_FONT_ANIMATION_STEPS) {
                literataTypeface
            }

        val fontAxes = FONT_AXES.merge(settings.axes)
        val clockSettings = settings.copy(axes = ClockAxisStyle(fontAxes))

        val clockCtx =
            EdithClockLiterataContext(
                typefaceCache,
                ClockContextImpl(
                    pluginCtx,
                    pluginCtx.resources,
                    clockSettings,
                    buffers.infraMessageBuffer,
                    vibrator = null,
                    timeKeeper = TimeKeeperImpl(),
                    isAnimationEnabled = true,
                ),
            )

        return EdithClockLiterataController(clockCtx, buffers)
    }

    override fun getClockPickerConfig(settings: ClockSettings): ClockPickerConfig {
        if (settings.clockId != CLOCK_ID)
            throw IllegalArgumentException("${settings.clockId} unsupported by this provider")

        val fontAxes = FONT_AXES.merge(settings.axes)
        val presetConfig =
            AxisPresetConfig(groups = listOf(buildPresetGroup())).let { cfg ->
                cfg.copy(current = cfg.findStyle(ClockAxisStyle(fontAxes)))
            }
        val thumbnail =
            pluginCtx.resources.getDrawable(R.drawable.edith_clock_literata_thumbnail, null)

        return ClockPickerConfig(
            CLOCK_ID,
            pluginCtx.resources.getString(R.string.edith_literata_clock_name),
            pluginCtx.resources.getString(R.string.edith_literata_clock_description),
            thumbnail,
            isReactiveToTone = true,
            axes = fontAxes,
            presetConfig = presetConfig,
        )
    }

    private fun buildPresetGroup(): AxisPresetConfig.Group {
        return AxisPresetConfig.Group(
            presets =
                PRESET_AXES.map { (opsz, wght) ->
                    ClockAxisStyle {
                        put(OPTICAL_SIZE_AXIS, opsz)
                        put(WEIGHT_AXIS, wght)
                    }
                },
            icon = pluginCtx.resources.getDrawable(R.drawable.edith_clock_literata_thumbnail, null),
        )
    }

    companion object {
        val CLOCK_ID = "EDITH_CLOCK_LITERATA"
        private const val LITERATA_FAMILY = "literata-clock"

        private const val NUM_CLOCK_FONT_ANIMATION_STEPS = 30

        private val OPTICAL_SIZE_AXIS =
            AxisDefinition(
                tag = "opsz",
                minValue = 7f,
                defaultValue = 72f,
                maxValue = 72f,
                animationStep = 1f,
            )
        private val WEIGHT_AXIS =
            AxisDefinition(
                tag = "wght",
                minValue = 200f,
                defaultValue = 900f,
                maxValue = 900f,
                animationStep = 10f,
            )

        private val PRESET_AXES =
            listOf(7f to 400f, 18f to 500f, 36f to 600f, 54f to 700f, 72f to 900f)

        private val FONT_AXES =
            listOf(
                WEIGHT_AXIS.toClockAxis(
                    type = AxisType.Float,
                    currentValue = WEIGHT_AXIS.defaultValue,
                    name = "Weight",
                    description = "Glyph weight",
                ),
                OPTICAL_SIZE_AXIS.toClockAxis(
                    type = AxisType.Float,
                    currentValue = OPTICAL_SIZE_AXIS.defaultValue,
                    name = "Width",
                    description = "Glyph width (optical size)",
                ),
            )
    }
}
