package com.example.recording.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


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