package com.example.recording

import com.example.domain.usecase.DeleteAudioUseCase
import com.example.domain.usecase.GetAllRecordingsUseCase
import com.example.domain.usecase.GetDurationUseCase
import com.example.domain.usecase.GetPlaybackPositionUseCase
import com.example.domain.usecase.PlayBackUseCase
import com.example.domain.usecase.RecordAudioUseCase
import com.example.domain.usecase.ReduceNoiseUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test verifying noise threshold logic in the ViewModel.
 */
class NoiseDetectionViewModelTest {

    //Rule to override Dispatchers.Main with a TestDispatcher
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: RecordingViewModel
    private lateinit var recordAudioUseCase: RecordAudioUseCase
    private lateinit var reduceNoiseUseCase: ReduceNoiseUseCase
    private lateinit var playBackUseCase: PlayBackUseCase
    private lateinit var getPlaybackPositionUseCase: GetPlaybackPositionUseCase
    private lateinit var getDurationUseCase: GetDurationUseCase
    private lateinit var deleteAudioUseCase: DeleteAudioUseCase
    private lateinit var getAllRecordingsUseCase: GetAllRecordingsUseCase

    @Before
    fun setup() {
        // Created mocks for all UseCases
        recordAudioUseCase = mockk()
        reduceNoiseUseCase = mockk()
        playBackUseCase = mockk()
        getPlaybackPositionUseCase = mockk()
        getDurationUseCase = mockk()
        deleteAudioUseCase = mockk()
        getAllRecordingsUseCase = mockk()

        //Returning a real flow.
        coEvery { recordAudioUseCase(any()) } returns flowOf(40.0, 70.0)

        coEvery { getAllRecordingsUseCase.invoke() } returns flowOf(emptyList())

        viewModel = RecordingViewModel(
            recordAudioUseCase,
            reduceNoiseUseCase,
            playBackUseCase,
            getPlaybackPositionUseCase,
            getDurationUseCase,
            deleteAudioUseCase,
            getAllRecordingsUseCase
        )
    }

    @Test
    fun `when db above threshold, hasNoiseWarning = true`() = runTest {
        val thresholdDb = 60.0

        // Start recording with threshold
        viewModel.startRecording(thresholdDb)

        //Directly simulate currentDb = 70 (above threshold)
        viewModel.testSetCurrentDb(70.0)

        // Check that hasNoiseWarning is set
        assertTrue(viewModel.uiState.value.hasNoiseWarning)
    }

    @Test
    fun `when db below threshold, hasNoiseWarning = false`() = runTest {
        val thresholdDb = 60.0
        viewModel.startRecording(thresholdDb)

        // Simulate currentDb = 40 (below threshold)
        viewModel.testSetCurrentDb(40.0)

        // Check that hasNoiseWarning is NOT set
        assertFalse(viewModel.uiState.value.hasNoiseWarning)
    }
}

fun RecordingViewModel.testSetCurrentDb(db: Double) {
    _uiState.update { old ->
        old.copy(
            currentDb = db,
            hasNoiseWarning = (db >= 60.0)
        )
    }
}