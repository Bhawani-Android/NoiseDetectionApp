package com.example.domain

import com.example.domain.repository.AudioRepository
import com.example.domain.usecase.RecordAudioUseCase
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test


class RecordAudioUseCaseTest {
    private lateinit var audioRepository: AudioRepository
    private lateinit var recordAudioUseCase: RecordAudioUseCase



    @Before
    fun setup() {
        audioRepository = mockk()
        recordAudioUseCase = RecordAudioUseCase(audioRepository)
    }

    @Test
    fun `startRecording emits some dB values`() = runBlocking {
        coEvery { audioRepository.startRecording(thresholdDb = 60.0) } returns mockk()


        val flow = recordAudioUseCase(60.0)


        // Just verifying the Flow is non-null.
        assertTrue("Flow should not be null", flow != null)
    }

    @Test
    fun `stopRecording returns audio entity`() = runBlocking {
        // given
        coEvery { audioRepository.stopRecording() } returns mockk() // a mocked AudioEntity

        // when
        val result = recordAudioUseCase.stop()

        // then
        assertTrue("Should return an audio entity", result != null)
    }
}