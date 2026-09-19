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
class EdithClockFrauncesProvider : ClockProviderPlugin {
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
        val frauncesTypeface =
            try {
                pluginCtx.resources.getFont(R.font.fraunces_variable)
            } catch (ex: Exception) {
                Typeface.create(FRAUNCES_FAMILY, Typeface.NORMAL)
            }
        val typefaceCache =
            TypefaceCache<Unit>(buffers.infraMessageBuffer, NUM_CLOCK_FONT_ANIMATION_STEPS) {
                frauncesTypeface
            }

        val fontAxes = FONT_AXES.merge(settings.axes)
        val clockSettings = settings.copy(axes = ClockAxisStyle(fontAxes))

        val clockCtx =
            EdithClockFrauncesContext(
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

        return EdithClockFrauncesController(clockCtx, buffers)
    }

    override fun getClockPickerConfig(settings: ClockSettings): ClockPickerConfig {
        if (settings.clockId != CLOCK_ID)
            throw IllegalArgumentException("${settings.clockId} unsupported by this provider")

        val fontAxes = FONT_AXES.merge(settings.axes)
        val presetConfig =
            AxisPresetConfig(
                    groups =
                        listOf(
                            buildPresetGroup(isRounded = true),
                            buildPresetGroup(isRounded = false),
                        )
                )
                .let { cfg -> cfg.copy(current = cfg.findStyle(ClockAxisStyle(fontAxes))) }
        val thumbnail = pluginCtx.resources.getDrawable(R.drawable.edith_clock_thumbnail, null)

        return ClockPickerConfig(
            CLOCK_ID,
            pluginCtx.resources.getString(R.string.edith_clock_name),
            pluginCtx.resources.getString(R.string.edith_clock_description),
            thumbnail,
            isReactiveToTone = true,
            axes = fontAxes,
            presetConfig = presetConfig,
        )
    }

    private fun buildPresetGroup(isRounded: Boolean): AxisPresetConfig.Group {
        val soft = if (isRounded) SOFT_AXIS.minValue else SOFT_AXIS.maxValue
        val wonk = if (isRounded) WONK_AXIS.maxValue else WONK_AXIS.minValue
        return AxisPresetConfig.Group(
            presets =
                PRESET_AXES.map { (opsz, wght) ->
                    ClockAxisStyle {
                        put(OPTICAL_SIZE_AXIS, opsz)
                        put(WEIGHT_AXIS, wght)
                        put(SOFT_AXIS, soft)
                        put(WONK_AXIS, wonk)
                    }
                },
            icon = pluginCtx.resources.getDrawable(R.drawable.edith_clock_thumbnail, null),
        )
    }

    companion object {
        val CLOCK_ID = "EDITH_CLOCK_FRAUNCES"
        private const val FRAUNCES_FAMILY = "fraunces-clock"

        private const val NUM_CLOCK_FONT_ANIMATION_STEPS = 30

        private val OPTICAL_SIZE_AXIS =
            AxisDefinition(
                tag = "opsz",
                minValue = 9f,
                defaultValue = 144f,
                maxValue = 144f,
                animationStep = 1f,
            )
        private val WEIGHT_AXIS =
            AxisDefinition(
                tag = "wght",
                minValue = 100f,
                defaultValue = 900f,
                maxValue = 900f,
                animationStep = 10f,
            )

        private val SOFT_AXIS =
            AxisDefinition(
                tag = "SOFT",
                minValue = 0f,
                defaultValue = 0f,
                maxValue = 100f,
                animationStep = 1f,
            )
        private val WONK_AXIS =
            AxisDefinition(
                tag = "WONK",
                minValue = 0f,
                defaultValue = 1f,
                maxValue = 1f,
                animationStep = 1f,
            )

        private val PRESET_AXES =
            listOf(9f to 400f, 36f to 500f, 72f to 600f, 108f to 700f, 144f to 900f)

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
                SOFT_AXIS.toClockAxis(
                    type = AxisType.Boolean,
                    currentValue = SOFT_AXIS.defaultValue,
                    name = "Soft",
                    description = "Rounded glyph terminals",
                ),
                WONK_AXIS.toClockAxis(
                    type = AxisType.Boolean,
                    currentValue = WONK_AXIS.defaultValue,
                    name = "Wonk",
                    description = "Rounded glyph alternates",
                ),
            )
    }
}
