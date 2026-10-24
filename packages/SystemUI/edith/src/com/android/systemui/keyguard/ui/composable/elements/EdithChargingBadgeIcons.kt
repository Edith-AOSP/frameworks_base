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

package com.android.systemui.keyguard.ui.composable.elements

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Material Symbols Outlined `battery_android_full`: the stock Android full-battery glyph, used once
 * the charging badge has finished its opening spin on a full battery.
 *
 * Kept as an [ImageVector] because Material Symbols are not part of the Material Icons pack that
 * ships with this build.
 */
internal val BatteryAndroidFull: ImageVector by lazy {
    ImageVector.Builder(
            name = "battery_android_full",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 18f)
                quadTo(2.75f, 18f, 1.88f, 17.13f)
                reflectiveQuadTo(1f, 15f)
                verticalLineTo(9f)
                quadTo(1f, 7.75f, 1.88f, 6.88f)
                reflectiveQuadTo(4f, 6f)
                horizontalLineTo(17.5f)
                quadToRelative(1.25f, 0f, 2.13f, 0.88f)
                reflectiveQuadTo(20.5f, 9f)
                verticalLineToRelative(6f)
                quadToRelative(0f, 1.25f, -0.88f, 2.13f)
                reflectiveQuadTo(17.5f, 18f)
                horizontalLineTo(4f)
                close()
                moveTo(21.5f, 14.5f)
                verticalLineToRelative(-5f)
                horizontalLineTo(22f)
                quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                reflectiveQuadTo(23f, 10.5f)
                verticalLineToRelative(3f)
                quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                reflectiveQuadTo(22f, 14.5f)
                horizontalLineTo(21.5f)
                close()
            }
        }
        .build()
}

/**
 * Material Symbols Outlined `battery_android_frame_alert`: the battery frame with an alert mark,
 * used once the opening spin is over while the battery defender is holding the charge.
 */
internal val BatteryAndroidFrameAlert: ImageVector by lazy {
    ImageVector.Builder(
            name = "battery_android_frame_alert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.29f, 16.21f)
                quadTo(20f, 15.93f, 20f, 15.5f)
                reflectiveQuadToRelative(0.29f, -0.71f)
                reflectiveQuadTo(21f, 14.5f)
                quadToRelative(0.43f, 0f, 0.71f, 0.29f)
                reflectiveQuadTo(22f, 15.5f)
                reflectiveQuadToRelative(-0.29f, 0.71f)
                reflectiveQuadTo(21f, 16.5f)
                reflectiveQuadTo(20.29f, 16.21f)
                close()
                moveTo(20f, 13f)
                verticalLineTo(7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                horizontalLineTo(20f)
                close()
                moveTo(4f, 18f)
                quadTo(2.75f, 18f, 1.88f, 17.13f)
                reflectiveQuadTo(1f, 15f)
                verticalLineTo(9f)
                quadTo(1f, 7.75f, 1.88f, 6.88f)
                reflectiveQuadTo(4f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(8f)
                horizontalLineTo(4f)
                quadTo(3.58f, 8f, 3.29f, 8.29f)
                reflectiveQuadTo(3f, 9f)
                verticalLineToRelative(6f)
                quadToRelative(0f, 0.42f, 0.29f, 0.71f)
                reflectiveQuadTo(4f, 16f)
                horizontalLineTo(18f)
                quadToRelative(0f, 0.57f, 0.21f, 1.09f)
                reflectiveQuadTo(18.78f, 18f)
                horizontalLineTo(4f)
                close()
                moveTo(4f, 15f)
                verticalLineTo(9f)
                horizontalLineTo(18f)
                verticalLineToRelative(6f)
                horizontalLineTo(4f)
                close()
            }
        }
        .build()
}
