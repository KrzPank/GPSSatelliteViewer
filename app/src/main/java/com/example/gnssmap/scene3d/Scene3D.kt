package com.example.gnssmap.scene3d

import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gnssmap.data.AzElHistory
import com.example.gnssmap.data.GNSSStatusData
import com.example.gnssmap.scene3d.manager.camera.CameraManager
import com.example.gnssmap.scene3d.manager.EarthManager
import com.example.gnssmap.scene3d.manager.LightHandler
import com.example.gnssmap.scene3d.manager.LocationMarkerManager
import com.example.gnssmap.scene3d.manager.satellite.SatelliteManager
import com.google.android.filament.Engine
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.Texture.PixelBufferDescriptor
import com.google.android.filament.View
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.Scene
import io.github.sceneview.loaders.EnvironmentLoader
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.node.Node
import io.github.sceneview.rememberOnGestureListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class Scene3D(
    private val engine: Engine,
    private val view: View,
    private val renderer: Renderer,
    private val scene: Scene,
    private val modelLoader: ModelLoader,
    private val environmentLoader: EnvironmentLoader,
    private val modifier: Modifier = Modifier.Companion.fillMaxSize(),
    private var parameters: Scene3DParameters = Scene3DParameters()
) {
    private val centerNode = Node(engine)

    private val cameraManager: CameraManager = CameraManager(
        engine = engine,
        view = view,
        centerNode = centerNode,
        sceneParameters = parameters
    )
    // Inicjacja pozostałych menedżerów
    // ...

    private val earthManager: EarthManager = EarthManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        parameters = parameters
    )

    private val locationMarkerManager: LocationMarkerManager = LocationMarkerManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        cameraManager = cameraManager,
        parameters = parameters
    )

    private val mainLightManager: LightHandler = LightHandler(
        engine = engine,
        centerNode = centerNode,
        cameraManager = cameraManager,
        parameters = parameters
    )

    private val satelliteManager: SatelliteManager = SatelliteManager(
        modelLoader = modelLoader,
        centerNode = centerNode,
        cameraManager = cameraManager,
        parameters = parameters
    )

    // Menu on/off
    private var _menuVisible by mutableStateOf(true)
    private fun toggleMenu() { _menuVisible = !_menuVisible }
    fun isMenuVisible() = _menuVisible

    // satellite click handling
    private val _clickedSatelliteKey = MutableStateFlow<String?>(null)
    val clickedSatelliteKeyState: StateFlow<String?> = _clickedSatelliteKey.asStateFlow()

    private val _isSatelliteInfoBoxVisible = MutableStateFlow(false)
    val satelliteInfoBoxVisible: StateFlow<Boolean> = _isSatelliteInfoBoxVisible.asStateFlow()

    private val _isEarthInfoBoxVisible = MutableStateFlow(false)
    val earthInfoBoxVisible: StateFlow<Boolean> = _isEarthInfoBoxVisible.asStateFlow()

    private fun onSceneDoubleTap() {
        toggleMenu()
    }

    private fun onSceneSingleTapConfirmed(node: Node?) {
        val key = node?.name
        Log.d("NodeHit", "Hit node name: $key")
        when (key) {
            null -> {
                _clickedSatelliteKey.value = null
                _isSatelliteInfoBoxVisible.value = false
                _isEarthInfoBoxVisible.value = false
                satelliteManager.clearOrbit()
                satelliteManager.setClickedSatelliteKey(null)
            }
            earthManager.getEarthNode().name -> {
                _clickedSatelliteKey.value = null
                _isSatelliteInfoBoxVisible.value = false
                _isEarthInfoBoxVisible.value = !_isEarthInfoBoxVisible.value
                satelliteManager.clearOrbit()
                satelliteManager.setClickedSatelliteKey(null)
            }
            else -> {
                //val tKey = if (key.contains("aura")) key.substring(0, key.length - 5)
                //    else key
                _clickedSatelliteKey.value = key
                _isSatelliteInfoBoxVisible.value = true
                _isEarthInfoBoxVisible.value = false
                satelliteManager.setClickedSatelliteKey(key)
                satelliteManager.showOrbitForSatellite(key)
            }
        }
    }

    @Composable
    fun Render(onSceneLoaded: () -> Unit = {}) {
        var hasLoaded by remember { mutableStateOf(false) }

        applyVisualEffects(view, parameters)
        Scene(
            modifier = modifier,
            engine = engine,
            view = view,
            renderer = renderer,
            scene = scene,
            modelLoader = modelLoader,
            cameraNode = cameraManager.getCameraNode(),
            cameraManipulator = cameraManager.getCameraManipulator(),
            childNodes = listOf(centerNode),
            onViewCreated = { ->
                val engine = this.engine
                val context = this.context

                val cubeMapPaths = listOf(
                    "envs/px.png", "envs/nx.png",
                    "envs/py.png", "envs/ny.png",
                    "envs/pz.png", "envs/nz.png"
                )

                val bitmaps = cubeMapPaths.map { path ->
                    context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                }

                val width = bitmaps[0].width
                val height = bitmaps[0].height
                val faceSizeInBytes = bitmaps[0].byteCount

                val combinedBuffer = ByteBuffer.allocateDirect(faceSizeInBytes * 6)
                val faceOffsets = IntArray(6)

                bitmaps.forEachIndexed { index, bitmap ->
                    faceOffsets[index] = combinedBuffer.position()
                    bitmap.copyPixelsToBuffer(combinedBuffer)
                }
                combinedBuffer.flip()

                val pixelBuffer = PixelBufferDescriptor(
                    combinedBuffer,
                    Texture.Format.RGBA,
                    Texture.Type.UBYTE
                )

                val cubemapTexture = Texture.Builder()
                    .width(width)
                    .height(height)
                    .levels(1)
                    .sampler(Texture.Sampler.SAMPLER_CUBEMAP)
                    .format(Texture.InternalFormat.SRGB8_A8)
                    .build(engine)

                cubemapTexture.setImage(
                    engine,
                    0,
                    pixelBuffer,
                    faceOffsets
                )

                val skybox = Skybox.Builder()
                    .environment(cubemapTexture)
                    .build(engine)

                this.scene.skybox = skybox
                this.scene.indirectLight = null
            },
            onFrame = {
                cameraManager.onFrame()
                locationMarkerManager.onFrame()
                satelliteManager.onFrame()
                mainLightManager.onFrame()
                if (!hasLoaded) {
                    hasLoaded = true
                    onSceneLoaded()
                }
            },
            mainLightNode = mainLightManager.getSunLightNode(),
            onGestureListener = rememberOnGestureListener(
                onDown = { event, _ ->
                    true
                },
                onDoubleTap = { _, _ ->
                    //Log.d("CameraDebug", "Went in onDoubleTap")
                    onSceneDoubleTap()
                },
                onSingleTapConfirmed = { event, node ->
                    //Log.d("CameraDebug", "Went in onSingleTapConfirmed")
                    onSceneSingleTapConfirmed(node)
                },
            ),
        )
    }

    fun setLocationMarkerVisible(visible: Boolean) { locationMarkerManager.setVisible(visible) }
    fun isLocationMarkerVisible() = locationMarkerManager.isLocationMarkerVisible()

    fun updateSatelliteList(newList: List<GNSSStatusData>, azElHistory: Map<String, AzElHistory>){
        satelliteManager.updateSatelliteList(newList, azElHistory)
    }

    fun updateParameters(newParameters: Scene3DParameters) {
        val oldParameters = parameters

        // Assign new parameters (single source of truth)
        parameters = newParameters

        // Update sub-systems that depend on parameters
        mainLightManager.updateParameters(newParameters)
        satelliteManager.updateParameters(newParameters)
        earthManager.updateParameters(newParameters)
        locationMarkerManager.updateParameters(newParameters)

        updateVisualEffects(view, oldParameters, newParameters)
    }

    fun updateUserLocation(userLocation: Float3?) { parameters.userLocation = userLocation }

    fun cleanup() {
        satelliteManager.cleanup()
        locationMarkerManager.cleanup()
        earthManager.cleanup()

        mainLightManager.cleanup()

        cameraManager.cleanup()
        centerNode.destroy()
    }
}

private fun updateVisualEffects(
    view: View,
    oldParams: Scene3DParameters,
    newParams: Scene3DParameters
) {
    //Log.d("Scene3D", "applyVisualEffects: checking differences")

    // HDR Color Buffer Quality
    if (oldParams.hdrColorBufferQuality != newParams.hdrColorBufferQuality) {
        view.renderQuality = view.renderQuality.apply {
            hdrColorBuffer = when (newParams.hdrColorBufferQuality) {
                Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
            }
        }
    }

    // Dynamic Resolution enabled/quality
    if (oldParams.dynamicResolutionEnabled != newParams.dynamicResolutionEnabled ||
        oldParams.dynamicResolutionQuality != newParams.dynamicResolutionQuality
    ) {
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            if (newParams.dynamicResolutionEnabled) {
                quality = when (newParams.dynamicResolutionQuality) {
                    Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                    Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                    Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                    Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
                }
            }
        }
    }

    // MSAA
    if (oldParams.msaaEnabled != newParams.msaaEnabled) {
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
            enabled = newParams.msaaEnabled
        }
    }

    // FXAA
    if (oldParams.fxaaEnabled != newParams.fxaaEnabled) {
        view.antiAliasing =
            if (newParams.fxaaEnabled) View.AntiAliasing.FXAA
            else View.AntiAliasing.NONE
    }

    // TAA
    if (oldParams.temporalAntiAliasingEnabled != newParams.temporalAntiAliasingEnabled) {
        view.temporalAntiAliasingOptions = view.temporalAntiAliasingOptions.apply {
            enabled = newParams.temporalAntiAliasingEnabled
        }
    }

    // Ambient Occlusion
    if (oldParams.ambientOcclusionEnabled != newParams.ambientOcclusionEnabled) {
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = newParams.ambientOcclusionEnabled
        }
    }

    // Bloom
    if (oldParams.bloomEnabled != newParams.bloomEnabled) {
        view.bloomOptions = view.bloomOptions.apply {
            enabled = newParams.bloomEnabled
        }
    }

    // SSR
    if (oldParams.screenSpaceReflectionsEnabled != newParams.screenSpaceReflectionsEnabled) {
        view.screenSpaceReflectionsOptions = view.screenSpaceReflectionsOptions.apply {
            enabled = newParams.screenSpaceReflectionsEnabled
        }
    }
}

private fun applyVisualEffects(view: View, sceneParameters: Scene3DParameters) {
    // HDR Color Buffer Quality
    view.renderQuality = view.renderQuality.apply {
        hdrColorBuffer = when (sceneParameters.hdrColorBufferQuality) {
            Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
            Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
            Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
            Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
        }
    }

    // Dynamic Resolution
    view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
        if (sceneParameters.dynamicResolutionEnabled) {
            quality = when (sceneParameters.dynamicResolutionQuality) {
                Scene3DParameters.QualityLevel.LOW -> View.QualityLevel.LOW
                Scene3DParameters.QualityLevel.MEDIUM -> View.QualityLevel.MEDIUM
                Scene3DParameters.QualityLevel.HIGH -> View.QualityLevel.HIGH
                Scene3DParameters.QualityLevel.ULTRA -> View.QualityLevel.ULTRA
            }
        }
    }

    // MSAA (Multi-Sample Anti-Aliasing)
    view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
        enabled = sceneParameters.msaaEnabled
    }

    // FXAA (Fast Approximate Anti-Aliasing)
    view.antiAliasing = if (sceneParameters.fxaaEnabled) {
        View.AntiAliasing.FXAA
    } else {
        View.AntiAliasing.NONE
    }

    // Temporal Anti-Aliasing
    view.temporalAntiAliasingOptions = view.temporalAntiAliasingOptions.apply {
        enabled = sceneParameters.temporalAntiAliasingEnabled
    }

    // Ambient Occlusion
    view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
        enabled = sceneParameters.ambientOcclusionEnabled
    }

    // Bloom
    view.bloomOptions = view.bloomOptions.apply {
        enabled = sceneParameters.bloomEnabled
    }

    // Screen Space Reflections
    view.screenSpaceReflectionsOptions = view.screenSpaceReflectionsOptions.apply {
        enabled = sceneParameters.screenSpaceReflectionsEnabled
    }
}
