package com.mobile.core.designsystem.components.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mobile.core.designsystem.theme.BottomSheetShape
import com.mobile.core.designsystem.theme.GoZipoTheme
import com.mobile.core.designsystem.theme.extended

@Composable
fun SheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.extended.surfaceOutline,
                    shape = CircleShape
                )
        )
    }
}

/**
 * The ride screens' layout: a full screen map with a draggable sheet on top.
 * [mapContent] fills the screen behind the sheet; [mapOverlay] draws floating
 * controls (back button, location card) above the map.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoZipoMapSheetScaffold(
    sheetContent: @Composable ColumnScope.() -> Unit,
    mapContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: BottomSheetScaffoldState = rememberBottomSheetScaffoldState(),
    sheetPeekHeight: Dp = 240.dp,
    sheetSwipeEnabled: Boolean = true,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    mapOverlay: @Composable BoxScope.() -> Unit = {},
) {
    val extended = MaterialTheme.colorScheme.extended
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        sheetShape = BottomSheetShape,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContentColor = extended.textPrimary,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = { SheetDragHandle() },
        sheetSwipeEnabled = sheetSwipeEnabled,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = extended.surfaceLower,
        sheetContent = {
            Column(modifier = Modifier.navigationBarsPadding()) {
                sheetContent()
            }
        },
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            mapContent()
            mapOverlay()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoZipoModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit,
) {
    val extended = MaterialTheme.colorScheme.extended
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = BottomSheetShape,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = extended.textPrimary,
        scrimColor = extended.overlay,
        dragHandle = { SheetDragHandle() },
        modifier = modifier,
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun GoZipoMapSheetScaffoldPreview() {
    GoZipoTheme(darkTheme = true) {
        GoZipoMapSheetScaffold(
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.extended.surfaceLower)
                )
            },
            sheetContent = {
                Text(
                    text = "Driver is arriving in ~5 min",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
}
