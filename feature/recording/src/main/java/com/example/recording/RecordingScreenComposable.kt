package com.example.recording

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.entity.AudioEntity
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Formats milliseconds to mm:ss
 */
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreenComposable(
    viewModel: RecordingViewModel,
    modifier: Modifier = Modifier
) {
    // Observe the UI state from the ViewModel
    val uiState = viewModel.uiState.collectAsState().value

    // Permissions for RECORD_AUDIO
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val isMicGranted = recordAudioPermissionState.status.isGranted

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        /** Title **/
        Text(
            text = "Noise Detection Recorder",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        /**
         * Section 1: Microphone permission request (if not granted).
         * Otherwise, show Start/Stop Recording controls.
         */
        if (!isMicGranted) {
            PermissionRequestSection(
                onRequestPermission = {
                    recordAudioPermissionState.launchPermissionRequest()
                }
            )
        } else {
            RecordingControlsSection(uiState, viewModel)
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        /**
         * Section 2: If we have a recorded file, show playback & noise reduction UI,
         * plus an indicator for the current playback position vs. total duration.
         */
        uiState.recordedAudio?.let { audio ->
            PlaybackSection(uiState, viewModel, audio)
        } ?: run {
            Text(
                text = "No Audio Recorded Yet",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        /**
         * Section 3 (Future): List of previously recorded files.
         * For now, we show a placeholder or commented out code.
         *
         * If you had a list of recordings in your state (uiState.recordings),
         * you could do:
         */

//        if (uiState.recordings.isNotEmpty()) {
//            Text(
//                text = "Your Recordings",
//                style = MaterialTheme.typography.titleLarge,
//                modifier = Modifier.padding(bottom = 8.dp)
//            )
//            LazyColumn {
//                items(uiState.recordings) { audio ->
//                    RecordingItemCard(audio = audio, onPlay = { ... }, onDelete = { ... })
//                }
//            }
//        }
    }
}

/**
 * Simple composable to request microphone permission.
 */
@Composable
fun PermissionRequestSection(onRequestPermission: () -> Unit) {
    Text(
        text = "Microphone permission is required to record audio.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Button(onClick = onRequestPermission) {
        Text("Request Microphone Permission")
    }
}

/**
 * Start/Stop recording UI controls.
 */
@Composable
fun RecordingControlsSection(
    uiState: RecordingUiState,
    viewModel: RecordingViewModel
) {
    if (!uiState.isRecording) {
        Button(onClick = { viewModel.startRecording(thresholdDb = 60.0) }) {
            Text("Start Recording")
        }
    } else {
        Button(onClick = { viewModel.stopRecording() }) {
            Text("Stop Recording")
        }
    }

    // If you want to show the decibel + noise warning while recording, or always:
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Current dB: %.1f".format(uiState.currentDb),
        style = MaterialTheme.typography.bodyMedium
    )
    if (uiState.hasNoiseWarning) {
        Text(
            text = "Noise threshold exceeded!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Playback, time indicator, noise reduction, and delete UI.
 */
@Composable
fun PlaybackSection(
    uiState: RecordingUiState,
    viewModel: RecordingViewModel,
    audio: AudioEntity
) {
    // Show file path or name
    Text(
        text = "Recorded Audio:\n${audio.filePath}",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    // Time indicator row: mm:ss / mm:ss
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(
            text = formatTime(uiState.currentPositionMs),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = " / ",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatTime(uiState.totalDurationMs),
            style = MaterialTheme.typography.bodyMedium
        )
    }

    // Playback, Noise Reduction, Delete
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isPlaying) {
            Button(onClick = { viewModel.playAudio(false) }) {
                Text("Pause")
            }
        } else {
            Button(onClick = { viewModel.playAudio(true) }) {
                Text("Play")
            }
        }

        Button(onClick = { viewModel.reduceNoise() }) {
            Text("Reduce Noise")
        }

        Button(onClick = { viewModel.deleteAudio() }) {
            Text("Delete")
        }
    }
}

/**
 * Future: show an individual recording in a Card layout if we had a list of them.
 */
@Composable
fun RecordingItemCard(
    audio: AudioEntity,
    onPlay: (AudioEntity) -> Unit,
    onDelete: (AudioEntity) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = audio.filePath,
                style = MaterialTheme.typography.titleSmall
            )
            // Possibly show duration or last modified time
            Row {
                Button(onClick = { onPlay(audio) }) {
                    Text("Play")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { onDelete(audio) }) {
                    Text("Delete")
                }
            }
        }
    }
}
