@file:Suppress("UNCHECKED_CAST")

package com.bhm.support.sdk.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.bhm.support.sdk.R
import com.bhm.support.sdk.core.GlideCircleTransform
import com.bhm.support.sdk.core.GlideRoundTransform
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

/**
 * @author Buhuiming
 * @description:
 * @date :2022/6/28 14:44
 */
@Suppress("MemberVisibilityCanBePrivate")
object ViewUtil {

    private const val INTERNAL_TIME: Long = 600

    @JvmStatic
    fun <VB : ViewBinding> inflateWithGeneric(genericOwner: Any, layoutInflater: LayoutInflater): VB =
        withGenericBindingClass<VB>(genericOwner) { clazz ->
            clazz.getMethod("inflate", LayoutInflater::class.java).invoke(null, layoutInflater) as VB
        }.withLifecycleOwner(genericOwner)

    private fun <VB : ViewBinding> VB.withLifecycleOwner(genericOwner: Any) = apply {
        if (this is ViewDataBinding && genericOwner is ComponentActivity) {
            lifecycleOwner = genericOwner
        } else if (this is ViewDataBinding && genericOwner is Fragment) {
            lifecycleOwner = genericOwner.viewLifecycleOwner
        }
    }

    private fun <VB : ViewBinding> withGenericBindingClass(genericOwner: Any, block: (Class<VB>) -> VB): VB {
        var genericSuperclass = genericOwner.javaClass.genericSuperclass
        var superclass = genericOwner.javaClass.superclass
        while (superclass != null) {
            if (genericSuperclass is ParameterizedType) {
                genericSuperclass.actualTypeArguments.forEach {
                    try {
                        return block.invoke(it as Class<VB>)
                    } catch (e: NoSuchMethodException) {
                    } catch (e: ClassCastException) {
                    } catch (e: InvocationTargetException) {
                        var tagException: Throwable? = e
                        while (tagException is InvocationTargetException) {
                            tagException = e.cause
                        }
                        throw tagException ?: IllegalArgumentException("ViewBinding generic was found, but creation failed.")
                    }
                }
            }
            genericSuperclass = superclass.genericSuperclass
            superclass = superclass.superclass
        }
        throw IllegalArgumentException("There is no generic of ViewBinding.")
    }

    /**
     * Whether this click event is invalid.
     *
     * @param target target view
     * @return true, invalid click event.
     * @see .isInvalidClick
     */
    @JvmStatic
    fun isInvalidClick(target: View): Boolean {
        return isInvalidClick(target, INTERNAL_TIME)
    }

    /**
     * Whether this click event is invalid.
     *
     * @param target       target view
     * @param internalTime the internal time. The unit is millisecond.
     * @return true, invalid click event.
     */
    @JvmStatic
    fun isInvalidClick(target: View, @IntRange(from = 0) internalTime: Long): Boolean {
        val curTimeStamp = System.currentTimeMillis()
        val lastClickTimeStamp: Long
        val o = target.getTag(R.id.last_click_time)
        if (o == null) {
            target.setTag(R.id.last_click_time, curTimeStamp)
            return false
        }
        lastClickTimeStamp = o as Long
        val isInvalid = curTimeStamp - lastClickTimeStamp < internalTime
        if (!isInvalid) target.setTag(R.id.last_click_time, curTimeStamp)
        return isInvalid
    }

    @JvmStatic
    fun loadRoundImg(
        imageView: ImageView,
        url: String?,
        @DrawableRes placeholderId: Int,
        radius: Float
    ) {
        //占位图和失败图是一样的时候
        loadRoundImg(imageView, url, placeholderId, placeholderId, radius, true)
    }

    //Glide设置了圆角，并且是结合CenterCrop，所以选择跳过缓存，否则不一样的圆角图片会取错
    //当然提供了保留缓存，在一些特定的场合使用
    @JvmStatic
    fun loadRoundImg(
        imageView: ImageView,
        url: String?,
        @DrawableRes placeholderId: Int,
        radius: Float,
        skipMemoryCache: Boolean
    ) {
        //占位图和失败图是一样的时候
        loadRoundImg(imageView, url, placeholderId, placeholderId, radius, skipMemoryCache)
    }

    @JvmStatic
    fun loadRoundImg(
        imageView: ImageView, url: String?, @DrawableRes placeholderId: Int,
        @DrawableRes errorId: Int, radius: Float, skipMemoryCache: Boolean
    ) {
        loadRoundImg(imageView, url, placeholderId, errorId, radius, true, skipMemoryCache)
    }

    @JvmStatic
    fun loadRoundImg(
        imageView: ImageView, imageUrl: String?, @DrawableRes placeholderId: Int,
        @DrawableRes errorId: Int, radius: Float, centerCrop: Boolean, skipMemoryCache: Boolean
    ) {
        var url = imageUrl
        if (url == null) {
            url = ""
        }
        Glide.with(imageView.context)
            .load(url)
            .skipMemoryCache(skipMemoryCache)
            .diskCacheStrategy(if (skipMemoryCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
            .apply(
                RequestOptions()
                    .placeholder(placeholderId)
                    .error(errorId)
                    .transform(GlideRoundTransform(radius, centerCrop))
            )
            .thumbnail(
                loadTransform(
                    imageView.context,
                    placeholderId,
                    radius,
                    centerCrop,
                    skipMemoryCache
                )
            )
            .thumbnail(
                loadTransform(
                    imageView.context,
                    errorId,
                    radius,
                    centerCrop,
                    skipMemoryCache
                )
            )
            .into(imageView)
    }

    @JvmStatic
    fun loadRoundImg(
        imageView: ImageView,
        uri: Uri?,
        @DrawableRes placeholderId: Int,
        radius: Float,
        skipMemoryCache: Boolean
    ) {
        //占位图和失败图是一样的时候
        loadRoundImg(imageView, uri, placeholderId, placeholderId, radius, skipMemoryCache)
    }

    @JvmStatic
    fun loadRoundImg(
        imageView: ImageView,
        uri: Uri?,
        @DrawableRes placeholderId: Int,
        radius: Float
    ) {
        //占位图和失败图是一样的时候
        loadRoundImg(imageView, uri, placeholderId, placeholderId, radius, true)
    }

    @JvmStatic
    fun loadRoundImg(
        imageView: ImageView, uri: Uri?, @DrawableRes placeholderId: Int,
        @DrawableRes errorId: Int, radius: Float, skipMemoryCache: Boolean
    ) {
        Glide.with(imageView.context)
            .load(uri ?: errorId)
            .skipMemoryCache(skipMemoryCache)
            .diskCacheStrategy(if (skipMemoryCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
            .apply(
                RequestOptions()
                    .placeholder(placeholderId)
                    .error(errorId)
                    .transform(GlideRoundTransform(radius))
            )
            .thumbnail(loadTransform(imageView.context, placeholderId, radius, skipMemoryCache))
            .thumbnail(loadTransform(imageView.context, errorId, radius, skipMemoryCache))
            .into(imageView)
    }

    //圆形
    @JvmStatic
    fun loadCircleImg(
        imageView: ImageView,
        url: String?,
        @DrawableRes placeholderId: Int,
        skipMemoryCache: Boolean
    ) {
        //占位图和失败图是一样的时候
        loadCircleImg(imageView, url, placeholderId, placeholderId, skipMemoryCache)
    }

    //圆形
    @JvmStatic
    fun loadCircleImg(imageView: ImageView, url: String?, @DrawableRes placeholderId: Int) {
        //占位图和失败图是一样的时候
        loadCircleImg(imageView, url, placeholderId, placeholderId, true)
    }

    //圆形
    @JvmStatic
    fun loadCircleImg(
        imageView: ImageView, imageUrl: String?, @DrawableRes placeholderId: Int,
        @DrawableRes errorId: Int, skipMemoryCache: Boolean
    ) {
        var url = imageUrl
        if (url == null) {
            url = ""
        }
        Glide.with(imageView.context)
            .load(url)
            .skipMemoryCache(skipMemoryCache)
            .diskCacheStrategy(if (skipMemoryCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
            .apply(
                RequestOptions()
                    .placeholder(placeholderId)
                    .error(errorId)
                    .transform(GlideCircleTransform())
            )
            .thumbnail(loadTransform(imageView.context, placeholderId, skipMemoryCache))
            .thumbnail(loadTransform(imageView.context, errorId, skipMemoryCache))
            .into(imageView)
    }

    private fun loadTransform(
        context: Context, @DrawableRes placeholderId: Int,
        radius: Float, skipMemoryCache: Boolean
    ): RequestBuilder<Drawable?> {
        return loadTransform(context, placeholderId, radius, true, skipMemoryCache)
    }

    private fun loadTransform(
        context: Context, @DrawableRes placeholderId: Int,
        radius: Float, centerCrop: Boolean, skipMemoryCache: Boolean
    ): RequestBuilder<Drawable?> {
        return Glide.with(context)
            .load(placeholderId)
            .skipMemoryCache(skipMemoryCache)
            .diskCacheStrategy(if (skipMemoryCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
            .apply(RequestOptions().transform(GlideRoundTransform(radius, centerCrop)))
    }

    private fun loadTransform(
        context: Context,
        @DrawableRes placeholderId: Int,
        skipMemoryCache: Boolean
    ): RequestBuilder<Drawable?> {
        return Glide.with(context)
            .load(placeholderId)
            .skipMemoryCache(skipMemoryCache)
            .diskCacheStrategy(if (skipMemoryCache) DiskCacheStrategy.NONE else DiskCacheStrategy.AUTOMATIC)
            .apply(RequestOptions().transform(GlideCircleTransform()))
    }
}