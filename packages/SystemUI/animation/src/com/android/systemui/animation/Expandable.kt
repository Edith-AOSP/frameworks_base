/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.systemui.animation

import android.content.ComponentName
import android.view.View

/**
 * A long-lived coordinator for a logical transition.
 *
 * This class manages a [TransitionSource] UI components that correspond to the same logical target.
 * Its primary role is to resolve the "best" active [TransitionSource] from its set when an
 * animation is requested.
 *
 * This object is designed to be long-lived (e.g., held in a ViewModel or Interactor) to survive
 * configuration changes, while the [TransitionSource] it manages are short-lived and tied to the
 * View/Composable lifecycle.
 */
class Expandable(
    // TODO(b/412899675) remove this once flag rolls out.
    val transitionSources: MutableSet<TransitionSource> = mutableSetOf()
) {

    constructor(transitionSource: TransitionSource) : this() {
        transitionSources.add(transitionSource)
    }

    // Hide the source if the associated launch is visible
    private var isSourceVisible: Boolean = true

    private fun updateSourceVisibility(sourceVisible: Boolean) {
        isSourceVisible = sourceVisible
        transitionSources.forEach { source -> source.isSourceVisible = sourceVisible }
    }

    /**
     * Registers a [TransitionSource] with this coordinator. This should be called when the source
     * UI component becomes active (e.g., attached to window, composed, placed).
     *
     * @param source The [TransitionSource] to add.
     */
    fun addSource(source: TransitionSource) {
        source.isSourceVisible = isSourceVisible
        source.onSourceVisibilityChanged = this::updateSourceVisibility
        transitionSources.add(source)
    }

    /**
     * Unregisters a [TransitionSource] from this coordinator. This should be called when the source
     * UI component becomes inactive (e.g., detached from window, disposed, unplaced) to prevent
     * memory leaks.
     *
     * @param source The [TransitionSource] to remove.
     */
    fun removeSource(source: TransitionSource) {
        transitionSources.remove(source)
    }

    private fun requiredTransitionSource(): TransitionSource? {
        // A TransitionSource is only registered while its source UI is attached and laid out (see
        // addSource/removeSource and the layout-aware registration in the Compose implementation).
        // A caller can therefore legitimately request a transition while no source is currently
        // registered (e.g. the tile is temporarily unplaced during a transition, or the source is
        // owned by a View-based tile whose registration the framework does not control). In that
        // case there is no on-screen geometry to animate from, so there is no morph to run and we
        // return null. This mirrors the rest of the framework, which treats a missing/unattached
        // source as "skip the animation" rather than a fatal error (see
        // DialogTransitionAnimator.Controller.fromView, which returns null when the view is not
        // attached, and show(resolveController = ...), which falls back to dialog.show()). The
        // dynamicTargetResolution flag only selects which transition controller is resolved, it
        // does not change whether a missing source is tolerated.
        return transitionSources.firstOrNull()
    }

    /**
     * Create a [DialogTransitionAnimator.Controller] that can be used to expand this [Expandable]
     * into a Dialog, or return `null` if this [Expandable] should not be animated (e.g. if it is
     * currently not attached or visible).
     */
    fun dialogTransitionController(cuj: DialogCuj? = null): DialogTransitionAnimator.Controller? {
        val controller = requiredTransitionSource()?.dialogTransitionController(cuj) ?: return null
        return object : DialogTransitionAnimator.Controller by controller {
            override val dialogIdentity: Any
                get() = this@Expandable
        }
    }

    fun activityTransitionController(
        launchCujType: Int? = null
    ): ActivityTransitionAnimator.Controller? {
        return activityTransitionController(
            launchCujType,
            cookie = null,
            component = null,
            returnCujType = null,
            isEphemeral = true,
        )
    }

    /**
     * Create an [ActivityTransitionAnimator.Controller] that can be used to expand this
     * [Expandable] into an Activity, or return `null` if this [Expandable] should not be animated
     * (e.g. if it is currently not attached or visible).
     *
     * @param launchCujType The CUJ type from the [com.android.internal.jank.InteractionJankMonitor]
     *   associated to the launch that will use this controller.
     * @param cookie The unique cookie associated with the launch that will use this controller.
     *   This is required iff a return animation should be included.
     * @param component The name of the activity that will be launched by this controller. This is
     *   required for long-lived registrations only.
     * @param returnCujType The CUJ type from the [com.android.internal.jank.InteractionJankMonitor]
     *   associated to the return animation that will use this controller.
     */
    fun activityTransitionController(
        launchCujType: Int? = null,
        cookie: ActivityTransitionAnimator.TransitionCookie? = null,
        component: ComponentName? = null,
        returnCujType: Int? = null,
        isEphemeral: Boolean = true,
    ): ActivityTransitionAnimator.Controller? {
        return requiredTransitionSource()
            ?.activityTransitionController(
                launchCujType,
                cookie,
                component,
                returnCujType,
                isEphemeral,
            )
    }

    companion object {

        @JvmStatic
        fun fromView(view: View): Expandable {
            val transitionSource = TransitionSource.fromView(view)
            // TODO(b/412899675): make the addition lifecycle aware
            val expandable = Expandable(transitionSource)
            return expandable
        }
    }
}
