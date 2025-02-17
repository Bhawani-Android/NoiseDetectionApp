package com.example.recording.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recording.RecordingViewModel
import com.example.recording.ui.components.MicPermissionView
import com.example.recording.ui.components.PlaybackSectionScreen
import com.example.recording.ui.components.RecordingScreenBody
import com.example.recording.ui.components.RecordingScreenHeader

/**
 * Main screen composable for recording and playback.
 *
 * Note: In a real app this will work fine; if you’re testing this in a preview,
 * you may need to use collectAsState() instead.
 */
@Composable
fun RecordingScreenComposable(
    viewModel: RecordingViewModel,
    modifier: Modifier = Modifier
) {
    // At runtime, this extension uses the LifecycleOwner from the current context.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        RecordingScreenHeader()
        MicPermissionView(
            uiState = uiState,
            onStartRecording = { viewModel.startRecording(thresholdDb = 60.0) },
            onStopRecording = {viewModel.stopRecording()},
            onSaveRecording = {viewModel.saveRecording(fileName = it)}
        )

        Divider(Modifier.padding(vertical = 16.dp))

        PlaybackSectionScreen(uiState, viewModel)
        RecordingScreenBody(
            recordings = uiState.recordings,
            deleteRecording = { rec -> viewModel.deleteRecording(rec) },
            playRecording = { rec -> viewModel.playRecording(rec) }
        )
    }
}
