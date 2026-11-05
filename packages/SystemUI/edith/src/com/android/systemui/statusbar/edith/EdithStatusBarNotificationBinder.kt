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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.Configuration
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.android.systemui.display.dagger.SystemUIDisplaySubcomponent.DisplayAware
import com.android.systemui.display.dagger.SystemUIDisplaySubcomponent.PerDisplaySingleton
import com.android.systemui.lifecycle.repeatWhenAttached
import com.android.systemui.plugins.DarkIconDispatcher
import com.android.systemui.res.R
import com.android.systemui.statusbar.edith.EdithStatusBarNotificationStyleRepository.Companion.MODE_ICONS
import com.android.systemui.statusbar.edith.EdithStatusBarNotificationStyleRepository.Companion.MODE_NUMBER
import com.android.systemui.statusbar.notification.icon.domain.interactor.StatusBarNotificationIconsInteractor
import com.android.systemui.statusbar.phone.PhoneStatusBarView
import com.android.systemui.statusbar.pipeline.shared.ui.viewmodel.HomeStatusBarViewModel
import com.android.systemui.statusbar.policy.Clock
import com.android.systemui.statusbar.policy.ConfigurationController
import javax.inject.Inject
import kotlinx.coroutines.flow.combine

/**
 * Binds the Edith clock pill/count to a [PhoneStatusBarView].
 *
 * The stock clock view is moved into [EdithClockPill] without changing its identity, so the stock
 * dark-icon tint and visibility collectors keep working. The pill draws nothing in icons/none mode,
 * keeping the status bar byte-for-byte stock there; number mode adds the pill chrome, detaches the
 * clock from the wallpaper tint and shows the count badge.
 */
@PerDisplaySingleton
class EdithStatusBarNotificationBinder
@Inject
constructor(
    @DisplayAware private val darkIconDispatcher: DarkIconDispatcher,
    private val configurationController: ConfigurationController,
    private val styleRepository: EdithStatusBarNotificationStyleRepository,
    private val notificationIconsInteractor: StatusBarNotificationIconsInteractor,
) {
    private var boundView: PhoneStatusBarView? = null
    private var pill: EdithClockPill? = null
    private var clock: Clock? = null
    private var clockTintRemoved = false
    private var clockColorAnimator: ValueAnimator? = null
    private var mode = MODE_ICONS
    private var count = 0
    private var clockVisible = true
    private var initialized = false

    fun bind(view: PhoneStatusBarView, viewModel: HomeStatusBarViewModel) {
        if (boundView === view) return
        boundView = view

        val clock = view.requireViewById<Clock>(R.id.clock)
        this.clock = clock
        val parent = clock.parent as ViewGroup
        val index = parent.indexOfChild(clock)
        val pill = EdithClockPill(view.context)
        parent.removeView(clock)
        pill.attachClock(clock)
        parent.addView(
            pill,
            index,
            LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
                .apply { gravity = Gravity.CENTER_VERTICAL },
        )
        this.pill = pill
        pill.visibility = View.GONE

        view.repeatWhenAttached {
            val configurationListener =
                object : ConfigurationController.ConfigurationListener {
                    override fun onConfigChanged(newConfig: Configuration) {
                        apply(animate = false)
                    }

                    override fun onUiModeChanged() {
                        apply(animate = false)
                    }

                    override fun onThemeChanged() {
                        apply(animate = false)
                    }

                    override fun onDensityOrFontScaleChanged() {
                        apply(animate = false)
                    }
                }
            configurationController.addCallback(configurationListener)
            try {
                combine(
                        styleRepository.mode,
                        notificationIconsInteractor.statusBarNotifs,
                        viewModel.isClockVisible,
                    ) { mode, notifications, visible ->
                        Triple(mode, notifications.size, visible.visibility == View.VISIBLE)
                    }
                    .collect { (mode, count, visible) ->
                        this@EdithStatusBarNotificationBinder.mode = mode
                        this@EdithStatusBarNotificationBinder.count = count
                        clockVisible = visible
                        apply(animate = initialized)
                        initialized = true
                    }
            } finally {
                configurationController.removeCallback(configurationListener)
            }
        }
    }

    private fun apply(animate: Boolean) {
        val pill = pill ?: return
        val clock = clock ?: return
        val context = pill.context
        val surface = context.getColor(com.android.internal.R.color.materialColorSurfaceContainerHigh)
        val onSurface = context.getColor(com.android.internal.R.color.materialColorOnSurface)
        val primary = context.getColor(com.android.internal.R.color.materialColorPrimary)
        val onPrimary = context.getColor(com.android.internal.R.color.materialColorOnPrimary)

        pill.setColors(surface, onSurface, primary, onPrimary)
        pill.setPillVisible(clockVisible)

        // The pill chrome only exists with a notification to count. Without one the clock keeps its
        // stock wallpaper tint and the pill background/padding disappear entirely.
        val showPill = mode == MODE_NUMBER && count > 0
        if (showPill) {
            val start = clock.currentTextColor
            // Idempotent: also covers a re-attach that re-registered the clock with the tint
            // dispatcher while the pill was showing.
            darkIconDispatcher.removeDarkReceiver(clock)
            if (!clockTintRemoved) {
                clockTintRemoved = true
                animateClockColor(clock, start, onSurface, animate)
            } else if (clockColorAnimator == null) {
                // Theme/configuration reapply, or a no-op count change while already showing.
                clock.setTextColor(onSurface)
            }
        } else if (clockTintRemoved) {
            val start = clock.currentTextColor
            // Adding the receiver immediately re-applies the current wallpaper tint.
            darkIconDispatcher.addDarkReceiver(clock)
            val target = clock.currentTextColor
            clockTintRemoved = false
            animateClockColor(clock, start, target, animate)
        }

        pill.apply(mode, count, animate)
    }

    private fun animateClockColor(clock: Clock, start: Int, target: Int, animate: Boolean) {
        clockColorAnimator?.removeAllListeners()
        clockColorAnimator?.cancel()
        clockColorAnimator = null
        if (!animate || start == target) {
            clock.setTextColor(target)
            return
        }
        clockColorAnimator =
            ValueAnimator.ofArgb(start, target).apply {
                duration = COLOR_FADE_MS
                addUpdateListener { clock.setTextColor(it.animatedValue as Int) }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            clock.setTextColor(target)
                            clockColorAnimator = null
                        }
                    }
                )
                start()
            }
    }

    private companion object {
        const val COLOR_FADE_MS = 220L
    }
}
