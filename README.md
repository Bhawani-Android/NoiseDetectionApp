# NoiseDetectionApp

A mobile application for audio recording with built-in noise detection, noise reduction, and playback capabilities. The project uses Jetpack Compose for UI, Hilt for dependency injection, and a multimodular clean architecture approach. It stores recorded files in a Room database and uses an approximate 5MB file-size limit and 1 minute time limit for recordings.

# Architecture
The project follows a Clean Architecture and multimodule design:

App module (entry point): sets up Hilt, hosts the Compose UI.
Domain module: defines core use cases (RecordAudioUseCase, PlayBackUseCase, etc.) and domain models (AudioEntity, Recording).
Data module: implements repository interfaces using Room (for storage) and AudioRecord/MediaPlayer logic for capturing and playback.
Core or Feature modules (optional separation): utility classes, common logic, or specific features (like recording UI).


# Module Overview
- domain

Entities (Recording, AudioEntity)
Use cases (RecordAudioUseCase, ReduceNoiseUseCase, GetAllRecordingsUseCase, etc.)
Repository interface(s) (AudioRepository).

 - data

Implementation of AudioRepository (AudioRepositoryImpl), referencing Room DAOs.
Room database (RecordingDao, RecordingDatabase).
AudioRecord logic for capturing raw audio and saving to WAV.
Mappers for converting database entities <-> domain models.

- feature:recording (optional separate module)

RecordingViewModel for Compose UI states.
UI composables (RecordingScreenComposable) for record/playback.

- app

MainActivity, top-level Compose setContent.
Hilt @AndroidEntryPoint application class.




                           +------------------------+
                           |       Domain          |
                           | (UseCases, Entities)  |
                           +----------+------------+
                                      ^
                                      | (implements)
                                      |
                +---------------------+-------------------+
                |                   Data                 |
                |(Room DB, AudioRecord, Repos, Mappers) |
                +---------------------+-------------------+
                                      ^
                                      | (UI calls domain)
                                      |
         +-------------+------+-------+--------------+------+
         |  feature:recording  (ViewModels, Compose UI)    |
         +----------------------+---------------------------+
                                      ^
                                      | (final composition)
                                      |
                           +----------+----------+
                           |        App         |
                           | (MainActivity, DI) |
                           +--------------------+



Setup and Installation
Clone the repository:
git clone https://github.com/bhawanisinghlnmiit/NoiseDetactionApp.git
cd NoiseDetectionApp

Open the project in Android Studio.

Sync Gradle. Make sure you have the latest gradle plugin and a stable internet connection for dependencies (Jetpack Compose, Hilt, Room, etc.).

Run the app on an emulator or device with microphone access. For emulators, enable “Virtual microphone uses host audio input” in AVD settings if you want real mic input.

