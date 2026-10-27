/*
 * Copyright (C) 2026 Edith AOSP
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

package com.android.systemui.statusbar.edith

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.transition.AutoTransition
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.android.systemui.statusbar.policy.Clock
import kotlin.math.roundToInt

/**
 * The status bar clock, optionally wrapped in a Material pill with a notification-count badge.
 *
 * The pill chrome is only present while there is at least one status bar notification: with no
 * notifications the clock looks exactly like the stock clock. The first notification fades the pill
 * background in and scales the badge up; the last notification fades both away. Count changes move
 * only the number inside the badge, never the badge itself.
 */
class EdithClockPill(context: Context) : LinearLayout(context) {

    private val density = resources.displayMetrics.density
    private val countText =
        TextView(context).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            includeFontPadding = false
            isSingleLine = true
        }
    private val countBadge =
        FrameLayout(context).apply {
            minimumWidth = dp(18)
            minimumHeight = dp(18)
            setPadding(dp(4), 0, dp(4), 0)
            clipChildren = false
            clipToPadding = false
            visibility = GONE
            addView(
                countText,
                FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER,
                    )
                    .apply { gravity = Gravity.CENTER },
            )
        }

    private var clock: Clock? = null
    private var mode = MODE_ICONS
    private var count = 0
    private var surfaceColor = 0
    private var onSurfaceColor = 0
    private var primaryColor = 0
    private var onPrimaryColor = 0
    private var pillBackground: GradientDrawable? = null
    private var countBackground: GradientDrawable? = null
    private var backgroundAnimator: ValueAnimator? = null
    private var pendingEnterAnimation = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        clipChildren = false
        clipToPadding = false
        addView(
            countBadge,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dp(4)
                gravity = Gravity.CENTER_VERTICAL
            },
        )
        countBadge.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            countBackground?.cornerRadius = countBadge.height.coerceAtLeast(dp(18)) / 2f
        }
    }

    fun attachClock(clock: Clock) {
        this.clock = clock
        addView(
            clock,
            0,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_VERTICAL
            },
        )
    }

    fun setColors(surface: Int, onSurface: Int, primary: Int, onPrimary: Int) {
        surfaceColor = surface
        onSurfaceColor = onSurface
        primaryColor = primary
        onPrimaryColor = onPrimary
        if (mode == MODE_NUMBER && count > 0) {
            pillBackground?.setColor(surfaceColor)
            countBackground?.setColor(primaryColor)
            countText.setTextColor(onPrimaryColor)
            background = pillBackground
        }
    }

    fun setPillVisible(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.GONE
        maybeReplayEnterAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        maybeReplayEnterAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            maybeReplayEnterAnimation()
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) {
            maybeReplayEnterAnimation()
        }
    }

    /**
     * Applies a new mode/count. [animate] is false for the first bind and for configuration
     * re-applies, so opening the lock screen or rotating does not replay the entrance.
     */
    fun apply(mode: Int, count: Int, animate: Boolean) {
        val wasShowing = this.mode == MODE_NUMBER && this.count > 0
        val show = mode == MODE_NUMBER && count > 0
        val previousCount = this.count
        this.mode = mode
        this.count = count

        when {
            show && !wasShowing -> {
                if (animate) beginEnterTransition()
                showPillAndBadge(animate = animate)
            }
            show -> updateCount(previousCount, count, animate = animate && previousCount > 0)
            wasShowing -> {
                if (animate) beginExitTransition()
                hidePillAndBadge(animate = animate)
            }
            else -> hidePillAndBadge(animate = false)
        }
    }

    private fun showPillAndBadge(animate: Boolean) {
        cancelBackgroundAnimator()
        pendingEnterAnimation = false
        ensurePillBackground()
        setPaddingRelative(dp(8), dp(2), dp(4), dp(2))
        pillBackground?.setColor(surfaceColor)

        val canAnimate = animate && isShown
        if (animate && !canAnimate) {
            // The mode changed while the status bar/window was hidden. Keep the final state and
            // replay the entrance the next time the pill is actually visible.
            pendingEnterAnimation = true
        }
        pillBackground?.alpha = if (canAnimate) 0 else 255
        background = pillBackground
        animate().cancel()
        scaleX = if (canAnimate) PILL_START_SCALE else 1f
        scaleY = if (canAnimate) PILL_START_SCALE else 1f

        ensureCountBackground()
        countText.text = countLabel(count)
        countText.setTextColor(onPrimaryColor)
        countBadge.animate().cancel()
        countText.animate().cancel()
        countText.translationY = 0f
        countBadge.alpha = if (canAnimate) 0f else 1f
        countBadge.scaleX = if (canAnimate) BADGE_START_SCALE else 1f
        countBadge.scaleY = if (canAnimate) BADGE_START_SCALE else 1f
        countBadge.visibility = VISIBLE

        if (canAnimate) {
            animatePillIn()
            animateBadgeIn()
            animateBackgroundAlpha(target = 255)
        }
    }

    private fun hidePillAndBadge(animate: Boolean) {
        pendingEnterAnimation = false
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        if (!animate || !isShown) {
            cancelBackgroundAnimator()
            background = null
            pillBackground?.alpha = 0
            setPaddingRelative(0, 0, 0, 0)
            countBadge.animate().cancel()
            countText.animate().cancel()
            countBadge.visibility = GONE
            return
        }

        countBadge.animate().cancel()
        countBadge.visibility = GONE
        setPaddingRelative(0, 0, 0, 0)
        animateBackgroundAlpha(target = 0)
    }

    private fun maybeReplayEnterAnimation() {
        if (!pendingEnterAnimation || !isShown) return
        if (mode != MODE_NUMBER || count <= 0) return
        pendingEnterAnimation = false

        cancelBackgroundAnimator()
        ensurePillBackground()
        ensureCountBackground()
        pillBackground?.alpha = 0
        background = pillBackground
        countText.text = countLabel(count)
        countText.setTextColor(onPrimaryColor)
        countText.translationY = 0f
        countBadge.animate().cancel()
        countBadge.alpha = 0f
        countBadge.scaleX = BADGE_START_SCALE
        countBadge.scaleY = BADGE_START_SCALE
        countBadge.visibility = VISIBLE
        animate().cancel()
        scaleX = PILL_START_SCALE
        scaleY = PILL_START_SCALE
        animatePillIn()
        animateBadgeIn()
        animateBackgroundAlpha(target = 255)
    }

    private fun animatePillIn() {
        animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(PILL_SCALE_MS)
            .setInterpolator(OvershootInterpolator(1.1f))
            .start()
    }

    private fun animateBadgeIn() {
        countBadge.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(BADGE_SCALE_MS)
            .setInterpolator(OvershootInterpolator())
            .start()
    }

    private fun updateCount(previousCount: Int, count: Int, animate: Boolean) {
        val label = countLabel(count)
        val widthChanged = countText.text.length != label.length
        if (animate && widthChanged && isAttachedToWindow) {
            beginBoundsTransition()
        }
        countText.text = label

        if (animate && previousCount > 0 && previousCount != count) {
            // Move the number only: the circular background stays put.
            countText.animate().cancel()
            countText.translationY = 0f
            val offset = (if (count > previousCount) -NUDGE_DP else NUDGE_DP) * density
            countText.animate()
                .translationY(offset)
                .setDuration(NUDGE_OUT_MS)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    countText.animate()
                        .translationY(0f)
                        .setDuration(NUDGE_BACK_MS)
                        .setInterpolator(OvershootInterpolator())
                        .start()
                }
                .start()
        }
    }

    private fun ensurePillBackground() {
        if (pillBackground == null) {
            pillBackground =
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                }
        }
    }

    private fun ensureCountBackground() {
        if (countBackground == null) {
            countBackground =
                GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(9).toFloat()
                }
            countBadge.background = countBackground
        }
        countBackground?.setColor(primaryColor)
        countBackground?.cornerRadius = countBadge.height.coerceAtLeast(dp(18)) / 2f
    }

    private fun cancelBackgroundAnimator() {
        backgroundAnimator?.removeAllListeners()
        backgroundAnimator?.cancel()
        backgroundAnimator = null
    }

    private fun animateBackgroundAlpha(target: Int) {
        cancelBackgroundAnimator()
        val drawable = pillBackground ?: return
        if (!isAttachedToWindow) {
            drawable.alpha = target
            if (target == 0) background = null
            return
        }
        backgroundAnimator =
            ValueAnimator.ofInt(drawable.alpha, target).apply {
                duration = BACKGROUND_FADE_MS
                interpolator = DecelerateInterpolator()
                addUpdateListener { drawable.alpha = it.animatedValue as Int }
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: android.animation.Animator) {
                            drawable.alpha = target
                            if (target == 0) {
                                background = null
                            }
                            backgroundAnimator = null
                        }
                    }
                )
                start()
            }
    }

    private fun beginBoundsTransition() {
        if (!isAttachedToWindow) return
        val transitionRoot = parent as? ViewGroup ?: return
        TransitionManager.beginDelayedTransition(
            transitionRoot,
            ChangeBounds().setDuration(TRANSITION_MS),
        )
    }

    private fun beginEnterTransition() {
        // A plain bounds transition; the badge fades/scales itself. AutoTransition would delay the
        // badge's fade-in until after the bounds step, hiding the scale animation. A small
        // overshoot gives the new pill a bouncy expansion instead of a linear resize.
        if (!isAttachedToWindow) return
        val transitionRoot = parent as? ViewGroup ?: return
        TransitionManager.beginDelayedTransition(
            transitionRoot,
            ChangeBounds()
                .setDuration(ENTER_TRANSITION_MS)
                .setInterpolator(OvershootInterpolator(0.5f)),
        )
    }

    private fun beginExitTransition() {
        if (!isAttachedToWindow) return
        val transitionRoot = parent as? ViewGroup ?: return
        TransitionManager.beginDelayedTransition(
            transitionRoot,
            AutoTransition().setDuration(TRANSITION_MS),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        pillBackground?.cornerRadius = h / 2f
        countBackground?.cornerRadius = countBadge.height.coerceAtLeast(dp(18)) / 2f
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        pillBackground?.cornerRadius = height / 2f
        countBackground?.cornerRadius = countBadge.height.coerceAtLeast(dp(18)) / 2f
    }

    private fun countLabel(value: Int): String = if (value > 99) "99+" else value.toString()

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private companion object {
        const val TRANSITION_MS = 220L
        const val ENTER_TRANSITION_MS = 260L
        const val BACKGROUND_FADE_MS = 220L
        const val BADGE_SCALE_MS = 240L
        const val BADGE_START_SCALE = 0.6f
        const val PILL_START_SCALE = 0.94f
        const val PILL_SCALE_MS = 260L
        const val NUDGE_DP = 3f
        const val NUDGE_OUT_MS = 90L
        const val NUDGE_BACK_MS = 220L
    }
}

private const val MODE_ICONS = 0
private const val MODE_NUMBER = 1
