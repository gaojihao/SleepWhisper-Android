package com.lizhi1026.sleepwhisper.features.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.lizhi1026.sleepwhisper.core.visualkit.LocalSWScheme
import com.lizhi1026.sleepwhisper.core.visualkit.SWColor
import com.lizhi1026.sleepwhisper.core.visualkit.SWScheme

// Shared bottom-sheet wrapper for the white-noise picker. Forces DARK scheme +
// dark container so the popup looks identical whether launched from Home (light
// aurora bg) or Sleeping (dark immersive bg). ModalBottomSheet's default M3
// drag handle is kept — PlayerScreen's internal DragHandle is gated off when
// embedded.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SWColor.surface(SWScheme.DARK)
    ) {
        CompositionLocalProvider(LocalSWScheme provides SWScheme.DARK) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayerScreen(embedded = true)
            }
        }
    }
}
