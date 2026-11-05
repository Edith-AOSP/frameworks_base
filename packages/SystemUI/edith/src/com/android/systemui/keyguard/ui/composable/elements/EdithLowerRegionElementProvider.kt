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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ElementContentScope
import com.android.compose.animation.scene.Key
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.keyguard.ui.viewmodel.EdithLowerRegionViewModel
import com.android.systemui.keyguard.ui.viewmodel.KeyguardQuickAffordanceViewModel
import com.android.systemui.keyguard.ui.viewmodel.KeyguardQuickAffordancesCombinedViewModel
import com.android.systemui.keyguard.ui.viewmodel.KeyguardQuickAffordancesCombinedViewModelModule.Companion.LOCKSCREEN_INSTANCE
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.media.remedia.ui.compose.Media
import com.android.systemui.plugins.keyguard.ui.composable.elements.BaseLockscreenElement.ElementSource
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElement
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys.IndicationArea
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementKeys.Shortcuts
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenElementProvider
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope
import com.android.systemui.plugins.keyguard.ui.composable.elements.LockscreenScope.Companion.LockscreenElement
import com.android.systemui.qs.edith.ChargingChipStatus
import com.android.systemui.qs.edith.EdithNotificationChipState
import com.android.systemui.res.R
import com.android.systemui.shade.ShadeDisplayAware
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
            // While the player is expanded the shortcuts sit on its dark surface: white glyphs on
            // translucent black, keeping the wallpaper visible through the circles.
            val expandedShortcutColors =
                if (viewModel.isMediaExpanded) {
                    KeyguardShortcutColors(
                        iconTint = Color.White,
                        background = Color.Black.copy(alpha = 0.54f),
                    )
                } else {
                    null
                }
            // One padded container for both the shortcut row and the pill, so they share the same
            // bottom edge. Giving the pill its own copy of the padding put it lower than the row.
            Box(Modifier.fillMaxWidth().padding()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    CompositionLocalProvider(
                        LocalKeyguardShortcutColors provides expandedShortcutColors
                    ) {
                        ShortcutElement(Shortcuts.Start, viewModel)
                    }

                    Box(
                        Modifier.weight(1f)
                            // Matches the stock lower region: the indication view is an AndroidView
                            // that fills its constraints, so without the unbounded wrap it takes
                            // the whole free height and its text ends up at the bottom of the
                            // screen, clipped by the navigation bar.
                            .wrapContentHeight(Alignment.CenterVertically, unbounded = true)
                    ) {
                        // The indication area carries the owner/charging text; it is hidden while
                        // the player is expanded, the shortcuts stay.
                        if (!viewModel.isMediaExpanded) {
                            LockscreenElement(IndicationArea)
                        }
                    }

                    CompositionLocalProvider(
                        LocalKeyguardShortcutColors provides expandedShortcutColors
                    ) {
                        ShortcutElement(Shortcuts.End, viewModel)
                    }
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
                val mediaExpanded = viewModel.isMediaExpanded
                val expanded = mediaExpanded && count > 0
                // The pill must stay composed while it drops, but it must not keep taking layout
                // space once it is gone: with no notifications the charging chip would otherwise
                // sit off-centre beside an invisible pill.
                var notificationComposed by remember { mutableStateOf(false) }
                LaunchedEffect(expanded) {
                    if (expanded) {
                        notificationComposed = true
                    } else {
                        kotlinx.coroutines.delay(CHIP_HIDE_MS)
                        notificationComposed = false
                    }
                }

                // The charging chip keeps its last state while it animates out, like the count
                // above: the battery update that unplugs the device must not blank the pill
                // halfway down.
                val chargingStatus = viewModel.chargingStatus
                var shownCharging by remember { mutableStateOf<ChargingChipStatus?>(null) }
                LaunchedEffect(chargingStatus) {
                    if (chargingStatus != null) {
                        shownCharging = chargingStatus
                    }
                }
                val chargingToShow = chargingStatus ?: shownCharging
                val chargingTarget = mediaExpanded && chargingStatus != null
                // While the player is collapsed, remember whether a charger is already connected.
                // On expansion that decides whether the charging chip joins the row or plays its
                // own entrance; once consumed, a charger plugged later gets the entrance.
                var chargingBeforeExpand by remember { mutableStateOf(false) }

                // A charger connected after the player opened gets the staged entrance: the
                // pill pops out from behind the notification chip, the notification pill
                // compacts, then the charging pill grows its text. If the charger was already
                // connected before the player opened, none of that runs: the pair starts joined
                // and slides in with the row like any other chip.
                var chargingStage by remember { mutableIntStateOf(ChargingChipHidden) }
                var chargingComposed by remember { mutableStateOf(false) }
                // Only the first part of the pop entrance needs a forced width: the pill has to
                // take room gradually rather than appearing at 48dp and shoving the notification
                // pill left. Once it has expanded, its own content size drives the width.
                var chargingEverExpanded by remember { mutableStateOf(false) }
                // Draw the chip with the row's slide (shared entrance/exit) instead of its pop.
                var chargingRowMotion by remember { mutableStateOf(false) }
                // Entered already joined: the corners and gap must not animate open.
                var chargingJoinedRow by remember { mutableStateOf(false) }
                // The charger went away while the player stayed up: collapse into the
                // notification pill instead of rewinding the entrance in stages.
                var chargingClosing by remember { mutableStateOf(false) }
                // The width the close starts from and the live width it animates down to, so
                // the pill shrinks from whatever it actually measured instead of snapping to
                // the compact width first. Freezing the origin keeps the progress well defined
                // while the layout narrows.
                val closeWidth = remember { Animatable(0.dp, Dp.VectorConverter) }
                var closeFromWidth by remember { mutableStateOf(0.dp) }
                var chargingMeasuredWidth by remember { mutableStateOf(0.dp) }
                val density = LocalDensity.current
                LaunchedEffect(chargingStage) {
                    if (chargingStage >= ChargingChipExpanding) chargingEverExpanded = true
                }
                LaunchedEffect(chargingTarget, mediaExpanded) {
                    if (chargingTarget) {
                        chargingClosing = false
                        if (!chargingComposed) {
                            chargingComposed = true
                            val alreadyPlugged = chargingBeforeExpand
                            chargingBeforeExpand = false
                            chargingRowMotion = alreadyPlugged
                            chargingJoinedRow = alreadyPlugged
                            if (alreadyPlugged) {
                                chargingEverExpanded = true
                                chargingStage = ChargingChipJoined
                            } else {
                                chargingEverExpanded = false
                                chargingStage = ChargingChipRising
                            }
                        }
                        while (chargingStage < ChargingChipJoined) {
                            kotlinx.coroutines.delay(enterStageDelay(chargingStage))
                            chargingStage++
                        }
                    } else if (chargingComposed) {
                        if (!mediaExpanded) {
                            // The player is closing: the pair slides down together, still joined
                            // and compact, on the same beat as the notification pill.
                            chargingClosing = false
                            chargingRowMotion = true
                            chargingJoinedRow = true
                            kotlinx.coroutines.delay(CHIP_HIDE_MS)
                            chargingComposed = false
                            chargingStage = ChargingChipHidden
                            chargingRowMotion = false
                            chargingJoinedRow = false
                        } else {
                            // Unplugged while the player is up: fold the pill back into the
                            // notification chip like a Dynamic Island closing its expanded
                            // state. The pill keeps its content and narrows to nothing while the
                            // notification grows over it, so the text is swallowed instead of
                            // snapping away, and the row does not jump when the chip is removed.
                            chargingRowMotion = false
                            chargingJoinedRow = false
                            closeFromWidth =
                                chargingMeasuredWidth.coerceAtLeast(ChargingChipCompactWidth)
                            closeWidth.snapTo(closeFromWidth)
                            chargingClosing = true
                            closeWidth.animateTo(0.dp, CloseWidthSpring)
                            chargingComposed = false
                            chargingClosing = false
                            chargingStage = ChargingChipHidden
                        }
                    }
                }

                // Runs after the entrance effect above, so it cannot clear the remembered value
                // before a join entrance has read it.
                LaunchedEffect(mediaExpanded, chargingStatus, chargingComposed) {
                    if (!mediaExpanded) {
                        chargingBeforeExpand = chargingStatus != null
                    } else if (chargingComposed) {
                        // The chip stayed composed across a quick collapse/re-expand, so the
                        // remembered value has already been applied.
                        chargingBeforeExpand = false
                    }
                }

                // The chip counts as present only while it is actually composed with a status:
                // a stale stage must never square off the notification pill on its own.
                val chargingPresent = chargingComposed && chargingToShow != null
                // 1 at the start of the close, 0 once the pill has collapsed into the notification.
                val closeProgress =
                    if (chargingClosing) {
                        if (closeFromWidth.value > 0f) {
                            (closeWidth.value.value / closeFromWidth.value).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                    } else {
                        0f
                    }

                val connectedAnimated by
                    animateFloatAsState(
                        targetValue =
                            if (
                                chargingPresent &&
                                    !chargingClosing &&
                                    chargingStage >= ChargingChipJoined
                            ) {
                                1f
                            } else {
                                0f
                            },
                        // During a close this rides the same curve as the width, so the switch
                        // back to this value when the chip leaves cannot replay the decouple.
                        animationSpec = if (chargingClosing) CloseSpring else ChipSpring,
                        label = "chipsConnected",
                    )
                val connected =
                    when {
                        !chargingPresent -> connectedAnimated
                        // The close detaches in step with the shrink, so the notification pill is
                        // fully round again by the time the charging chip is gone. Taking the
                        // smaller value also avoids snapping the corners joined when a charger is
                        // pulled before the entrance had finished joining them.
                        chargingClosing -> minOf(closeProgress, connectedAnimated)
                        chargingJoinedRow -> 1f
                        else -> connectedAnimated
                    }

                // The lock screen pills are opaque on purpose: no wallpaper or blur bleeding
                // through, even when lockscreen blur is enabled. In AOD that opaque surface is a
                // bright slab over black, so it and its glyphs are pulled toward black as doze
                // comes in instead of switching off abruptly.
                val dozeAmount = viewModel.dozeAmount.coerceIn(0f, 1f)
                val containerColor =
                    if (dozeAmount > 0f) {
                        lerpColor(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            Color.Black,
                            dozeAmount * AOD_SURFACE_DIM,
                        )
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                val contentColor =
                    lerpColor(
                        MaterialTheme.colorScheme.onSurface,
                        Color.Black,
                        dozeAmount * AOD_CONTENT_DIM,
                    )
                val accentDim = dozeAmount * AOD_ACCENT_DIM

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
                // The same slide for the whole row, including a charging chip that was already
                // connected before the player opened, so the pair rises and falls as one.
                val rowAlpha by
                    animateFloatAsState(
                        targetValue = if (mediaExpanded) 1f else 0f,
                        animationSpec = ChipSpring,
                        label = "rowAlpha",
                    )
                val rowScale by
                    animateFloatAsState(
                        targetValue = if (mediaExpanded) 1f else 0.55f,
                        animationSpec = ChipSpring,
                        label = "rowScale",
                    )
                val rowDrop by
                    animateFloatAsState(
                        targetValue = if (mediaExpanded) 0f else 1f,
                        animationSpec = ChipSpring,
                        label = "rowDrop",
                    )
                val chargingRise by
                    animateFloatAsState(
                        targetValue = if (chargingStage >= ChargingChipRising) 1f else 0f,
                        animationSpec = ChipSpring,
                        label = "chargingChipRise",
                    )
                // Before the first expansion the wrapper owns the pill's width so it can grow out
                // of nothing. Afterwards the pill's own content size drives the width, which keeps
                // the exit smooth: re-forcing 48dp would snap the text away.
                val chargingWidth by
                    animateDpAsState(
                        targetValue =
                            if (chargingStage >= ChargingChipRising) {
                                ChargingChipCompactWidth
                            } else {
                                0.dp
                            },
                        animationSpec = ChipWidthSpring,
                        label = "chargingChipWidth",
                    )
                // Each pill animates its own reported width (with `animateContentSize` inside its
                // background), so the row only ever arranges the widths the pills actually draw.
                // Animating the row's size as well made it center by the old width for a frame.
                val gapAmount =
                    when {
                        chargingClosing -> closeProgress
                        chargingJoinedRow -> 1f
                        else -> chargingRise.coerceIn(0f, 1f)
                    }
                val chipGap =
                    lerp(ChipGap, ChipGapJoined, connected.coerceIn(0f, 1f)) * gapAmount

                Row(
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            // AOD keeps these pills on screen, so move them with the rest of the
                            // dozing content instead of leaving them static to burn in.
                            .burnInAware(isClock = false)
                            .wrapContentHeight(Alignment.Bottom, unbounded = true),
                    horizontalArrangement = Arrangement.spacedBy(chipGap),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    if (chargingComposed && chargingToShow != null) {
                        val widthModifier =
                            when {
                                // The close owns the width: it animates from the measured pill
                                // down to zero, so the row narrows with it and nothing jumps when
                                // the chip leaves.
                                chargingClosing ->
                                    Modifier.width(closeWidth.value.coerceAtLeast(0.dp))
                                chargingEverExpanded ||
                                    chargingStage >= ChargingChipExpanding -> Modifier
                                else -> Modifier.width(chargingWidth)
                            }
                        LockscreenChargingChip(
                            status = chargingToShow,
                            containerColor = containerColor,
                            contentColor = contentColor,
                            dimAmount = accentDim,
                            rotationEnabled = dozeAmount < 0.5f,
                            expanded = chargingStage >= ChargingChipExpanding,
                            besideNotification = notificationComposed,
                            // Without a notification pill there is nothing to join: the charging
                            // chip must stay a full pill instead of squaring its end corner.
                            connected = if (notificationComposed) connected else 0f,
                            // The close fades the badge and text out quickly, leaving the
                            // background to fold away, the way the island drops its content
                            // before its outline finishes collapsing.
                            contentAlpha =
                                if (chargingClosing) {
                                    (closeProgress / 0.62f).coerceIn(0f, 1f)
                                } else {
                                    1f
                                },
                            modifier =
                                widthModifier
                                    .onSizeChanged {
                                        if (!chargingClosing) {
                                            chargingMeasuredWidth =
                                                with(density) { it.width.toDp() }
                                        }
                                    }
                                    .graphicsLayer {
                                        // A pre-plugged chip rides the row's slide; a chip that
                                        // arrived later pops out from under the notification pill.
                                        // The notification chip is drawn after this one, so the
                                        // overlap stays hidden while it slides into its slot.
                                        val rowMotion = chargingRowMotion
                                        val closing = chargingClosing
                                        if (closing) {
                                            // The width folds to zero; squash the height a
                                            // little as it goes, pivoting on the row's bottom
                                            // edge so the pill does not float up. Fade the last
                                            // sliver so it cannot linger as a strip after the
                                            // badge and text have gone.
                                            alpha = (closeProgress / 0.12f).coerceIn(0f, 1f)
                                            scaleX = 1f
                                            scaleY = 0.82f + 0.18f * closeProgress
                                            transformOrigin = TransformOrigin(0.5f, 1f)
                                        } else {
                                            alpha =
                                                (if (rowMotion) rowAlpha else chargingRise)
                                                    .coerceIn(0f, 1f)
                                            val scale =
                                                if (rowMotion) rowScale
                                                else 0.55f + 0.45f * chargingRise
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        translationY =
                                            if (rowMotion) rowDrop * size.height else 0f
                                        translationX =
                                            if (!rowMotion && !closing && notificationComposed) {
                                                (1f - chargingRise.coerceIn(0f, 1f)) *
                                                    ChargingChipCompactWidth.toPx()
                                            } else {
                                                0f
                                            }
                                    },
                        )
                    }

                    Box(
                        Modifier.graphicsLayer {
                            alpha = chipAlpha.coerceIn(0f, 1f)
                            scaleX = chipScale
                            scaleY = chipScale
                            translationY = chipDrop * size.height
                        }
                    ) {
                        if (notificationComposed) {
                            NotificationChip(
                                viewModel = viewModel,
                                count = shownCount,
                                icons = shownIcons,
                                containerColor = containerColor,
                                contentColor = contentColor,
                                interactive = expanded,
                                compact =
                                    chargingPresent &&
                                        !chargingClosing &&
                                        chargingStage >= ChargingChipCompacting,
                                growFast = chargingClosing,
                                dimAmount = accentDim,
                                connected = connected,
                            )
                        }
                    }
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
            containerColor: Color,
            contentColor: Color,
            interactive: Boolean,
            compact: Boolean,
            connected: Float,
            growFast: Boolean,
            dimAmount: Float,
            modifier: Modifier = Modifier,
        ) {
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
                contentColor = contentColor,
                interactive = interactive,
                compact = compact,
                connected = connected,
                growFast = growFast,
                dimAmount = dimAmount,
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

/** How long the notification pill takes to drop before it can leave the layout. */
private const val CHIP_HIDE_MS = 600L

/**
 * How far the pills move toward black at full AOD. Keep the pill readable as a shape on the black
 * doze screen: at 0.85 the surface read as gone entirely, while the undimmed surface was a lit
 * slab. The text and accent use smaller fractions so the state stays legible.
 */
private const val AOD_SURFACE_DIM = 0.5f
private const val AOD_CONTENT_DIM = 0.35f
private const val AOD_ACCENT_DIM = 0.45f

/** The charging chip's entrance/exit stages, in the order the two pills settle. */
private const val ChargingChipHidden = 0
private const val ChargingChipRising = 1
private const val ChargingChipCompacting = 2
private const val ChargingChipExpanding = 3
private const val ChargingChipJoined = 4

/**
 * How long to hold each stage before moving on. The chip's spring settles in roughly 500 ms, so the
 * beats are spaced closely enough that the sequence reads as one gesture rather than three.
 */
private fun enterStageDelay(stage: Int): Long =
    when (stage) {
        ChargingChipRising -> 140L
        ChargingChipCompacting -> 180L
        else -> 200L
    }

/** Gap between the two pills; there is room for the pair to breathe on a phone. */
internal val ChipGap = 12.dp
private val ChipGapJoined = 6.dp

/** Same spring as the expanded media player's morph (bounce 0.4, 500 ms). */
private val ChipSpring =
    spring<Float>(
        dampingRatio = Media.MORPH_SPRING_DAMPING,
        stiffness = Media.MORPH_SPRING_STIFFNESS,
    )

/** The charge pill's width uses the same spring, so it grows and folds away with its fade. */
private val ChipWidthSpring =
    spring<Dp>(
        dampingRatio = Media.MORPH_SPRING_DAMPING,
        stiffness = Media.MORPH_SPRING_STIFFNESS,
    )

/**
 * The close is a single bouncy fold like the Dynamic Island reference, but softer than that
 * reference's own spring: the pill is wide, and the faster curve made the collapse feel like a cut.
 */
private val CloseWidthSpring = spring<Dp>(dampingRatio = 0.65f, stiffness = 170f)

/** The same close curve for the connected float, so the corner does not trail the fold. */
private val CloseSpring = spring<Float>(dampingRatio = 0.65f, stiffness = 170f)
