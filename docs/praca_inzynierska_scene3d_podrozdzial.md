# 4.3 Wizualizacja 3D satelitów GNSS

## 4.3.1 Wprowadzenie do modułu Scene3D

Jednym z kluczowych elementów aplikacji GPS Satellite Viewer jest moduł wizualizacji 3D, który umożliwia użytkownikowi przestrzenne oglądanie pozycji satelitów w czasie rzeczywistym. Moduł ten, określany jako `scene3d`, stanowi kompleksowe rozwiązanie łączące zaawansowane techniki renderowania 3D z danymi pozycjonowania satelitarnego odbieranymi z systemu Android.

### Cel i założenia modułu

Głównym celem modułu `scene3d` jest zapewnienie użytkownikowi intuicyjnego i efektywnego sposobu wizualizacji konstelacji satelitów GNSS w trójwymiarowej przestrzeni. Moduł został zaprojektowany z następującymi założeniami:

1. **Realistyczna reprezentacja przestrzenna** - satelity są wyświetlane w rzeczywistych pozycjach względem Ziemi, obliczonych na podstawie danych azymutowych i elewacyjnych
2. **Interaktywność** - użytkownik może swobodnie obracać scenę, przybliżać oraz klikać w poszczególne obiekty w celu uzyskania szczegółowych informacji
3. **Wydajność** - aplikacja musi działać płynnie na urządzeniach mobilnych, co wymaga optymalizacji renderowania i zarządzania zasobami
4. **Informacyjność** - wizualizacja zawiera dodatkowe wskaźniki jakości sygnału poprzez kolorowanie satelitów według poziomu SNR (Signal-to-Noise Ratio)

### Architektura i komponenty

Moduł `scene3d` został zaimplementowany w oparciu o wzorzec architektoniczny **Manager Pattern**, w którym centralna klasa orkiestrująca (`Scene3D`) koordynuje pracę wyspecjalizowanych menedżerów odpowiedzialnych za poszczególne aspekty sceny. Taka struktura zapewnia wysoki stopień separacji odpowiedzialności (Separation of Concerns) oraz ułatwia rozwój i utrzymanie kodu.

Główne komponenty modułu przedstawiono na rysunku 4.X:

```
┌─────────────────────────────────────────────────────────┐
│                    Scene3D                               │
│              (Klasa orkiestrująca)                      │
│  • Koordynacja managerów                                │
│  • Obsługa interakcji użytkownika                       │
│  • Zarządzanie cyklem życia komponentów                 │
└────────────┬────────────────────────────────────────────┘
             │
    ┌────────┴────────┬──────────┬──────────┬─────────┐
    │                 │          │          │         │
    ▼                 ▼          ▼          ▼         ▼
┌─────────┐   ┌──────────┐  ┌────────┐  ┌──────┐  ┌─────────┐
│ Camera  │   │  Earth   │  │Satellit│  │Light │  │Location │
│ Manager │   │ Manager  │  │Manager │  │Handle│  │ Marker  │
└─────────┘   └──────────┘  └────┬───┘  └──────┘  └─────────┘
                                  │
                          ┌───────┴────────┐
                          │                │
                          ▼                ▼
                    ┌─────────┐      ┌─────────┐
                    │  Orbit  │      │  Aura   │
                    │ Manager │      │ Manager │
                    └─────────┘      └─────────┘
```

*Rysunek 4.X: Architektura modułu Scene3D*

Kluczowe komponenty systemu:

- **Scene3D** - główna klasa orkiestrująca, odpowiedzialna za inicjalizację wszystkich menedżerów, koordynację aktualizacji w każdej klatce renderowania oraz obsługę gestów użytkownika
- **CameraManager** - zarządza kamerą wirtualną, jej pozycją, rotacją oraz ograniczeniami ruchu (zoom, orbit)
- **EarthManager** - odpowiada za załadowanie i wyświetlenie modelu 3D Ziemi
- **SatelliteManager** - zarządza populacją satelitów, ich pozycjami, kolorowaniem oraz optymalizacją poprzez pooling węzłów
- **LightHandler** - kontroluje oświetlenie sceny, dynamicznie dostosowując kierunek światła słonecznego w zależności od pozycji kamery
- **LocationMarkerManager** - wyświetla marker wskazujący pozycję użytkownika na powierzchni Ziemi
- **OrbitManager** - wizualizuje orbitę wybranego satelity
- **AuraManager** - zarządza efektami wizualnymi wokół satelitów (obecnie w fazie rozwoju)

### Wykorzystane technologie

Implementacja modułu scene3d opiera się na następujących technologiach:

#### Google Filament
Google Filament [1] to wysokowydajny, otwartoźródłowy silnik renderowania 3D w czasie rzeczywistym, stworzony specjalnie dla platform mobilnych. Filament wykorzystuje nowoczesne techniki renderingu opartego fizycznie (Physically Based Rendering - PBR), co zapewnia realistyczny wygląd sceny przy jednoczesnym zachowaniu wysokiej wydajności. Silnik ten został wybrany ze względu na:

- Optymalizację pod kątem urządzeń mobilnych
- Wsparcie dla zaawansowanych efektów renderowania (HDR, bloom, anti-aliasing)
- Wydajne zarządzanie zasobami graficznymi
- Dobrą integrację z ekosystemem Android

#### SceneView
SceneView (wersja 2.3.0) [2] stanowi warstwę abstrakcji nad silnikiem Filament, znacząco upraszczającą integrację renderowania 3D w aplikacjach Android. Biblioteka ta dostarcza:

- Gotowe komponenty Jetpack Compose do wyświetlania scen 3D
- Uproszczony system zarządzania węzłami sceny (Node System)
- Integrację z gestami dotykowymi Android
- Mechanizmy ładowania modeli 3D w formatach GLB/GLTF

#### Jetpack Compose
Interfejs użytkownika modułu został zaimplementowany w całości przy użyciu Jetpack Compose - nowoczesnego, deklaratywnego frameworka UI dla Android. Compose umożliwia reaktywne zarządzanie stanem interfejsu oraz płynną integrację renderowania 3D z resztą aplikacji.

### Przepływ danych i cykl życia

Moduł scene3d integruje się z architekturą MVVM (Model-View-ViewModel) aplikacji, odbierając dane o satelitach z warstwy ViewModels. Podstawowy przepływ danych przedstawia się następująco:

1. **Pozyskiwanie danych** - `GNSSViewModel` odbiera dane o satelitach z systemu Android (LocationManager, GnssStatus)
2. **Przetwarzanie** - dane są przetwarzane i agregowane w struktury `GNSSStatusData` zawierające informacje o konstelacji, PRN, azymucie, elewacji oraz jakości sygnału
3. **Transformacja geometryczna** - współrzędne azymutowo-elewacyjne są konwertowane do układu ECEF (Earth-Centered, Earth-Fixed), a następnie do lokalnego układu współrzędnych sceny 3D
4. **Renderowanie** - przekształcone dane są przekazywane do `SatelliteManager`, który aktualizuje pozycje węzłów 3D reprezentujących satelity

Cykl życia sceny 3D jest ściśle związany z cyklem życia komponentu Compose:

```kotlin
@Composable
fun Scene3DScreen() {
    // Inicjalizacja komponentów Filament
    val engine = rememberEngine()
    val view = rememberView(engine)
    val renderer = rememberRenderer(engine)
    
    // Utworzenie sceny
    val scene = remember {
        Scene3D(engine, view, renderer, ...)
    }
    
    // Renderowanie
    scene.Render()
    
    // Sprzątanie zasobów
    DisposableEffect(Unit) {
        onDispose { scene.cleanup() }
    }
}
```

### Optymalizacje wydajności

Ze względu na ograniczenia obliczeniowe urządzeń mobilnych, w module zaimplementowano szereg optymalizacji:

1. **Node Pooling** - węzły reprezentujące satelity są ponownie wykorzystywane zamiast tworzone od nowa, co redukuje alokację pamięci i presję na garbage collector
   
2. **Dynamiczny threshold aktualizacji** - częstotliwość aktualizacji pozycji i orientacji obiektów jest dostosowywana dynamicznie w zależności od odległości kamery od centrum sceny

3. **Frame-based updates** - kosztowne operacje (np. aktualizacja kierunku światła) są wykonywane co N klatek zamiast w każdej klatce

4. **Conditional rendering** - obiekty są aktualizowane tylko wtedy, gdy zmiana jest znacząca (przekroczenie progu)

5. **Konfigurowalna jakość renderowania** - użytkownik może dostosować parametry jakości (anti-aliasing, bloom, ambient occlusion) do możliwości swojego urządzenia

### Transformacje układów współrzędnych

Kluczowym wyzwaniem technicznym w implementacji była prawidłowa transformacja między różnymi układami współrzędnych:

1. **Układ geograficzny** (Geodetic) - szerokość i długość geograficzna użytkownika
2. **Układ azymutalno-elewacyjny** (Horizontal) - dane odbierane z Android LocationManager
3. **Układ ECEF** - geocentryczny układ kartezjański związany z Ziemią
4. **Układ lokalny sceny 3D** - układ związany z centrum renderowanego modelu Ziemi

Transformacje te są realizowane przez dedykowany moduł `CoordinateConverter`, który implementuje standardowe algorytmy geodezyjne:

```kotlin
// Konwersja pozycji użytkownika do układu sceny
val ecef = CoordinateConverter.geodeticToECEF(
    latitude, longitude, altitude
)
val scenePos = CoordinateConverter.ecefToScenePos(ecef)

// Konwersja pozycji satelity
val satEcef = CoordinateConverter.azElToECEF(
    azimuth, elevation, userLocation, satelliteAltitude
)
val satScenePos = CoordinateConverter.ecefToScenePos(satEcef)
```

Poprawność tych transformacji jest kluczowa dla realistycznego przedstawienia geometrii konstelacji satelitarnej.

### Interakcja użytkownika

Moduł scene3d implementuje dwa podstawowe typy interakcji:

1. **Manipulacja kamerą**
   - Przeciąganie palcem (drag) - obrót orbitalny wokół Ziemi
   - Szczypanie (pinch) - przybliżanie i oddalanie kamery
   - Ograniczenia - kamera nie może zbliżyć się bardziej niż 0.62 jednostki ani oddalić dalej niż 9 jednostek od centrum; kąt pochylenia ograniczony do ±85°

2. **Selekcja obiektów**
   - Pojedyncze kliknięcie - wybór satelity lub Ziemi, wyświetlenie panelu informacyjnego
   - Podwójne kliknięcie - przełączanie widoczności menu konfiguracyjnego

Implementacja detekcji kolizji (ray casting) została zrealizowana przy pomocy wbudowanych mechanizmów biblioteki SceneView, które automatycznie wykrywają węzeł znajdujący się pod punktem dotyku.

### Wizualizacja jakości sygnału

Istotną funkcjonalnością modułu jest wizualne kodowanie jakości sygnału satelitarnego poprzez system kolorowania. Każdy satelita jest kolorowany zgodnie z jego wartością C/N₀ (Carrier-to-Noise Density Ratio):

| Zakres C/N₀ [dB-Hz] | Kolor          | Interpretacja          |
|---------------------|----------------|------------------------|
| Brak sygnału        | Czerwony       | Satelita niewidoczny   |
| 0 - 20              | Czerwony       | Sygnał bardzo słaby    |
| 20 - 28             | Pomarańczowy   | Sygnał słaby           |
| 28 - 35             | Żółty          | Sygnał dobry           |
| > 35                | Zielony        | Sygnał bardzo dobry    |

*Tabela 4.X: Schemat kolorowania satelitów według jakości sygnału*

Taki schemat kolorowania pozwala użytkownikowi na szybką ocenę wizualną jakości dostępnych sygnałów oraz identyfikację potencjalnych problemów z odbiorem (np. przeszkody na horyzoncie).

### Podsumowanie

Moduł scene3d stanowi kompleksowe rozwiązanie do wizualizacji 3D satelitów GNSS, łącząc zaawansowane techniki renderowania z danymi pozycjonowania w czasie rzeczywistym. Jego architektura oparta na wzorcu Manager Pattern oraz szereg zaimplementowanych optymalizacji zapewniają płynne działanie na urządzeniach mobilnych. W kolejnych podrozdziałach zostaną omówione szczegóły implementacyjne poszczególnych komponentów oraz zastosowane algorytmy transformacji współrzędnych.

---

**Przypisy:**

[1] Google Filament - https://google.github.io/filament/

[2] SceneView for Android - https://github.com/SceneView/sceneview-android
