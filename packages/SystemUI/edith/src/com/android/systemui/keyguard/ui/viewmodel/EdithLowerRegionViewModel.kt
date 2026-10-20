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

package com.android.systemui.keyguard.ui.viewmodel

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.getValue
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.keyguard.ui.composable.elements.UnfoldTranslations
import com.android.systemui.lifecycle.HydratedActivatable
import com.android.systemui.qs.edith.EdithLockscreenMediaExpansionInteractor
import com.android.systemui.statusbar.notification.domain.interactor.ActiveNotificationsInteractor
import com.android.systemui.statusbar.notification.shared.ActiveNotificationModel
import com.android.systemui.statusbar.notification.shelf.domain.interactor.NotificationShelfInteractor
import com.android.systemui.unfold.domain.interactor.UnfoldTransitionInteractor
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.map

class EdithLowerRegionViewModel
@AssistedInject
constructor(
    unfoldTransitionInteractor: UnfoldTransitionInteractor,
    activeNotificationsInteractor: ActiveNotificationsInteractor,
    expansionInteractor: EdithLockscreenMediaExpansionInteractor,
    private val shelfInteractor: NotificationShelfInteractor,
    @Application private val context: Context,
) : HydratedActivatable() {

    val unfoldTranslations: UnfoldTranslations =
        object : UnfoldTranslations {
            override val start: Float by
                unfoldTransitionInteractor
                    .unfoldTranslationX(isOnStartSide = true)
                    .hydratedStateOf(traceName = "unfoldTranslations.start", initialValue = 0f)

            override val end: Float by
                unfoldTransitionInteractor
                    .unfoldTranslationX(isOnStartSide = false)
                    .hydratedStateOf(traceName = "unfoldTranslations.end", initialValue = 0f)
        }

    /**
     * The notifications Quick Quick Settings shows: the top-level entries minus the silent and
     * ambient ones, which only appear in the fully expanded shade. The chip must not count
     * notifications the user cannot see there.
     */
    private val shownNotifications: List<ActiveNotificationModel> by
        activeNotificationsInteractor.topLevelRepresentativeNotifications
            .map { models -> models.filter { !it.isSilent && !it.isAmbient } }
            .hydratedStateOf(traceName = "shownNotifications", initialValue = emptyList())

    /** How many notifications the chip summarises; hidden at zero. */
    val notificationCount: Int
        get() = shownNotifications.size

    /**
     * Background colour of the first shown notification when it asks to be colorized (a red
     * screen-recording notification, for example), so its circle matches the notification card.
     */
    val firstNotificationColor: Int?
        get() = shownNotifications.firstOrNull()?.backgroundColor

    /**
     * The first app icons of [shownNotifications], in stack order: the first one is drawn large,
     * the rest as the small row.
     *
     * The icon is the one the notification stack/shelf draws ([ActiveNotificationModel.shelfIcon]);
     * the compact status-bar chip view is only a fallback for entries that do not have one.
     */
    val notificationIcons: List<Drawable>
        get() =
            shownNotifications
                .mapNotNull { model ->
                    model.shelfIcon?.loadDrawable(context)
                        ?: model.statusBarChipIconView?.drawable
                }
                .take(MAX_CHIP_ICONS)

    /** Whether the expanded media player (and therefore the notification chip) is showing. */
    val isMediaExpanded: Boolean by
        expansionInteractor.isExpandedFlow.hydratedStateOf(
            traceName = "isMediaExpanded",
            initialValue = false,
        )

    /** Opens the locked shade, like the notification shelf does. */
    fun openNotifications() {
        shelfInteractor.goToLockedShadeFromShelf()
    }

    @AssistedFactory
    interface Factory {
        fun create(): EdithLowerRegionViewModel
    }

    private companion object {
        /** One in the ring plus three small ones. */
        const val MAX_CHIP_ICONS = 4
    }
}
