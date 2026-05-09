# Motion Cues - Android Vehicle Motion Sickness Prevention

An Android implementation of Apple's Vehicle Motion Cues feature, designed to reduce motion sickness when using your device in a moving vehicle.

## What is Vehicle Motion Cues?

Motion sickness is commonly caused by a sensory conflict between what your eyes see (a stationary screen) and what your body feels (vehicle movement). Vehicle Motion Cues resolves this by displaying animated dots on the screen that move in response to the vehicle's motion, providing visual cues that align with what your inner ear is sensing.

## Features

- **Three Modes**
  - **Off** - No cues displayed
  - **On** - Dots are always visible
  - **Auto** - Automatically detects vehicle motion and shows cues only when needed

- **Motion-Responsive Dots**
  - Dots spawn at one screen edge and traverse horizontally across the screen
  - Flow direction is opposite to detected phone tilt/vehicle turn
  - Dots smoothly fade in at the spawn edge and fade out at the destination edge
  - Traversal speed increases with stronger vehicle motion

- **Idle State**
  - When no motion is detected, dots gently pulse at the left and right screen edges

- **Customization**
  - **Pattern**: Regular (stable) or Dynamic (with size pulsing)
  - **Color**: Auto, Blue, Green, Orange, Purple, Red
  - **Dot Size**: Normal or Larger
  - **Dot Count**: Normal or More dots

## How It Works

```
┌─────────────────────────────────────┐
│         Motion Cues View            │
│  ●  ●  ●  →  →  →  →  →  →  ●  ●  │  ← Animated dots traverse the screen
│                                     │
│         [Your App Content]          │
│                                     │
│  ●  ●  ●  →  →  →  →  →  →  ●  ●  │  ← Fade in/out at edges
└─────────────────────────────────────┘
         ▲                        ▲
    Fade In Zone            Fade Out Zone
```

### Architecture

```
app/src/main/java/com/example/motioncues/
├── MainActivity.kt              # Compose UI - mode selection (Off/On/Auto)
├── SettingsActivity.kt          # Compose UI - appearance customization
│
├── service/
│   └── MotionCuesService.kt     # Foreground service managing overlay window
│                                # and sensor lifecycle
│
├── view/
│   └── MotionCuesView.kt        # Custom View rendering animated dots
│                                # Hardware-accelerated, Choreographer-synced
│
├── sensor/
│   └── VehicleMotionDetector.kt # Accelerometer + gyroscope fusion
│                                # Detects vehicle motion state and vector
│
├── prefs/
│   └── PrefsManager.kt          # SharedPreferences wrapper for settings
│
└── ui/theme/
    └── Theme.kt                 # Material 3 theming (light/dark)
```

### Motion Detection

The app uses Android's `SensorManager` to read data from:
- **Accelerometer** - Measures linear acceleration (forward/backward, side-to-side)
- **Gyroscope** - Measures rotational movement (turns, tilts)

Sensor data is fused using a weighted combination (60% accelerometer, 40% gyroscope) with low-pass filtering to produce a smooth motion vector that drives the dot animation.

### Overlay Rendering

- Uses `TYPE_APPLICATION_OVERLAY` window type to draw over other apps
- `FLAG_NOT_FOCUSABLE` and `FLAG_NOT_TOUCHABLE` ensure zero interference with underlying apps
- Hardware-accelerated `View` with `Choreographer` frame callbacks for smooth 60fps rendering
- Minimal CPU footprint - only active when motion cues are needed

## Requirements

- **Android 7.0+** (API 24)
- **Overlay Permission** - Required to draw dots over other apps
- **Foreground Service Permission** - Required for persistent overlay
- **Notification Permission** (Android 13+) - For service notification

## Installation

1. Build the project:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install the APK:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. Open the app and grant the overlay permission when prompted

4. Select your preferred mode (On or Auto)

5. The dots will appear over all apps on your screen

## Usage Tips

- **Best results**: Hold your phone normally while riding as a passenger facing forward
- **Auto mode**: Dots appear automatically when vehicle motion is detected
- **On mode**: Dots are always visible, useful for testing or if you experience motion sickness outside of vehicles
- **Settings**: Customize dot color, size, count, and animation pattern from the settings screen

## Tech Stack

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern declarative UI
- **Material 3** - Design system
- **SensorManager** - Device motion sensors
- **WindowManager** - System overlay rendering
- **Choreographer** - Frame-synchronized animation at 60fps

## License

MIT License
