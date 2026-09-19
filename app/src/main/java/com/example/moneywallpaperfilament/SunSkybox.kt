package com.example.moneywallpaperfilament

import com.google.android.filament.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class SunSkybox(
    @Entity val renderable: Int,
    val vertexBuffer: VertexBuffer,
    val material: Material,
    val materialInstance: MaterialInstance
) {
    fun destroy(engine: Engine) {
        engine.destroyEntity(renderable)
        engine.destroyVertexBuffer(vertexBuffer)
        engine.destroyMaterialInstance(materialInstance)
        engine.destroyMaterial(material)
        EntityManager.get().destroy(renderable)
    }


    fun applySunSettings(visible: Boolean, intensity: Float, radiusDeg: Float, glow: Float) {
        materialInstance.setParameter("sunVisible", if (visible) 1.0f else 0.0f)
        materialInstance.setParameter("sunIntensity", intensity)
        materialInstance.setParameter("sunRadiusDeg", radiusDeg)
        materialInstance.setParameter("glowStrength", glow)
    }
}

object SunConfig {
    const val RADIUS_DEG = 2.0f
    const val INTENSITY = 8.0f
    const val GLOW = 0.5f
    val SUN_DIR = floatArrayOf(0.0f, 0.250f, -0.918f)
    val SKY_ZENITH = floatArrayOf(0.014f, 0.349f, 0.823f)
    val SKY_HORIZON = floatArrayOf(0.737f, 1.000f, 0.979f)
}

fun buildSunSkybox(
    engine: Engine,
    payload: ByteBuffer,
    sunR: Float,
    sunG: Float,
    sunB: Float
): SunSkybox {
    val material = Material.Builder().payload(payload, payload.remaining()).build(engine)
    val mi = material.createInstance()

    mi.setParameter("sunDir", SunConfig.SUN_DIR[0], SunConfig.SUN_DIR[1], SunConfig.SUN_DIR[2])
    mi.setParameter("sunRadiusDeg", SunConfig.RADIUS_DEG)
    mi.setParameter("sunIntensity", SunConfig.INTENSITY)
    mi.setParameter("glowStrength", SunConfig.GLOW)
    mi.setParameter("sunVisible", 1.0f)
    mi.setParameter("sunColor", sunR, sunG, sunB)
    mi.setParameter(
        "skyZenith",
        SunConfig.SKY_ZENITH[0],
        SunConfig.SKY_ZENITH[1],
        SunConfig.SKY_ZENITH[2]
    )
    mi.setParameter(
        "skyHorizon",
        SunConfig.SKY_HORIZON[0],
        SunConfig.SKY_HORIZON[1],
        SunConfig.SKY_HORIZON[2]
    )


    val vertexData = ByteBuffer.allocateDirect(3 * 16).order(ByteOrder.nativeOrder())
    val floatBuffer = vertexData.asFloatBuffer()
    floatBuffer.put(
        floatArrayOf(
            -1.0f, -1.0f, 0.0f, 1.0f,
            3.0f, -1.0f, 0.0f, 1.0f,
            -1.0f, 3.0f, 0.0f, 1.0f
        )
    )

    val vb = VertexBuffer.Builder()
        .vertexCount(3)
        .bufferCount(1)
        .attribute(
            VertexBuffer.VertexAttribute.POSITION,
            0,
            VertexBuffer.AttributeType.FLOAT4,
            0,
            16
        )
        .build(engine)
    vb.setBufferAt(engine, 0, vertexData)

    val entity = EntityManager.get().create()
    RenderableManager.Builder(1)
        .boundingBox(Box(-1f, -1f, -1f, 1f, 1f, 1f))
        .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb)
        .material(0, mi)
        .priority(0)
        .castShadows(false)
        .receiveShadows(false)
        .culling(false)
        .build(engine, entity)

    return SunSkybox(entity, vb, material, mi)
}