package com.example.noisedetectionapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NoiseDetectionApp: Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize anything needed here, e.g. logging, analytics
    }
}