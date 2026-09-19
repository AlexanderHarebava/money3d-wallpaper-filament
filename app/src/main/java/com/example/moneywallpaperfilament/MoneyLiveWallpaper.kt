package com.example.moneywallpaperfilament

import android.app.Service
import android.graphics.PixelFormat
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.Display
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.google.android.filament.*
import com.google.android.filament.android.ChoreographerHelper
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.*
import java.nio.ByteBuffer
import java.nio.channels.Channels

class MoneyLiveWallpaper : WallpaperService() {

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

    override fun onCreateEngine(): WallpaperService.Engine = MoneyEngine()

    private inner class MoneyEngine : WallpaperService.Engine() {

        private lateinit var uiHelper: UiHelper
        private lateinit var displayHelper: DisplayHelper

        private lateinit var filamentEngine: com.google.android.filament.Engine
        private lateinit var renderer: Renderer
        private lateinit var scene: Scene
        private lateinit var view: View
        private lateinit var camera: Camera

        private lateinit var settingsRepo: SettingsRepository
        private lateinit var parallax: ParallaxController
        private var settings = WallpaperSettings()

        private var sunSkybox: SunSkybox? = null
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

        private var currentBillCount = BillConfig.BILL_COUNT

        private var timeBase = 0.0
        private var fallPhase = 0.0
        private var spinPhase = 0.0
        private var lastFrameNanos = 0L

        private var fallSpeed = 1.0
        private var spinMultiplier = 1.0

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            surfaceHolder.setSizeFromLayout()
            surfaceHolder.setFormat(PixelFormat.RGBA_8888)

            settingsRepo = SettingsRepository(this@MoneyLiveWallpaper)
            settings = settingsRepo.load()
            currentBillCount = settings.billCount

            parallax = ParallaxController(this@MoneyLiveWallpaper)
            displayHelper = DisplayHelper(this@MoneyLiveWallpaper)

            setupFilament()
            setupView()
            setupScene()
            setupUiHelper()

            applySettings(settings)
        }

        private fun setupUiHelper() {
            uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
            uiHelper.renderCallback = SurfaceCallback()
            uiHelper.attachTo(surfaceHolder)
        }

        private fun setupFilament() {
            filamentEngine = com.google.android.filament.Engine.create()

            renderer = filamentEngine.createRenderer()
            renderer.clearOptions = Renderer.ClearOptions().apply {
                clearColor = doubleArrayOf(0.02, 0.03, 0.05, 1.0)
                clear = true
                discard = false
            }

            frameScheduler.setRenderer(renderer)

            scene = filamentEngine.createScene()
            view = filamentEngine.createView()
            camera = filamentEngine.createCamera(filamentEngine.entityManager.create())
        }

        private fun setupView() {
            view.camera = camera
            view.scene = scene
        }

        private fun setupScene() {
            setupEnvironment()
            setupSunSkybox()
            setupBills()
            setupLights()
            setupViewOptions()

            camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)
            camera.lookAt(
                CAM_EYE_X,
                CAM_EYE_Y,
                CAM_EYE_Z,
                CAM_TARGET_X,
                CAM_TARGET_Y,
                CAM_TARGET_Z,
                0.0,
                1.0,
                0.0
            )
        }

        private fun setupEnvironment() {
            scene.skybox = null

            val sh = floatArrayOf(
                0.8f,
                0.8f,
                0.9f,
                0.1f,
                0.1f,
                0.2f,
                0.1f,
                0.1f,
                0.1f,
                0.2f,
                0.2f,
                0.3f,
                0.05f,
                0.05f,
                0.1f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f,
                0.0f
            )

            fallbackIndirectLight =
                IndirectLight.Builder().irradiance(3, sh).intensity(30_000.0f).build(filamentEngine)

            scene.indirectLight = fallbackIndirectLight
        }

        private fun setupSunSkybox() {
            val (r, g, b) = Colors.cct(5_600.0f)

            readUncompressedAsset("materials/sun_skybox.filamat").let { payload ->
                val sun = buildSunSkybox(filamentEngine, payload, r, g, b)
                scene.addEntity(sun.renderable)
                sunSkybox = sun
            }
        }

        private fun setupBills() {
            readUncompressedAsset("materials/bill.filamat").let { payload ->
                billMaterial =
                    Material.Builder().payload(payload, payload.remaining()).build(filamentEngine)
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


            frontMap =
                loadTexture(filamentEngine, resources, R.drawable.bill_front, TextureType.COLOR)
            backMap =
                loadTexture(filamentEngine, resources, R.drawable.bill_back, TextureType.COLOR)

            billMI.setParameter("frontMap", frontMap, sColor)
            billMI.setParameter("backMap", backMap, sColor)

            frontOrmMap =
                loadTexture(filamentEngine, resources, R.drawable.bill_front_orm, TextureType.DATA)
            backOrmMap =
                loadTexture(filamentEngine, resources, R.drawable.bill_back_orm, TextureType.DATA)
            frontNormalMap = loadTexture(
                filamentEngine, resources, R.drawable.bill_front_normal, TextureType.DATA
            )
            backNormalMap = loadTexture(
                filamentEngine, resources, R.drawable.bill_back_normal, TextureType.DATA
            )

            billMI.setParameter("frontOrmMap", frontOrmMap, sData)
            billMI.setParameter("backOrmMap", backOrmMap, sData)
            billMI.setParameter("frontNormalMap", frontNormalMap, sData)
            billMI.setParameter("backNormalMap", backNormalMap, sData)

            currentBillCount = settings.billCount
            billField = buildBillField(filamentEngine, billMI, currentBillCount)
            scene.addEntity(billField.renderable)
        }

        private fun setupLights() {
            keyLight = EntityManager.get().create()
            val (r, g, b) = Colors.cct(5_600.0f)
            LightManager.Builder(LightManager.Type.DIRECTIONAL).color(r, g, b).intensity(60_000.0f)
                .direction(-0.40f, -1.0f, 0.35f).castShadows(true).build(filamentEngine, keyLight)
            scene.addEntity(keyLight)

            fillLight = EntityManager.get().create()
            val (fr, fg, fb) = Colors.cct(7_500.0f)
            LightManager.Builder(LightManager.Type.DIRECTIONAL).color(fr, fg, fb)
                .intensity(20_000.0f).direction(0.30f, -1.0f, -0.40f).castShadows(false)
                .build(filamentEngine, fillLight)
            scene.addEntity(fillLight)

            bounceLight = EntityManager.get().create()
            val (br, bg, bb) = Colors.cct(4_000.0f)
            LightManager.Builder(LightManager.Type.DIRECTIONAL).color(br, bg, bb)
                .intensity(12_000.0f).direction(0.0f, 1.0f, -0.30f).castShadows(false)
                .build(filamentEngine, bounceLight)
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

        private fun applySettings(newSettings: WallpaperSettings) {
            settings = newSettings

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

        private fun reloadSettings() {
            val newSettings = settingsRepo.load()
            val billCountChanged = newSettings.billCount != currentBillCount

            applySettings(newSettings)

            if (::billField.isInitialized && billCountChanged) {
                rebuildBillField(newSettings.billCount)
            }
        }

        private fun rebuildBillField(count: Int) {
            if (!::billField.isInitialized || !::billMI.isInitialized) return

            scene.removeEntity(billField.renderable)
            billField.destroy(filamentEngine)

            billField = buildBillField(filamentEngine, billMI, count)
            scene.addEntity(billField.renderable)

            currentBillCount = count
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                reloadSettings()
                lastFrameNanos = 0L
                parallax.onResume()
                frameScheduler.post()
            } else {
                frameScheduler.remove()
                parallax.onPause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()

            frameScheduler.remove()

            if (::parallax.isInitialized) {
                parallax.release()
            }

            if (::uiHelper.isInitialized) {
                uiHelper.detach()
            }

            if (::billField.isInitialized) {
                scene.removeEntity(billField.renderable)
                billField.destroy(filamentEngine)
            }

            sunSkybox?.let {
                scene.removeEntity(it.renderable)
                it.destroy(filamentEngine)
                sunSkybox = null
            }

            if (keyLight != 0) {
                scene.removeEntity(keyLight)
                filamentEngine.destroyEntity(keyLight)
            }

            if (fillLight != 0) {
                scene.removeEntity(fillLight)
                filamentEngine.destroyEntity(fillLight)
            }

            if (bounceLight != 0) {
                scene.removeEntity(bounceLight)
                filamentEngine.destroyEntity(bounceLight)
            }

            fallbackIndirectLight?.let {
                scene.indirectLight = null
                filamentEngine.destroyIndirectLight(it)
                fallbackIndirectLight = null
            }

            if (::billMI.isInitialized) {
                filamentEngine.destroyMaterialInstance(billMI)
            }

            if (::billMaterial.isInitialized) {
                filamentEngine.destroyMaterial(billMaterial)
            }

            if (::frontMap.isInitialized) filamentEngine.destroyTexture(frontMap)
            if (::backMap.isInitialized) filamentEngine.destroyTexture(backMap)
            if (::frontOrmMap.isInitialized) filamentEngine.destroyTexture(frontOrmMap)
            if (::backOrmMap.isInitialized) filamentEngine.destroyTexture(backOrmMap)
            if (::frontNormalMap.isInitialized) filamentEngine.destroyTexture(frontNormalMap)
            if (::backNormalMap.isInitialized) filamentEngine.destroyTexture(backNormalMap)

            if (::renderer.isInitialized) {
                filamentEngine.destroyRenderer(renderer)
            }

            if (::view.isInitialized) {
                filamentEngine.destroyView(view)
            }

            if (::scene.isInitialized) {
                filamentEngine.destroyScene(scene)
            }

            if (::camera.isInitialized) {
                filamentEngine.destroyCameraComponent(camera.entity)
                EntityManager.get().destroy(camera.entity)
            }

            val em = EntityManager.get()
            if (keyLight != 0) em.destroy(keyLight)
            if (fillLight != 0) em.destroy(fillLight)
            if (bounceLight != 0) em.destroy(bounceLight)

            filamentEngine.destroy()
        }

        inner class FrameCallback : ChoreographerHelper() {
            override fun onFrame(frameTimeNanos: Long) {
                val sc = swapChain ?: return

                if (!uiHelper.isReadyToRender) {
                    return
                }

                advanceAnimation(frameTimeNanos)

                if (renderer.beginFrame(sc, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }

        private fun advanceAnimation(frameTimeNanos: Long) {
            var dt = if (lastFrameNanos == 0L) {
                0.0
            } else {
                (frameTimeNanos - lastFrameNanos) * 1e-9
            }

            lastFrameNanos = frameTimeNanos

            if (dt < 0.0 || dt > 0.25) {
                dt = 0.0
            }

            timeBase = (timeBase + dt) % SHADER_PERIOD
            fallPhase = (fallPhase + dt * fallSpeed) % SHADER_PERIOD
            spinPhase = (spinPhase + dt * spinMultiplier) % SHADER_PERIOD

            if (::billMI.isInitialized) {
                billMI.setParameter("timeBase", timeBase.toFloat())
                billMI.setParameter("fallPhase", fallPhase.toFloat())
                billMI.setParameter("spinPhase", spinPhase.toFloat())
            }

            val (ox, oy) = parallax.update(dt)

            if (ox.isFinite() && oy.isFinite()) {
                camera.lookAt(
                    CAM_EYE_X,
                    CAM_EYE_Y,
                    CAM_EYE_Z,
                    CAM_TARGET_X + ox * PARALLAX_RANGE_X,
                    CAM_TARGET_Y + oy * PARALLAX_RANGE_Y,
                    CAM_TARGET_Z,
                    0.0,
                    1.0,
                    0.0
                )
            }
        }

        inner class SurfaceCallback : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { filamentEngine.destroySwapChain(it) }
                swapChain = filamentEngine.createSwapChain(surface)
                displayHelper.attach(renderer, getDisplayCompat())
            }

            override fun onDetachedFromSurface() {
                displayHelper.detach()

                swapChain?.let {
                    filamentEngine.destroySwapChain(it)
                    filamentEngine.flushAndWait()
                    swapChain = null
                }
            }

            override fun onResized(width: Int, height: Int) {
                if (width <= 0 || height <= 0) return

                val aspect = width.toDouble() / height.toDouble()

                camera.setProjection(
                    45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL
                )

                view.viewport = Viewport(0, 0, width, height)
                FilamentHelper.synchronizePendingFrames(filamentEngine)
            }
        }

        private fun getDisplayCompat(): Display {
            if (Build.VERSION.SDK_INT >= 30) {


                val dContext = displayContext
                if (dContext != null) {
                    return Api30Display.getDisplay(dContext)
                }
            }

            @Suppress("DEPRECATION") return (getSystemService(Service.WINDOW_SERVICE) as WindowManager).defaultDisplay
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
}

@RequiresApi(30)
private object Api30Display {
    fun getDisplay(context: android.content.Context): Display = context.display!!
}