package com.example.moneywallpaperfilament

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.moneywallpaperfilament.ui.theme.MoneywallpaperfilamentTheme
import com.google.android.filament.*
import com.google.android.filament.android.*
import com.google.android.filament.utils.TextureType
import com.google.android.filament.utils.Utils
import com.google.android.filament.utils.loadTexture
import java.nio.ByteBuffer
import java.nio.channels.Channels

class MainActivity : ComponentActivity() {

    companion object {
        init {
            Utils.init()
        }

        private const val SHADER_PERIOD = 120.0
        private const val CAM_EYE_X = 0.0
        private const val CAM_EYE_Y = 1.0
        private const val CAM_EYE_Z = 2.8
        private const val CAM_TARGET_X = 0.0
        private const val CAM_TARGET_Y = 0.9
        private const val CAM_TARGET_Z = 0.0
        private const val PARALLAX_RANGE_X = 0.35
        private const val PARALLAX_RANGE_Y = 0.20
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var uiHelper: UiHelper
    private lateinit var displayHelper: DisplayHelper
    private lateinit var engine: Engine
    private lateinit var renderer: Renderer
    private lateinit var scene: Scene
    private lateinit var view: View
    private lateinit var camera: Camera

    private var sunSkybox: SunSkybox? = null
    private var ibl: Ibl? = null
    private var fallbackIndirectLight: IndirectLight? = null
    private lateinit var billMaterial: Material
    private lateinit var billMI: MaterialInstance
    private lateinit var frontMap: Texture
    private lateinit var backMap: Texture
    private lateinit var frontOrmMap: Texture
    private lateinit var backOrmMap: Texture
    private lateinit var frontNormalMap: Texture
    private lateinit var backNormalMap: Texture
    private lateinit var billField: BillField

    @Entity
    private var keyLight = 0
    @Entity
    private var fillLight = 0
    @Entity
    private var bounceLight = 0

    private var swapChain: SwapChain? = null
    private val frameScheduler = FrameCallback()


    private lateinit var settingsRepo: SettingsRepository
    private lateinit var parallax: ParallaxController

    private var settings by mutableStateOf(WallpaperSettings())
    private var showSettings by mutableStateOf(false)

    private var currentBillCount = BillConfig.BILL_COUNT


    private var timeBase = 0.0
    private var fallPhase = 0.0
    private var spinPhase = 0.0
    private var lastFrameNanos = 0L

    private var fallSpeed = 1.0
    private var spinMultiplier = 1.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepo = SettingsRepository(this)
        settings = settingsRepo.load()
        parallax = ParallaxController(this)


        surfaceView = SurfaceView(this)
        val root = FrameLayout(this)
        root.addView(
            surfaceView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        val composeView = ComposeView(this)
        root.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)

        displayHelper = DisplayHelper(this)
        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()


        applySettings(settings)

        composeView.setContent {
            MoneywallpaperfilamentTheme {
                SettingsOverlay()
            }
        }
    }

    @Composable
    private fun SettingsOverlay() {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { openLiveWallpaperPicker() }) {
                    Text(stringResource(R.string.btn_set_live_wallpaper))
                }
                FloatingActionButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.cd_settings)
                    )
                }
            }

            if (showSettings) {
                SettingsDialog(
                    settings = settings,
                    onSettingsChange = { applySettings(it) },
                    onBillCountCommit = { rebuildBillFieldIfChanged(settings.billCount) },
                    onReset = {
                        applySettings(WallpaperSettings())
                        rebuildBillFieldIfChanged(settings.billCount)
                    },
                    onDismiss = {
                        showSettings = false
                        rebuildBillFieldIfChanged(settings.billCount)
                    }
                )
            }
        }
    }


    private fun openLiveWallpaperPicker() {
        try {
            val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
            startActivity(chooserIntent)
            return
        } catch (e: Exception) {
        }

        try {
            val directIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, MoneyLiveWallpaper::class.java)
                )
            }
            startActivity(directIntent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(
                    R.string.toast_open_settings_manually,
                    getString(R.string.wallpaper_label)
                ),
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun applySettings(newSettings: WallpaperSettings) {
        settings = newSettings
        settingsRepo.save(newSettings)

        sunSkybox?.applySunSettings(
            visible = newSettings.sunVisible,
            intensity = newSettings.sunIntensity,
            radiusDeg = newSettings.sunRadiusDeg,
            glow = newSettings.sunGlow
        )

        fallSpeed = newSettings.fallSpeed.toDouble()
        spinMultiplier = newSettings.spinIntensity.toDouble()
        if (::billMI.isInitialized) {
            billMI.setParameter("swayMult", newSettings.swayIntensity)
            billMI.setParameter("spinMult", newSettings.spinIntensity)
            billMI.setParameter("bendMult", newSettings.bendIntensity)
        }

        parallax.sensitivity = newSettings.parallaxSensitivity
        parallax.setEnabled(newSettings.parallaxEnabled)
    }

    private fun rebuildBillFieldIfChanged(count: Int) {
        if (!::billField.isInitialized || count == currentBillCount) return
        scene.removeEntity(billField.renderable)
        billField.destroy(engine)
        billField = buildBillField(engine, billMI, count)
        scene.addEntity(billField.renderable)
        currentBillCount = count
    }

    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(surfaceView)
    }

    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        renderer.clearOptions = Renderer.ClearOptions().apply {
            clearColor = doubleArrayOf(0.02, 0.03, 0.05, 1.0)
            clear = true
            discard = false
        }
        frameScheduler.setRenderer(renderer)
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
    }

    private fun setupScene() {
        setupEnvironment()
        setupSunSkybox()
        setupBills()
        setupLights()
        setupViewOptions()

        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)
        camera.lookAt(
            CAM_EYE_X, CAM_EYE_Y, CAM_EYE_Z,
            CAM_TARGET_X, CAM_TARGET_Y, CAM_TARGET_Z,
            0.0, 1.0, 0.0
        )
    }

    private fun setupSunSkybox() {
        val (r, g, b) = Colors.cct(5_600.0f)
        android.util.Log.d("SunSkybox", "Creating sun skybox with color: $r, $g, $b")
        readUncompressedAsset("materials/sun_skybox.filamat").let { payload ->
            val sun = buildSunSkybox(engine, payload, r, g, b)
            scene.addEntity(sun.renderable)
            sunSkybox = sun
        }
    }

    private fun setupEnvironment() {
        scene.skybox = null
        val sh = floatArrayOf(
            0.8f, 0.8f, 0.9f,
            0.1f, 0.1f, 0.2f,
            0.1f, 0.1f, 0.1f,
            0.2f, 0.2f, 0.3f,
            0.05f, 0.05f, 0.1f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f
        )
        fallbackIndirectLight = IndirectLight.Builder()
            .irradiance(3, sh)
            .intensity(30_000.0f)
            .build(engine)
        scene.indirectLight = fallbackIndirectLight
    }

    private fun setupView() {
        view.camera = camera
        view.scene = scene
        val options = View.DynamicResolutionOptions()
        options.enabled = true
        view.dynamicResolutionOptions = options
    }

    private fun setupBills() {
        readUncompressedAsset("materials/bill.filamat").let {
            billMaterial = Material.Builder().payload(it, it.remaining()).build(engine)
        }
        billMI = billMaterial.createInstance()

        val sColor = TextureSampler().apply {
            minFilter = TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR
            magFilter = TextureSampler.MagFilter.LINEAR
            anisotropy = 8.0f
        }
        val sData = TextureSampler().apply {
            minFilter = TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR
            magFilter = TextureSampler.MagFilter.LINEAR
            anisotropy = 4.0f
        }

        frontMap = loadTexture(engine, resources, R.drawable.bill_front, TextureType.COLOR)
        backMap = loadTexture(engine, resources, R.drawable.bill_back, TextureType.COLOR)
        billMI.setParameter("frontMap", frontMap, sColor)
        billMI.setParameter("backMap", backMap, sColor)

        frontOrmMap = loadTexture(engine, resources, R.drawable.bill_front_orm, TextureType.DATA)
        backOrmMap = loadTexture(engine, resources, R.drawable.bill_back_orm, TextureType.DATA)
        frontNormalMap =
            loadTexture(engine, resources, R.drawable.bill_front_normal, TextureType.DATA)
        backNormalMap =
            loadTexture(engine, resources, R.drawable.bill_back_normal, TextureType.DATA)
        billMI.setParameter("frontOrmMap", frontOrmMap, sData)
        billMI.setParameter("backOrmMap", backOrmMap, sData)
        billMI.setParameter("frontNormalMap", frontNormalMap, sData)
        billMI.setParameter("backNormalMap", backNormalMap, sData)

        currentBillCount = settings.billCount
        billField = buildBillField(engine, billMI, currentBillCount)
        scene.addEntity(billField.renderable)
    }

    private fun setupLights() {
        keyLight = EntityManager.get().create()
        val (r, g, b) = Colors.cct(5_600.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            .intensity(60_000.0f)
            .direction(-0.40f, -1.0f, 0.35f)
            .castShadows(true)
            .build(engine, keyLight)
        scene.addEntity(keyLight)

        fillLight = EntityManager.get().create()
        val (fr, fg, fb) = Colors.cct(7_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(fr, fg, fb)
            .intensity(20_000.0f)
            .direction(0.30f, -1.0f, -0.40f)
            .castShadows(false)
            .build(engine, fillLight)
        scene.addEntity(fillLight)

        bounceLight = EntityManager.get().create()
        val (br, bg, bb) = Colors.cct(4_000.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(br, bg, bb)
            .intensity(12_000.0f)
            .direction(0.0f, 1.0f, -0.30f)
            .castShadows(false)
            .build(engine, bounceLight)
        scene.addEntity(bounceLight)
    }

    private fun setupViewOptions() {
        view.dynamicResolutionOptions = View.DynamicResolutionOptions().apply {
            enabled = false
        }
        view.ambientOcclusionOptions = View.AmbientOcclusionOptions().apply {
            enabled = false
        }
        view.bloomOptions = View.BloomOptions().apply {
            enabled = true
            strength = 0.20f
            resolution = 384
        }
        view.fogOptions = View.FogOptions().apply {
            enabled = false
            density = 0.02f
            heightFalloff = 0.6f
            maximumOpacity = 0.35f
            distance = 8.0f
        }
    }

    override fun onResume() {
        super.onResume()
        parallax.onResume()
        frameScheduler.post()
    }

    override fun onPause() {
        super.onPause()
        frameScheduler.remove()
        parallax.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        frameScheduler.remove()
        parallax.release()
        uiHelper.detach()

        if (::frontOrmMap.isInitialized) engine.destroyTexture(frontOrmMap)
        if (::backOrmMap.isInitialized) engine.destroyTexture(backOrmMap)
        if (::frontNormalMap.isInitialized) engine.destroyTexture(frontNormalMap)
        if (::backNormalMap.isInitialized) engine.destroyTexture(backNormalMap)

        if (::billField.isInitialized) billField.destroy(engine)
        if (::frontMap.isInitialized) engine.destroyTexture(frontMap)
        if (::backMap.isInitialized) engine.destroyTexture(backMap)
        if (::billMI.isInitialized) engine.destroyMaterialInstance(billMI)
        if (::billMaterial.isInitialized) engine.destroyMaterial(billMaterial)

        ibl?.let { destroyIbl(engine, it) }
        sunSkybox?.destroy(engine)
        fallbackIndirectLight?.let { engine.destroyIndirectLight(it) }

        engine.destroyEntity(keyLight)
        engine.destroyEntity(fillLight)
        engine.destroyEntity(bounceLight)
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)

        val em = EntityManager.get()
        em.destroy(keyLight)
        em.destroy(fillLight)
        em.destroy(bounceLight)
        em.destroy(camera.entity)
        engine.destroy()
    }

    inner class FrameCallback : ChoreographerHelper() {
        override fun onFrame(frameTimeNanos: Long) {
            if (uiHelper.isReadyToRender) {
                advanceAnimation(frameTimeNanos)
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }
    }


    private fun advanceAnimation(frameTimeNanos: Long) {
        var dt = if (lastFrameNanos == 0L) 0.0 else (frameTimeNanos - lastFrameNanos) * 1e-9
        lastFrameNanos = frameTimeNanos
        if (dt < 0.0 || dt > 0.25) dt = 0.0

        timeBase = (timeBase + dt) % SHADER_PERIOD
        fallPhase = (fallPhase + dt * fallSpeed) % SHADER_PERIOD
        spinPhase = (spinPhase + dt * spinMultiplier) % SHADER_PERIOD

        billMI.setParameter("timeBase", timeBase.toFloat())
        billMI.setParameter("fallPhase", fallPhase.toFloat())
        billMI.setParameter("spinPhase", spinPhase.toFloat())



        val (ox, oy) = parallax.update(dt)
        if (ox.isFinite() && oy.isFinite()) {
            camera.lookAt(
                CAM_EYE_X, CAM_EYE_Y, CAM_EYE_Z,
                CAM_TARGET_X + ox * PARALLAX_RANGE_X,
                CAM_TARGET_Y + oy * PARALLAX_RANGE_Y,
                CAM_TARGET_Z,
                0.0, 1.0, 0.0
            )
        }
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            displayHelper.attach(renderer, surfaceView.display)
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)
            view.viewport = Viewport(0, 0, width, height)
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        assets.openFd(assetName).use { fd ->
            val input = fd.createInputStream()
            val dst = ByteBuffer.allocate(fd.length.toInt())
            val src = Channels.newChannel(input)
            src.read(dst)
            src.close()
            return dst.apply { rewind() }
        }
    }
}