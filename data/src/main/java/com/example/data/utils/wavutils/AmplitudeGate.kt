package com.example.data.utils.wavutils

import android.util.Log

object AmplitudeGate {
    fun applyAmplitudeGate(samples: ShortArray, threshold: Int) {
        var zeroCount = 0
        for (i in samples.indices) {
            val absVal = kotlin.math.abs(samples[i].toInt())
            if (absVal < threshold) {
                samples[i] = 0
                zeroCount++
            }
        }
        Log.d("AmplitudeGate", "Zeroed out $zeroCount of ${samples.size} samples.")
    }
}
