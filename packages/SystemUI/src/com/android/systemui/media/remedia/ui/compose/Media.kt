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

@file:OptIn(ExperimentalMaterial3Api::class)

package com.android.systemui.media.remedia.ui.compose

import android.util.Log
import androidx.annotation.FloatRange
import android.view.ViewConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults.colors
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale as drawScopeScale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastRoundToInt
import com.android.compose.PlatformButton
import com.android.compose.PlatformIconButton
import com.android.compose.PlatformOutlinedButton
import com.android.compose.animation.Expandable
import com.android.compose.animation.rememberExpandableController
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.SceneTransitionLayout
import com.android.compose.animation.scene.TransitionBuilder
import com.android.compose.animation.scene.rememberMutableSceneTransitionLayoutState
import com.android.compose.animation.scene.transitions
import com.android.compose.gesture.effect.rememberOffsetOverscrollEffect
import com.android.compose.gesture.overscrollToDismiss
import com.android.compose.modifiers.height
import com.android.compose.modifiers.thenIf
import com.android.compose.ui.graphics.painter.rememberDrawablePainter
import com.android.systemui.animation.Expandable
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.common.shared.model.asImageBitmap
import com.android.systemui.common.ui.compose.Icon
import com.android.systemui.common.ui.compose.PagerDots
import com.android.systemui.common.ui.compose.byLayoutId
import com.android.systemui.common.ui.compose.load
import com.android.systemui.common.ui.compose.singleton
import com.android.systemui.compose.modifiers.sysuiResTag
import com.android.systemui.lifecycle.rememberViewModel
import com.android.systemui.media.remedia.shared.model.MediaCardActionButtonLayout
import com.android.systemui.media.remedia.shared.model.MediaColorScheme
import com.android.systemui.media.remedia.shared.model.MediaSessionState
import com.android.systemui.media.remedia.ui.viewmodel.MediaCardGutsViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaCardViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaCarouselVisibility
import com.android.systemui.media.remedia.ui.viewmodel.MediaDeviceChipViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaNavigationViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaPlayPauseActionViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaSecondaryActionViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaSettingsButtonViewModel
import com.android.systemui.media.remedia.ui.viewmodel.MediaViewModel
import com.android.systemui.qs.panels.ui.compose.infinitegrid.verticalSquish
import com.android.systemui.res.R
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.launch

/**
 * Renders a media controls UI element.
 *
 * This composable supports a multitude of presentation styles/layouts controlled by the
 * [presentationStyle] parameter. If the card carousel can be swiped away to dismiss by the user,
 * the [onDismissed] callback will be invoked when/if that happens.
 */
@Composable
fun Media(
    viewModelFactory: MediaViewModel.Factory,
    presentationStyle: MediaPresentationStyle,
    behavior: MediaUiBehavior,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    visible: () -> Boolean = { true },
    mediaSquishiness: () -> Float = { 1f },
    location: Media.Location,
    expansion: () -> Float = { 0F },
    animateWithExpansion: Boolean = true,
    morphProgress: (() -> Float)? = null,
    holdDuringTransition: Boolean = false,
    // When non-null and this is the lock screen, overrides the card body tap so it can expand the
    // media player instead of launching the app.
    onCardBodyClick: ((Expandable) -> Unit)? = null,
    // When false, the card does not draw the album art as its own background (used by the expanded
    // lock screen media player, which draws the art as a separate square card).
    drawArtBackground: Boolean = true,
    // Whether the Edith expanded lock screen media player is showing (art as a square card with the
    // content below). Only used on the lock screen.
    expanded: Boolean = false,
    // Called when the expanded player should collapse (tap on the art, or drag down).
    onCollapse: () -> Unit = {},
    // Notifies the expansion drag progress in [0,1] while the user drags the expanded art.
    onExpansionDragProgress: (Float) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: MediaViewModel =
        rememberViewModel("Media.viewModel") {
            viewModelFactory.create(
                context = context,
                carouselVisibility = behavior.carouselVisibility,
            )
        }

    LaunchedEffect(visible) { viewModel.setVisibility(visible) }

    // Keep the last cards (and thus the shared element) alive while a scene transition is running,
    // so clearing the session does not unmount the card a frame early ("closing late").
    LaunchedEffect(holdDuringTransition) {
        viewModel.setHeldDuringTransition(holdDuringTransition)
    }

    viewModel.mediaUiEventLogger.logCarouselLocation(location)

    // Collapse when the shown session changes, so an expansion never carries over to a different
    // track (the expansion state lives in a process-wide singleton).
    if (expanded) {
        val key = viewModel.cards.getOrNull(viewModel.currentIndex)?.key
        LaunchedEffect(key) { if (key == null) onCollapse() }
    }

    val currentCard =
        viewModel.cards.getOrNull(
            viewModel.currentIndex.coerceIn(0, (viewModel.cards.size - 1).coerceAtLeast(0))
        )

    // Animated expansion progress (0 = collapsed, 1 = expanded), shared by the morphing art and the
    // ambient backdrop, so everything stays in sync and the art never disappears (which used to
    // flicker the wallpaper on tap).
    val expansionProgress = remember { Animatable(if (expanded) 1f else 0f) }
    // The block height and the controls' slide run at the morph spring's frequency but critically
    // damped. The morph spring's decaying tail crosses its target every half period, and passing it
    // through moved the block, the controls and the notification stack below by a few dp just after
    // the morph had settled.
    val layoutProgress = remember { Animatable(if (expanded) 1f else 0f) }
    LaunchedEffect(expanded) {
        val target = if (expanded) 1f else 0f
        launch {
            expansionProgress.animateTo(
                targetValue = target,
                animationSpec =
                    spring(
                        stiffness = Media.MORPH_SPRING_STIFFNESS,
                        dampingRatio = Media.MORPH_SPRING_DAMPING,
                    ),
            )
        }
        launch {
            layoutProgress.animateTo(
                targetValue = target,
                animationSpec =
                    spring(
                        stiffness = Media.MORPH_SPRING_STIFFNESS,
                        dampingRatio = 1f,
                    ),
            )
        }
    }

    // The expandable-player presentation is *lock screen only*. Every other host (QS, communal,
    // status bar popup) must keep the stock card, including its own art background.
    if (location == Media.Location.LOCKSCREEN && currentCard != null) {
        // A single morphing layout: the album art starts as the card's backing (behind the content,
        // progress 0) and morphs into the centred square (above the content, progress 1), with the
        // content sliding down to make room. Driving one layout (rather than cross-fading two) is
        // what makes the transition continuous in *both* directions.
        ExpandedMediaArt(
            card = currentCard,
            progress = expansionProgress,
            layoutProgress = layoutProgress,
            onCollapse = onCollapse,
            onProgressChanged = onExpansionDragProgress,
            modifier = modifier,
            // The collapsed card keeps the height of the active presentation style (the compact
            // two-row card is shorter than the three-row one).
            collapsedHeight =
                if (presentationStyle == MediaPresentationStyle.Compressed) {
                    Media.EDITH_COMPRESSED_HEIGHT
                } else {
                    Media.EDITH_DEFAULT_HEIGHT
                },
        ) {
            CardCarousel(
                viewModel = viewModel,
                presentationStyle = presentationStyle,
                behavior = behavior,
                onDismissed = onDismissed,
                mediaSquishiness = mediaSquishiness,
                expansion = expansion,
                animateWithExpansion = animateWithExpansion,
                morphProgress = morphProgress,
                // The ExpandedMediaArt layer draws the art in this path.
                drawArtBackground = false,
                onCardBodyClick = if (expanded) {
                    { onCollapse() }
                } else {
                    onCardBodyClick
                },
            )
        }
        return
    }

    CardCarousel(
        viewModel = viewModel,
        presentationStyle = presentationStyle,
        behavior = behavior,
        onDismissed = onDismissed,
        modifier = modifier,
        mediaSquishiness = mediaSquishiness,
        expansion = expansion,
        animateWithExpansion = animateWithExpansion,
        morphProgress = morphProgress,
        drawArtBackground = drawArtBackground,
        onCardBodyClick = onCardBodyClick,
    )
}

/**
 * Renders a media controls carousel of cards.
 *
 * This composable supports a multitude of presentation styles/layouts controlled by the
 * [presentationStyle] parameter. The behavior is controlled by [behavior]. If
 * [MediaUiBehavior.isCarouselDismissible] is `true`, the [onDismissed] callback will be invoked
 * when/if that happens.
 */
@Composable
private fun CardCarousel(
    viewModel: MediaViewModel,
    presentationStyle: MediaPresentationStyle,
    behavior: MediaUiBehavior,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    mediaSquishiness: () -> Float,
    expansion: () -> Float,
    animateWithExpansion: Boolean,
    morphProgress: (() -> Float)?,
    drawArtBackground: Boolean = true,
    onCardBodyClick: ((Expandable) -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = viewModel.isCarouselVisible,
        modifier = modifier,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        CardCarouselContent(
            viewModel = viewModel,
            presentationStyle = presentationStyle,
            behavior = behavior,
            onDismissed = onDismissed,
            mediaSquishiness = mediaSquishiness,
            expansion = expansion,
            animateWithExpansion = animateWithExpansion,
            morphProgress = morphProgress,
            drawArtBackground = drawArtBackground,
            onCardBodyClick = onCardBodyClick,
        )
    }
}

@Composable
private fun CardCarouselContent(
    viewModel: MediaViewModel,
    presentationStyle: MediaPresentationStyle,
    behavior: MediaUiBehavior,
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    mediaSquishiness: () -> Float,
    expansion: () -> Float,
    animateWithExpansion: Boolean,
    morphProgress: (() -> Float)?,
    drawArtBackground: Boolean = true,
    onCardBodyClick: ((Expandable) -> Unit)? = null,
) {
    val pagerState = rememberPagerState {
        if (behavior.isCarouselScrollingEnabled) {
            viewModel.cards.size
        } else {
            1
        }
    }
    var userTappedIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(viewModel.currentIndex) {
        if (viewModel.currentIndex != pagerState.currentPage) {
            userTappedIndex = viewModel.currentIndex
            // Snap (don't animate): this fires when a scene composes/reveals a pager whose
            // position is out of sync with the shared selection (e.g. the QQS copy catching up to
            // the expanded page on a close). Animating it would visibly swipe the shared card.
            // User swipes drive pagerState directly, so they stay animated by the pager itself.
            pagerState.scrollToPage(viewModel.currentIndex)
        }
    }
    LaunchedEffect(pagerState.currentPage, viewModel.cards.size) {
        if (pagerState.currentPage < viewModel.cards.size) {
            // This effect is triggered from both a swipe between pages in the pager, and the
            // user tapping an index, which could animate more than one page away.
            // In the second case, we need to ignore the intermediate events caused by
            // `animateScrollToPage`, so that scrolling can fully complete.
            if (userTappedIndex == null || pagerState.currentPage == userTappedIndex) {
                viewModel.onCardSelected(pagerState.currentPage)

                val areDeviceChipsVisible =
                    presentationStyle !in
                        listOf(MediaPresentationStyle.Thumbnail, MediaPresentationStyle.Compact)
                viewModel.onCardVisible(pagerState.currentPage, areDeviceChipsVisible)

                userTappedIndex = null
            }
        }
    }
    var isFalseTouchDetected: Boolean by
        remember(behavior.isCarouselScrollFalseTouch) { mutableStateOf(false) }
    val isSwipingEnabled = behavior.isCarouselScrollingEnabled && !isFalseTouchDetected

    val roundedCornerShape = remember { RoundedCornerShape(32.dp) }

    // Non-null when the scene path drives a height morph; used to size the card from the progress.
    val morph = morphProgress

    Box(
        modifier =
            modifier
                .pointerInput(behavior) {
                    if (behavior.isCarouselScrollFalseTouch != null) {
                        awaitEachGesture {
                            awaitFirstDown(false, PointerEventPass.Initial)
                            isFalseTouchDetected = behavior.isCarouselScrollFalseTouch.invoke()
                        }
                    }
                }
                .graphicsLayer {
                    shape = roundedCornerShape
                    clip = true
                }
    ) {
        @Composable
        fun PagerContent() {
            // The pager dots sit BELOW the card (in a strip inside this container) rather than
            // overlaying it, so the card never has to reserve space inside itself: that reserve used
            // to squeeze the controls and leave a gap below them.
            Column(modifier = Modifier.sysuiResTag(MediaRes.MEDIA_CAROUSEL_SCROLLER)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.sysuiResTag(MediaRes.MEDIA_CAROUSEL),
                    userScrollEnabled = isSwipingEnabled,
                    pageSpacing = 0.dp,
                    beyondViewportPageCount = 1,
                    key = { index: Int -> viewModel.cards[index].key },
                ) { pageIndex: Int ->
                    if (behavior.isCarouselScrollingEnabled || pageIndex == 0) {
                        if (Media.DEBUG) {
                            Log.d(
                                Media.TAG,
                                "composing media card ${viewModel.cards[pageIndex].key}",
                            )
                        }
                        // Classic pager: every page is the full card. There is no adjacent
                        // thumbnail treatment (the hero carousel's peek of the neighboring cards).
                        Card(
                            mediaViewModel = viewModel,
                            cardIndex = pageIndex,
                            cardStyle = presentationStyle,
                            carouselStyle = presentationStyle,
                            mediaSquishiness = mediaSquishiness,
                            expansion = expansion,
                            animateWithExpansion = animateWithExpansion,
                            morphProgress = morphProgress,
                            drawArtBackground = drawArtBackground,
                            onCardBodyClick = onCardBodyClick,
                            modifier =
                                Modifier.clip(roundedCornerShape)
                                    .fillMaxWidth()
                                    // When morphing (scene path), set a BOUNDED height driven by the
                                    // shared progress (lerp between the two-row and three-row
                                    // heights) rather than filling the parent — the QQS header is
                                    // measured with full-screen max height, so fillMaxHeight would
                                    // blow the compact card up to ~half the screen.
                                    .thenIf(morph != null) {
                                        Modifier.height {
                                            lerp(
                                                    Media.EDITH_COMPRESSED_HEIGHT,
                                                    Media.EDITH_DEFAULT_HEIGHT,
                                                    morph!!().coerceIn(0f, 1f),
                                                )
                                                .roundToPx()
                                        }
                                    }
                                    .sysuiResTag(MediaRes.MEDIA_CONTROLS),
                        )
                    }
                }

                if (pagerState.pageCount > 1) {
                    PagerDots(
                        pagerState = pagerState,
                        // Use the theme primary (not the artwork-derived color).
                        activeColor = MaterialTheme.colorScheme.primary,
                        nonActiveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                        dotSize = 6.dp,
                        spaceSize = 6.dp,
                        modifier =
                            Modifier.align(Alignment.CenterHorizontally)
                                // Balance the dots between the card and the notification shade: the
                                // QQS header already adds 16dp below the media (qqsLayoutPaddingBottom),
                                // so use 16dp above the dots and 0 below → ~16dp on each side.
                                .padding(top = 16.dp, bottom = 0.dp),
                    )
                }
            }
        }

        if (behavior.isCarouselDismissible) {
            Box(
                modifier =
                    Modifier.overscrollToDismiss(
                        orientation = Orientation.Horizontal,
                        enabled = isSwipingEnabled,
                        onDismissed = onDismissed,
                    )
            ) {
                PagerContent()
            }
        } else {
            val overscrollEffect = rememberOffsetOverscrollEffect()
            SwipeToReveal(
                foregroundContent = { PagerContent() },
                foregroundContentEffect = overscrollEffect,
                revealedContent = { revealAmount ->
                    RevealedContent(
                        viewModel = viewModel.settingsButtonViewModel,
                        revealAmount = revealAmount,
                    )
                },
                isSwipingEnabled = isSwipingEnabled,
            )
        }
    }

    LaunchedEffect(viewModel.scrollToFirst) {
        if (viewModel.scrollToFirst && viewModel.cards.isNotEmpty()) {
            pagerState.animateScrollToPage(0)
            viewModel.onScrollToFirstCard()
        }
    }
}

/** Renders the UI of a single media card. */
@Composable
private fun Card(
    mediaViewModel: MediaViewModel,
    cardIndex: Int,
    cardStyle: MediaPresentationStyle,
    carouselStyle: MediaPresentationStyle,
    modifier: Modifier = Modifier,
    mediaSquishiness: () -> Float,
    expansion: () -> Float,
    animateWithExpansion: Boolean,
    morphProgress: (() -> Float)? = null,
    // When false, the album art is not drawn as the card's own background (the Edith expanded
    // lock screen media player draws the art as a separate square card above the content instead).
    drawArtBackground: Boolean = true,
    // When non-null, overrides the card body tap (which otherwise launches the playing app). Used
    // by the Edith lock screen to expand the media player instead.
    onCardBodyClick: ((Expandable) -> Unit)? = null,
) {
    val viewModel = mediaViewModel.cards[cardIndex]
    val stlState =
        rememberMutableSceneTransitionLayoutState(
            // When morphing, the card starts from the two-row Compressed scene and morphs to Default
            // with the shared progress.
            initialScene = (if (morphProgress != null) MediaPresentationStyle.Compressed else cardStyle)
                .toScene(),
            transitions = Media.Transitions,
        )

    val colorScheme = rememberAnimatedColorScheme(viewModel.colorScheme)

    // Each time the presentation style changes, animate to the corresponding scene. When a shared
    // morph progress drives the card (scene path), the morph owns the scene instead.
    if (morphProgress == null) {
        LaunchedEffect(cardStyle) {
            stlState.setTargetScene(targetScene = cardStyle.toScene(), animationScope = this)
        }
    }

    // When a shared morph progress is provided (scene-container path), drive the card's internal
    // Compressed<->Default scene from it, so the two-row/three-row rows morph in sync with the
    // Shade<->QS pull. The card's height is set (bounded) from the same progress at the call site.
    if (morphProgress != null) {
        LaunchedEffect(morphProgress) {
            synchronizeMediaState(
                stlState,
                morphProgress,
                Media.Scenes.Compressed,
                Media.Scenes.Default,
            )
        }
    }

    Box(modifier) {
        if (carouselStyle != MediaPresentationStyle.Compact && drawArtBackground) {
            CardBackground(
                image = viewModel.background,
                colorScheme = colorScheme,
                modifier =
                    Modifier.matchParentSize()
                        .sysuiResTag(MediaRes.BACKGROUND)
                        .clip(RoundedCornerShape(32.dp)),
            )
        }

        Expandable(
            controller =
                rememberExpandableController(color = Color.Transparent, shape = RectangleShape),
            useModifierBasedImplementation = true,
            modifier = Modifier.verticalSquish(mediaSquishiness),
        ) {
            key(stlState) {
                SceneTransitionLayout(state = stlState, debugName = "Media Card") {
                    scene(Media.Scenes.Default) {
                        CardForeground(
                            expandable = it,
                            mediaViewModel = mediaViewModel,
                            cardIndex = cardIndex,
                            colorScheme = colorScheme,
                            threeRows = true,
                            fillHeight = false,
                            onCardBodyClick = onCardBodyClick,
                        )
                    }

                    scene(Media.Scenes.Large) {
                        CardForeground(
                            expandable = it,
                            mediaViewModel = mediaViewModel,
                            cardIndex = cardIndex,
                            colorScheme = colorScheme,
                            threeRows = true,
                            fillHeight = true,
                            onCardBodyClick = onCardBodyClick,
                        )
                    }

                    scene(Media.Scenes.Compressed) {
                        CardForeground(
                            expandable = it,
                            mediaViewModel = mediaViewModel,
                            cardIndex = cardIndex,
                            colorScheme = colorScheme,
                            threeRows = false,
                            fillHeight = false,
                            onCardBodyClick = onCardBodyClick,
                        )
                    }

                    scene(Media.Scenes.Compact) {
                        CompactCardForeground(expandable = it, viewModel = viewModel)
                    }

                    scene(Media.Scenes.Thumbnail) {
                        // Thumbnail is album art only, no foreground elements
                        var thumbnailModifier =
                            Modifier.clickable(onClick = { viewModel.onClick(it) }).height {
                                when (carouselStyle) {
                                    MediaPresentationStyle.Default -> Media.DEFAULT_HEIGHT
                                    // Large is only used for communal, which isn't scrollable
                                    MediaPresentationStyle.Large -> Dp.Unspecified
                                    MediaPresentationStyle.Compressed ->
                                        lerp(
                                            Media.COMPRESSED_HEIGHT,
                                            Media.DEFAULT_HEIGHT,
                                            expansion(),
                                        )
                                    MediaPresentationStyle.Compact -> Media.COMPACT_HEIGHT
                                    else ->
                                        throw IllegalArgumentException(
                                            "Invalid carousel presentation"
                                        )
                                }.roundToPx()
                            }
                        if (carouselStyle == MediaPresentationStyle.Compact) {
                            thumbnailModifier =
                                thumbnailModifier.then(
                                    Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
                                )
                        }
                        Spacer(modifier = modifier.then(thumbnailModifier))
                    }
                }
            }
        }

        LaunchedEffect(cardStyle, animateWithExpansion) {
            launch {
                if (
                    animateWithExpansion &&
                        cardStyle != MediaPresentationStyle.Thumbnail &&
                        carouselStyle == MediaPresentationStyle.Compressed
                ) {
                    synchronizeMediaState(
                        stlState,
                        expansion,
                        Media.Scenes.Compressed,
                        Media.Scenes.Default,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberAnimatedColorScheme(colorScheme: MediaColorScheme?): AnimatedColorScheme {
    val primaryColor = colorScheme?.primary ?: MaterialTheme.colorScheme.primaryFixed
    val onPrimaryColor = colorScheme?.onPrimary ?: MaterialTheme.colorScheme.onPrimaryFixed
    val backgroundColor = colorScheme?.background ?: MaterialTheme.colorScheme.onSurface
    val animatedPrimary by animateColorAsState(targetValue = primaryColor)
    val animatedOnPrimary by animateColorAsState(targetValue = onPrimaryColor)
    val animatedBackground by animateColorAsState(targetValue = backgroundColor)

    return remember {
        object : AnimatedColorScheme {
            override val primary: Color
                get() = animatedPrimary

            override val onPrimary: Color
                get() = animatedOnPrimary

            override val background: Color
                get() = animatedBackground
        }
    }
}

/**
 * Renders the foreground of a card, including all UI content and the internal "guts".
 *
 * If [threeRows] is `true`, the layout will be organized as three horizontal rows; if `false`, two
 * rows will be used, resulting in a more compact layout.
 *
 * If [fillHeight] is `true`, the card will grow vertically to fill all available space in its
 * parent. If not, it'll only be as tall as needed to show its UI.
 */
@Composable
private fun ContentScope.CardForeground(
    expandable: Expandable,
    mediaViewModel: MediaViewModel,
    cardIndex: Int,
    colorScheme: AnimatedColorScheme,
    threeRows: Boolean,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
    onCardBodyClick: ((Expandable) -> Unit)? = null,
) {
    val viewModel = mediaViewModel.cards[cardIndex]
    // Can't use a Crossfade composable because of the custom layout logic below. Animate the alpha
    // of the guts (and, indirectly, of the content) from here.
    val gutsAlphaAnimatable = remember { Animatable(0f) }
    val isGutsVisible = viewModel.guts.isVisible
    LaunchedEffect(isGutsVisible) { gutsAlphaAnimatable.animateTo(if (isGutsVisible) 1f else 0f) }

    // Use a custom layout to measure the content even if the content is being hidden because the
    // internal guts are showing. This is needed because only the content knows the size the of the
    // card and the guts are set to be the same size of the content.
    Layout(
        content = {
            CardForegroundContent(
                expandable = expandable,
                mediaViewModel = mediaViewModel,
                cardIndex = cardIndex,
                threeRows = threeRows,
                fillHeight = fillHeight,
                colorScheme = colorScheme,
                onCardBodyClick = onCardBodyClick,
                modifier =
                    Modifier.layoutId(Media.LayoutId.CardForeground).graphicsLayer {
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        alpha = 1f - gutsAlphaAnimatable.value
                    },
            )

            CardGuts(
                viewModel = viewModel.guts,
                colorScheme = colorScheme,
                modifier =
                    Modifier.layoutId(Media.LayoutId.CardGuts).graphicsLayer {
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        alpha = gutsAlphaAnimatable.value
                    },
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val measurableByLayoutId = measurables.byLayoutId<Media.LayoutId>()
        val contentPlaceable =
            measurableByLayoutId[Media.LayoutId.CardForeground]!!.measure(constraints)
        // Guts should always have the exact dimensions as the content, even if we don't show the
        // content.
        val gutsPlaceable =
            measurableByLayoutId[Media.LayoutId.CardGuts]!!.measure(
                Constraints.fixed(contentPlaceable.width, contentPlaceable.height)
            )

        layout(contentPlaceable.measuredWidth, contentPlaceable.measuredHeight) {
            // Place content so its modifiers stay active and don't wait for previous pointer events
            // after the switch from guts to content.
            contentPlaceable.place(0, 0)
            if (viewModel.guts.isVisible || gutsAlphaAnimatable.isRunning) {
                gutsPlaceable.place(0, 0)
            }
        }
    }
}

@Composable
private fun ContentScope.CardForegroundContent(
    expandable: Expandable,
    mediaViewModel: MediaViewModel,
    cardIndex: Int,
    threeRows: Boolean,
    fillHeight: Boolean,
    colorScheme: AnimatedColorScheme,
    modifier: Modifier = Modifier,
    onCardBodyClick: ((Expandable) -> Unit)? = null,
) {
    val viewModel = mediaViewModel.cards[cardIndex]
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                // Note: no fixed height. The card is content-wrapped like stock; the QS panel's
                // media `Element` (QSFragmentCompose.MediaObject) owns any expansion-driven height,
                // and pinning a hard 176dp here fought that animation during the QS transition.
                .combinedClickable(
                    onClick = { (onCardBodyClick ?: viewModel.onClick).invoke(expandable) },
                    onLongClick = viewModel.onLongClick,
                )
                .semantics { contentDescription = viewModel.contentDescription }
    ) {
        // The Edith three-row layout renders its own context header, so the stock top row is only
        // used for the other presentation styles. The two-row header is a single row:
        // (app icon) [device name >], same shape as the three-row context header (minus the app
        // name / "on"), so the device name + chevron already sit in the same place in both scenes
        // and no chevron morph is needed.
        if (!threeRows) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    // Record the card's max width so the (fractional-width) suggestion chip can be
                    // capped to a fraction of it.
                    .layout { measurable, constraints ->
                        mediaViewModel.cardMaxWidth =
                            mediaViewModel.cardMaxWidth.coerceAtLeast(constraints.maxWidth)
                        val placeable = measurable.measure(constraints)
                        layout(placeable.measuredWidth, placeable.measuredHeight) {
                            placeable.place(0, 0)
                        }
                    },
        ) {
            Icon(
                icon = viewModel.icon,
                tint = colorScheme.primary,
                modifier = Modifier.size(24.dp).clip(CircleShape),
            )
            Spacer(Modifier.width(6.dp))
            // Shared element with the three-row header's output control so the device name +
            // chevron slides between its two positions during the morph.
            Element(key = Media.Elements.OutputControl, modifier = Modifier) {
                EdithOutputControl(viewModel = viewModel.outputSwitcherChip)
            }

            // Keep the suggested-device chip, right-aligned after the output control.
            Spacer(Modifier.weight(1f))
            val cardMaxWidth = mediaViewModel.cardMaxWidth
            AnimatedVisibility(
                visible = viewModel.deviceSuggestionChip != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                rememberLastNonNull(viewModel.deviceSuggestionChip)?.let {
                    DeviceChip(
                        viewModel = it,
                        style =
                            DeviceChipStyle(
                                fillColor = Color.Transparent,
                                contentColor = colorScheme.primary,
                                borderColor = colorScheme.primary,
                            ),
                        modifier =
                            Modifier.fractionalMaxWidth(
                                    containerMaxWidth = cardMaxWidth,
                                    fraction = 0.5f,
                                )
                                .sysuiResTag(MediaRes.SUGGESTED_DEVICE_CHIP),
                    )
                }
            }
        }
        }

        // If the card is taller than necessary to show all the rows, this adds spacing
        // between the top row and the rows below, anchoring the next rows to the bottom
        // of the card.
        if (fillHeight) {
            Spacer(Modifier.weight(1f))
        }

        if (threeRows) {
            // Edith expanded layout:
            //   1. Context header: (app icon) [App] on (output icon) [Device]
            //   2. Metadata (title + artist), left aligned
            //   3. Scrubber row: [elapsed] (seekbar) [duration]
            //   4. Control bar: [left action] [prev] (pill play/pause) [next]
            val areActionsVisible =
                viewModel.actionButtonLayout == MediaCardActionButtonLayout.WithPlayPause

            // 1. Context header: (app icon) App on (output device name >).
            // The "(app icon) App on" part is keyed as an element that cross-fades (it exists only
            // in this three-row scene), while the output control is a SEPARATE shared element
            // (sibling, not nested) that morphs/slides between the two scenes.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
            ) {
                Element(key = Media.Elements.ContextHeader, modifier = Modifier) {
                    EdithContextHeader(
                        icon = viewModel.icon,
                        iconTint = colorScheme.primary,
                        appName = viewModel.appName,
                        deviceName = deviceNameFor(viewModel.outputSwitcherChip),
                        modifier = Modifier,
                        // The card body tap expands the lock screen player, so the app is opened
                        // from the header app icon instead (unchanged elsewhere — this is the same
                        // click intent the card body used to run).
                        onAppIconClick = { viewModel.onClick(expandable) },
                    )
                }
                Spacer(Modifier.width(4.dp))
                Element(key = Media.Elements.OutputControl, modifier = Modifier) {
                    EdithOutputControl(viewModel = viewModel.outputSwitcherChip)
                }
            }

            // 2. Metadata.
            Metadata(
                title = viewModel.title,
                subtitle = viewModel.subtitle,
                isExplicit = viewModel.isExplicit,
                color = Color.White,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        .animateContentSize(),
            )

            // 3. Scrubber row. The timestamps sit flush against the same 16dp insets as the
            // metadata above, so the row is horizontally balanced.
            // Keyed as an element so the whole row (timestamps + seek bar) cross-fades during the
            // morph; the row exists only in this scene.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val navigationViewModel = viewModel.navigation
                Element(
                    key = Media.Elements.ScrubberRow,
                    modifier =
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (navigationViewModel is MediaNavigationViewModel.Showing) {
                            TimestampText(navigationViewModel.progressText)
                            EdithSeekBar(
                                viewModel = navigationViewModel,
                                modifier = Modifier.weight(1f),
                            )
                            TimestampText(navigationViewModel.durationText)
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // 4. Control bar: [additional0] [prev] (play) [next] [additional1], with the play/pause
            // button centered. The far slots reserve equal width so the middle stays centered for
            // 0/1/2 additional actions.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val navigationViewModel = viewModel.navigation as? MediaNavigationViewModel.Showing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
                ) {
                    // Far-left slot: additional action #0 (or reserved space).
                    SecondarySlot(viewModel = viewModel.additionalActions.getOrNull(0), elementIndex = 0)

                    Spacer(Modifier.weight(1f))

                    // Center group: skip back, play/pause, skip forward.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        navigationViewModel?.left?.let {
                            SecondaryAction(
                                viewModel = it,
                                modifier = Modifier.sysuiResTag(MediaRes.PREV_BTN),
                                element = Media.Elements.PrevButton,
                            )
                        }

                        if (areActionsVisible) {
                            AnimatedVisibility(visible = viewModel.playPauseAction != null) {
                                PlayPauseAction(
                                    viewModel = viewModel.playPauseAction,
                                    buttonColor = colorScheme.primary,
                                    iconColor = colorScheme.onPrimary,
                                    buttonCornerRadius = { isPlaying ->
                                        if (isPlaying) 22.dp else 44.dp
                                    },
                                )
                            }
                        }

                        navigationViewModel?.right?.let {
                            SecondaryAction(
                                viewModel = it,
                                modifier = Modifier.sysuiResTag(MediaRes.NEXT_BTN),
                                element = Media.Elements.NextButton,
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Far-right slot: additional action #1 (or reserved space).
                    SecondarySlot(viewModel = viewModel.additionalActions.getOrNull(1), elementIndex = 1)
                }
            }
        } else {
            // Two row presentation style.
            //
            // Bottom row: metadata (title + artist stacked) on the left, transport controls on the
            // right. The controls are nudged up so their vertical center lines up with the track
            // TITLE (not the title+artist block center).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Metadata(
                    title = viewModel.title,
                    subtitle = viewModel.subtitle,
                    isExplicit = viewModel.isExplicit,
                    color = Color.White,
                    modifier = Modifier.weight(1f).padding(end = 8.dp).animateContentSize(),
                )

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    val areActionsVisible =
                        viewModel.actionButtonLayout ==
                            MediaCardActionButtonLayout.WithPlayPause

                    if (
                        viewModel.actionButtonLayout
                            is MediaCardActionButtonLayout.SecondaryActionsOnly
                    ) {
                        (viewModel.actionButtonLayout
                                as MediaCardActionButtonLayout.SecondaryActionsOnly)
                            .indicesForCompressed
                            .fastForEach { index ->
                                SecondaryAction(
                                    viewModel = viewModel.additionalActions[index],
                                    resId = "action$index",
                                    element = Media.Elements.additionalActionButton(index),
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                    }

                    // Center group matching the three-row control bar: prev, play/pause, next,
                    // with the same visibility rules (so the two scenes morph in sync).
                    val navigationViewModel =
                        viewModel.navigation as? MediaNavigationViewModel.Showing
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        // Align the control's vertical center with the title line (~half the
                        // artist line height above the title+artist block center).
                        modifier = Modifier.offset(y = (-10).dp),
                    ) {
                        navigationViewModel?.left?.let {
                            SecondaryAction(
                                viewModel = it,
                                modifier = Modifier.sysuiResTag(MediaRes.PREV_BTN),
                                element = Media.Elements.PrevButton,
                            )
                        }

                        if (areActionsVisible) {
                            AnimatedVisibility(visible = viewModel.playPauseAction != null) {
                                PlayPauseAction(
                                    viewModel = viewModel.playPauseAction,
                                    buttonColor = colorScheme.primary,
                                    iconColor = colorScheme.onPrimary,
                                    buttonCornerRadius = { isPlaying ->
                                        if (isPlaying) 16.dp else 48.dp
                                    },
                                )
                            }
                        }

                        navigationViewModel?.right?.let {
                            SecondaryAction(
                                viewModel = it,
                                modifier = Modifier.sysuiResTag(MediaRes.NEXT_BTN),
                                element = Media.Elements.NextButton,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Renders a simplified version of [CardForeground] that puts everything on a single row and doesn't
 * support the guts.
 */
@Composable
private fun ContentScope.CompactCardForeground(
    expandable: Expandable,
    viewModel: MediaCardViewModel,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clickable(onClick = { viewModel.onClick(expandable) })
                .semantics { contentDescription = viewModel.contentDescription }
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(16.dp),
    ) {
        Icon(
            icon = viewModel.icon,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp),
        )

        Metadata(
            title = viewModel.title,
            subtitle = viewModel.subtitle,
            isExplicit = viewModel.isExplicit,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        SecondaryAction(
            viewModel = viewModel.outputSwitcherChipButton,
            resId = MediaRes.OUTPUT_CHIP,
            element = Media.Elements.OutputSwitcherButton,
            iconColor = MaterialTheme.colorScheme.onSurface,
        )

        val rightAction = viewModel.navigation.right
        if (rightAction !is MediaSecondaryActionViewModel.None) {
            SecondaryAction(
                viewModel = rightAction,
                resId = MediaRes.NEXT_BTN,
                element = Media.Elements.NextButton,
                iconColor = MaterialTheme.colorScheme.onSurface,
            )
        }

        AnimatedVisibility(visible = viewModel.playPauseAction != null) {
            PlayPauseAction(
                viewModel = viewModel.playPauseAction,
                buttonColor = MaterialTheme.colorScheme.primaryContainer,
                iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                buttonCornerRadius = { isPlaying -> if (isPlaying) 16.dp else 24.dp },
            )
        }
    }
}

/** Renders the background of a card, loading the artwork and showing an overlay on top of it. */
@Composable
private fun CardBackground(
    image: Icon?,
    colorScheme: AnimatedColorScheme,
    modifier: Modifier = Modifier,
) {
    // Note: `fillMaxSize()` is required here because the art `Image` below only has
    // `fillMaxSize()`, and `Crossfade`'s content slot is content-wrapping. Without forcing the
    // Crossfade node to fill the (matched) parent, the content slot is unbounded and the image
    // lays out at its intrinsic bitmap size, centered, leaving gaps on all sides when the art is
    // smaller than the card (e.g. a 16:9 bitmap in the 176dp expanded card).
    //
    // Cross-fade on the resolved bitmap (remembered by the underlying `Drawable` reference), not
    // on the `Icon` wrapper: `Icon.Loaded` compares its `Drawable` by reference, and a background
    // re-emit can wrap the *same* drawable in a new `Icon.Loaded`. Keying on the drawable avoids
    // restarting the cross-fade (which showed up as a flicker) for unchanged art.
    val drawable = (image as? Icon.Loaded)?.drawable
    val backgroundImage =
        remember(drawable) { drawable?.let { Icon.Loaded(it, contentDescription = null).asImageBitmap() } }
    Crossfade(targetState = backgroundImage, modifier = modifier.fillMaxSize()) { bitmap ->
        if (bitmap != null) {
            // Loaded art.
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier.fillMaxSize().drawWithContent {
                        // Over-crop: draw the art slightly larger than the node so thin
                        // letterbox/border bars that some artwork bakes into its own pixels
                        // (e.g. video thumbnails) are cropped off the edges. `Crop` already fills
                        // the node; this only shaves the outermost fraction of the frame.
                        drawScopeScale(
                            scaleX = Media.ART_OVER_CROP_SCALE,
                            scaleY = Media.ART_OVER_CROP_SCALE,
                        ) {
                            // Draw the content (loaded art). Explicit receiver: inside the scale
                            // block the implicit receiver is the inner DrawScope.
                            this@drawWithContent.drawContent()
                        }

                        if (bitmap != null) {
                            // Then draw the scrim: the artwork's background color at 50% alpha
                            // (flat, no gradient).
                            drawRect(color = colorScheme.background.copy(alpha = 0.5f))
                        }
                    },
            )
        } else {
            // Placeholder.
            Box(Modifier.background(colorScheme.background).fillMaxSize())
        }
    }
}

/**
 * Renders the navigation UI (seek bar and/or previous/next buttons).
 *
 * If [isSeekBarVisible] is `false`, the seek bar will not be included in the layout, even if it
 * would otherwise be showing based on the view-model alone. This is meant for callers to decide
 * whether they'd like to show the seek bar in addition to the prev/next buttons or just show the
 * buttons.
 *
 * If [areActionsVisible] is `false`, the left/right buttons to the left and right of the seek bar
 * will not be included in the layout.
 */
@Composable
private fun ContentScope.Navigation(
    viewModel: MediaNavigationViewModel,
    isSeekBarVisible: Boolean,
    isLeftActionVisible: Boolean,
    isRightActionVisible: Boolean,
    modifier: Modifier = Modifier,
) {
    when (viewModel) {
        is MediaNavigationViewModel.Showing -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = modifier,
            ) {
                if (viewModel.isScrubbing) {
                    TimestampText(viewModel.progressText)
                } else if (
                    !isLeftActionVisible || viewModel.left is MediaSecondaryActionViewModel.None
                ) {
                    Spacer(Modifier.size(width = 16.dp, height = 48.dp))
                } else {
                    SecondaryAction(
                        viewModel = viewModel.left,
                        modifier = Modifier.sysuiResTag(MediaRes.PREV_BTN),
                        element = Media.Elements.PrevButton,
                    )
                }

                val interactionSource = remember { MutableInteractionSource() }
                val colors =
                    colors(
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                        thumbColor = Color.White,
                    )
                if (isSeekBarVisible) {
                    // To allow the seek bar slider to fade in and out, it's tagged as an element.
                    Element(key = Media.Elements.SeekBarSlider, modifier = Modifier.weight(1f)) {
                        val sliderDragDelta = remember {
                            // Not a mutableStateOf - this is never accessed in composition and
                            // using an anonymous object avoids generics boxing of inline Offset.
                            object {
                                var value = Offset.Zero
                            }
                        }
                        val isEnabled = viewModel.onScrubChange != null
                        val velocityTracker = remember { VelocityTracker() }
                        val context = LocalContext.current
                        val flingVelocity =
                            remember(context) {
                                ViewConfiguration.get(context).scaledMinimumFlingVelocity * 10
                            }
                        var isDrag by remember { mutableStateOf(false) }
                        var isDragStartedOnThumb by remember { mutableStateOf(false) }
                        val currentProgress by rememberUpdatedState(viewModel.progress)
                        Slider(
                            interactionSource = interactionSource,
                            value = viewModel.progress,
                            enabled = isEnabled,
                            onValueChange = { progress ->
                                if (!isDrag || isDragStartedOnThumb) {
                                    viewModel.onScrubChange?.invoke(progress)
                                }
                            },
                            onValueChangeFinished = {
                                val velocity = velocityTracker.calculateVelocity().x
                                if (!isDrag || isDragStartedOnThumb) {
                                    viewModel.onScrubFinished?.invoke(
                                        sliderDragDelta.value,
                                        abs(velocity) < abs(flingVelocity),
                                    )
                                }

                                isDragStartedOnThumb = false
                                isDrag = false
                            },
                            colors = colors,
                            thumb = {
                                if (isEnabled) {
                                    SeekBarThumb(
                                        interactionSource = interactionSource,
                                        colors = colors,
                                    )
                                }
                            },
                            track = { sliderState ->
                                SeekBarTrack(
                                    sliderState = sliderState,
                                    isSquiggly = viewModel.isSquiggly,
                                    colors = colors,
                                    isThumbEnabled = isEnabled,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .semantics { stateDescription = viewModel.contentDescription }
                                    .pointerInput(Unit) {
                                        // Track and report the drag delta to the view-model so it
                                        // can decide whether to accept the next
                                        // onValueChangeFinished or reject it if the drag was overly
                                        // vertical.
                                        awaitPointerEventScope {
                                            var down: PointerInputChange? = null
                                            while (true) {
                                                val event =
                                                    awaitPointerEvent(PointerEventPass.Initial)

                                                event.changes.forEach { change ->
                                                    // Feed the velocity tracker to detect flings
                                                    velocityTracker.addPosition(
                                                        change.uptimeMillis,
                                                        change.position,
                                                    )
                                                }
                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        // A new gesture has begun.
                                                        // Record the initial down input change.
                                                        down = event.changes.last()
                                                        isDrag = false
                                                    }

                                                    PointerEventType.Move -> {
                                                        // The pointer has moved. If it's the same
                                                        // pointer as the latest down, calculate and
                                                        // report the drag delta.
                                                        val change = event.changes.last()

                                                        if (change.id == down?.id) {
                                                            if (!isDrag) {
                                                                val thumbX =
                                                                    size.width * currentProgress
                                                                // Add some forgiveness to hit
                                                                // target by 20dp radius
                                                                isDragStartedOnThumb =
                                                                    abs(down.position.x - thumbX) <
                                                                        20.dp.toPx()
                                                            }
                                                            isDrag = true
                                                            if (isDragStartedOnThumb) {
                                                                sliderDragDelta.value =
                                                                    change.position - down.position
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                        )
                    }
                }

                if (viewModel.isScrubbing) {
                    TimestampText(viewModel.durationText)
                } else if (
                    !isRightActionVisible || viewModel.right is MediaSecondaryActionViewModel.None
                ) {
                    Spacer(Modifier.size(width = 16.dp, height = 48.dp))
                } else {
                    SecondaryAction(
                        viewModel = viewModel.right,
                        modifier = Modifier.sysuiResTag(MediaRes.NEXT_BTN),
                        element = Media.Elements.NextButton,
                    )
                }
            }
        }

        is MediaNavigationViewModel.Hidden -> {
            if (!isLeftActionVisible || viewModel.left is MediaSecondaryActionViewModel.None) {
                Spacer(Modifier.size(width = 16.dp, height = 48.dp))
            } else {
                SecondaryAction(
                    viewModel = viewModel.left,
                    modifier = Modifier.sysuiResTag(MediaRes.PREV_BTN),
                    element = Media.Elements.PrevButton,
                )
            }
            if (!isRightActionVisible || viewModel.right is MediaSecondaryActionViewModel.None) {
                Spacer(Modifier.size(width = 16.dp, height = 48.dp))
            } else {
                SecondaryAction(
                    viewModel = viewModel.right,
                    modifier = Modifier.sysuiResTag(MediaRes.NEXT_BTN),
                    element = Media.Elements.NextButton,
                )
            }
        }
    }
}

/**
 * A bare seek bar (no navigation buttons on the sides), used by the Edith expanded media layout so
 * the slider can span the full width between the two timestamps. Behavior is identical to the seek
 * bar inside [Navigation].
 */
@Composable
private fun ContentScope.EdithSeekBar(
    viewModel: MediaNavigationViewModel.Showing,
    modifier: Modifier = Modifier,
) {
    val colors =
        colors(
            activeTrackColor = Color.White,
            inactiveTrackColor = Color.White.copy(alpha = 0.3f),
            thumbColor = Color.White,
        )
    if (viewModel.isIndeterminate) {
        // No known duration (e.g. a livestream): a full-width, non-interactive track. It animates
        // (squiggly) while playing and is static when paused. No thumb, no scrubbing.
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .semantics { stateDescription = viewModel.contentDescription }
        ) {
            SeekBarTrack(
                progress = { 1f },
                isSquiggly = viewModel.isSquiggly,
                colors = colors,
                isThumbEnabled = false,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            )
        }
        return
    }
    val interactionSource = remember { MutableInteractionSource() }
    val sliderDragDelta = remember {
        // Not a mutableStateOf - this is never accessed in composition and using an anonymous
        // object avoids generics boxing of inline Offset.
        object {
            var value = Offset.Zero
        }
    }
    val isEnabled = viewModel.onScrubChange != null
    val velocityTracker = remember { VelocityTracker() }
    val context = LocalContext.current
    val flingVelocity =
        remember(context) { ViewConfiguration.get(context).scaledMinimumFlingVelocity * 10 }
    var isDrag by remember { mutableStateOf(false) }
    var isDragStartedOnThumb by remember { mutableStateOf(false) }
    val currentProgress by rememberUpdatedState(viewModel.progress)
    Slider(
        interactionSource = interactionSource,
        value = viewModel.progress,
        enabled = isEnabled,
        onValueChange = { progress ->
            if (!isDrag || isDragStartedOnThumb) {
                viewModel.onScrubChange?.invoke(progress)
            }
        },
        onValueChangeFinished = {
            val velocity = velocityTracker.calculateVelocity().x
            if (!isDrag || isDragStartedOnThumb) {
                viewModel.onScrubFinished?.invoke(
                    sliderDragDelta.value,
                    abs(velocity) < abs(flingVelocity),
                )
            }

            isDragStartedOnThumb = false
            isDrag = false
        },
        colors = colors,
        thumb = {
            if (isEnabled) {
                SeekBarThumb(interactionSource = interactionSource, colors = colors)
            }
        },
        track = { sliderState ->
            SeekBarTrack(
                sliderState = sliderState,
                isSquiggly = viewModel.isSquiggly,
                colors = colors,
                isThumbEnabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .semantics { stateDescription = viewModel.contentDescription }
                .pointerInput(Unit) {
                    // Track and report the drag delta to the view-model so it can decide whether to
                    // accept the next onValueChangeFinished or reject it if the drag was overly
                    // vertical.
                    awaitPointerEventScope {
                        var down: PointerInputChange? = null
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)

                            event.changes.forEach { change ->
                                // Feed the velocity tracker to detect flings
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                            }
                            when (event.type) {
                                PointerEventType.Press -> {
                                    down = event.changes.last()
                                    isDrag = false
                                }

                                PointerEventType.Move -> {
                                    val change = event.changes.last()

                                    if (change.id == down?.id) {
                                        if (!isDrag) {
                                            val thumbX = size.width * currentProgress
                                            // Add some forgiveness to hit target by 20dp radius
                                            isDragStartedOnThumb =
                                                abs(down.position.x - thumbX) < 20.dp.toPx()
                                        }
                                        isDrag = true
                                        if (isDragStartedOnThumb) {
                                            sliderDragDelta.value =
                                                change.position - down.position
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
    )
}

/** Renders the thumb of the seek bar. */
@Composable
private fun SeekBarThumb(
    interactionSource: MutableInteractionSource,
    colors: SliderColors,
    modifier: Modifier = Modifier,
) {
    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    Spacer(
        modifier
            .size(width = 4.dp, height = 16.dp)
            .hoverable(interactionSource = interactionSource)
            .background(color = colors.thumbColor, shape = RoundedCornerShape(16.dp))
    )
}

/**
 * Renders the track of the seek bar.
 *
 * If [isSquiggly] is `true`, the part to the left of the thumb will animate a squiggly line that
 * oscillates up and down. The [waveLength] and [amplitude] control the geometry of the squiggle and
 * the [waveSpeedDpPerSec] controls the speed by which it seems to "move" horizontally.
 */
@Composable
private fun SeekBarTrack(
    sliderState: SliderState,
    isSquiggly: Boolean,
    colors: SliderColors,
    isThumbEnabled: Boolean,
    modifier: Modifier = Modifier,
    waveLength: Dp = 20.dp,
    amplitude: Dp = 3.dp,
    waveSpeedDpPerSec: Dp = 8.dp,
) {
    SeekBarTrack(
        // Read the slider value lazily inside the draw pass (see SeekBarTrack) so the track does
        // not recompose on every progress tick / scrub frame.
        progress = { sliderState.value },
        isSquiggly = isSquiggly,
        colors = colors,
        isThumbEnabled = isThumbEnabled,
        modifier = modifier,
        waveLength = waveLength,
        amplitude = amplitude,
        waveSpeedDpPerSec = waveSpeedDpPerSec,
    )
}

@Composable
private fun SeekBarTrack(
    @FloatRange(from = 0.0, to = 1.0) progress: () -> Float,
    isSquiggly: Boolean,
    colors: SliderColors,
    isThumbEnabled: Boolean,
    modifier: Modifier = Modifier,
    waveLength: Dp = 20.dp,
    amplitude: Dp = 3.dp,
    waveSpeedDpPerSec: Dp = 8.dp,
) {
    // Animating the amplitude allows the squiggle to gradually grow to its full height or shrink
    // back to a flat line as needed.
    val animatedAmplitude by
        animateDpAsState(
            targetValue = if (isSquiggly) amplitude else 0.dp,
            label = "SeekBarTrack.amplitude",
        )

    // This animates the horizontal movement of the squiggle.
    val animatedWaveOffset = remember { Animatable(0f) }

    LaunchedEffect(isSquiggly) {
        if (isSquiggly) {
            animatedWaveOffset.snapTo(0f)
            animatedWaveOffset.animateTo(
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = (1000 * (waveLength / waveSpeedDpPerSec)).toInt(),
                                easing = LinearEasing,
                            ),
                        repeatMode = RepeatMode.Restart,
                    ),
            )
        }
    }

    val path = remember { Path() }

    // Render the track.
    Canvas(modifier = modifier) {
        val thumbPositionPx = size.width * progress()
        val amplitudePx = amplitude.toPx()
        val animatedAmplitudePx = animatedAmplitude.toPx()
        val waveLengthPx = waveLength.toPx()
        val halfWaveLengthPx = waveLengthPx / 2

        val offsetPx = waveLengthPx * animatedWaveOffset.value
        val totalWidth = size.width + waveLengthPx
        val halfWaveCount = (totalWidth / halfWaveLengthPx).toInt()

        var currentX = 0f
        path.reset()
        path.apply {
            repeat(halfWaveCount + 3) { index ->
                // the position of either the peak or the trough.
                val centerX = currentX + (halfWaveLengthPx / 2)

                // We subtract offsetPx because we are shifting the whole path to the left by
                // offsetPx, in order to calculate the new point.
                val posDiff = centerX - (offsetPx + thumbPositionPx)
                val transitionRatio =
                    when {
                        posDiff <= 0 -> 1f // Active part
                        posDiff < waveLengthPx ->
                            1f - (posDiff / waveLengthPx) // Active -> Inactive
                        else -> 0f // Inactive part (flat)
                    }
                val calculatedAmplitude = animatedAmplitudePx * transitionRatio

                // Draw a half wave (either a hill or a valley shape starting and ending on
                // the horizontal center).
                relativeQuadraticTo(
                    // The control point for the bezier curve is on top of the peak of the
                    // hill or the very center bottom of the valley shape.
                    dx1 = halfWaveLengthPx / 2,
                    dy1 = if (index % 2 == 0) -calculatedAmplitude else calculatedAmplitude,
                    // Advance horizontally, half a wave length at a time.
                    dx2 = halfWaveLengthPx,
                    dy2 = 0f,
                )

                currentX += halfWaveLengthPx
            }
        }

        // To handle the change in colors, we use the same path twice.
        // Paths are shifted by offsetPx due to the translation to the left.
        translate(left = -offsetPx, top = 0f) {
            // Draw inactive path first.
            if (isThumbEnabled) {
                // The flat line after the thumb, if thumb is enabled.
                drawLine(
                    color = colors.inactiveTrackColor,
                    start = Offset(thumbPositionPx + offsetPx, 0f),
                    end = Offset(size.width + offsetPx, 0f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else {
                // Draw the full path (wave -> transition -> flat).
                clipRect(
                    left = offsetPx,
                    top = -amplitudePx * 2,
                    right = size.width + offsetPx,
                    bottom = amplitudePx * 2,
                ) {
                    drawPath(
                        path = path,
                        color = colors.inactiveTrackColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            // Draw the same path with active Color clipped to the thumb.
            clipRect(
                left = offsetPx,
                top = -amplitudePx * 2,
                right = thumbPositionPx + offsetPx,
                bottom = amplitudePx * 2,
            ) {
                drawPath(
                    path = path,
                    color = colors.activeTrackColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** Renders the internal "guts" of a card. */
@Composable
private fun CardGuts(
    viewModel: MediaCardGutsViewModel,
    colorScheme: AnimatedColorScheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier.pointerInput(Unit) {
                detectTapGestures(onTap = {}, onLongPress = { viewModel.onLongClick() })
            }
    ) {
        // Settings button.
        Icon(
            icon = checkNotNull(viewModel.settingsButton.icon),
            modifier =
                Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 16.dp).clickable {
                    viewModel.settingsButton.onClick()
                },
            tint = Color.White,
        )

        //  Content.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier.align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 32.dp, top = 16.dp),
        ) {
            Text(
                text = viewModel.text,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlatformButton(
                    onClick = viewModel.primaryAction.onClick,
                    modifier = Modifier.sysuiResTag(MediaRes.HIDE_BTN),
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                ) {
                    Text(
                        text = checkNotNull(viewModel.primaryAction.text),
                        color = colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 14.sp,
                    )
                }

                viewModel.secondaryAction?.let { button ->
                    PlatformOutlinedButton(
                        onClick = button.onClick,
                        border = BorderStroke(width = 1.dp, color = colorScheme.primary),
                    ) {
                        Text(
                            text = checkNotNull(button.text),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The app icon + "App on" part of the three-row context header. The output device control
 * (`EdithOutputControl`) is rendered separately as a sibling [Media.Elements.OutputControl] element
 * so it can morph (slide) between the two-row and three-row headers.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContentScope.EdithContextHeader(
    icon: Icon,
    iconTint: Color,
    appName: String,
    deviceName: String,
    modifier: Modifier = Modifier,
    onAppIconClick: (() -> Unit)? = null,
) {
    val headerDescription =
        stringResource(R.string.edith_media_context_header, appName, deviceName)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier.clearAndSetSemantics { contentDescription = headerDescription },
    ) {
        Icon(
            icon = icon,
            tint = iconTint,
            // Match the compressed (two-row) header icon: same size + circular clip, so the app
            // icon doesn't jump when the card cross-fades between the two scenes.
            modifier =
                Modifier.size(24.dp)
                    .clip(CircleShape)
                    .thenIf(onAppIconClick != null) {
                        Modifier.clickable(onClick = onAppIconClick!!)
                    },
        )
        // Gap after the app icon.
        Spacer(Modifier.width(6.dp))
        Text(
            text = appName,
            color = Color.White,
            style = MaterialTheme.typography.titleSmallEmphasized,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
        // Balanced seams around the "on" connector so it reads as one phrase without looking
        // over-spaced (uniform spacedBy) or cramped.
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.edith_media_on),
            color = Color.White,
            // The emphasized title-small family is baked at weight 600, so the connector uses the
            // regular (400) Google Sans text/Flex face to read as a light connector.
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

/** Resolves the display name for an output device chip (falls back to "This phone"). */
@Composable
private fun deviceNameFor(outputChip: MediaDeviceChipViewModel): String =
    outputChip.text?.takeIf { it.isNotBlank() }?.toString()
        ?: stringResource(com.android.settingslib.R.string.media_transfer_this_device_name)

/** The output device name (semibold, text only) + chevron, clickable to open the output switcher. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContentScope.EdithOutputControl(
    viewModel: MediaDeviceChipViewModel,
    modifier: Modifier = Modifier,
) {
    val deviceName = deviceNameFor(viewModel)
    val clickInteractionSource = remember { MutableInteractionSource() }
    val expandable = remember { Expandable() }
    Expandable(
        expandable = expandable,
        controller =
            rememberExpandableController(
                color = Color.Transparent,
                // The controller shape drives the output-switcher dialog's expand/collapse morph;
                // a rounded shape (matching DeviceChip) avoids closing into a rectangle.
                shape = RoundedCornerShape(12.dp),
            ),
        useModifierBasedImplementation = true,
        defaultMinSize = false,
        modifier = Modifier.wrapContentSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                modifier
                    .clickable(
                        interactionSource = clickInteractionSource,
                        indication = null,
                    ) {
                        viewModel.onClick(expandable)
                    }
                    .clearAndSetSemantics { contentDescription = deviceName },
        ) {
            Text(
                text = deviceName,
                color = Color.White,
                style = MaterialTheme.typography.titleSmallEmphasized,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
            // Trailing affordance: indicates the device name is clickable (opens the output
            // switcher). Decorative — the row's content description is the device name.
            Spacer(Modifier.width(2.dp))
            MediaOutputChevron()
        }
    }
}

/** The trailing chevron of the output control (decorative; the parent provides the semantics). */
@Composable
private fun MediaOutputChevron(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 18.dp,
) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/** Renders the metadata labels of a track. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContentScope.Metadata(
    title: String,
    subtitle: String,
    isExplicit: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // This element can be animated when switching between scenes inside a media card.
    Element(key = Media.Elements.Metadata, modifier = modifier) {
        // When the title and/or subtitle change, crossfade between the old and the new.
        Crossfade(targetState = title to subtitle, label = "Labels.crossfade") { (title, subtitle)
            ->
            Column {
                Text(
                    text = title,
                    modifier = Modifier.sysuiResTag(MediaRes.TITLE),
                    style =
                        MaterialTheme.typography.titleMediumEmphasized.copy(
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both,
                                ),
                        ),
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExplicit) {
                        Icon(
                            icon =
                                Icon.Resource(
                                    resId = R.drawable.ic_media_explicit_indicator,
                                    contentDescription = null,
                                ),
                            modifier =
                                Modifier.sysuiResTag(MediaRes.EXPLICIT_INDICATOR)
                                    .padding(end = 8.dp)
                                    .size(13.dp),
                            tint = color,
                        )
                    }

                    Text(
                        text = subtitle,
                        modifier = Modifier.sysuiResTag(MediaRes.ARTIST),
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                lineHeightStyle =
                                    LineHeightStyle(
                                        alignment = LineHeightStyle.Alignment.Center,
                                        trim = LineHeightStyle.Trim.Both,
                                    ),
                            ),
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceChip(
    viewModel: MediaDeviceChipViewModel,
    style: DeviceChipStyle,
    modifier: Modifier = Modifier,
) {
    // For accessibility reasons, the touch area for the chip needs to be at least 48dp in height.
    // At the same time, the rounded corner chip should only be as tall as it needs to be to contain
    // its contents and look like a nice design; also, the ripple effect should only be shown within
    // the bounds of the chip.
    //
    // This is achieved by sharing this InteractionSource between the outer and inner composables.
    //
    // The outer composable hosts that clickable that writes user events into the InteractionSource.
    // The inner composable consumes the user events from the InteractionSource and feeds them into
    // its indication.
    val clickInteractionSource = remember { MutableInteractionSource() }
    val expandable = remember { Expandable() }
    Box(
        modifier =
            modifier
                .padding(top = 16.dp, bottom = 0.dp)
                .heightIn(min = 48.dp)
                .clickable(interactionSource = clickInteractionSource, indication = null) {
                    viewModel.onClick(expandable)
                }
                .clearAndSetSemantics {
                    contentDescription = viewModel.text?.toString() ?: viewModel.defaultDescription
                }
    ) {
        Expandable(
            expandable = expandable,
            controller =
                rememberExpandableController(
                    color = style.fillColor,
                    shape = RoundedCornerShape(12.dp),
                ),
            useModifierBasedImplementation = false,
            defaultMinSize = false,
            modifier = Modifier.wrapContentSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.clip(RoundedCornerShape(12.dp))
                        .background(style.fillColor)
                        .thenIf(style.borderColor != null) {
                            Modifier.border(
                                width = 1.dp,
                                color = style.borderColor!!,
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                        .indication(clickInteractionSource, ripple())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                if (viewModel.isConnecting) {
                    CircularProgressIndicator(
                        color = style.contentColor,
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.dp,
                    )
                } else {
                    Icon(
                        icon = viewModel.icon,
                        tint = style.contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                AnimatedVisibility(visible = viewModel.text != null) {
                    rememberLastNonNull(viewModel.text)?.let {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = style.contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Renders the primary action of media controls: the play/pause button. */
@Composable
private fun ContentScope.PlayPauseAction(
    viewModel: MediaPlayPauseActionViewModel?,
    buttonColor: Color,
    iconColor: Color,
    buttonCornerRadius: (isPlaying: Boolean) -> Dp,
    modifier: Modifier = Modifier,
) {
    if (viewModel == null) return

    val buttonSize = DpSize(width = 64.dp, height = 44.dp)
    val cornerRadius: Dp by
        animateDpAsState(
            targetValue = buttonCornerRadius(viewModel.state != MediaSessionState.Paused),
            label = "PlayPauseAction.cornerRadius",
        )

    val scaleAnimatable = remember { Animatable(1.0f) }
    val scope = rememberCoroutineScope()

    // This element can be animated when switching between scenes inside a media card.
    Element(key = Media.Elements.PlayPauseButton, modifier = modifier) {
        PlatformButton(
            onClick = {
                viewModel.onClick?.let {
                    it()
                    scope.launch {
                        scaleAnimatable.animateTo(
                            targetValue = 1.125f,
                            animationSpec = tween(durationMillis = 250),
                        )
                        scaleAnimatable.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(durationMillis = 250),
                        )
                    }
                }
            },
            enabled = viewModel.onClick != null,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(cornerRadius),
            modifier = Modifier.size(buttonSize).scale(scaleAnimatable.value),
        ) {
            when (viewModel.state) {
                is MediaSessionState.Playing,
                is MediaSessionState.Paused -> {
                    val painterOrNull =
                        when (viewModel.icon) {
                            is Icon.Resource ->
                                rememberAnimatedVectorPainter(
                                    animatedImageVector =
                                        AnimatedImageVector.animatedVectorResource(
                                            id = viewModel.icon.resId
                                        ),
                                    atEnd = viewModel.state == MediaSessionState.Playing,
                                )
                            is Icon.Loaded -> rememberDrawablePainter(viewModel.icon.drawable)
                            null -> null
                        }
                    painterOrNull?.let { painter ->
                        Icon(
                            painter = painter,
                            contentDescription = viewModel.icon?.contentDescription?.load(),
                            tint = iconColor,
                            modifier = Modifier.size(22.dp).sysuiResTag(MediaRes.PLAY_PAUSE_BTN),
                        )
                    }
                }
                is MediaSessionState.Buffering -> {
                    CircularProgressIndicator(color = iconColor, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/**
 * Renders an icon button for an action that's not the play/pause action.
 *
 * If [element] is provided, the secondary action element will be able to animate when switching
 * between scenes inside a media card.
 */
@Composable
private fun ContentScope.SecondaryAction(
    viewModel: MediaSecondaryActionViewModel,
    modifier: Modifier = Modifier,
    resId: String = MediaRes.EMPTY_STRING,
    element: ElementKey? = null,
    iconColor: Color = Color.White,
    buttonSize: Dp = 48.dp,
    contentPadding: Dp = 13.dp,
    shape: Shape = IconButtonDefaults.standardShape,
    iconSize: Dp? = null,
) {
    if (viewModel !is MediaSecondaryActionViewModel.None && element != null) {
        Element(key = element, modifier = modifier) {
            SecondaryActionContent(
                viewModel = viewModel,
                iconColor = iconColor,
                resId = resId,
                buttonSize = buttonSize,
                contentPadding = contentPadding,
                shape = shape,
                iconSize = iconSize,
            )
        }
    } else {
        SecondaryActionContent(
            viewModel = viewModel,
            iconColor = iconColor,
            resId = resId,
            modifier = modifier,
            buttonSize = buttonSize,
            contentPadding = contentPadding,
            shape = shape,
            iconSize = iconSize,
        )
    }
}

/**
 * Renders additional action [elementIndex] in a fixed-width slot, or a spacer of the same width when
 * the action is absent, so the control bar's far slots always reserve equal width.
 */
private val FarSlotSize = 56.dp

/** Glyph size for the far additional-action slots; kept smaller than the slot so a big icon fits. */
private val FarSlotIconSize = 26.dp

@Composable
private fun ContentScope.SecondarySlot(
    viewModel: MediaSecondaryActionViewModel?,
    elementIndex: Int,
) {
    Box(modifier = Modifier.size(FarSlotSize), contentAlignment = Alignment.Center) {
        val action = viewModel as? MediaSecondaryActionViewModel.Action
        if (action != null) {
            // Key by the action's stable identity so Compose keeps each action bound to the same
            // slot across updates, instead of remapping (or reusing a stale binding) by index.
            key(action.stableKey ?: "action$elementIndex") {
                SecondaryAction(
                    viewModel = action,
                    resId = "action$elementIndex",
                    element = Media.Elements.additionalActionButton(elementIndex),
                    // Custom (far) icons are loaded bitmaps; use a slightly larger glyph than the
                    // center prev/next so they match visually. The outer button/slot keeps its full
                    // size; only the glyph is scaled down so a large icon isn't cropped.
                    // The rounded (non-circular) shape is applied only here, so the center
                    // prev/play/next buttons keep their stock circular clip.
                    buttonSize = FarSlotSize,
                    contentPadding = 12.dp,
                    shape = RoundedCornerShape(12.dp),
                    iconSize = FarSlotIconSize,
                )
            }
        }
    }
}

/** The content of a [SecondaryAction]. */
@Composable
private fun SecondaryActionContent(
    viewModel: MediaSecondaryActionViewModel,
    iconColor: Color,
    resId: String,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 48.dp,
    contentPadding: Dp = 13.dp,
    shape: Shape = IconButtonDefaults.standardShape,
    iconSize: Dp? = null,
) {
    val sharedModifier = modifier.size(buttonSize).padding(contentPadding)
    when (viewModel) {
        is MediaSecondaryActionViewModel.Action ->
            when (viewModel.icon) {
                is Icon.Resource ->
                    PlatformIconButton(
                        onClick = viewModel.onClick ?: {},
                        modifier = sharedModifier.sysuiResTag(resId),
                        enabled = viewModel.onClick != null,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = iconColor),
                        shape = shape,
                        iconResource = viewModel.icon.resId,
                        contentDescription = viewModel.icon.contentDescription?.load(),
                        iconSize = iconSize,
                    )

                is Icon.Loaded ->
                    PlatformIconButton(
                        onClick = viewModel.onClick ?: {},
                        modifier = sharedModifier.sysuiResTag(resId),
                        enabled = viewModel.onClick != null,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = iconColor),
                        shape = shape,
                        iconDrawable = viewModel.icon.drawable,
                        contentDescription = viewModel.icon.contentDescription?.load(),
                        iconSize = iconSize,
                    )
            }

        is MediaSecondaryActionViewModel.ReserveSpace -> Spacer(modifier = sharedModifier)

        is MediaSecondaryActionViewModel.None -> Unit
    }
}

@Composable
private fun TimestampText(text: String) {
    // No horizontal padding: the timestamp sits flush against the scrubber row's 16dp inset so its
    // start aligns with the track title above; spacing to the seek bar comes from the row's
    // Arrangement.spacedBy.
    Text(
        text = text,
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/**
 * Renders the revealed content on the sides of the horizontal pager.
 *
 * @param revealAmount A callback that can return the amount of revealing done. This value will be
 *   in a range slightly larger than `-1` to `+1` where `1` is fully revealed on the left-hand side,
 *   `-1` is fully revealed on the right-hand side, and `0` is not revealed at all. Numbers lower
 *   than `-1` or greater than `1` are possible when the overscroll effect adds additional pixels of
 *   offset.
 */
@Composable
private fun RevealedContent(
    viewModel: MediaSettingsButtonViewModel,
    revealAmount: () -> Float,
    modifier: Modifier = Modifier,
) {
    val horizontalPadding = 18.dp

    // This custom layout's purpose is only to place the icon in the center of the revealed content,
    // taking into account the amount of reveal.
    Layout(
        content = {
            val revealAlpha =
                revealAmount().let { if (it.isNaN()) 0f else abs(it).fastCoerceIn(0f, 1f) }
            Icon(
                icon = viewModel.icon,
                modifier =
                    Modifier.background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = revealAlpha),
                            shape = CircleShape,
                        )
                        .layoutId(Media.LayoutId.CardRevealedContent)
                        .size(48.dp)
                        .padding(12.dp)
                        .graphicsLayer {
                            alpha = revealAlpha
                            rotationZ = revealAmount() * 90
                        }
                        .clickable { viewModel.onClick() },
                tint = MaterialTheme.colorScheme.onSurface,
            )
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val placeable =
            measurables.singleton(Media.LayoutId.CardRevealedContent).measure(constraints)

        val totalWidth =
            min(horizontalPadding.roundToPx() * 2 + placeable.measuredWidth, constraints.maxWidth)

        layout(totalWidth, constraints.maxHeight) {
            coordinates?.size?.let { size ->
                val reveal = revealAmount()
                val x =
                    if (reveal >= 0f) {
                        ((size.width * abs(reveal)) - placeable.measuredWidth) / 2
                    } else {
                        size.width * (1 - abs(reveal) / 2) - placeable.measuredWidth / 2
                    }

                placeable.place(
                    x = x.fastRoundToInt(),
                    y = (size.height - placeable.measuredHeight) / 2,
                )
            }
        }
    }
}

/** Enumerates all supported media presentation styles. */
enum class MediaPresentationStyle {
    /** The "normal" 3-row carousel look. */
    Default,
    /** Similar to [Default] but with full height. Used in communal hub. */
    Large,
    /** Similar to [Default] but not as tall (2-row carousel look). */
    Compressed,
    /** A special single-row treatment that fits nicely in quick settings. */
    Compact,
    /** A minimal, non-interactive layout. Used for secondary cards in carousel view */
    Thumbnail,
}

data class MediaUiBehavior(
    val isCarouselDismissible: Boolean = true,
    /** If false, carousel will not be scrollable and will only show the first item */
    val isCarouselScrollingEnabled: Boolean = true,
    val carouselVisibility: MediaCarouselVisibility = MediaCarouselVisibility.WhenNotEmpty,
    /**
     * If provided, this callback will be consulted at the beginning of each carousel scroll gesture
     * to see if the falsing system thinks that it's a false touch. If it then returns `true`, the
     * scroll will be canceled.
     */
    val isCarouselScrollFalseTouch: (() -> Boolean)? = null,
)

@Stable
private interface AnimatedColorScheme {
    val primary: Color
    val onPrimary: Color
    val background: Color
}

private object MediaRes {
    const val EMPTY_STRING = ""
    const val BACKGROUND = "album_art"
    const val PLAY_PAUSE_BTN = "actionPlayPause"
    const val NEXT_BTN = "actionNext"
    const val PREV_BTN = "actionPrev"
    const val MEDIA_CAROUSEL_SCROLLER = "media_carousel_scroller"
    const val MEDIA_CAROUSEL = "media_carousel"
    const val MEDIA_CONTROLS = "qs_media_controls"
    const val OUTPUT_CHIP = "media_seamless"
    const val SUGGESTED_DEVICE_CHIP = "device_suggestion_button"
    const val TITLE = "header_title"
    const val ARTIST = "header_artist"
    const val EXPLICIT_INDICATOR = "media_explicit_indicator"
    const val HIDE_BTN = "dismiss"
}

object Media {

    /**
     * Scenes.
     *
     * The implementation uses a [SceneTransitionLayout] to smoothly animate transitions between
     * different card layouts. Each card layout is identified as its own "scene" and the STL
     * framework takes care of animating the layouts and their elements as the card morphs between
     * scenes.
     */
    object Scenes {
        /** The "normal" 3-row carousel look. */
        val Default = SceneKey("default")
        /** Similar to [Default] but with full height. Used in communal hub. */
        val Large = SceneKey("large")
        /** Similar to [Default] but not as tall (2-row carousel look). */
        val Compressed = SceneKey("compressed")
        /** A special single-row treatment that fits nicely in quick settings. */
        val Compact = SceneKey("compact")
        /** A minimal, non-interactive layout. Used for secondary cards in carousel view */
        val Thumbnail = SceneKey("thumbnail")
    }

    private fun TransitionBuilder.fadeAll() {
        fade(Elements.PlayPauseButton)
        fade(Elements.Metadata)
        fade(Elements.PrevButton)
        fade(Elements.NextButton)
        fade(Elements.SeekBarSlider)
        fade(Elements.OutputSwitcherButton)
        for (i in 0..5) {
            fade(Elements.additionalActionButton(i))
        }
    }

    /**
     * Fades the parts that exist in only one of the two-row ([Scenes.Compressed]) and three-row
     * ([Scenes.Default]) scenes, so they cross-fade during the morph instead of overlapping (the
     * STL composes both scene contents during a transition; only element-keyed content gets alpha).
     *
     * `fade` handles both directions (enter/leave), so this is shared by the two directions. Any
     * future content added to only one of these scenes must be element-keyed and listed here.
     *
     * The two-row header now mirrors the three-row one (app icon + device name + chevron, unkeyed)
     * so it needs no fade; only the three-row-only rows are listed.
     */
    private fun TransitionBuilder.fadeSceneSpecificElements() {
        fade(Elements.ContextHeader)
        fade(Elements.ScrubberRow)
    }

    /** Definitions of how scene changes are transition-animated. */
    val Transitions = transitions {
        from(Scenes.Default, to = Scenes.Compact) {}
        from(Scenes.Default, to = Scenes.Large) {}
        from(Scenes.Default, to = Scenes.Compressed) {
            fadeSceneSpecificElements()
            // Reverse of the Compressed->Default sequence: controls fade out, the seek bar leaves,
            // then the controls fade back in.
            fadeControlsForMorph(reverse = true)
        }
        from(Scenes.Compressed, to = Scenes.Default) {
            fadeSceneSpecificElements()
            // Controls (Prev/Play/Next) fade out first, then the seek bar appears, then the
            // controls fade back in. The out/in handoff comes from the drawn copy switching as the
            // transition's current content moves from Compressed to Default (the shared-element
            // morph is disabled for these keys so they fade instead of sliding).
            fadeControlsForMorph(reverse = false)
        }
        from(Scenes.Compact, to = Scenes.Compressed) { fade(Elements.SeekBarSlider) }
        from(Scenes.Thumbnail, to = Scenes.Default) { fadeAll() }
        from(Scenes.Thumbnail, to = Scenes.Compact) { fadeAll() }
        from(Scenes.Thumbnail, to = Scenes.Large) { fadeAll() }
        from(Scenes.Thumbnail, to = Scenes.Compressed) { fadeAll() }
    }

    /**
     * Fades (instead of morphing) the transport controls during the two-row <-> three-row morph:
     * the controls fade (out then back in, as the drawn copy switches from the from-content to the
     * to-content) first, and the seek bar (three-row only) fades in slightly later, so the effect
     * reads top-to-bottom. `sharedElement(enabled = false)` disables the shared-element morph for
     * these keys so they are drawn per the current content and cross-faded.
     *
     * Only ONE alpha transformation is allowed per element/content, so the controls get a single
     * `fade` (the out/in handoff comes from the drawn-copy switch, not from two fades).
     *
     * [reverse] mirrors the timing for the Default -> Compressed direction.
     */
    private fun TransitionBuilder.fadeControlsForMorph(reverse: Boolean) {
        // Must be top level (sharedElement() cannot be inside a fractionRange).
        sharedElement(Elements.PrevButton, enabled = false)
        sharedElement(Elements.NextButton, enabled = false)
        sharedElement(Elements.PlayPauseButton, enabled = false)

        val controls = if (reverse) 0.5f to 1f else 0f to 0.5f
        val seekBar = if (reverse) 0.4f to 0.7f else 0.3f to 0.6f

        fractionRange(start = controls.first, end = controls.second) {
            fade(Elements.PrevButton)
            fade(Elements.NextButton)
            fade(Elements.PlayPauseButton)
        }
        fractionRange(start = seekBar.first, end = seekBar.second) { fade(Elements.SeekBarSlider) }
        for (i in 0..5) {
            fade(Elements.additionalActionButton(i))
        }
    }

    /**
     * Element keys.
     *
     * Composables that are wrapped in [ContentScope.Element] with one of these as their `key`
     * parameter will automatically be picked up by the STL transition animation framework and will
     * be animated from their bounds in the original scene to their bounds in the destination scene.
     *
     * In addition, tagging such elements with a key allows developers to customize the transition
     * animations even further.
     */
    object Elements {
        val PlayPauseButton = ElementKey("play_pause")
        val Metadata = ElementKey("metadata")
        val PrevButton = ElementKey("prev")
        val NextButton = ElementKey("next")
        val SeekBarSlider = ElementKey("seek_bar_slider")
        val OutputSwitcherButton = ElementKey("output_switcher")
        val MediaCarousel = ElementKey("media_carousel")
        /** Three-row-only context header "(app) on (device)"; faded during the two<->three morph. */
        val ContextHeader = ElementKey("context_header")
        /**
         * The output device control (device name + chevron), shared between the two-row and
         * three-row headers so it morphs (slides) between its positions instead of cross-fading.
         */
        val OutputControl = ElementKey("output_control")
        /** Three-row-only scrubber row (timestamps + seek bar); faded during the morph. */
        val ScrubberRow = ElementKey("scrubber_row")

        fun additionalActionButton(index: Int): ElementKey {
            val name = "additional_action_$index"
            return ElementKey(debugName = name, identity = name)
        }
    }

    enum class LayoutId {
        CardForeground,
        CardGuts,
        CardRevealedContent,
    }

    enum class Location {
        QS,
        SHADE,
        LOCKSCREEN,
        COMMUNAL_HUB,
        STATUS_BAR_POPUP,
    }

    const val TAG = "Media"
    val DEBUG = Log.isLoggable(TAG, Log.DEBUG)

    /**
     * How much to over-scale the background art past the card bounds (see `CardBackground`), so
     * thin letterbox/border bars baked into the artwork's own pixels are cropped off the edges.
     * `1.05` shaves ~2.5% off each edge.
     */
    const val ART_OVER_CROP_SCALE = 1.05f

    /** Duration of the collapsed <-> expanded lock screen media cross-fade. */
    const val EXPAND_FADE_MILLIS = 220

    /**
     * Spring for the lock screen media expansion morph, shared by the tap-to-expand transition, the
     * drag release and the ambient backdrop, so the whole feature moves as one.
     *
     * A 500 ms spring with 40% bounce. For mass 1 and no initial velocity the physical spring is
     *
     *     stiffness = (2 * PI / durationSeconds)^2
     *     damping   = (1 - bounce) * 4 * PI / durationSeconds
     *
     * so `bounce = 0.4, duration = 500ms` gives `stiffness = (4 * PI)^2 ~= 157.91` and
     * `damping ~= 15.08`. Compose takes a damping ratio instead of a coefficient, and
     * `damping / (2 * sqrt(stiffness * mass))` collapses to `1 - bounce`, i.e. **0.6**; stiffness
     * carries over unchanged because both models are mass-normalised. That is ~12.6 rad/s, ~10%
     * overshoot, settling in roughly 500 ms.
     *
     * The values are written out here rather than read from `MaterialTheme.motionScheme`: the lock
     * screen is not wrapped in `PlatformTheme` (only the QS fragment is), so the ambient theme would
     * return the standard scheme instead of the expressive one.
     */
    const val MORPH_SPRING_STIFFNESS = 157.91f
    const val MORPH_SPRING_DAMPING = 0.6f

    val DEFAULT_HEIGHT = 176.dp
    val COMPRESSED_HEIGHT = 128.dp
    val COMPACT_HEIGHT = 80.dp

    /**
     * Natural (content) height of the Edith two-row ("Compressed") card, used as the QQS end of the
     * two-row <-> three-row height morph. The stock [COMPRESSED_HEIGHT] (128dp) is taller than the
     * Edith two-row content, so forcing it in QQS made the card noticeably taller than the
     * content-sized lock-screen card (which has no morph and is content-wrapped).
     */
    val EDITH_COMPRESSED_HEIGHT = 104.dp

    /**
     * Height of the Edith three-row (expanded) card, used as the expanded end of the two-row <->
     * three-row height morph. Larger than the stock [DEFAULT_HEIGHT] (176dp) so the Edith three-row
     * content (bigger context header + the scrollable rows + the 56dp control bar) fits without the
     * card clipping the control buttons at the bottom.
     */
    val EDITH_DEFAULT_HEIGHT = 204.dp
}

private fun MediaPresentationStyle.toScene(): SceneKey {
    return when (this) {
        MediaPresentationStyle.Default -> Media.Scenes.Default
        MediaPresentationStyle.Large -> Media.Scenes.Large
        MediaPresentationStyle.Compressed -> Media.Scenes.Compressed
        MediaPresentationStyle.Compact -> Media.Scenes.Compact
        MediaPresentationStyle.Thumbnail -> Media.Scenes.Thumbnail
    }
}

/** Allows to set the maxWidth constraint as a fractional value. */
private fun Modifier.fractionalMaxWidth(containerMaxWidth: Int, fraction: Float): Modifier {
    return layout { measurable, constraints ->
        val placeable =
            measurable.measure(
                constraints.copy(
                    maxWidth =
                        min((containerMaxWidth * fraction).fastRoundToInt(), constraints.maxWidth)
                )
            )

        layout(placeable.measuredWidth, placeable.measuredHeight) { placeable.place(0, 0) }
    }
}

@Composable
fun <T> rememberLastNonNull(value: T?): T? {
    val ref = remember { Ref<T?>() }
    ref.value = value ?: ref.value
    return ref.value
}

private data class DeviceChipStyle(
    val fillColor: Color,
    val contentColor: Color,
    val borderColor: Color? = null,
)
