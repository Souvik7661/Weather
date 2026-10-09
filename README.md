# Atmospheric Weather 🌦️⚡️❄️

> **A modern, ultra-fluid Android weather app built in Jetpack Compose featuring 60fps atmospheric particle physics—dripping rain on glass, volumetric lightning, and 3D snow. Powered by pinpoint dual-tier GPS geocoding, contextual safety advisories, and a minimalist frosted glass 24-hour forecast pill.**

---

## 🌟 Key Features

- **60fps Atmospheric Animation Canvas**:
  - 💧 **Tokyo Low Rain**: Realistic water droplets on glass with condensation beads, specular glints, gravity dripping physics, and angled rain streaks.
  - ⚡ **Sydney Thunderstorm**: Volumetric sky flashes, procedurally generated branched lightning bolts, and dark storm billows.
  - ❄ **New York Snow Rain**: Multi-depth 3D snowflakes with horizontal wind drift and melting raindrop blends.
  - ☀️ **Clear Day**: Pulsing solar corona, rotating radiant sunbeams, and floating warm atmospheric dust motes.
  - 🌙 **Clear Night**: Dynamic twinkling star field and streaking meteors / shooting stars.
  - 🌫️ **Fog & Mist**: Sinusoidal undulating volumetric mist ribbons.
  - ⛅ **Cloudy & Overcast**: Rolling layered cloud banks with fluid lighting.

- **High-Accuracy GPS & Dual-Tier Geocoding**:
  - `Priority.PRIORITY_HIGH_ACCURACY` with fast fallback to balanced power and cached locations.
  - Native Android `Geocoder` paired with an automatic HTTPS reverse-geocoding fallback for 100% reliable city name resolution without API keys.
  - Timeout-guarded async execution prevents any UI freezing.

- **Minimalist Design & Mockup Fidelity**:
  - `⊞+` Quad-grid plus button opening the Cities & Condition Switcher drawer.
  - Centered City Name with GPS pinpoint dot and formatted date subtitle (`Friday, January 3, 2024`).
  - `⬡` Hexagon-dot button for Quick Settings (unit toggles °C/°F, GPS diagnostics).
  - Cyan `ⓘ Safety Reminder` card with contextual 2-line safety advice.
  - Giant 76sp temperature display with subtle breathing animation.
  - 3-column atmospheric metrics row: `Wind`, `Humidity`, and `Precip`.
  - Frosted glass 24-hour forecast pill container displaying synchronized Temperatures, Weather Icons, and Hours.

---

## 🏗️ Architecture & Tech Stack

- **UI Framework**: Modern Jetpack Compose (100% Kotlin)
- **Design System**: Material Design 3 + Custom Canvas Graphics
- **Architecture**: MVVM with Kotlin Coroutines & StateFlow
- **Location**: Google Play Services Location (`FusedLocationProviderClient`) + Reverse Geocoding Fallback
- **Build System**: Gradle 9.3.1 with Android Gradle Plugin & Kotlin 2.1.0

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio Ladybug | 2024.2+](https://developer.android.com/studio)
- Android SDK 35/36
- Java 17+

### Installation & Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Souvik7661/Weather.git
   cd Weather
   ```

2. **Open in Android Studio:**
   - Select **Open** and choose the `Weather` project directory.
   - Allow Gradle sync to complete.

### Run the Web Companion & Live App
To run the live web app and companion portal:
```bash
# Install dependencies
npm install

# Start local dev server (Vite)
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 🌐 Web Companion Features
- **Full 1:1 Live Weather Web App**: Real-time Open-Meteo forecasts, 24-hour frosted glass pill, 7-day forecast, air quality (US AQI), and GPS location detection.
- **60fps Particle Physics Canvas**: Tokyo rain on glass with gravity drips, Sydney volumetric lightning, New York 3D snow, solar corona, and meteor starfields.
- **Interactive 3D Phone Mockup**: Live 3D perspective smartphone with real-time UI mirroring and gyroscope mouse tilt tracking.
- **Interactive Atmospheric Recharts & UV Gauge**: Hourly humidity bezier curves, semi-circular UV gauge, and astronomical moon phase calculator.

---

## 🧪 Testing

Run local unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
