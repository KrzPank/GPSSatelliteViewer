# Dokumentacja modułu Scene3D

## Spis treści
1. [Biblioteki i zależności](#biblioteki-i-zależności)
2. [Przegląd modułu](#przegląd-modułu)
3. [Architektura systemu](#architektura-systemu)
4. [Komponenty główne](#komponenty-główne)
5. [Managery](#managery)
6. [Parametry konfiguracyjne](#parametry-konfiguracyjne)
7. [Komponenty UI](#komponenty-ui)
8. [Przepływ danych](#przepływ-danych)
9. [API publiczne](#api-publiczne)
10. [Przykłady użycia](#przykłady-użycia)

---

## Biblioteki i zależności

### Biblioteki renderowania 3D

#### SceneView (v2.3.0)
- **Źródło**: `io.github.sceneview:sceneview`
- **Opis**: Główna biblioteka do renderowania 3D w Android, wrapper nad Google Filament
- **Funkcjonalność**:
  - `ModelLoader` - ładowanie modeli 3D (GLB/GLTF)
  - `EnvironmentLoader` - ładowanie środowisk HDR
  - `Scene`, `View`, `Renderer`, `Engine` - podstawowe komponenty renderowania
  - `Node`, `ModelNode`, `CameraNode`, `LightNode` - system węzłów sceny
  - `CameraGestureDetector` - detekcja gestów dla kamery
  - Integracja z Jetpack Compose

#### Google Filament (przez SceneView)
- **Opis**: Silnik renderowania 3D w czasie rzeczywistym od Google
- **Funkcjonalność**:
  - `Engine` - rdzeń silnika renderowania
  - `View`, `Scene`, `Renderer` - komponenty pipeline renderowania
  - `LightManager` - zarządzanie oświetleniem sceny
  - `EntityManager` - zarządzanie bytami (entities) w silniku
  - `Manipulator` - kontrola kamery (orbit, zoom, pan)
  - Zaawansowane efekty renderowania (HDR, bloom, ambient occlusion, itp.)

### Biblioteki Android

#### Jetpack Compose
- **Wersja BOM**: 2025.09.01
- **Komponenty używane**:
  - `androidx.compose.ui` - podstawowe komponenty UI
  - `androidx.compose.material3` - Material Design 3
  - `androidx.compose.material:material-icons-extended` (v1.7.8) - rozszerzone ikony
  - `androidx.activity:activity-compose` (v1.11.0) - integracja z Activity
  - `androidx.navigation:navigation-compose` (v2.9.5) - nawigacja

#### AndroidX Core
- **androidx.core:core-ktx** (v1.17.0) - rozszerzenia Kotlin dla Android
- **androidx.lifecycle** (v2.9.4):
  - `lifecycle-runtime-ktx` - zarządzanie cyklem życia
  - `lifecycle-viewmodel-compose` - ViewModels dla Compose
- **androidx.core:core-splashscreen** (v1.0.1) - ekran powitalny

#### Pozostałe
- **com.google.accompanist:accompanist-permissions** (v0.36.0) - zarządzanie uprawnieniami
- **org.jetbrains.kotlinx:kotlinx-coroutines-android** (v1.9.0) - korutyny
- **dev.romainguy.kotlin.math** - biblioteka matematyczna (wektory, macierze)

### Biblioteki pomocnicze
- **com.github.PhilJay:MPAndroidChart** (v3.1.0) - wykresy (używane w innych modułach)

---

## Przegląd modułu

### Cel modułu
Moduł `scene3d` jest odpowiedzialny za wizualizację 3D satelitów GNSS w czasie rzeczywistym. Umożliwia:
- Renderowanie modelu Ziemi w 3D
- Wizualizację pozycji satelitów w przestrzeni
- Interakcję użytkownika z sceną (obracanie, przybliżanie)
- Wyświetlanie orbit satelitów
- Zaawansowane efekty wizualne (oświetlenie, anti-aliasing, bloom)
- Dynamiczne kolorowanie satelitów według jakości sygnału (SNR/C/N0)

### Struktura pakietów
```
scene3d/
├── Scene3D.kt                    # Główna klasa orkiestrująca
├── Scene3DParameters.kt          # Konfiguracja sceny
├── manager/                      # Managery komponentów
│   ├── EarthManager.kt
│   ├── LightHandler.kt
│   ├── LocationMarkerManager.kt
│   ├── camera/
│   │   ├── CameraManager.kt
│   │   └── CameraManipulatorWrapper.kt
│   └── satellite/
│       ├── SatelliteManager.kt
│       ├── OrbitManager.kt
│       └── AuraManager.kt
└── ui/                           # Komponenty interfejsu
    ├── Scene3DScreen.kt
    ├── Scene3DLoadingScreen.kt
    ├── infobox/                  # Okna informacyjne
    │   ├── SatelliteInfoBox.kt
    │   └── EarthInfoBox.kt
    └── menu/
        ├── Scene3DParametersMenu.kt
        └── SatelliteFilterMenu.kt
```

---

## Architektura systemu

### Wzorzec architektoniczny
Moduł wykorzystuje wzorzec **Manager Pattern** z centralną klasą orkiestrującą (`Scene3D`).

### Diagram komponentów
```
┌─────────────────────────────────────────────┐
│           Scene3D (Orchestrator)             │
│  - Zarządza cyklem życia komponentów        │
│  - Koordynuje aktualizacje                  │
│  - Obsługuje interakcje użytkownika         │
└─────────────────┬───────────────────────────┘
                  │
      ┌───────────┼───────────┬───────────┐
      │           │           │           │
      ▼           ▼           ▼           ▼
┌──────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│ Camera   │ │ Earth   │ │Location │ │Satellite│
│ Manager  │ │ Manager │ │ Marker  │ │ Manager │
└──────────┘ └─────────┘ └─────────┘ └────┬────┘
      │                                     │
      │                                ┌────┴────┐
      │                                │         │
      ▼                                ▼         ▼
┌──────────┐                      ┌────────┐ ┌──────┐
│  Light   │                      │ Orbit  │ │ Aura │
│ Handler  │                      │Manager │ │Mgr   │
└──────────┘                      └────────┘ └──────┘
```

### Odpowiedzialności

#### Scene3D (Orkiestrator)
- Inicjalizacja wszystkich managerów
- Koordynacja aktualizacji w każdej klatce (`onFrame`)
- Obsługa gestów użytkownika (tap, double tap)
- Zarządzanie widocznością menu i info boxów
- Propagacja zmian parametrów do managerów

#### Managery
Każdy manager odpowiada za konkretny aspekt sceny:
- **CameraManager**: Pozycja i ruch kamery
- **EarthManager**: Model Ziemi
- **SatelliteManager**: Satelity i ich pozycje
- **LightHandler**: Oświetlenie sceny
- **LocationMarkerManager**: Marker lokalizacji użytkownika

---

## Komponenty główne

### Scene3D

**Lokalizacja**: `scene3d/Scene3D.kt`

**Opis**: Główna klasa modułu, orkiestruje wszystkie komponenty sceny 3D.

**Konstruktor**:
```kotlin
class Scene3D(
    private val engine: Engine,
    private val view: View,
    private val renderer: Renderer,
    private val scene: Scene,
    private val modelLoader: ModelLoader,
    private val environmentLoader: EnvironmentLoader,
    private val modifier: Modifier = Modifier.fillMaxSize(),
    private var parameters: Scene3DParameters = Scene3DParameters()
)
```

**Kluczowe pola**:
```kotlin
// Węzeł centralny - parent wszystkich obiektów sceny
private val centerNode = Node(engine)

// Managery komponentów
private val cameraManager: CameraManager
private val earthManager: EarthManager
private val locationMarkerManager: LocationMarkerManager
private val mainLightManager: LightHandler
private val satelliteManager: SatelliteManager

// Stany UI
private var _menuVisible: Boolean
private val _clickedSatelliteKey: MutableStateFlow<String?>
private val _isSatelliteInfoBoxVisible: MutableStateFlow<Boolean>
private val _isEarthInfoBoxVisible: MutableStateFlow<Boolean>
```

**Główne metody**:

```kotlin
// Renderowanie sceny (Composable)
@Composable
fun Render()

// Aktualizacja listy satelitów
fun updateSatelliteList(
    newList: List<GNSSStatusData>, 
    azElHistory: Map<String, AzElHistory>
)

// Aktualizacja parametrów sceny
fun updateParameters(newParameters: Scene3DParameters)

// Aktualizacja lokalizacji użytkownika
fun updateUserLocation(userLocation: Float3?)

// Widoczność markera lokalizacji
fun setLocationMarkerVisible(visible: Boolean)
fun isLocationMarkerVisible(): Boolean

// Sprzątanie zasobów
fun cleanup()
```

**Obsługa gestów**:
```kotlin
// Podwójne kliknięcie - przełączanie menu
private fun onSceneDoubleTap()

// Pojedyncze kliknięcie - selekcja obiektu
private fun onSceneSingleTapConfirmed(node: Node?)
```

### Scene3DParameters

**Lokalizacja**: `scene3d/Scene3DParameters.kt`

**Opis**: Data class zawierająca wszystkie parametry konfiguracyjne sceny.

**Struktura**:
```kotlin
data class Scene3DParameters(
    // Parametry oświetlenia
    var lightIntensity: Float = 250_000.0f,
    var lightColor: Float3 = Float3(1.0f, 1.0f, 1.0f),
    var lightFalloff: Float = 1000.0f,
    val lightType: LightManager.Type = LightManager.Type.DIRECTIONAL,
    
    // Model Ziemi
    var earthModelPath: String = "models/NASA_EARTH.glb",
    var earthScale: Float = 1.0f,
    
    // Satelity
    val orbitModelPath: String = "models/orbit.glb",
    val satelliteModelPath: String = "models/RedCircle.glb",
    var satelliteScale: Float = 0.08f,
    
    // Marker lokalizacji
    var locationMarkerModelPath: String = "models/pointer.glb",
    var locationMarkerScale: Float = 0.05f,
    var userLocation: Float3? = null,
    
    // Środowisko
    var environmentPath: String = "envs/NightSkyHDRI002_2K.hdr",
    var environmentIntensity: Float = 1.0f,
    
    // Kamera
    var startingCameraLocation: Float3 = Float3(3.0f, 1.0f, 3.0f),
    
    // Jakość renderowania
    var hdrColorBufferQuality: QualityLevel = QualityLevel.MEDIUM,
    var dynamicResolutionEnabled: Boolean = true,
    var dynamicResolutionQuality: QualityLevel = QualityLevel.MEDIUM,
    var msaaEnabled: Boolean = false,
    var fxaaEnabled: Boolean = true,
    var ambientOcclusionEnabled: Boolean = false,
    var bloomEnabled: Boolean = true,
    var screenSpaceReflectionsEnabled: Boolean = false,
    var temporalAntiAliasingEnabled: Boolean = false
)
```

**Poziomy jakości**:
```kotlin
enum class QualityLevel(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    ULTRA("Ultra")
}
```

**Dostępne modele**:
```kotlin
companion object {
    val EARTH_MODEL_OPTIONS = listOf(
        "models/material_test_noQuad.glb" to "Material Test Earth",
        "models/Earth_base.glb" to "Base Earth",
        "models/NASA_EARTH.glb" to "NASA Earth"
    )
    
    val SATELLITE_MODEL_OPTIONS = listOf(
        "models/RedCircle.glb" to "Circle",
        "models/TDRS_A.glb" to "Simple Satellite"
    )
}
```

### Scene3DParametersState

**Opis**: Observable wrapper dla Scene3DParameters używający Compose State.

**Kluczowe metody**:
```kotlin
class Scene3DParametersState {
    var parameters by mutableStateOf(Scene3DParameters())
        private set
    
    fun updateLightIntensity(intensity: Float)
    fun updateLightColor(red: Float, green: Float, blue: Float)
    fun updateEarthModel(path: String)
    fun updateSatelliteModel(path: String)
    fun updateHdrColorBufferQuality(quality: Scene3DParameters.QualityLevel)
    // ... inne metody update
    fun resetToDefaults()
}
```

---

## Managery

### CameraManager

**Lokalizacja**: `scene3d/manager/camera/CameraManager.kt`

**Odpowiedzialność**: Zarządzanie kamerą, jej pozycją i ruchem.

**Kluczowe parametry**:
```kotlin
private const val MIN_CAMERA_DISTANCE = 0.62f  // Min odległość od centrum
private const val MAX_CAMERA_DISTANCE = 9f     // Max odległość od centrum
private const val MAX_PITCH_DEG = 85f          // Maksymalny kąt pochylenia
private const val ORBIT_SPEED = 0.0042f
private const val ZOOM_SPEED = 0.05f
```

**Kluczowe metody**:
```kotlin
fun getCameraNode(): CameraNode
fun getCameraManipulator(): CameraManipulatorWrapper
fun getCameraPosition(): Float3
fun getCameraDistanceToCenter(): Float
fun getDynamicUpdateThreshold(): Float
fun onFrame()  // Aktualizacja w każdej klatce
```

**Logika**:
- Pozycja startowa kamery obliczana na podstawie lokalizacji użytkownika
- Dynamiczny threshold aktualizacji zależny od odległości kamery
- Śledzenie ruchu kamery do optymalizacji aktualizacji

### CameraManipulatorWrapper

**Lokalizacja**: `scene3d/manager/camera/CameraManipulatorWrapper.kt`

**Opis**: Wrapper nad `Filament Manipulator` dodający ograniczenia ruchu kamery.

**Ograniczenia**:
- **Odległość**: Clampowanie do przedziału [MIN_CAMERA_DISTANCE, MAX_CAMERA_DISTANCE]
- **Pitch**: Ograniczenie pochylenia do ±MAX_PITCH_DEG
- **Zoom**: Podział dużych zmian na mniejsze kroki dla płynności

**Kluczowe metody**:
```kotlin
override fun scrollUpdate(x: Int, y: Int, prevSeparation: Float, currSeparation: Float)
override fun grabUpdate(x: Int, y: Int)
private fun clampTransform(transform: Transform): Transform
```

### EarthManager

**Lokalizacja**: `scene3d/manager/EarthManager.kt`

**Odpowiedzialność**: Zarządzanie modelem Ziemi.

**Funkcjonalność**:
```kotlin
// Zwraca węzeł Ziemi
fun getEarthNode(): ModelNode

// Aktualizacja parametrów (model, skala)
fun updateParameters(newParameters: Scene3DParameters)

// Handler kliknięcia na Ziemię
var onEarthClick: ((String) -> Unit)?

// Sprzątanie
fun cleanup()
```

**Logika aktualizacji modelu**:
```kotlin
private fun updateEarthModel() {
    try {
        // Usuń stary model
        earthNode.let { centerNode.removeChildNode(it) }
        earthNode.destroy()
        
        // Stwórz nowy model
        earthNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(parameters.earthModelPath),
            scaleToUnits = parameters.earthScale
        ).also { centerNode.addChildNode(it) }
    } catch (e: Exception) {
        // Fallback do domyślnego modelu
        earthNode = ModelNode(
            modelInstance = modelLoader.createModelInstance(
                Scene3DParameters().earthModelPath
            ),
            scaleToUnits = parameters.earthScale
        ).also { centerNode.addChildNode(it) }
    }
}
```

### LightHandler

**Lokalizacja**: `scene3d/manager/LightHandler.kt`

**Odpowiedzialność**: Zarządzanie oświetleniem sceny (światło słoneczne).

**Strategia oświetlenia**:
- Światło kierunkowe umiejscowione za kamerą
- Dynamiczne przeliczanie kierunku na podstawie pozycji kamery
- Optymalizacja: aktualizacja tylko przy znaczącym ruchu kamery

**Kluczowe metody**:
```kotlin
fun getSunLightNode(): LightNode
fun updateParameters(newParameters: Scene3DParameters)
fun onFrame()  // Aktualizacja kierunku światła
```

**Obliczanie kierunku światła**:
```kotlin
private fun calculateOptimalLightDirection(): Float3 {
    val cameraPos = cameraManager.getCameraPosition()
    val centerPos = centerNode.worldPosition
    val cameraForward = (centerPos - cameraPos).normalized()
    
    val worldUp = Float3(0f, 1f, 0f)
    val cameraRight = cross(cameraForward, worldUp).normalized()
    val cameraUp = cross(cameraRight, cameraForward).normalized()
    
    // Kombinacja wektorów dla optymalnego oświetlenia
    val lightDirection = (
        cameraForward * 0.75f +
        cameraRight * -0.55f +
        cameraUp * -0.25f
    ).normalized()
    
    return lightDirection
}
```

**Throttling**:
```kotlin
private const val minRecreationInterval = 16L  // 60 FPS
```

### LocationMarkerManager

**Lokalizacja**: `scene3d/manager/LocationMarkerManager.kt`

**Odpowiedzialność**: Zarządzanie markerem lokalizacji użytkownika na Ziemi.

**Kluczowe cechy**:
- Dynamiczne skalowanie w zależności od odległości kamery
- Aktualizacja pozycji co 5 minut lub przy znaczącym ruchu kamery
- Automatyczne ukrywanie gdy brak lokalizacji

**Parametry**:
```kotlin
private const val MAX_SCALE_SIZE = 0.2f
private const val MIN_SCALE_SIZE = 0.008f
private const val locationMarkerUpdateInterval = 60 * 1000 * 5  // 5 minut
```

**Kluczowe metody**:
```kotlin
fun setVisible(visible: Boolean)
fun isLocationMarkerVisible(): Boolean
fun onFrame()  // Aktualizacja w każdej klatce
```

**Obliczanie pozycji**:
```kotlin
private fun calculateLocationMarkerScenePosition(): Float3 {
    val userLocation = parameters.userLocation!!
    val ecef = CoordinateConverter.geodeticToECEF(
        userLocation.x.toDouble(),  // latitude
        userLocation.y.toDouble(),  // longitude
        userLocation.z.toDouble()   // altitude
    )
    return CoordinateConverter.ecefToScenePos(ecef)
}
```

### SatelliteManager

**Lokalizacja**: `scene3d/manager/satellite/SatelliteManager.kt`

**Odpowiedzialność**: Zarządzanie satelitami, ich pozycjami i wizualizacją.

**Wysokości orbit konstelacji** (w metrach):
```kotlin
private val constellationAltitudes = mapOf(
    "GPS" to 20_180_000f,
    "GLONASS" to 19_100_000f,
    "Galileo" to 23_222_000f,
    "BeiDou" to 21_500_000f,
    "QZSS" to 35_800_000f,
    "IRNSS" to 36_000_000f,
    "SBAS" to 35_786_000f
)
```

**Kolorowanie satelitów według SNR (C/N0)**:
```kotlin
private fun getColorForSNR(snr: Float): Float4 {
    return when {
        snr == NO_CNO -> Float4(1.0f, 0.0f, 0.0f, 1f)      // Czerwony - brak
        snr <= POOR_CNO -> Float4(0.89f, 0.18f, 0.14f, 1f) // Czerwony - słaby
        snr <= FAIR_CNO -> Float4(1.0f, 0.65f, 0.0f, 1f)   // Pomarańczowy
        snr <= GOOD_CNO -> Float4(0.97f, 0.97f, 0.0f, 1f)  // Żółty
        else -> Float4(0.0f, 1.0f, 0.4f, 1f)               // Zielony - dobry
    }
}
```

**Pool węzłów** (optymalizacja):
```kotlin
private val satelliteNodePool = mutableListOf<ModelNode>()
val activeSatelliteNodes = mutableMapOf<String, ModelNode>()
```

**Kluczowe metody**:
```kotlin
// Aktualizacja listy satelitów
fun updateSatelliteList(
    newList: List<GNSSStatusData>, 
    azElHistory: Map<String, AzElHistory>
)

// Zarządzanie klikniętym satelitą
fun setClickedSatelliteKey(key: String?)

// Zarządzanie orbitą
fun showOrbitForSatellite(key: String)
fun clearOrbit()

// Aktualizacja parametrów
fun updateParameters(newParameters: Scene3DParameters)

// Aktualizacja w każdej klatce
fun onFrame()

// Sprzątanie
fun cleanup()
```

**Logika aktualizacji**:
1. Wykrycie satelitów które zniknęły
2. Zwrócenie węzłów do poola
3. Aktualizacja istniejących satelitów (jeśli azymut/elewacja się zmieniły)
4. Dodanie nowych satelitów z poola lub stworzenie nowych

**Progi aktualizacji**:
```kotlin
private const val AZIMUTH_THRESHOLD_DEG = 0.2f
private const val ELEVATION_THRESHOLD_DEG = 0.2f
```

### OrbitManager

**Lokalizacja**: `scene3d/manager/satellite/OrbitManager.kt`

**Odpowiedzialność**: Wizualizacja orbity wybranego satelity.

**Funkcjonalność**:
```kotlin
fun showOrbitForSatellite(cache: SatelliteCache?, key: String)
fun clearOrbit()
fun updateOrbitForCache(cache: SatelliteCache?)
```

**Algorytm obliczania rotacji orbity**:
1. Oblicz wektor normalny do płaszczyzny orbity (cross product pozycji)
2. Przelicz na kwaternion (axis-angle to quaternion)
3. Konwertuj kwaternion na kąty Eulera

**Skalowanie orbity**:
```kotlin
val r1 = firstPos.length()
val r2 = lastPos.length()
val radius = (r1 + r2) / 2f
node.scale = Float3(radius, radius, radius)
```

### AuraManager

**Lokalizacja**: `scene3d/manager/satellite/AuraManager.kt`

**Odpowiedzialność**: Zarządzanie aurami (halo) wokół satelitów (obecnie nieużywane).

**Uwaga**: Manager zaimplementowany, ale animacje pulsowania są wyłączone w kodzie (zakomentowane wywołania `startPulseAnimation`).

---

## Parametry konfiguracyjne

### Parametry renderowania

#### HDR Color Buffer Quality
- **Typ**: `QualityLevel` (LOW, MEDIUM, HIGH, ULTRA)
- **Domyślnie**: MEDIUM
- **Wpływ**: Jakość bufora kolorów HDR, wpływa na jakość oświetlenia

#### Dynamic Resolution
- **Typ**: Boolean + QualityLevel
- **Domyślnie**: Enabled, MEDIUM
- **Wpływ**: Dynamiczne dostosowywanie rozdzielczości renderowania dla wydajności

#### Anti-Aliasing
- **MSAA** (Multi-Sample): Domyślnie wyłączone (kosztowne wydajnościowo)
- **FXAA** (Fast Approximate): Domyślnie włączone (szybkie)
- **TAA** (Temporal): Domyślnie wyłączone

#### Post-processing
- **Ambient Occlusion**: Domyślnie wyłączone (kosztowne)
- **Bloom**: Domyślnie włączone (efekt świecenia)
- **Screen Space Reflections**: Domyślnie wyłączone (bardzo kosztowne)

### Parametry oświetlenia

```kotlin
lightIntensity: Float = 250_000.0f      // Intensywność światła
lightColor: Float3 = (1.0, 1.0, 1.0)    // Kolor RGB
lightFalloff: Float = 1000.0f           // Spadek intensywności
lightType: DIRECTIONAL                  // Typ światła
```

### Parametry modeli

**Ziemia**:
- Domyślny model: `models/NASA_EARTH.glb`
- Skala: 1.0

**Satelity**:
- Domyślny model: `models/RedCircle.glb`
- Skala: 0.08

**Marker lokalizacji**:
- Model: `models/pointer.glb`
- Skala: 0.05 (dynamicznie dostosowywana)

### Środowisko
- Domyślne HDR: `envs/NightSkyHDRI002_2K.hdr`
- Intensywność: 1.0

---

## Komponenty UI

### Scene3DScreen

**Lokalizacja**: `scene3d/ui/Scene3DScreen.kt`

**Opis**: Główny ekran sceny 3D z menu i info boxami.

**Struktura**:
```
┌─────────────────────────────────────────┐
│  Menu (prawe)  │   Scena 3D             │
│  ┌──────────┐  │                        │
│  │Satellites│  │     [Model Ziemi]      │
│  │ Settings │  │  [Satelity]  [Marker]  │
│  └──────────┘  │                        │
└─────────────────────────────────────────┘
         │                    │
    Info Box (lewy)    Info Box (lewy)
    [Satellite Info]   [Earth Info]
```

**Animacje**:
- **Menu**: Wysuwane z lewej strony (300ms)
- **Info Boxes**: Przełączanie z animacją slide (300ms)
- **Loading Screen**: Fade in/out (100ms)

**Zarządzanie stanem**:
```kotlin
// Filtrowanie satelitów
val selectedConstellations = remember { mutableStateListOf<String>() }
var onlyUsedInFix by remember { mutableStateOf(false) }

// Scena i parametry
val scene = remember { Scene3D(...) }
val parametersState = remember { Scene3DParametersState() }

// Info boxy
val isSatelliteInfoBoxVisible by scene.satelliteInfoBoxVisible.collectAsState()
val isEarthInfoBoxVisible by scene.earthInfoBoxVisible.collectAsState()
```

### Scene3DLoadingScreen

**Lokalizacja**: `scene3d/ui/Scene3DLoadingScreen.kt`

**Opis**: Animowany ekran ładowania z wizualizacją orbit satelitarnych.

**Elementy**:
- Tytuł "GNSS Monitor"
- Animowane pierścienie orbitalne (rotation)
- Centralny "model Ziemi" (niebieski punkt)
- Wskaźnik postępu (CircularProgressIndicator)
- Tekst "Initializing 3D Scene..."

### Scene3DParametersMenu

**Lokalizacja**: `scene3d/ui/menu/Scene3DParametersMenu.kt`

**Opis**: Menu konfiguracji parametrów sceny.

**Sekcje**:
1. **Light Parameters**
   - Logarytmiczny slider intensywności (10k - 100M)
   - Suwaki RGB dla koloru światła
   - Podgląd koloru

2. **Earth Model**
   - Dropdown wyboru modelu Ziemi

3. **Satellites**
   - Dropdown wyboru modelu satelity

4. **Rendering Quality**
   - HDR Color Buffer quality selector
   - Dynamic Resolution switch + quality
   - MSAA switch
   - FXAA switch
   - Temporal AA switch
   - Ambient Occlusion switch
   - Bloom switch

**Przycisk Reset**: Przywrócenie wartości domyślnych

### SatelliteFilterMenu

**Lokalizacja**: `scene3d/ui/menu/SatelliteFilterMenu.kt`

**Opis**: Menu filtrowania widocznych satelitów.

**Funkcjonalność**:
- Checkboxy dla konstelacji (GPS, GLONASS, Galileo, etc.)
- Przełącznik "Only Used in Fix"
- Przełącznik widoczności markera lokalizacji
- Przycisk powrotu do głównego ekranu

---

## Przepływ danych

### Inicjalizacja sceny

```
┌─────────────────────┐
│  Scene3DScreen      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ rememberEngine()    │ ◄─── Filament Engine
│ rememberView()      │ ◄─── View (viewport)
│ rememberRenderer()  │ ◄─── Renderer
│ rememberScene()     │ ◄─── Scene (graph)
│ rememberModelLoader │ ◄─── Model Loader
│ rememberEnvironment │ ◄─── Environment Loader
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│   Scene3D(...)      │ ◄─── Inicjalizacja
└──────────┬──────────┘
           │
           ├─► CameraManager
           ├─► EarthManager
           ├─► LocationMarkerManager
           ├─► LightHandler
           └─► SatelliteManager
                    └─► OrbitManager
```

### Aktualizacja satelitów

```
┌───────────────────┐
│  GNSSViewModel    │
│  - satelliteList  │
│  - azElHistory    │
└─────────┬─────────┘
          │
          │ collectAsState()
          ▼
┌───────────────────┐
│  Scene3DScreen    │
│  - filteredSats   │
└─────────┬─────────┘
          │
          │ LaunchedEffect
          ▼
┌───────────────────┐
│ Scene3D           │
│ updateSatellite   │
│ List()            │
└─────────┬─────────┘
          │
          ▼
┌───────────────────┐
│ SatelliteManager  │
│ - Add new sats    │
│ - Update existing │
│ - Remove old      │
│ - Update colors   │
└───────────────────┘
```

### Pętla renderowania (onFrame)

```
Scene3D.onFrame()
    │
    ├─► cameraManager.onFrame()
    │   └─► Aktualizuj pozycję kamery
    │       Przelicz threshold
    │
    ├─► locationMarkerManager.onFrame()
    │   └─► Jeśli ruch kamery > threshold:
    │       - Zaktualizuj skalę markera
    │       - Zaktualizuj rotację
    │
    ├─► satelliteManager.onFrame()
    │   └─► Jeśli ruch kamery > threshold:
    │       - Zaktualizuj lookAt dla satelitów
    │
    └─► mainLightManager.onFrame()
        └─► Jeśli ruch kamery > threshold:
            - Przelicz kierunek światła
            - Odtwórz węzeł światła
```

### Interakcje użytkownika

```
Użytkownik dotyka sceny
    │
    ├─► Single Tap
    │   │
    │   ├─► Hit: Satellite Node
    │   │   └─► - Pokaż SatelliteInfoBox
    │   │       - Pokaż orbitę
    │   │       - Zmień kolor satelity
    │   │
    │   ├─► Hit: Earth Node
    │   │   └─► - Pokaż EarthInfoBox
    │   │       - Ukryj orbitę
    │   │
    │   └─► Hit: Nothing
    │       └─► - Ukryj info boxy
    │           - Ukryj orbitę
    │
    └─► Double Tap
        └─► Przełącz widoczność menu
```

---

## API publiczne

### Scene3D

#### Metody publiczne

```kotlin
/**
 * Renderuje scenę 3D w Compose
 */
@Composable
fun Render()

/**
 * Aktualizuje listę satelitów do wyświetlenia
 * @param newList Lista danych satelitarnych GNSS
 * @param azElHistory Historia azymutów i elewacji satelitów
 */
fun updateSatelliteList(
    newList: List<GNSSStatusData>, 
    azElHistory: Map<String, AzElHistory>
)

/**
 * Aktualizuje parametry sceny
 * @param newParameters Nowe parametry konfiguracyjne
 */
fun updateParameters(newParameters: Scene3DParameters)

/**
 * Aktualizuje lokalizację użytkownika
 * @param userLocation Współrzędne w formacie (lat, lon, alt)
 */
fun updateUserLocation(userLocation: Float3?)

/**
 * Ustawia widoczność markera lokalizacji
 * @param visible true jeśli marker ma być widoczny
 */
fun setLocationMarkerVisible(visible: Boolean)

/**
 * Sprawdza czy marker lokalizacji jest widoczny
 * @return true jeśli marker jest widoczny
 */
fun isLocationMarkerVisible(): Boolean

/**
 * Sprawdza czy menu jest widoczne
 * @return true jeśli menu jest widoczne
 */
fun isMenuVisible(): Boolean

/**
 * Zwolnienie zasobów sceny
 * Wywołać w onDispose
 */
fun cleanup()
```

#### StateFlows (observable)

```kotlin
/**
 * Klucz klikniętego satelity (format: "CONSTELLATION:PRN")
 * null jeśli żaden satelita nie jest kliknięty
 */
val clickedSatelliteKeyState: StateFlow<String?>

/**
 * Czy okno info satelity jest widoczne
 */
val satelliteInfoBoxVisible: StateFlow<Boolean>

/**
 * Czy okno info Ziemi jest widoczne
 */
val earthInfoBoxVisible: StateFlow<Boolean>
```

### Scene3DParametersState

#### Metody aktualizacji

```kotlin
// Oświetlenie
fun updateLightIntensity(intensity: Float)
fun updateLightColor(red: Float, green: Float, blue: Float)

// Modele
fun updateEarthModel(path: String)
fun updateSatelliteModel(path: String)

// Jakość renderowania
fun updateHdrColorBufferQuality(quality: Scene3DParameters.QualityLevel)
fun updateDynamicResolutionEnabled(enabled: Boolean)
fun updateDynamicResolutionQuality(quality: Scene3DParameters.QualityLevel)
fun updateMsaaEnabled(enabled: Boolean)
fun updateFxaaEnabled(enabled: Boolean)
fun updateTemporalAntiAliasingEnabled(enabled: Boolean)
fun updateAmbientOcclusionEnabled(enabled: Boolean)
fun updateBloomEnabled(enabled: Boolean)

// Lokalizacja
fun updateLocation(location: Float3?)

// Reset
fun resetToDefaults()
```

#### Dostęp do parametrów

```kotlin
val parameters: Scene3DParameters  // Observable przez Compose State
```

---

## Przykłady użycia

### Podstawowa inicjalizacja w Composable

```kotlin
@Composable
fun MyScreen() {
    // Inicjalizacja komponentów Filament
    val engine = rememberEngine()
    val view = rememberView(engine)
    val renderer = rememberRenderer(engine)
    val coreScene = rememberScene(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    
    // Stan parametrów
    val parametersState = remember { Scene3DParametersState() }
    
    // Stworzenie sceny
    val scene = remember {
        Scene3D(
            engine = engine,
            view = view,
            renderer = renderer,
            scene = coreScene,
            modelLoader = modelLoader,
            environmentLoader = environmentLoader,
            parameters = parametersState.parameters,
            modifier = Modifier.fillMaxSize()
        )
    }
    
    // Renderowanie
    scene.Render()
    
    // Sprzątanie przy wyjściu
    DisposableEffect(Unit) {
        onDispose {
            scene.cleanup()
        }
    }
}
```

### Aktualizacja satelitów

```kotlin
// ViewModel
class GNSSViewModel : ViewModel() {
    val satelliteList = MutableStateFlow<List<GNSSStatusData>>(emptyList())
    val azElHistory = MutableStateFlow<Map<String, AzElHistory>>(emptyMap())
}

// Composable
@Composable
fun Scene3DScreen(gnssViewModel: GNSSViewModel) {
    val scene = remember { /* ... */ }
    
    val satelliteList by gnssViewModel.satelliteList.collectAsState()
    val azElHistory by gnssViewModel.azElHistory.collectAsState()
    
    // Automatyczna aktualizacja przy zmianie danych
    LaunchedEffect(satelliteList, azElHistory) {
        scene.updateSatelliteList(satelliteList, azElHistory)
    }
    
    scene.Render()
}
```

### Zmiana parametrów z menu

```kotlin
@Composable
fun Scene3DParametersMenu(
    parametersState: Scene3DParametersState,
    onParametersChanged: (Scene3DParameters) -> Unit
) {
    Column {
        // Slider intensywności światła
        Slider(
            value = parametersState.parameters.lightIntensity,
            onValueChange = { newValue ->
                parametersState.updateLightIntensity(newValue)
                onParametersChanged(parametersState.parameters)
            },
            valueRange = 10_000f..100_000_000f
        )
        
        // Przełącznik bloom
        Switch(
            checked = parametersState.parameters.bloomEnabled,
            onCheckedChange = { enabled ->
                parametersState.updateBloomEnabled(enabled)
                onParametersChanged(parametersState.parameters)
            }
        )
    }
}

// Użycie w ekranie
val parametersState = remember { Scene3DParametersState() }
val scene = remember { Scene3D(..., parameters = parametersState.parameters) }

Scene3DParametersMenu(
    parametersState = parametersState,
    onParametersChanged = { newParams ->
        scene.updateParameters(newParams)
    }
)
```

### Obserwowanie klikniętego satelity

```kotlin
@Composable
fun Scene3DWithInfo(scene: Scene3D) {
    val clickedSatelliteKey by scene.clickedSatelliteKeyState.collectAsState()
    val isSatelliteInfoVisible by scene.satelliteInfoBoxVisible.collectAsState()
    
    Box {
        // Renderowanie sceny
        scene.Render()
        
        // Info box gdy satelita kliknięty
        AnimatedVisibility(visible = isSatelliteInfoVisible) {
            SatelliteInfoBox(
                satelliteKey = clickedSatelliteKey,
                // ... inne parametry
            )
        }
    }
}
```

### Aktualizacja lokalizacji użytkownika

```kotlin
@Composable
fun Scene3DScreen(
    scene: Scene3D,
    locationViewModel: LocationViewModel
) {
    val location by locationViewModel.location.collectAsState()
    
    // Konwersja lokalizacji na Float3
    val userLocation = remember(location) {
        if (location.isValid) {
            Float3(
                location.latitude.toFloat(),
                location.longitude.toFloat(),
                location.altitude.toFloat()
            )
        } else null
    }
    
    // Aktualizacja w scenie
    LaunchedEffect(userLocation) {
        scene.updateUserLocation(userLocation)
    }
    
    scene.Render()
}
```

### Przełączanie widoczności markera lokalizacji

```kotlin
@Composable
fun LocationMarkerControl(scene: Scene3D) {
    var showMarker by remember { 
        mutableStateOf(scene.isLocationMarkerVisible()) 
    }
    
    Switch(
        checked = showMarker,
        onCheckedChange = { visible ->
            showMarker = visible
            scene.setLocationMarkerVisible(visible)
        }
    )
}
```

### Filtrowanie satelitów

```kotlin
@Composable
fun SatelliteFilter(
    allSatellites: List<GNSSStatusData>,
    scene: Scene3D
) {
    val selectedConstellations = remember { 
        mutableStateListOf<String>() 
    }
    var onlyUsedInFix by remember { mutableStateOf(false) }
    
    // Filtrowanie
    val filteredSatellites = remember(
        allSatellites, 
        selectedConstellations.toList(), 
        onlyUsedInFix
    ) {
        allSatellites.filter { sat ->
            selectedConstellations.contains(sat.constellation) && 
            (!onlyUsedInFix || sat.usedInFix)
        }
    }
    
    // Aktualizacja w scenie
    LaunchedEffect(filteredSatellites) {
        scene.updateSatelliteList(filteredSatellites, azElHistory)
    }
    
    // UI filtrów
    Column {
        ConstellationCheckboxes(selectedConstellations)
        Switch(
            checked = onlyUsedInFix,
            onCheckedChange = { onlyUsedInFix = it }
        )
    }
}
```

### Reset parametrów do wartości domyślnych

```kotlin
@Composable
fun SettingsMenu(
    parametersState: Scene3DParametersState,
    scene: Scene3D
) {
    Button(
        onClick = {
            parametersState.resetToDefaults()
            scene.updateParameters(parametersState.parameters)
        }
    ) {
        Text("Reset to Defaults")
    }
}
```

---

## Optymalizacje wydajności

### 1. Node Pooling (SatelliteManager)
```kotlin
private val satelliteNodePool = mutableListOf<ModelNode>()
```
- Ponowne użycie węzłów zamiast tworzenia nowych
- Redukcja alokacji pamięci i garbage collection

### 2. Dynamic Update Threshold
```kotlin
dynamicUpdateThreshold = cameraDistanceToCenter * UPDATE_THRESHOLD
```
- Adaptacyjny threshold aktualizacji zależny od odległości kamery
- Mniej aktualizacji gdy kamera daleko od centrum

### 3. Frame-based Updates
```kotlin
private var frameCount = 0
if (frameCount >= frameCountUpdateInterval) {
    // Update
}
```
- Aktualizacje co N klatek zamiast w każdej klatce
- Zmniejszenie obciążenia CPU

### 4. Conditional Rendering
```kotlin
if (shouldUpdate) {
    updateLookAt()
}
```
- Aktualizacja tylko gdy kamera się znacząco przesunęła
- Unikanie zbędnych obliczeń

### 5. Light Recreation Throttling
```kotlin
if (currentTime - lastRecreationTime < minRecreationInterval) return
```
- Ograniczenie częstotliwości przetwarzania światła do 60 FPS

### 6. Material Instance Caching
```kotlin
val primIdx = 0
val matIdx = 0
val mat = node.materialInstances.getOrNull(primIdx)?.getOrNull(matIdx)
```
- Bezpośredni dostęp do instancji materiału
- Unikanie iteracji po wszystkich materiałach

---

## Debugowanie

### Tagi logów
- `Scene3D` - główne operacje sceny
- `SatelliteManager` - zarządzanie satelitami
- `CameraDebug` - operacje kamery
- `AuraManager` - zarządzanie aurami
- `LocationMarkerPos` - pozycjonowanie markera
- `NodeHit` - detekcja kliknięć

### Typowe problemy

#### 1. Satelity nie są widoczne
**Przyczyny**:
- Brak lokalizacji użytkownika (`userLocation == null`)
- Wysokość orbity ustawiona na 0
- Satelity poza frustum kamery

**Rozwiązanie**:
```kotlin
// Sprawdź logi
Log.e("SatPos", "Constellation: ${sat.constellation} altitude: $altitude")
```

#### 2. Scena nie aktualizuje się
**Przyczyny**:
- Brak wywołania `onFrame` callbacks
- Threshold aktualizacji za wysoki

**Rozwiązanie**:
```kotlin
// Zredukuj threshold
private const val UPDATE_THRESHOLD = 0.001f  // zmniejsz wartość
```

#### 3. Niska wydajność
**Przyczyny**:
- Za dużo efektów post-processingu włączonych
- MSAA włączone
- SSR włączone

**Rozwiązanie**:
```kotlin
Scene3DParameters(
    msaaEnabled = false,
    screenSpaceReflectionsEnabled = false,
    ambientOcclusionEnabled = false
)
```

---

## Dalszy rozwój

### Planowane funkcjonalności
1. **Aura animations** - dokończenie animacji pulsujących aur
2. **Multiple light sources** - dodanie dodatkowych źródeł światła
3. **Satellite trails** - ślady orbit w czasie
4. **Ground stations** - wizualizacja stacji naziemnych
5. **Signal strength visualization** - wizualizacja zasięgu sygnału

### Możliwe ulepszenia
1. **LOD (Level of Detail)** - różne poziomy szczegółowości modeli
2. **Frustum culling** - nie renderuj obiektów poza kamerą
3. **Instanced rendering** - dla dużej liczby satelitów
4. **Custom shaders** - specjalne efekty materiałów
5. **VR support** - wsparcie dla wirtualnej rzeczywistości

---

## Licencja i odniesienia

### Dane orbit satelitarnych
- GPS, GLONASS, Galileo: https://en.wikipedia.org/wiki/Satellite_navigation
- BeiDou: https://en.wikipedia.org/wiki/List_of_BeiDou_satellites

### Modele 3D
- Ziemia: NASA Earth model
- Satelity: Custom models

### Bibliografia
- Google Filament Documentation: https://google.github.io/filament/
- SceneView Documentation: https://github.com/SceneView/sceneview-android
- Android Jetpack Compose: https://developer.android.com/jetpack/compose

---

**Wersja dokumentacji**: 1.0  
**Data ostatniej aktualizacji**: 2026-01-08  
**Autor**: Dokumentacja wygenerowana dla modułu scene3d GPS Satellite Viewer
