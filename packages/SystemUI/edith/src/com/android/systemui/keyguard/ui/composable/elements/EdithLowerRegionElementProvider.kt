/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.dimensionResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ElementContentScope
import com.android.compose.animation.scene.Key
import com.android.compose.theme.LocalAndroidColorScheme
import com.android.systemui.Flags.enableLockscreenBlur
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.keyguard.ui.viewmodel.KeyguardQuickAffordanceViewModel
import com.android.systemui.keyguard.ui.viewmodel.KeyguardQuickAffordancesCombinedViewModel
import com.android.systemui.keyguard.ui.viewmodel.KeyguardQuickAffordancesCombinedViewModelModule.Companion.LOCKSCREEN_INSTANCE
import com.android.systemui.keyguard.ui.viewmodel.EdithLowerRegionViewModel
import com.android.systemui.media.remedia.ui.compose.Media
import com.android.systemui.qs.edith.EdithNotificationChipState
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.plugins.keyguard.ui.composable.elements.BaseLockscreenElement.ElementSource
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElement
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys.IndicationArea
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys.Shortcuts
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementProvider
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope.Companion.LockscreenElement
import com.android.systemui.res.R
import com.android.systemui.shade.ShadeDisplayAware
import com.android.systemui.window.domain.interactor.WindowRootViewBlurInteractor
import javax.inject.Inject
import javax.inject.Named

@SysUISingleton
/** The lock screen lower region: shortcuts, indication area and notification chip. */
class EdithLowerRegionElementProvider
@Inject
constructor(
    @ShadeDisplayAware private val context: Context,
    private val viewModelFactory: EdithLowerRegionViewModel.Factory,
    @Named(LOCKSCREEN_INSTANCE)
    private val quickAffordancesCombinedViewModel: KeyguardQuickAffordancesCombinedViewModel,
    private val windowRootViewBlurInteractor: WindowRootViewBlurInteractor,
    private val notificationChipState: EdithNotificationChipState,
) : LockscreenElementProvider {
    override val elements: List<LockscreenElement> by lazy { listOf(LowerRegionElement()) }

    private inner class LowerRegionElement : LockscreenElement {
        override val key = LockscreenElementKeys.Region.Lower
        override val context = this@EdithLowerRegionElementProvider.context
        override val source = ElementSource.VENDOR

        @Composable
        override fun LockscreenScope<ElementContentScope>.LockscreenElement() {
            val viewModel = rememberViewModel("LockscreenLowerRegion") { viewModelFactory.create() }
            // One padded container for both the shortcut row and the pill, so they share the same
            // bottom edge. Giving the pill its own copy of the padding put it lower than the row.
            Box(Modifier.fillMaxWidth().padding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShortcutElement(Shortcuts.Start, viewModel)

                    Box(
                        Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        // The indication area carries the owner/charging text; it is hidden while
                        // the player is expanded, the shortcuts stay.
                        if (!viewModel.isMediaExpanded) {
                            LockscreenElement(IndicationArea)
                        }
                    }

                    ShortcutElement(Shortcuts.End, viewModel)
                }

                // Same treatment as the shortcut slots: the lower region is only as tall as the
                // space below the lock icon, so measure the pill unbounded and bottom-align it.
                // The entrance/exit is a draw-layer animation driven directly by the expansion
                // state, so it runs in both directions and cannot be clipped by a layout pass.
                val count = viewModel.notificationCount
                val icons = viewModel.notificationIcons
                // Keep the last non-empty content while the chip animates out, so it does not turn
                // into "0 Notifications" on the way down.
                var shownCount by remember { mutableIntStateOf(0) }
                var shownIcons by remember { mutableStateOf<List<Drawable>>(emptyList()) }
                LaunchedEffect(count, icons) {
                    if (count > 0) {
                        shownCount = count
                        shownIcons = icons
                    }
                }
                val expanded = viewModel.isMediaExpanded && count > 0
                val chipAlpha by
                    animateFloatAsState(
                        targetValue = if (expanded) 1f else 0f,
                        animationSpec = ChipSpring,
                        label = "chipAlpha",
                    )
                val chipScale by
                    animateFloatAsState(
                        targetValue = if (expanded) 1f else 0.55f,
                        animationSpec = ChipSpring,
                        label = "chipScale",
                    )
                val chipDrop by
                    animateFloatAsState(
                        targetValue = if (expanded) 0f else 1f,
                        animationSpec = ChipSpring,
                        label = "chipDrop",
                    )
                Box(
                    Modifier.align(Alignment.BottomCenter)
                        .wrapContentHeight(Alignment.Bottom, unbounded = true)
                        .graphicsLayer {
                            alpha = chipAlpha.coerceIn(0f, 1f)
                            scaleX = chipScale
                            scaleY = chipScale
                            translationY = chipDrop * size.height
                        }
                ) {
                    NotificationChip(
                        viewModel = viewModel,
                        count = shownCount,
                        icons = shownIcons,
                        interactive = expanded,
                    )
                }
            }
        }

        @Composable
        private fun Modifier.padding(): Modifier {
            val horizontalPadding = dimensionResource(R.dimen.keyguard_affordance_horizontal_offset)
            return padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    bottom = dimensionResource(R.dimen.keyguard_affordance_vertical_offset),
                )
                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Bottom))
        }

        /**
         * The notification summary that replaces the hidden shelf while the expanded media player
         * is up. Tapping it fades it out and opens the locked shade.
         */
        @Composable
        private fun LockscreenScope<ElementContentScope>.NotificationChip(
            viewModel: EdithLowerRegionViewModel,
            count: Int,
            icons: List<Drawable>,
            interactive: Boolean,
            modifier: Modifier = Modifier,
        ) {
            val isBlurSupported by
                windowRootViewBlurInteractor.isBlurCurrentlySupported.collectAsStateWithLifecycle()
            val containerColor =
                if (enableLockscreenBlur() && isBlurSupported) {
                    LocalAndroidColorScheme.current.surfaceEffect1
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            var tapped by remember { mutableStateOf(false) }
            LaunchedEffect(tapped) {
                if (tapped) {
                    kotlinx.coroutines.delay(CHIP_TAP_HIDE_MS)
                    tapped = false
                }
            }
            val alpha by
                animateFloatAsState(
                    targetValue = if (tapped) 0f else 1f,
                    label = "notificationChipTap",
                )
            LockscreenNotificationChip(
                count = count,
                icons = icons,
                containerColor = containerColor,
                contentColor = MaterialTheme.colorScheme.onSurface,
                circleColor = viewModel.firstNotificationColor?.let { Color(it) },
                interactive = interactive,
                chipState = notificationChipState,
                onClick = {
                    tapped = true
                    viewModel.openNotifications()
                },
                modifier = modifier.graphicsLayer { this.alpha = alpha },
            )
        }

        @Composable
        private fun LockscreenScope<ElementContentScope>.ShortcutElement(
            key: Key,
            viewModel: EdithLowerRegionViewModel,
        ) {

            val endButton by
                quickAffordancesCombinedViewModel.endButton.collectAsStateWithLifecycle(
                    initialValue = KeyguardQuickAffordanceViewModel(slotId = "")
                )

            val startButton by
                quickAffordancesCombinedViewModel.startButton.collectAsStateWithLifecycle(
                    initialValue = KeyguardQuickAffordanceViewModel(slotId = "")
                )

            // If neither shortcut is visible, do not display anything to allow indication area and
            // other features to take the full width of the device.
            if (!startButton.isVisible && !endButton.isVisible) {
                return
            }

            LockscreenElement(
                key,
                Modifier.graphicsLayer {
                        translationX =
                            // unfoldTranslations may be updated every frame, so only read value in
                            // the draw phase.
                            when (key) {
                                Shortcuts.Start -> {
                                    viewModel.unfoldTranslations.start
                                }

                                Shortcuts.End -> {
                                    viewModel.unfoldTranslations.end
                                }
                                else -> {
                                    throw IllegalArgumentException(
                                        "Invalid keyguard shortcut key: $key"
                                    )
                                }
                            }
                    }
                    .wrapContentHeight(Alignment.Bottom, unbounded = true)
                    .size(
                        height = dimensionResource(R.dimen.keyguard_affordance_fixed_height),
                        width = dimensionResource(R.dimen.keyguard_affordance_fixed_width),
                    ),
            )
        }
    }
}

/** How long the chip stays hidden after a tap, so it is back when the shade is dismissed. */
private const val CHIP_TAP_HIDE_MS = 400L

/** Same spring as the expanded media player's morph (bounce 0.4, 500 ms). */
private val ChipSpring =
    spring<Float>(
        dampingRatio = Media.MORPH_SPRING_DAMPING,
        stiffness = Media.MORPH_SPRING_STIFFNESS,
    )
