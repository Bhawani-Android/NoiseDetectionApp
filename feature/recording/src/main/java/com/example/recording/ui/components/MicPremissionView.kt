package com.example.recording.ui.components

import android.Manifest
import androidx.compose.runtime.Composable
import com.example.recording.ui.RecordingUiState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MicPermissionView(
    uiState: RecordingUiState,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onSaveRecording: (fileName: String) -> Unit
) {
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val isMicGranted = recordAudioPermissionState.status.isGranted
    if (!isMicGranted) {
        PermissionRequestSection {
            recordAudioPermissionState.launchPermissionRequest()
        }
    } else {
        RecordingControlsSection(
            uiState = uiState,
            onStartRecording = {onStartRecording.invoke()},
            onStopRecording = {onStopRecording.invoke()},
            onSaveRecording = {onSaveRecording.invoke(it)}
        )
    }
}
