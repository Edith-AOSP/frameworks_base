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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.animation.scene.ContentScope
import com.android.compose.theme.LocalAndroidColorScheme
import com.android.systemui.qs.panels.ui.compose.DragType
import com.android.systemui.qs.panels.ui.compose.EditTileListState
import com.android.systemui.qs.panels.ui.compose.infinitegrid.AnimatedAvailableTilesGrid
import com.android.systemui.qs.panels.ui.compose.infinitegrid.AutoSelectTiles
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CommonTileDefaults
import com.android.systemui.qs.panels.ui.compose.infinitegrid.CurrentTilesGrid
import com.android.systemui.qs.panels.ui.compose.infinitegrid.EditAction
import com.android.systemui.qs.panels.ui.compose.infinitegrid.EditModeExpandableTopBar
import com.android.systemui.qs.panels.ui.compose.infinitegrid.RemoveButton
import com.android.systemui.qs.panels.ui.compose.infinitegrid.TopBarSubtitle
import com.android.systemui.qs.panels.ui.compose.selection.rememberSelectionState
import com.android.systemui.qs.panels.ui.viewmodel.EditTopBarActionViewModel
import com.android.systemui.qs.panels.ui.viewmodel.QuickActionsGridViewModel
import com.android.systemui.qs.panels.ui.viewmodel.QSColumnsViewModel
import com.android.systemui.qs.panels.ui.viewmodel.TileViewModel
import com.android.systemui.qs.pipeline.shared.TileSpec
import com.android.systemui.res.R

/**
 * Dedicated editor for the "Quick Actions" grid.
 *
 * Uses the same edit-grid layout as the stock QS editor: the current Quick Actions grid plus an
 * AOSP-categorized list of all available tiles. Tiles can be added by tapping their "+" badge or by
 * dragging them into the grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsEdit(
    viewModel: QuickActionsEditViewModel,
    modifier: Modifier = Modifier,
) {
    val currentTiles by viewModel.currentTiles.collectAsStateWithLifecycle()
    val allTiles by viewModel.tileCatalog.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()

    BackHandler { viewModel.backToLanding() }

    val selectionState = rememberSelectionState()
    val listState =
        remember {
            EditTileListState(
                currentTiles,
                emptySet(),
                columns = QuickActionsGridViewModel.COLUMNS,
                largeTilesSpan = 1,
            )
        }
    LaunchedEffect(currentTiles) { listState.updateTiles(currentTiles, emptySet()) }

    AutoSelectTiles(listState, selectionState)

    val onEditAction: (EditAction) -> Unit = { action ->
        when (action) {
            is EditAction.AddTile -> viewModel.addTile(action.tileSpec)
            is EditAction.InsertTile -> viewModel.addTile(action.tileSpec, action.position)
            is EditAction.RemoveTile -> viewModel.removeTile(action.tileSpec)
            is EditAction.SetTiles -> viewModel.setOrder(action.tileSpecs)
            EditAction.ResetGrid -> viewModel.showResetDialog()
            is EditAction.ResizeTile -> Unit // Not resizable.
        }
    }

    // Don't allow removing tiles once at the minimum (keep at least 2 tiles).
    val isTileRemovable: (TileSpec) -> Boolean = { currentTiles.size > MIN_QUICK_ACTION_TILES }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        modifier =
            modifier
                .consumeWindowInsets(WindowInsets.displayCutout)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            EditModeExpandableTopBar(
                onStopEditing = viewModel::backToLanding,
                subtitle = { expanded: Boolean ->
                    if (expanded) {
                        TopBarSubtitle(
                            listState,
                            selectionState,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                modifier = Modifier.statusBarsPadding(),
                scrollBehavior = scrollBehavior,
                collapsibleActions =
                    remember { mutableStateListOf<EditTopBarActionViewModel>() },
                actions = {
                    AnimatedVisibility(visible = canUndo, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(
                            enabled = canUndo,
                            onClick = { viewModel.undo() },
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    containerColor =
                                        LocalAndroidColorScheme.current.surfaceEffect1,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                        ) {
                            Icon(
                                com.android.systemui.common.ui.icons.Undo,
                                contentDescription =
                                    stringResource(R.string.quick_settings_edit_undo),
                            )
                        }
                    }

                    RemoveButton(
                        enabled = selectionState.selection?.let(isTileRemovable) ?: false
                    ) {
                        selectionState.selection?.let { currentSelection ->
                            listState
                                .findNeighboringTile(currentSelection)
                                ?.let(selectionState::select)
                            viewModel.removeTile(currentSelection)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            CurrentTilesGrid(
                listState = listState,
                selectionState = selectionState,
                onEditAction = onEditAction,
                labelTiles = true,
            )
            AnimatedAvailableTilesGrid(
                allTiles = allTiles,
                listState = listState,
                selectionState = selectionState,
                showAvailableTiles =
                    !(listState.dragInProgress || selectionState.placementEnabled) ||
                        listState.dragType == DragType.Move,
                canLayoutTile = true,
                onEditAction = onEditAction,
                availableColumns = QSColumnsViewModel.EDITH_COLUMNS,
                edithTileStyle = true,
            )
        }
    }
}

/**
 * Landing screen for the Edith QS editor.
 *
 * Vertically scrollable, with a header, the Quick Actions section (2x2 wide tiles), the Toggles &
 * Shortcuts section (5x4 grid of small tiles) and a bottom action bar (Reset / Done). Each section
 * has a "+" button that opens the corresponding editor.
 */
@Composable
fun ContentScope.EdithEditLanding(
    quickActionsGridViewModel: QuickActionsGridViewModel,
    quickActionsSpecs: List<TileSpec>,
    qsTiles: List<TileViewModel>,
    onEditQuickActions: () -> Unit,
    onEditQsTiles: () -> Unit,
    onReset: () -> Unit,
    onStopEditing: () -> Unit,
    modifier: Modifier = Modifier,
    edithColorEnabled: Boolean = false,
) {
    val quickActionTiles =
        quickActionsGridViewModel.tileViewModels(
            quickActionsSpecs,
            QuickActionsGridViewModel.TILE_COUNT,
        )

    BackHandler { onStopEditing() }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Header(onReset = onReset, onDone = onStopEditing)

            EditSectionCard(
                title = stringResource(R.string.quick_settings_edit_quick_actions_title),
                onAdd = onEditQuickActions,
            ) {
                if (quickActionTiles.isEmpty()) {
                    EmptySlot()
                } else {
                    RealTilePreview(
                        tiles = quickActionTiles,
                        columns = QuickActionsGridViewModel.COLUMNS,
                        pillShapes = true,
                    )
                }
            }

            EditSectionCard(
                title = stringResource(R.string.quick_settings_edit_toggles_shortcuts_title),
                onAdd = onEditQsTiles,
            ) {
                if (qsTiles.isEmpty()) {
                    EmptySlot()
                } else {
                    RealTilePreview(
                        tiles = qsTiles,
                        columns = QSColumnsViewModel.EDITH_COLUMNS,
                        iconOnly = true,
                        edithColorEnabled = edithColorEnabled,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(onReset: () -> Unit, onDone: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.qs_edit_tiles),
                style = MaterialTheme.typography.headlineLargeEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.quick_settings_edit_subtitle),
                style = MaterialTheme.typography.bodyMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.End,
        ) {
            FilledTonalButton(
                onClick = onReset,
                shape = RoundedCornerShape(percent = 50),
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(stringResource(R.string.quick_settings_edit_reset))
            }
            Button(
                onClick = onDone,
                shape = RoundedCornerShape(percent = 50),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(stringResource(R.string.quick_settings_edit_done))
            }
        }
    }
}

@Composable
private fun EditSectionCard(
    title: String,
    onAdd: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(
                onClick = onAdd,
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp),
            ) {
                Icon(
                    Icons.Outlined.AddCircleOutline,
                    contentDescription = stringResource(
                        R.string.quick_settings_edit_quick_actions_add
                    ),
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp),
                    )
                    .padding(12.dp)
        ) {
            content()
        }
    }
}

private const val MIN_QUICK_ACTION_TILES = 2

@Composable
private fun EmptySlot() {
    Box(
        modifier = Modifier.fillMaxWidth().height(CommonTileDefaults.TileHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.quick_settings_edit_quick_actions_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

