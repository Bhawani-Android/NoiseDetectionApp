package com.example.recording.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.core.CommonUtils.formatTime
import com.example.domain.entity.AudioEntity
import com.example.recording.R
import com.example.recording.RecordingViewModel
import com.example.recording.ui.RecordingUiState
import java.io.File


/**
 * Playback UI section to control audio playback and noise reduction.
 */
@Composable
fun PlaybackSection(
    uiState: RecordingUiState,
    togglePlayback: (shouldPlay: Boolean) -> Unit,
    reduceNoise: () -> Unit,
    deleteAudio: () -> Unit,
    audio: AudioEntity
) {
    Text(
        text = "Recorded Audio:\n${File(audio.filePath).name}",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 8.dp)
    )

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Text(formatTime(uiState.currentPositionMs))
        Text(" / ")
        Text(formatTime(uiState.totalDurationMs))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.isPlaying) {
            Button(onClick = { togglePlayback.invoke(false) }) {
                Text(stringResource(R.string.pause))
            }
            Button(onClick = {}, enabled = false) {
                Text(stringResource(R.string.reduce_noise))
            }
        } else {
            Button(onClick = { togglePlayback.invoke(true) }) {
                Text(stringResource(R.string.play))
            }
            Button(onClick = { reduceNoise.invoke() }) {
                Text(stringResource(R.string.reduce_noise_btn))
            }
        }

        Button(onClick = { deleteAudio.invoke() }) {
            Text(stringResource(R.string.delete))
        }
    }
}

@Composable
fun PlaybackSectionScreen(
    uiState: RecordingUiState,
    viewModel: RecordingViewModel
) {
    if (uiState.recordedAudio != null) {
        PlaybackSection(
            uiState = uiState,
            togglePlayback = {
                viewModel.togglePlayback(it)
            },
            reduceNoise = {
                viewModel.reduceNoise()
            }, deleteAudio = {
                viewModel.deleteAudio()
            },
            audio = uiState.recordedAudio!!
        )
        Divider(Modifier.padding(vertical = 16.dp))
    }
}
