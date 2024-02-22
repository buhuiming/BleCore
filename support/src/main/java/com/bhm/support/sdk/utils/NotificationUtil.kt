package com.bhm.support.sdk.utils

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.text.TextUtils
import androidx.core.app.NotificationCompat


/**
 * 从Android 8.1（API级别27）开始，如果同一时间发布了多个通知的话, 只有第一个通知会发出声音
 */
class NotificationUtil private constructor(
    context: Context,
) : ContextWrapper(context) {

    private var largeIcon: Bitmap? = null

    private var smallIcon: Int = 0

    companion object {
        const val DEFAULT_CHANNEL_ID = "DEFAULT_CHANNEL_ID" //默认的channel

        private const val DEFAULT_CHANNEL = "DEFAULT_CHANNEL"

        private var notificationUtil: NotificationUtil? = null

        @Synchronized
        fun getInstance(context: Context): NotificationUtil? {
            if (notificationUtil == null) {
                notificationUtil = NotificationUtil(context)
            }
            return notificationUtil
        }
    }

    private var manager: NotificationManager? = null
        get() {
            if (field == null) {
                field = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            }
            return field
        }

    fun init(smallIconId: Int, largeIconId: Int, channels: Map<String, String>?) {
        setIcon(smallIconId, largeIconId)
        createChannels(channels)
    }

    /*设置图标*/
    private fun setIcon(smallIconId: Int, largeIconId: Int){
        largeIcon = BitmapFactory.decodeResource(resources, largeIconId)
        smallIcon = smallIconId
    }

    //设置消息渠道
    private fun createChannels(channels: Map<String, String>?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(DEFAULT_CHANNEL_ID, DEFAULT_CHANNEL)
            if (channels == null || channels.isEmpty()) {
                return
            }
            channels.forEach {
                createNotificationChannel(it.key, it.value)
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String,
        channelName: String
    ) {
        val channel =
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        channel.setShowBadge(true)
        channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager?.createNotificationChannel(channel)
    }

    /**
     * 获取默认通道的 NotificationCompat.Builder
     */
    private fun getNotificationBuilderByChannel(channel: String): NotificationCompat.Builder {
        val builder: NotificationCompat.Builder
        val channelId = if (TextUtils.isEmpty(channel)) DEFAULT_CHANNEL_ID else channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = NotificationCompat.Builder(applicationContext, channelId)
        } else {
            builder = NotificationCompat.Builder(this, channelId).setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { //8.0以下 && 7.0及以上 设置优先级
                builder.priority = NotificationManager.IMPORTANCE_HIGH
            } else {
                builder.priority = NotificationCompat.PRIORITY_HIGH
            }
        }
        return builder
    }

    /**
     * 创建普通的文字通知1
     * note: 默认通知只显示一行(系统自动截取)
     * 可以通过NotificationCompat.BigTextStyle()显示多行文本
     */
    private fun buildNotificationText(
        title: String,
        body: String,
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        return getNotificationBuilderByChannel(channelId)
            .setAutoCancel(true)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            //.setTimeoutAfter(3000)//时间过后自动取消该通知
            //.setNumber(1127)//超过999 系统就直接显示999
            .setBadgeIconType(NotificationCompat.BADGE_ICON_LARGE) //长按应用图标,通知显示的图标类型, 默认显示大图标
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(body))
    }

    /**
     * 创建普通的文字通知2 添加Action
     * @param actions 在通知消息中添加按钮 最多添加3个
     */
    private fun buildNotificationTextAction(
        title: String,
        body: String,
        pendingIntent: PendingIntent,
        channelId: String,
        vararg actions: NotificationCompat.Action
    ): NotificationCompat.Builder {
        val builder = getNotificationBuilderByChannel(channelId)
            .setAutoCancel(true)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
            )
        if (actions.isNotEmpty()) {
            for (action in actions) {
                builder.addAction(action)
            }
        }
        return builder
    }

    /**
     * 创建带图片的通知
     * 消息折叠时显示小图, 展开后显示大图
     */
    private fun buildNotificationImage(
        title: String,
        body: String,
        imgBitmap: Bitmap,
        pendingIntent: PendingIntent,
        channelId: String
    ): NotificationCompat.Builder {
        return getNotificationBuilderByChannel(channelId)
            .setAutoCancel(true)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .setBigContentTitle(title)
                    .bigLargeIcon(imgBitmap)
                    .bigPicture(imgBitmap)
            )
    }

    /**
     * 创建带图片的通知2 添加Action
     * 消息折叠时显示小图, 展开后显示大图
     */
    private fun buildNotificationImageAction(
        title: String,
        body: String,
        imgBitmap: Bitmap,
        pendingIntent: PendingIntent,
        channelId: String,
        vararg actions: NotificationCompat.Action
    ): NotificationCompat.Builder {
        val builder = getNotificationBuilderByChannel(channelId)
            .setAutoCancel(true)
            .setSmallIcon(smallIcon)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigPictureStyle()
                    .setBigContentTitle(title)
                    .bigLargeIcon(imgBitmap)
                    .bigPicture(imgBitmap)
            )
        if (actions.isNotEmpty()) {
            for (action in actions) {
                builder.addAction(action)
            }
        }
        return builder
    }

    fun buildNotificationText(
        title: String,
        body: String,
        pendingIntent: PendingIntent,
        channelId: String,
        vararg actions: NotificationCompat.Action
    ): NotificationCompat.Builder {
        return if (actions.isNotEmpty()) buildNotificationTextAction(
            title,
            body,
            pendingIntent,
            channelId = channelId,
            *actions
        ) else buildNotificationText(title, body, pendingIntent, channelId)
    }

    fun buildNotificationImage(
        title: String,
        body: String,
        imgBitmap: Bitmap,
        pendingIntent: PendingIntent,
        channelId: String,
        vararg actions: NotificationCompat.Action
    ): NotificationCompat.Builder {
        return if (actions.isNotEmpty()) buildNotificationImageAction(
            title,
            body,
            imgBitmap,
            pendingIntent,
            channelId,
            *actions
        ) else buildNotificationImage(title, body, imgBitmap, pendingIntent, channelId)
    }

    fun notify(id: Int, notification: NotificationCompat.Builder) {
        manager!!.notify(id, notification.build())
    }
}