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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.android.systemui.dagger.SysUISingleton
import javax.inject.Inject

/**
 * Where the notification chip of the expanded lock screen media player currently sits, in root
 * coordinates.
 *
 * The chip publishes its centre while it is laid out; the lockscreen → shade transition reads it to
 * grow the shade out of the chip instead of revealing it from the shelf's edge. A stale value is
 * harmless: the chip's spot at the bottom centre is also where the notification shelf lives.
 */
@SysUISingleton
class EdithNotificationChipState @Inject constructor() {
    var centre: Offset? by mutableStateOf(null)
}
