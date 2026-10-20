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

package com.android.systemui.scene.ui.composable.transitions

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.util.lerp
import com.android.compose.animation.scene.ContentKey
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.Scale
import com.android.compose.animation.scene.content.state.TransitionState
import com.android.compose.animation.scene.transformation.CustomPropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformation
import com.android.compose.animation.scene.transformation.PropertyTransformationScope
import com.android.compose.animation.scene.transformation.Transformation
import kotlinx.coroutines.CoroutineScope

/**
 * Grows the incoming element(s) out of [point] (in root coordinates) instead of its own centre.
 *
 * Used for the lockscreen → notification shade transition: the shade grows out of the notification
 * chip at the bottom of the expanded media player, which is centred, rather than out of the
 * (left-aligned) notification shelf's bounds. Without a [point] — the chip was never laid out — the
 * transformation is a no-op and the transition keeps its stock origin.
 */
class ScaleFromPointUntilRelease(
    private val point: () -> Offset?,
    private val startScale: Float = 0.92f,
) : Transformation.Factory {
    override fun create(): Transformation = ScaleFromPoint(point, startScale)
}

private class ScaleFromPoint(
    private val point: () -> Offset?,
    private val startScale: Float,
) : CustomPropertyTransformation<Scale> {
    override val property = PropertyTransformation.Property.Scale

    override fun PropertyTransformationScope.transform(
        content: ContentKey,
        element: ElementKey,
        transition: TransitionState.Transition,
        transitionScope: CoroutineScope,
    ): Scale {
        // Only the content we are transitioning *to* grows in; the outgoing one keeps its own
        // scale, or it would shrink towards the same pivot while leaving.
        if (content != transition.toContent) return Scale.Default
        val origin = point() ?: return Scale.Default
        val offset = element.targetOffset(content) ?: return Scale.Default
        val size = element.targetSize(content) ?: return Scale.Default
        if (size.width == 0 || size.height == 0) return Scale.Default
        val scale = lerp(startScale, 1f, transition.progress.coerceIn(0f, 1f))
        return Scale(
            scale,
            scale,
            Offset(
                (origin.x - offset.x) / size.width,
                (origin.y - offset.y) / size.height,
            ),
        )
    }
}
