/*
 * Copyright (C) 2024 The Android Open Source Project
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

@file:OptIn(ExperimentalFoundationApi::class)

package com.android.systemui.qs.panels.ui.compose.infinitegrid

import android.content.Context
import android.content.res.Resources
import android.os.Trace
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.util.trace
import com.android.app.tracing.coroutines.launchTraced as launch
import com.android.compose.animation.Expandable
import com.android.compose.animation.bounceable
import com.android.compose.animation.rememberExpandableController
import com.android.compose.animation.scene.ContentScope
import com.android.compose.modifiers.thenIf
import com.android.compose.theme.LocalAndroidColorScheme
import com.android.mechanics.compose.modifier.verticalFadeContentReveal
import com.android.mechanics.compose.modifier.verticalTactileSurfaceReveal
import com.android.mechanics.effects.VerticalTactileSurfaceRevealEffect
import com.android.systemui.Flags
import com.android.systemui.animation.Expandable
import com.android.systemui.animation.TransitionAnimator.Companion.dynamicTargetResolutionEnabled
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.haptics.msdl.qs.TileHapticsViewModel
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.qs.flags.QsDetailedView
import com.android.systemui.qs.edith.EdithTileColorOverride
import com.android.systemui.qs.edith.EdithTileSwatches
import com.android.systemui.qs.edith.QuickActionsTileOverride
import com.android.systemui.qs.edith.QuickActionsTileSwatches
import com.android.systemui.qs.panels.ui.compose.BounceableInfo
import com.android.systemui.qs.panels.ui.compose.Tooltip
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.ActiveIconCornerRadius
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.ActiveTileCornerRadius
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.EdithInactiveTileAlpha
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.InactiveIconCornerRadius
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.InactiveTileCornerRadius
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.TileHeight
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.longPressLabelMoreDetails
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults.longPressLabelSettings
import com.android.systemui.qs.panels.ui.viewmodel.AccessibilityUiState
import com.android.systemui.qs.panels.ui.viewmodel.DetailsViewModel
import com.android.systemui.qs.panels.ui.viewmodel.IconProvider
import com.android.systemui.qs.panels.ui.viewmodel.TileUiState
import com.android.systemui.qs.panels.ui.viewmodel.TileViewModel
import com.android.systemui.qs.panels.ui.viewmodel.toIconProvider
import com.android.systemui.qs.panels.ui.viewmodel.toUiState
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.qs.ui.composable.QuickSettingsShade
import com.android.systemui.qs.ui.compose.borderOnFocus
import com.android.systemui.res.R
import kotlinx.coroutines.CoroutineScope
import platform.test.motion.compose.values.MotionTestValueKey
import platform.test.motion.compose.values.motionTestValues

private val TileViewModel.traceName
    get() = spec.toString().takeLast(Trace.MAX_SECTION_NAME_LEN)

/**
 * This composable function is responsible for rendering a tile based on the provided
 * [TileViewModel]. It handles different states of the tile (e.g., available, unavailable),
 * interactions (click, long click), and visual styles (icon only or large tile).
 *
 * @param tile The [TileViewModel] containing the data and logic for the tile.
 * @param iconOnly A boolean indicating whether to display only the icon of the tile or the full
 *   tile content (false for large tiles).
 * @param squishiness The float value representing the current squishiness factor of the tile, used
 *   for animations.
 * @param coroutineScope The [CoroutineScope] to launch coroutines for animations.
 * @param tileHapticsViewModelFactory A factory for creating a [TileHapticsViewModel] instance, used
 *   for haptic feedback.
 * @param modifier An optional [Modifier] to be applied to the root composable of the tile.
 * @param isVisible Whether the tile is currently visible. Defaults to true.
 * @param requestToggleTextFeedback A lambda function that is invoked when a toggleable icon only
 *   tile is clicked, used to request the feedback text.
 * @param detailsViewModel An optional [DetailsViewModel] used to handle navigation to a detailed
 *   view when a tile is clicked, if applicable.
 * @param enableRevealEffect If `true`, the tiles will animate using the reveal animation.
 */
@Composable
fun ContentScope.Tile(
    tile: TileViewModel,
    iconOnly: Boolean,
    squishiness: () -> Float,
    coroutineScope: CoroutineScope,
    bounceableInfo: BounceableInfo,
    tileHapticsViewModelFactory: TileHapticsViewModel.Factory,
    modifier: Modifier = Modifier,
    isVisible: () -> Boolean = { true },
    requestToggleTextFeedback: (TileSpec) -> Unit = {},
    detailsViewModel: DetailsViewModel?,
    enableRevealEffect: Boolean = false,
    edithTileStyle: Boolean = false,
    edithColorEnabled: Boolean = false,
    edithSquareSize: Dp? = null,
    edithTileColorOverride: EdithTileColorOverride? = null,
    edithQuickActionsTile: Boolean = false,
    edithQuickActionsOverride: QuickActionsTileOverride? = null,
) {
    trace(tile.traceName) {
        val currentBounceableInfo by rememberUpdatedState(bounceableInfo)
        val resources = resources()

        /*
         * Use produce state because [QSTile.State] doesn't have well defined equals (due to
         * inheritance). This way, even if tile.state changes, uiState may not change and lead to
         * recomposition.
         */
        val uiState by
            produceState(tile.currentState.toUiState(resources), tile, resources) {
                tile.state.collect { value = it.toUiState(resources) }
            }
        val isClickable = uiState.handlesMainClick

        val icon by
            produceState(tile.currentState.toIconProvider(), tile) {
                tile.state.collect { value = it.toIconProvider() }
            }

        val colors =
            when {
                // Quick Actions tiles keep stock colors unless the tuner overrides them; they're
                // not affected by the QS color scheme toggle.
                edithTileStyle && edithQuickActionsTile ->
                    TileDefaults.edithQuickActionsTileColors(
                        uiState = uiState,
                        iconOnly = iconOnly,
                        override = edithQuickActionsOverride,
                    )
                // The main grid only goes tertiary when the color scheme toggle is on.
                edithTileStyle && edithColorEnabled ->
                    TileDefaults.edithTertiaryTileColors(uiState.visualState, edithTileColorOverride)
                else -> TileDefaults.getColorForState(uiState, iconOnly)
            }
        val hapticsViewModel: TileHapticsViewModel =
            rememberViewModel(traceName = "TileHapticsViewModel") {
                tileHapticsViewModelFactory.create(tile)
            }

        val isDualTarget = uiState.handlesToggleClick

        // TODO(b/361789146): Draw the shapes instead of clipping
        // Edith QS style: square tiles, a perfect circle when inactive and a squircle when active.
        // The dual-target (dual-state) tile has its own outer radius (16dp), overridable from the
        // Quick Actions shape tuner.
        val tileShape by
            if (edithTileStyle) {
                val dualTargetOuterRadius =
                    edithQuickActionsOverride?.outerCornerRadiusDp.takeIf { isDualTarget }
                remember(uiState.visualState, isDualTarget, dualTargetOuterRadius) {
                    mutableStateOf(
                        when {
                            dualTargetOuterRadius != null ->
                                RoundedCornerShape(dualTargetOuterRadius.dp)
                            isDualTarget -> RoundedCornerShape(EdithDualTargetOuterCornerRadius)
                            uiState.visualState == STATE_ACTIVE ->
                                RoundedCornerShape(EdithActiveCornerRadius)
                            else -> RoundedCornerShape(percent = 50)
                        }
                    )
                }
            } else {
                TileDefaults.animateTileShapeAsState(uiState)
            }
        val animatedColor by animateColorAsState(colors.background, label = "QSTileBackgroundColor")
        val hasLongClickEffect = uiState.hasLongClickEffect
        val interactionSource = remember { MutableInteractionSource() }

        val surfaceRevealModifier: Modifier
        val contentRevealModifier: Modifier
        if (enableRevealEffect) {
            val marginBottom =
                with(LocalDensity.current) { QuickSettingsShade.Dimensions.VerticalPadding.toPx() }

            val animatedCornerRadius by animateDpAsState(TileDefaults.tileRadius(uiState))

            val inactiveCornerRadius = InactiveIconCornerRadius
            surfaceRevealModifier =
                Modifier.verticalTactileSurfaceReveal(
                    deltaY = marginBottom,
                    effectSpec =
                        remember(inactiveCornerRadius) {
                            VerticalTactileSurfaceRevealEffect(
                                maxCornerSize = { animatedCornerRadius },
                                phase1MarginX = inactiveCornerRadius,
                            )
                        },
                    label = tile.traceName,
                )

            contentRevealModifier =
                Modifier.verticalFadeContentReveal(deltaY = marginBottom, label = tile.traceName)
        } else {
            surfaceRevealModifier = Modifier
            contentRevealModifier = Modifier
        }

        val expandable =
            if (dynamicTargetResolutionEnabled()) tile.expandable
            else remember { Expandable(mutableSetOf()) }
        Tooltip(
            text = uiState.label,
            modifier = modifier,
            enabled = Flags.enableQsTileTooltips(),
        ) { modifier ->
            TileExpandable(
                expandable = expandable,
                color = { animatedColor },
                shape = tileShape,
                squishiness = squishiness,
                hapticsViewModel = hapticsViewModel.takeIf { hasLongClickEffect },
                modifier =
                    modifier
                        .then(
                            // Fixed square height (Edith) applied outermost so the tap
                            // squish/bounce animation cannot change the grid height (which moves
                            // the PagerDots).
                            if (edithSquareSize != null) {
                                Modifier.height(edithSquareSize)
                            } else {
                                Modifier
                            }
                        )
                        .then(surfaceRevealModifier)
                        .borderOnFocus(
                            color = MaterialTheme.colorScheme.secondary,
                            tileShape.topEnd,
                        )
                        .sysuiResTag("tile_expandable")
                        .fillMaxWidth()
                        .bounceable(
                            currentBounceableInfo.bounceable,
                            currentBounceableInfo.previousTile,
                            currentBounceableInfo.nextTile,
                            orientation = Orientation.Horizontal,
                            bounceEnd = currentBounceableInfo.bounceEnd,
                            interactionSource = interactionSource,
                        ),
            ) { expandable ->
                // Use main click on long press for small, available dual target tiles.
                // Open settings otherwise.
                val useLongClickToSettings = !(iconOnly && isDualTarget && isClickable)
                val longClick: (() -> Unit)? =
                    {
                            if (hasLongClickEffect) {
                                hapticsViewModel.setTileInteractionState(
                                    TileHapticsViewModel.TileInteractionState.LONG_CLICKED
                                )
                            }

                            if (useLongClickToSettings) {
                                tile.settingsClick(expandable)
                            } else {
                                val hasDetails =
                                    QsDetailedView.isEnabled &&
                                        detailsViewModel?.onTileClicked(tile.spec) == true
                                if (!hasDetails) {
                                    tile.mainClick(expandable)
                                }
                            }
                        }
                        .takeIf { !useLongClickToSettings || uiState.handlesSettingsClick }

                // Bounce the tile's container if it is toggleable and is not a large
                // dual target tile. These don't toggle on main click.
                val bounceContainer = uiState.isToggleable && (iconOnly || !isDualTarget)
                TileContainer(
                    interactionSource = interactionSource.takeIf { bounceContainer },
                    onClick = onClick@{
                            if (!isClickable) return@onClick

                            if (iconOnly && isDualTarget) {
                                tile.toggleClick()
                            } else {
                                val hasDetails =
                                    QsDetailedView.isEnabled &&
                                        detailsViewModel?.onTileClicked(tile.spec) == true
                                if (hasDetails) return@onClick

                                // For those tile's who doesn't have a detailed view, process with
                                // their `onClick` behavior.
                                tile.mainClick(expandable)
                            }

                            // Side effects of the click
                            hapticsViewModel.setTileInteractionState(
                                TileHapticsViewModel.TileInteractionState.CLICKED
                            )

                            coroutineScope.launch {
                                // Bounce the content of the tile if we're not animating the
                                // container.
                                if (!bounceContainer) {
                                    currentBounceableInfo.bounceable.animateContentBounce(iconOnly)
                                }
                            }
                            if (uiState.isToggleable && iconOnly) {
                                // And show footer text feedback for icons
                                requestToggleTextFeedback(tile.spec)
                            }
                        },
                    onLongClick = longClick,
                    accessibilityUiState = uiState.accessibilityUiState,
                    iconOnly = iconOnly,
                    isDualTarget = isDualTarget,
                    modifier = contentRevealModifier,
                    // The Edith square treatment only applies to the icon-only tiles of the main
                    // QS grid. Large (icon + label) tiles — including the Quick Actions tiles —
                    // keep the standard (short, wide) tile height instead of being forced square.
                    square = edithTileStyle && iconOnly,
                    fixedSquareSize = edithSquareSize,
                ) {
                    val iconProvider: Context.() -> Icon = { getTileIcon(icon = icon) }
                    if (iconOnly) {
                        // Edith square tiles are smaller than the standard tile, so scale the icon
                        // to the tile's actual size.
                        if (edithTileStyle) {
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                val iconSize = min(maxWidth, maxHeight) * EdithIconRatio
                                SmallTileContent(
                                    iconProvider = iconProvider,
                                    color = colors.icon,
                                    size = { iconSize },
                                    modifier =
                                        Modifier.bounceScale {
                                            currentBounceableInfo.bounceable.iconBounceScale
                                        },
                                )
                            }
                        } else {
                            SmallTileContent(
                                iconProvider = iconProvider,
                                color = colors.icon,
                                modifier =
                                    Modifier.align(Alignment.Center).bounceScale {
                                        currentBounceableInfo.bounceable.iconBounceScale
                                    },
                            )
                        }
                    } else {
                        // Dual-target inner box shape. In the Edith style it defaults to 10dp and
                        // can be overridden from the Quick Actions shape tuner; otherwise the stock
                        // active/inactive shape is used. All are computed unconditionally (the
                        // override can toggle live) and one is then selected.
                        val stockIconShape = TileDefaults.animateIconShapeAsState(uiState)
                        val innerRadiusOverride =
                            edithQuickActionsOverride?.innerCornerRadiusDp.takeIf {
                                edithTileStyle && isDualTarget
                            }
                        val overrideIconShape =
                            remember(innerRadiusOverride) {
                                mutableStateOf(
                                    innerRadiusOverride?.let { RoundedCornerShape(it.dp) }
                                )
                            }
                        val iconShape =
                            overrideIconShape.value
                                ?: if (edithTileStyle && isDualTarget) {
                                    RoundedCornerShape(EdithDualTargetInnerCornerRadius)
                                } else {
                                    stockIconShape.value
                                }
                        val secondaryClick: (() -> Unit)? =
                            {
                                    hapticsViewModel.setTileInteractionState(
                                        TileHapticsViewModel.TileInteractionState.CLICKED
                                    )
                                    tile.toggleClick()
                                }
                                .takeIf { isDualTarget }
                        LargeTileContent(
                            label = uiState.label,
                            secondaryLabel = uiState.secondaryLabel,
                            iconProvider = iconProvider,
                            sideDrawable = uiState.sideDrawable,
                            colors = colors,
                            iconShape = iconShape,
                            toggleClick = secondaryClick,
                            onLongClick = longClick,
                            accessibilityUiState = uiState.accessibilityUiState,
                            squishiness = squishiness,
                            isVisible = isVisible,
                            textScale = { currentBounceableInfo.bounceable.textBounceScale },
                            // Only when the QA colors are overridden do the tile and inner box share
                            // the same color; paint the box with the glyph color so it stays visible.
                            innerBoxColor =
                                if (
                                    edithTileStyle &&
                                        isDualTarget &&
                                        edithQuickActionsOverride?.hasAnyColor == true
                                ) {
                                    colors.icon.copy(alpha = EdithInnerBoxAlpha)
                                } else {
                                    null
                                },
                            modifier =
                                Modifier.largeTilePadding(
                                    isDualTarget = uiState.handlesSettingsClick
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileExpandable(
    expandable: Expandable,
    color: () -> Color,
    shape: Shape,
    squishiness: () -> Float,
    hapticsViewModel: TileHapticsViewModel?,
    modifier: Modifier = Modifier,
    content: @Composable (Expandable) -> Unit,
) {
    Expandable(
        expandable = expandable,
        controller = rememberExpandableController(color = color, shape = shape),
        modifier =
            modifier
                .clip(shape)
                .motionTestValues { squishiness() exportAs TileMotionTestKeys.Squishness }
                .verticalSquish(squishiness),
        useModifierBasedImplementation = true,
    ) {
        content(hapticsViewModel?.createStateAwareExpandable(it) ?: it)
    }
}

@Composable
fun TileContainer(
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    accessibilityUiState: AccessibilityUiState,
    iconOnly: Boolean,
    isDualTarget: Boolean,
    interactionSource: MutableInteractionSource?,
    modifier: Modifier = Modifier,
    square: Boolean = false,
    fixedSquareSize: Dp? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier
                .then(
                    when {
                        // Fixed square size (Edith): a fixed height unaffected by the bounce
                        // animation that changes the tile's width on tap.
                        fixedSquareSize != null ->
                            Modifier.height(fixedSquareSize).fillMaxWidth()
                        square -> Modifier.fillMaxWidth().aspectRatio(1f)
                        else -> Modifier.height(TileHeight).fillMaxWidth()
                    }
                )
                .tileCombinedClickable(
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                    accessibilityUiState = accessibilityUiState,
                    iconOnly = iconOnly,
                    isDualTarget = isDualTarget,
                    interactionSource = interactionSource,
                )
                .tileTestTag(iconOnly),
        content = content,
    )
}

@Composable
fun SmallStaticTile(
    uiState: TileUiState,
    iconProvider: IconProvider,
    modifier: Modifier = Modifier,
    edithTileStyle: Boolean = false,
    edithColorEnabled: Boolean = false,
    edithTileColorOverride: EdithTileColorOverride? = null,
    onClick: () -> Unit = {},
) {
    val colors =
        if (edithTileStyle && edithColorEnabled) {
            TileDefaults.edithTertiaryTileColors(uiState.visualState, edithTileColorOverride)
        } else {
            TileDefaults.getColorForState(uiState = uiState, iconOnly = true)
        }
    val shape =
        if (edithTileStyle) {
            RoundedCornerShape(percent = 50)
        } else {
            TileDefaults.animateTileShapeAsState(uiState).value
        }

    Box(
        modifier
            .clip(shape)
            .background(colors.background)
            .then(
                if (edithTileStyle) {
                    Modifier.fillMaxWidth().aspectRatio(1f)
                } else {
                    Modifier.size(TileHeight)
                }
            )
            .clickable(onClick = onClick)
    ) {
        if (edithTileStyle) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                val iconSize = min(maxWidth, maxHeight) * EdithIconRatio
                SmallTileContent(
                    iconProvider = { getTileIcon(icon = iconProvider) },
                    color = colors.icon,
                    size = { iconSize },
                )
            }
        } else {
            SmallTileContent(
                iconProvider = { getTileIcon(icon = iconProvider) },
                color = colors.icon,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
fun LargeStaticTile(
    uiState: TileUiState,
    iconProvider: IconProvider,
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    dualTarget: Boolean = false,
    innerShape: RoundedCornerShape = RoundedCornerShape(percent = 50),
    onClick: () -> Unit = {},
) {
    val colors = TileDefaults.getColorForState(uiState = uiState, iconOnly = false)

    Box(
        modifier
            .clip(shape ?: TileDefaults.animateTileShapeAsState(uiState).value)
            .background(colors.background)
            .height(TileHeight)
            .clickable(onClick = onClick)
            .largeTilePadding()
    ) {
        LargeTileContent(
            label = uiState.label,
            secondaryLabel = "",
            iconProvider = { getTileIcon(icon = iconProvider) },
            sideDrawable = null,
            colors = colors,
            squishiness = { 1f },
            // Preview of a dual-target tile: show the inner box without any interaction.
            showDualTargetBox = dualTarget,
            iconShape = if (dualTarget) innerShape else RoundedCornerShape(percent = 50),
        )
    }
}

private fun Context.getTileIcon(icon: IconProvider): Icon {
    return icon.icon?.let {
        if (it is QSTileImpl.ResourceIcon) {
            Icon.Resource(it.resId, null)
        } else {
            Icon.Loaded(it.getDrawable(this), null)
        }
    } ?: Icon.Resource(R.drawable.ic_error_outline, null)
}

fun tileHorizontalArrangement(): Arrangement.Horizontal {
    return spacedBy(space = CommonTileDefaults.TileArrangementPadding, alignment = Alignment.Start)
}

@Composable
fun Modifier.tileCombinedClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    accessibilityUiState: AccessibilityUiState,
    interactionSource: MutableInteractionSource?,
    iconOnly: Boolean,
    isDualTarget: Boolean,
): Modifier {
    val longPressLabel =
        if (iconOnly && isDualTarget) longPressLabelMoreDetails() else longPressLabelSettings()
    return combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            onClickLabel = accessibilityUiState.clickLabel,
            onLongClickLabel = longPressLabel,
            hapticFeedbackEnabled = false, // Haptics handled separately
            interactionSource = interactionSource,
        )
        .semantics {
            val accessibilityRole =
                if (iconOnly && isDualTarget) {
                    Role.Switch
                } else {
                    accessibilityUiState.accessibilityRole
                }
            if (accessibilityRole == Role.Switch) {
                accessibilityUiState.toggleableState?.let { toggleableState = it }
            }
            role = accessibilityRole
            stateDescription = accessibilityUiState.stateDescription
        }
        .thenIf(iconOnly) {
            Modifier.semantics { contentDescription = accessibilityUiState.contentDescription }
        }
}

data class TileColors(
    val background: Color,
    val iconBackground: Color,
    val label: Color,
    val secondaryLabel: Color,
    val icon: Color,
)

@VisibleForTesting
object TileMotionTestKeys {
    val Squishness = MotionTestValueKey<Float>("tile_squishiness")
}

/**
 * Fraction of the (square) Edith tile occupied by the icon. The Edith tiles are smaller than the
 * standard tile, so the icon scales with the tile size instead of using a fixed dimension.
 */
private const val EdithIconRatio = 0.38f

/** Corner radius of the active (squircle) Edith tile. */
private val EdithActiveCornerRadius = 16.dp

/** Default corner radius of the Quick Actions dual-state (dual-target) outer box. */
private val EdithDualTargetOuterCornerRadius = 16.dp

/** Default corner radius of the Quick Actions dual-state (dual-target) inner toggle-target box. */
private val EdithDualTargetInnerCornerRadius = 10.dp

/** Tint alpha for the Quick Actions dual-target inner box when the tile colors are overridden. */
private const val EdithInnerBoxAlpha = 0.24f

private object TileDefaults {
    /**
     * Default Edith QS tertiary tile colors, set per-theme via the `edith_qs_tile_*` color
     * resources (different accent3 tones in `values`/`values-night`):
     *
     *  - Dark:  active bg=a3_200, active icon=a3_800, inactive bg=a3_800, inactive icon=a3_100
     *  - Light: active bg=a3_600, active icon=a3_50,  inactive bg=a3_10,  inactive icon=a3_700
     *
     * They alias `@android:color/system_accent3_*`, which are dynamic (theme-aware), so the tiles
     * follow palette changes (wallpaper / ThemePicker). The inactive background is applied with
     * [EdithInactiveTileAlpha], matching AOSP's surfaceEffect1, so the wallpaper shows through.
     * Unavailable tiles use the stock dimmed look.
     *
     * @param edithTileColorOverride optional per-slot color overrides from the (debug) color tuner
     *   in Settings; unset slots fall back to the resource defaults above.
     */
    @Composable
    fun edithTertiaryTileColors(
        visualState: Int,
        edithTileColorOverride: EdithTileColorOverride? = null,
    ): TileColors {
        if (visualState != STATE_INACTIVE && visualState != STATE_ACTIVE) {
            // Unavailable (e.g. STATE_UNAVAILABLE): a dimmed neutral surface with a still-visible
            // glyph, matching the default unavailable tile look.
            return unavailableTileColors()
        }
        val context = LocalContext.current
        // Read the configuration (assetsSeq matches PlatformTheme) and the dark/light state so the
        // color resolution below is re-run when the theme changes: a dark<->light toggle changes the
        // values/values-night resources without necessarily changing assetsSeq, and picking new
        // colors in ThemePicker changes assetsSeq. Without both keys the tile keeps the previous
        // mode's color until something else forces a recomposition.
        val assetsSeq = LocalConfiguration.current.assetsSeq
        val isDark = isSystemInDarkTheme()
        val active = visualState == STATE_ACTIVE
        val inactiveAlpha = edithTileColorOverride?.inactiveAlpha ?: EdithInactiveTileAlpha

        val defaultBgRes =
            if (active) R.color.edith_qs_tile_active_bg else R.color.edith_qs_tile_inactive_bg
        val defaultIconRes =
            if (active) R.color.edith_qs_tile_active_icon else R.color.edith_qs_tile_inactive_icon

        // The override holds a swatch tag; resolve it against the current theme so the tile still
        // follows palette changes (wallpaper / ThemePicker / dark mode). Keyed on assetsSeq and the
        // dark/light state so the resolved colors are recomputed when the theme changes.
        val bgTag =
            if (active) edithTileColorOverride?.activeBg else edithTileColorOverride?.inactiveBg
        val iconTag =
            if (active) edithTileColorOverride?.activeFg else edithTileColorOverride?.inactiveFg
        val resolved =
            remember(context, isDark, assetsSeq, bgTag, iconTag, defaultBgRes, defaultIconRes) {
                val bgArgb =
                    EdithTileSwatches.resolve(context, bgTag) ?: context.getColor(defaultBgRes)
                val fgArgb =
                    EdithTileSwatches.resolve(context, iconTag) ?: context.getColor(defaultIconRes)
                bgArgb to fgArgb
            }
        val bg = Color(resolved.first).copy(alpha = if (active) 1f else inactiveAlpha)
        val fg = Color(resolved.second)
        return TileColors(
            background = bg,
            iconBackground = bg,
            label = fg,
            secondaryLabel = fg,
            icon = fg,
        )
    }

    /**
     * Colors for the Quick Actions tiles: the stock look, with any tuner overrides layered on top
     * (resolved through [QuickActionsTileSwatches]). Slots without an override keep their stock
     * color.
     */
    @Composable
    fun edithQuickActionsTileColors(
        uiState: TileUiState,
        iconOnly: Boolean,
        override: QuickActionsTileOverride? = null,
    ): TileColors {
        val stock = getColorForState(uiState, iconOnly)
        if (override == null || !override.hasAnyColor) {
            return stock
        }
        if (uiState.visualState != STATE_INACTIVE && uiState.visualState != STATE_ACTIVE) {
            return stock
        }
        val context = LocalContext.current
        // Keyed on assetsSeq and the dark/light state so the resolved colors are recomputed when the
        // theme changes (a dark<->light toggle may not change assetsSeq).
        val assetsSeq = LocalConfiguration.current.assetsSeq
        val isDark = isSystemInDarkTheme()
        val active = uiState.visualState == STATE_ACTIVE
        val inactiveAlpha = override.inactiveAlpha ?: EdithInactiveTileAlpha

        val bgTag = if (active) override.activeBg else override.inactiveBg
        val iconTag = if (active) override.activeFg else override.inactiveFg
        val resolved =
            remember(context, isDark, assetsSeq, bgTag, iconTag) {
                QuickActionsTileSwatches.resolve(context, bgTag) to
                    QuickActionsTileSwatches.resolve(context, iconTag)
            }
        // Anything the tuner didn't set keeps its stock color.
        val background =
            resolved.first?.let { Color(it).copy(alpha = if (active) 1f else inactiveAlpha) }
                ?: stock.background
        val fg = resolved.second?.let { Color(it) }
        return TileColors(
            background = background,
            iconBackground = background,
            label = fg ?: stock.label,
            secondaryLabel = fg ?: stock.secondaryLabel,
            icon = fg ?: stock.icon,
        )
    }

    /** An active tile uses the active color as background */
    @Composable
    @ReadOnlyComposable
    fun activeTileColors(): TileColors =
        TileColors(
            background = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primary,
            label = MaterialTheme.colorScheme.onPrimary,
            secondaryLabel = MaterialTheme.colorScheme.onPrimary,
            icon = MaterialTheme.colorScheme.onPrimary,
        )

    /** An active tile with dual target only show the active color on the icon */
    @Composable
    @ReadOnlyComposable
    fun activeDualTargetTileColors(): TileColors =
        TileColors(
            background = LocalAndroidColorScheme.current.surfaceEffect1,
            iconBackground = MaterialTheme.colorScheme.primary,
            label = MaterialTheme.colorScheme.onSurface,
            secondaryLabel = MaterialTheme.colorScheme.onSurface,
            icon = MaterialTheme.colorScheme.onPrimary,
        )

    @Composable
    @ReadOnlyComposable
    fun inactiveDualTargetTileColors(): TileColors =
        TileColors(
            background = LocalAndroidColorScheme.current.surfaceEffect1,
            iconBackground = LocalAndroidColorScheme.current.surfaceEffect2,
            label = MaterialTheme.colorScheme.onSurface,
            secondaryLabel = MaterialTheme.colorScheme.onSurface,
            icon = MaterialTheme.colorScheme.onSurface,
        )

    @Composable
    @ReadOnlyComposable
    fun inactiveTileColors(): TileColors =
        TileColors(
            background = LocalAndroidColorScheme.current.surfaceEffect1,
            iconBackground = Color.Transparent,
            label = MaterialTheme.colorScheme.onSurface,
            secondaryLabel = MaterialTheme.colorScheme.onSurface,
            icon = MaterialTheme.colorScheme.onSurface,
        )

    @Composable
    @ReadOnlyComposable
    fun unavailableTileColors(): TileColors {
        val surfaceColor = MaterialTheme.colorScheme.surface.copy(alpha = .18f)
        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .38f)
        return TileColors(
            background = surfaceColor,
            iconBackground = surfaceColor,
            label = onSurfaceVariantColor,
            secondaryLabel = onSurfaceVariantColor,
            icon = onSurfaceVariantColor,
        )
    }

    @Composable
    @ReadOnlyComposable
    fun getColorForState(uiState: TileUiState, iconOnly: Boolean): TileColors {
        return when (uiState.visualState) {
            STATE_ACTIVE -> {
                if (uiState.handlesToggleClick && !iconOnly) {
                    activeDualTargetTileColors()
                } else {
                    activeTileColors()
                }
            }

            STATE_INACTIVE -> {
                if (uiState.handlesToggleClick && !iconOnly) {
                    inactiveDualTargetTileColors()
                } else {
                    inactiveTileColors()
                }
            }

            else -> unavailableTileColors()
        }
    }

    @Composable
    fun iconRadius(uiState: TileUiState): Dp {
        return when (uiState.visualState) {
            STATE_ACTIVE -> ActiveIconCornerRadius
            STATE_INACTIVE -> InactiveIconCornerRadius
            else -> InactiveIconCornerRadius
        }
    }

    @Composable
    fun tileRadius(uiState: TileUiState): Dp {
        return when (uiState.visualState) {
            STATE_ACTIVE -> ActiveTileCornerRadius
            STATE_INACTIVE -> InactiveTileCornerRadius
            else -> InactiveTileCornerRadius
        }
    }

    @Composable
    fun animateIconShapeAsState(uiState: TileUiState): State<RoundedCornerShape> {
        return animateShapeAsState(
            targetValue = iconRadius(uiState),
            label = "QSTileIconCornerRadius",
        )
    }

    @Composable
    fun animateTileShapeAsState(uiState: TileUiState): State<RoundedCornerShape> {
        return animateShapeAsState(targetValue = tileRadius(uiState), label = "QSTileCornerRadius")
    }

    @Composable
    fun animateShapeAsState(targetValue: Dp, label: String): State<RoundedCornerShape> {
        val animatedCornerRadius by animateDpAsState(targetValue = targetValue, label = label)

        return remember {
            val corner =
                object : CornerSize {
                    override fun toPx(shapeSize: Size, density: Density): Float {
                        return with(density) { animatedCornerRadius.toPx() }
                    }
                }
            mutableStateOf(RoundedCornerShape(corner))
        }
    }
}

/**
 * A composable function that returns the [Resources]. It will be recomposed when [Configuration]
 * gets updated.
 */
@Composable
@ReadOnlyComposable
private fun resources(): Resources {
    LocalConfiguration.current
    return LocalResources.current
}
