package com.example.recording.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.recording.R
import com.example.recording.ui.RecordingUiState

/**
 * Recording controls with a dialog that prompts the user to save the recording
 * after stopping it.
 */

@Composable
fun RecordingControlsSection(
    uiState: RecordingUiState,
    onStartRecording: (thresholdDb: Double) -> Unit,
    onStopRecording: () -> Unit,
    onSaveRecording: (fileName: String) -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    if (!uiState.isRecording) {
        Button(onClick = { onStartRecording.invoke(60.0) }) {
            Text(stringResource(R.string.start_recording))
        }
    } else {
        Button(onClick = {
            onStopRecording.invoke()
            showSaveDialog = true
        }) {
            Text("Stop Recording")
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(stringResource(R.string.current_db_1f).format(uiState.currentDb))
    if (uiState.hasNoiseWarning) {
        Text(
            text = stringResource(R.string.noise_threshold_exceeded),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }

    if (showSaveDialog) {
        SaveRecordingDialog(
            onSave = { fileName ->
                onSaveRecording.invoke(fileName)
                showSaveDialog = false
            },
            onDismiss = {
                showSaveDialog = false
            }
        )
    }
}