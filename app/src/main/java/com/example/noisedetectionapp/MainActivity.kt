package com.example.noisedetectionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.noisedetectionapp.ui.theme.NoiseDetectionAppTheme
import com.example.recording.RecordingScreenComposable
import com.example.recording.RecordingViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoiseDetectionAppTheme {
                val recordingViewModel: RecordingViewModel = hiltViewModel()
                RecordingScreenComposable(recordingViewModel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NoiseDetectionAppPreview() {
    NoiseDetectionAppTheme {

    }
}