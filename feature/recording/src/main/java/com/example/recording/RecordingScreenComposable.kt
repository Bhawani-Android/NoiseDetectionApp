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
import com.example.domain.entity.Recording
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Formats milliseconds to mm:ss
 */

// move it to utils
fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

// RecordingScreen
// This function is to big, try to refactor it
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreenComposable(
    viewModel: RecordingViewModel,
    modifier: Modifier = Modifier
) {
    //Why not withLifeCycle
    val uiState = viewModel.uiState.collectAsState().value

    // Step 1: Check RECORD_AUDIO permission
    val recordAudioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val isMicGranted = recordAudioPermissionState.status.isGranted

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title
        // try to be generic(responsive) so as to accommodate to every screen size
        Text(
            text = "Noise Detection Recorder",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 2) If mic not granted, show a "Request Permission" section. Else show record controls
        if (!isMicGranted) {
            PermissionRequestSection {
                recordAudioPermissionState.launchPermissionRequest()
            }
        } else {
            RecordingControlsSection(uiState, viewModel)
        }

        Divider(Modifier.padding(vertical = 16.dp))

        // 3) If we have an active/last-recorded AudioEntity, show playback section
        uiState.recordedAudio?.let { audio ->
            // should not pass entire view model
            PlaybackSection(uiState, viewModel, audio)
            Divider(Modifier.padding(vertical = 16.dp))
        }

        // 4) Show the list of all recordings
        if (uiState.recordings.isNotEmpty()) {
            // use composable
            Text(
                text = "Your Recordings",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(uiState.recordings) { rec ->
                    RecordingItemCard(
                        recording = rec,
                        onPlay = { viewModel.playRecording(it) },
                        onDelete = { viewModel.deleteRecording(it) }
                    )
                }
            }
        } else {
            Text(
                text = "No Audio Recorded Yet",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// --- Permission UI

@Composable
fun PermissionRequestSection(onRequestPermission: () -> Unit) {
    Text(
        text = "Microphone permission is required to record audio.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    Button(onClick = onRequestPermission) {
        Text("Grant Microphone Permission")
    }
}

// --- Record/Stop Controls

// pass only required parameters
@Composable
fun RecordingControlsSection(
    uiState: RecordingUiState,
    viewModel: RecordingViewModel
) {
    //Optimize this
    if (!uiState.isRecording) {
        // create a global constant file for these values
        Button(onClick = { viewModel.startRecording(thresholdDb = 60.0) }) {
            // use xml string file for these
            Text("Start Recording")
        }
    } else {
        Button(onClick = { viewModel.stopRecording() }) {
            Text("Stop Recording")
        }
    }

    Spacer(Modifier.height(12.dp))


    // See if we can pause and start from same point
    Text("Current dB: %.1f".format(uiState.currentDb))
    if (uiState.hasNoiseWarning) {
        Text(
            text = "Noise threshold exceeded!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// --- Playback UI

@Composable
fun PlaybackSection(
    uiState: RecordingUiState,
    viewModel: RecordingViewModel,
    audio: AudioEntity
) {
    // Show file path
    Text(
        text = "Recorded Audio:\n${audio.filePath}",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    // Timer
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(formatTime(uiState.currentPositionMs))
        Text(" / ")
        Text(formatTime(uiState.totalDurationMs))
    }

    // If playing => show "Pause" + hide "Reduce Noise"
    // If NOT playing => show "Play" + show "Reduce Noise"
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.isPlaying) {
            Button(onClick = { viewModel.playAudio(false) }) {
                Text("Pause")
            }
            // Hide or disable the "Reduce Noise" button if currently playing:
            // e.g. disabled:
            Button(onClick = {}, enabled = false) {
                Text("Reduce Noise")
            }
        } else {
            Button(onClick = { viewModel.playAudio(true) }) {
                Text("Play")
            }
            Button(onClick = { viewModel.reduceNoise() }) {
                Text("Reduce Noise")
            }
        }

        Button(onClick = { viewModel.deleteAudio() }) {
            Text("Delete")
        }
    }
}

// --- Recording List UI

@Composable
fun RecordingItemCard(
    recording: Recording,
    onPlay: (Recording) -> Unit,
    onDelete: (Recording) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = recording.filePath, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Button(onClick = { onPlay(recording) }) {
                    Text("Play")
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = { onDelete(recording) }) {
                    Text("Delete")
                }
            }
        }
    }
}

