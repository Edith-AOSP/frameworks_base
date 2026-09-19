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
import androidx.compose.ui.text.AnnotatedString
import com.android.systemui.common.shared.model.Icon
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.qs.panels.domain.interactor.EditTilesListInteractor
import com.android.systemui.qs.panels.domain.interactor.EditTilesResetInteractor
import com.android.systemui.qs.panels.ui.dialog.QSResetDialogDelegate
import com.android.systemui.qs.panels.ui.viewmodel.EditTileViewModel
import com.android.systemui.qs.panels.ui.viewmodel.UnloadedEditTileViewModel
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.qs.shared.model.TileCategory
import com.android.systemui.shade.ShadeDisplayAware
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * View model for the dedicated "Quick Actions" editing screen.
 *
 * The screen lets the user pick up to [QuickActionsRepository.MAX_SPECS] tiles for the fixed 2x2
 * Quick Actions grid. Tap a tile in the bottom-sheet drawer to add it; tap a tile in the grid to
 * remove it.
 */
@SysUISingleton
class QuickActionsEditViewModel
@Inject
constructor(
    @Application private val applicationScope: CoroutineScope,
    @Background private val backgroundDispatcher: CoroutineDispatcher,
    @ShadeDisplayAware private val context: Context,
    private val quickActionsInteractor: QuickActionsInteractor,
    private val editTilesListInteractor: EditTilesListInteractor,
    private val resetInteractor: EditTilesResetInteractor,
    private val resetDialogDelegateFactory: QSResetDialogDelegate.Factory,
) {
    private val resetDialogDelegate by lazy {
        resetDialogDelegateFactory.create {
            // Reset both the Quick Actions grid and the main QS tiles.
            quickActionsInteractor.resetToDefaults()
            resetInteractor.reset()
            clearUndo()
        }
    }

    /** Shows the confirmation dialog to reset both Quick Actions and QS tiles. */
    fun showResetDialog() {
        resetDialogDelegate.showDialog()
    }

    private val _zone = MutableStateFlow(EditZone.None)

    /** History of previous Quick Actions lists, used to implement undo. */
    private val undoStack = mutableListOf<List<TileSpec>>()

    private val _canUndo = MutableStateFlow(false)

    /** Whether there is an edit that can be undone. */
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private fun recordForUndo() {
        undoStack.add(quickActionsInteractor.specs.value)
        if (undoStack.size > UNDO_LIMIT) undoStack.removeAt(0)
        _canUndo.value = undoStack.isNotEmpty()
    }

    /**
     * Applies [newSpecs] to the Quick Actions list, recording the previous value for undo only when
     * the list actually changes.
     */
    private fun commit(newSpecs: List<TileSpec>) {
        if (newSpecs == quickActionsInteractor.specs.value) return
        recordForUndo()
        quickActionsInteractor.setSpecs(newSpecs)
    }

    /** Reverts the last Quick Actions edit, if any. */
    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        _canUndo.value = undoStack.isNotEmpty()
        quickActionsInteractor.setSpecs(previous)
    }

    /** Clears the undo history. */
    private fun clearUndo() {
        undoStack.clear()
        _canUndo.value = false
    }

    /** Which editing zone is currently shown. */
    val zone: StateFlow<EditZone> = _zone.asStateFlow()

    /** Whether any zone of the Edith editor is open. */
    val isEditing: StateFlow<Boolean> =
        _zone
            .map { it != EditZone.None }
            .stateIn(applicationScope, SharingStarted.WhileSubscribed(), false)

    /** Loaded metadata for every tile that can be added to Quick Actions, sorted by label. */
    val tileCatalog: StateFlow<List<EditTileViewModel>> =
        _zone
            .flatMapLatest { zone ->
                if (zone != EditZone.None) flow { emit(loadAllTiles()) } else flowOf(emptyList())
            }
            .flowOn(backgroundDispatcher)
            .stateIn(applicationScope, SharingStarted.WhileSubscribed(), emptyList())

    /** The tiles currently in the Quick Actions grid, in order. */
    val currentTiles: StateFlow<List<EditTileViewModel>> =
        combine(quickActionsInteractor.specs, tileCatalog) { specs, tiles ->
            val bySpec = tiles.associateBy { it.tileSpec }
            // Every spec in the list must have an entry so it can be shown and reordered; if the
            // catalog doesn't know a spec, fall back to a minimal placeholder.
            specs.map { bySpec[it] ?: it.toPlaceholderEditTile() }
        }
            .stateIn(applicationScope, SharingStarted.WhileSubscribed(), emptyList())

    /** The ordered specs currently in the Quick Actions grid. */
    val currentSpecs: StateFlow<List<TileSpec>> = quickActionsInteractor.specs

    /** Available tiles that are not currently in the Quick Actions grid. */
    val availableTiles: StateFlow<List<EditTileViewModel>> =
        combine(quickActionsInteractor.specs, tileCatalog) { specs, tiles ->
            tiles.filterNot { it.tileSpec in specs }
        }
            .stateIn(applicationScope, SharingStarted.WhileSubscribed(), emptyList())

    /**
     * Whether there are no more tiles that can be added to the Quick Actions grid.
     *
     * There is no longer a fixed cap: edit mode shows all added tiles in a flexible grid. The value
     * is `true` when every available tile is already in the list.
     */
    val isFull: StateFlow<Boolean> =
        availableTiles
            .map { it.isEmpty() }
            .stateIn(applicationScope, SharingStarted.WhileSubscribed(), false)

    /** Resets the Quick Actions grid to its default tiles. */
    fun reset() {
        commit(QuickActionsRepository.defaultSpecs)
    }

    fun startEditing() {
        clearUndo()
        _zone.value = EditZone.Landing
    }

    fun stopEditing() {
        _zone.value = EditZone.None
    }

    /** Opens the editor for the given [zone] (e.g. from the landing screen's + button). */
    fun openZone(zone: EditZone) {
        _zone.value = zone
    }

    /** Returns to the landing screen (the two-zone overview). */
    fun backToLanding() {
        _zone.value = EditZone.Landing
    }

    /** Adds [spec] to the end of the Quick Actions list. */
    fun addTile(spec: TileSpec) {
        val current = quickActionsInteractor.specs.value
        if (spec in current) return
        commit(current + spec)
    }

    /** Inserts [spec] at [position] in the Quick Actions list. */
    fun addTile(spec: TileSpec, position: Int) {
        val current = quickActionsInteractor.specs.value.toMutableList()
        val existing = current.indexOf(spec)
        if (existing != -1) current.removeAt(existing)
        current.add(position.coerceIn(0, current.size), spec)
        commit(current)
    }

    /** Replaces the Quick Actions list with [specs] (used to apply a reorder). */
    fun setOrder(specs: List<TileSpec>) {
        commit(specs)
    }

    /** Removes [spec] from the Quick Actions grid. */
    fun removeTile(spec: TileSpec) {
        val current = quickActionsInteractor.specs.value
        if (spec !in current) return
        if (current.size <= MIN_TILES) return
        commit(current - spec)
    }

    /** Moves the tile at [fromIndex] to [toIndex] in the Quick Actions grid. */
    fun moveTile(fromIndex: Int, toIndex: Int) {
        quickActionsInteractor.moveSpec(fromIndex, toIndex)
    }

    private suspend fun loadAllTiles(): List<EditTileViewModel> {
        val model = editTilesListInteractor.getTilesToEdit()
        val currentSpecs = quickActionsInteractor.specs.value.toSet()
        return withContext(backgroundDispatcher) {
            (model.stockTiles + model.customTiles)
                .filterNot { it.category == TileCategory.UNKNOWN }
                .map { data ->
                    UnloadedEditTileViewModel(
                            data.tileSpec,
                            data.icon,
                            data.label,
                            data.appName,
                            data.appIcon,
                            /* isCurrent= */ data.tileSpec in currentSpecs,
                            /* isDualTarget= */ false,
                            /* availableEditActions= */ emptySet(),
                            data.category,
                        )
                        .load(context)
                }
                .sortedBy { it.label.text.toString().lowercase() }
        }
    }
}

/** The editing zones shown by the Edith QS editor. */
enum class EditZone {
    /** Not editing. */
    None,
    /** Two-zone overview: Quick Actions and QS tiles, each with a "+" button. */
    Landing,
    /** Editing the Quick Actions grid. */
    QuickActions,
    /** Editing the main QS tile grid (stock editor). */
    QsTiles,
}

private const val UNDO_LIMIT = 20
private const val MIN_TILES = 2

/** Minimal edit-tile entry for a spec that is not present in the loaded catalog. */
private fun TileSpec.toPlaceholderEditTile(): EditTileViewModel =
    EditTileViewModel(
        tileSpec = this,
        icon = Icon.Resource(android.R.drawable.ic_menu_help, null),
        label = AnnotatedString(spec),
        inlinedLabel = null,
        appName = null,
        appIcon = null,
        isCurrent = false,
        isDualTarget = false,
        availableEditActions = emptySet(),
        category = TileCategory.UNKNOWN,
    )
