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

package com.android.systemui.qs.edith

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.qs.flags.QsSplitInternetTile
import com.android.systemui.qs.panels.data.repository.QSPreferencesRepository
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.settings.UserFileManager
import com.android.systemui.user.data.repository.UserRepository
import com.android.systemui.util.kotlin.SharedPreferencesExt.observe
import com.android.systemui.util.kotlin.emitOnStart
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Repository for the user-editable list of [TileSpec] shown in the Edith Quick Actions grid.
 *
 * The list is per-user and stored as an ordered, comma-separated string in the same preferences
 * file used by the rest of the QS user preferences ([QSPreferencesRepository.FILE_NAME]).
 *
 * The list can hold any number of tiles; the live Quick Actions grid only shows the first
 * [MAX_SPECS] of them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@SysUISingleton
class QuickActionsRepository
@Inject
constructor(
    private val userFileManager: UserFileManager,
    private val userRepository: UserRepository,
    @Background private val backgroundDispatcher: CoroutineDispatcher,
) {
    /** Ordered list of [TileSpec] shown in the Quick Actions grid, for the current user. */
    val specs: Flow<List<TileSpec>> =
        userRepository.selectedUserInfo
            .flatMapLatest { userInfo ->
                val prefs = getSharedPrefs(userInfo.id)
                prefs.observe().emitOnStart().map { prefs.getOrSeedQuickActionSpecs() }
            }
            .flowOn(backgroundDispatcher)

    /** Replaces the Quick Actions list for the current user with [specs]. */
    fun writeQuickActionSpecs(specs: List<TileSpec>) {
        val sanitized = specs.distinct().filterNot { it is TileSpec.Invalid }
        getSharedPrefs(userRepository.getSelectedUserInfo().id).writeQuickActionSpecs(sanitized)
    }

    private fun SharedPreferences.writeQuickActionSpecs(specs: List<TileSpec>) {
        edit { putString(QUICK_ACTION_SPECS_KEY, specs.joinToString(SEPARATOR) { it.spec }) }
    }

    /**
     * Returns the stored specs, seeding (and persisting) the [defaultSpecs] the first time so the
     * stored list is always authoritative and user edits reliably persist.
     */
    private fun SharedPreferences.getOrSeedQuickActionSpecs(): List<TileSpec> {
        if (!contains(QUICK_ACTION_SPECS_KEY)) {
            writeQuickActionSpecs(defaultSpecs)
            return defaultSpecs
        }
        val stored = getString(QUICK_ACTION_SPECS_KEY, null).orEmpty()
        return stored
            .split(SEPARATOR)
            .map(TileSpec::create)
            .filterNot { it is TileSpec.Invalid }
            .distinct()
    }

    private fun getSharedPrefs(userId: Int): SharedPreferences {
        return userFileManager.getSharedPreferences(
            QSPreferencesRepository.FILE_NAME,
            Context.MODE_PRIVATE,
            userId,
        )
    }

    companion object {
        /** Number of tiles shown in the live 2x2 Quick Actions grid (the stored list can be longer). */
        const val MAX_SPECS = 4

        private const val QUICK_ACTION_SPECS_KEY = "quick_action_specs"
        private const val SEPARATOR = ","

        /**
         * Default specs used until the user customizes the list.
         *
         * When the internet tile is split, WiFi and Mobile data are separate tiles. Otherwise they
         * are combined into a single "internet" tile.
         */
        val defaultSpecs: List<TileSpec>
            get() =
                if (QsSplitInternetTile.isEnabled) {
                    listOf(
                        TileSpec.create("wifi"),
                        TileSpec.create("cell"),
                        TileSpec.create("bt"),
                        TileSpec.create("hotspot"),
                    )
                } else {
                    listOf(
                        TileSpec.create("internet"),
                        TileSpec.create("bt"),
                        TileSpec.create("hotspot"),
                        TileSpec.create("airplane"),
                    )
                }
    }
}
