package com.example.recording.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.domain.entity.Recording
import com.example.recording.R

@Composable
fun RecordingScreenBody(
    recordings: List<Recording>,
    deleteRecording: (Recording) -> Unit,
    playRecording: (Recording) -> Unit
) {
    if (recordings.isNotEmpty()) {
        Text(
            text = stringResource(R.string.your_recordings),
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(recordings.size) { pos ->
                RecordingItemCard(
                    recording = recordings[pos],
                    onPlay = { playRecording(it) },
                    onDelete = { deleteRecording(it) }
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