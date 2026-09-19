package com.example.moneywallpaperfilament

import com.google.android.filament.*

data class BillField(
    @Entity val renderable: Int,
    val vertexBuffer: VertexBuffer
) {
    fun destroy(engine: Engine) {
        engine.destroyEntity(renderable)
        engine.destroyVertexBuffer(vertexBuffer)
        EntityManager.get().destroy(renderable)
    }
}

object BillConfig {
    const val GRID_X = 4
    const val GRID_Y = 2
    const val VERTS_PER_BILL = GRID_X * GRID_Y * 2 * 6
    const val BILL_COUNT = 250
    val BOUNDS = Box(0f, 1.0f, 0f, 2.4f, 2.7f, 2.4f)


    const val CAST_SHADOWS = false
}

fun buildBillField(
    engine: Engine,
    materialInstance: MaterialInstance,
    count: Int = BillConfig.BILL_COUNT
): BillField {
    val vb = VertexBuffer.Builder()
        .vertexCount(BillConfig.VERTS_PER_BILL)
        .bufferCount(0)
        .build(engine)

    val entity = EntityManager.get().create()

    RenderableManager.Builder(1)
        .boundingBox(BillConfig.BOUNDS)
        .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb)
        .material(0, materialInstance)
        .instances(count)
        .castShadows(false)
        .receiveShadows(false)
        .culling(true)
        .build(engine, entity)

    return BillField(entity, vb)
}
