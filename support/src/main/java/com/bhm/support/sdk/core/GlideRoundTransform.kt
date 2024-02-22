package com.bhm.support.sdk.core

import android.content.res.Resources
import android.graphics.*
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.load.resource.bitmap.TransformationUtils
import java.security.MessageDigest

/** glide圆角图
 * Created by bhm on 2018/3/6.
 */
class GlideRoundTransform : BitmapTransformation {
    private var centerCrop = true

    @JvmOverloads
    constructor(dp: Float = 10f) {
        radius = Resources.getSystem().displayMetrics.density * dp
    }

    constructor(dp: Float, centerCrop: Boolean) {
        radius = Resources.getSystem().displayMetrics.density * dp
        this.centerCrop = centerCrop
    }

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        /* 解决 Glide的centerCrop会和圆角冲突*/
        if (centerCrop) {
            val bitmap = TransformationUtils.centerCrop(pool, toTransform, outWidth, outHeight)
            return roundCrop(pool, bitmap)!!
        }
        val bitmap = TransformationUtils.fitCenter(pool, toTransform, outWidth, outHeight)
        return roundCrop(pool, bitmap)!!
    }

    private fun roundCrop(pool: BitmapPool, source: Bitmap?): Bitmap? {
        if (source == null) return null
        val result = pool[source.width, source.height, Bitmap.Config.ARGB_8888]
        val canvas = Canvas(result)
        val paint = Paint()
        paint.shader =
            BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.isAntiAlias = true
        val rectF = RectF(
            0f, 0f, source.width.toFloat(), source.height
                .toFloat()
        )
        canvas.drawRoundRect(rectF, radius, radius, paint)
        return result
    }

    val id: String
        get() = javaClass.name + Math.round(radius)

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {}

    companion object {
        private var radius = 0f
    }
}