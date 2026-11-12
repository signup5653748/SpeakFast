# SpeakFast

A personal, offline, sideloaded replacement for Android's Select to Speak accessibility feature, built with Jetpack Compose and Kotlin.

> ***"Every line of code should be written once, and only once." - Ron Jeffries***

## Features 🎨

- **AccessibilityService** that reads on-screen accessibility node tree and speaks text from user-selected regions
- **Animation tolerance** with re-sampling of tapped node bounds (3 checks, ~50ms apart)
- **Node sorting** top-to-bottom, left-to-right by screen bounds before speaking
- **OCR fallback** using ML Kit Text Recognition v2 when node text isn't available
- **Text-to-Speech** wrapped in a class locked to a single Locale from app settings
- **Minimal Compose settings screen** for language/voice, speech rate, pitch, and trigger method
- **Settings persistence** using JSON storage
- **Quick settings tile**, **floating button**, and **gesture** trigger methods
- **Clean architecture** with MVVM and dependency injection

## Getting Started 👣

1. Clone the repository
2. Open the project in Android Studio
3. Build and run the app
4. Enable the SpeakFast accessibility service from the app settings
5. Use the accessibility service to select and speak text on your device

## Project Structure 🗺️📐🛠️

The project includes a single module **`app`**, with following packages:

- **`accessibility`:** Accessibility service implementation
- **`data`:** Settings persistence layer
- **`di`:** Dagger-Hilt dependency injection module
- **`model`:** Data classes for settings, languages, voices, and trigger methods
- **`presentation`:** UI components and navigation
  - **`screen`:** Main app screens
  - **`ui`:** UI components and themes
  - **`viewmodel`:** View models for MVVM architecture
- **`repository`:** Repository pattern for data access
- **`speech`:** Text-to-speech functionality
- **`util`:** Utility classes

## Accessibility Service Features

### Node Selection and Sorting
- Detects user taps and drags to select screen regions
- Re-samples node bounds multiple times for animation tolerance
- Sorts nodes by screen bounds (top-to-bottom, left-to-right)
- Combines text from multiple nodes for comprehensive speech output

### OCR Integration
- Captures selected regions using accessibility service screenshot API
- Runs on-device OCR using ML Kit Text Recognition v2
- Falls back to OCR when node text is unavailable

### Text-to-Speech
- Uses Android's TextToSpeech wrapped in SpeechManager class
- Locked to single Locale from app settings
- Configurable speech rate and pitch
- Supports multiple languages and voices

## Settings

The app provides a minimal settings screen with:

- **Language:** Select from 12 supported languages
- **Voice:** Choose from 5 voice options
- **Speech Rate:** Adjust speaking speed
- **Pitch:** Adjust voice pitch
- **Trigger Method:** Choose between quick settings tile, floating button, or gesture

## Build and Run

### Prerequisites
- Android Studio Chipmunk or later
- Android SDK with API level 26+

### Build
```bash
./gradlew assembleDebug
```

### Run
- Connect an Android device or start an emulator
- Sync the project in Android Studio
- Click the Run button

## Testing

The project includes GitHub Actions CI/CD pipeline that:

- Runs detekt code smell analysis
- Performs unit tests
- Builds and uploads debug APK

## Contributing 🤝

Join us in shaping the future of SpeakFast – your contributions are invaluable! Feel free to open an issue or submit a pull request for any bugs/improvements.

## License

Distributed under the MIT License. See `LICENSE` file for more information.

#### 🌟 If you find value in this project, please consider starring it! Your support keeps it thriving. 🚀
