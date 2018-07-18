package com.kubeapp.flutterkube.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.kkbox.openapi.model.Playlist
import com.kkbox.openapi.model.PlaylistInfo
import com.kkbox.openapi.model.Track
import com.kubeapp.flutterkube.MainActivity
import com.kubeapp.flutterkube.R
import org.jetbrains.anko.runOnUiThread


class NotificationController(private val service: KubeService, private val mediaSession: MediaSessionCompat) {

    companion object {
        private const val notificationChannelID = "KubePlayer"
        private const val ID_PLAYER_CONTROL = 1
        private const val ID_EXPORTING = 10
        private const val ID_EXPORT_SUCCESS = 11
    }

    enum class NotificationStyle {
        PLAYING, PAUSED
    }

    private val notificationManager: NotificationManager
    private val context get() = service.applicationContext

    init {
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(notificationChannelID, "Kube", NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(true)
            channel.enableVibration(false)
            channel.setSound(null, null)
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val stopPendingIntent: PendingIntent
        get() {
            val intent = Intent(context, KubeService::class.java)
            intent.action = KubeService.ACTION_STOP
            return PendingIntent.getService(context, 1, intent, 0)
        }

    private val clickPendingIntent: PendingIntent
        get() {
            val intent = Intent(context, MainActivity::class.java)
            intent.action = Intent.ACTION_MAIN
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            return PendingIntent.getActivity(context, 0, intent, 0)
        }

    private fun generateAction(icon: Int, title: String, intentAction: String): android.support.v4.app.NotificationCompat.Action {
        val intent = Intent(context, KubeService::class.java)
        intent.action = intentAction
        val pendingIntent = PendingIntent.getService(context, 1, intent, 0)
        return NotificationCompat.Action.Builder(icon, title, pendingIntent).build()
    }

    fun cancel() {
        notificationManager.cancel(ID_PLAYER_CONTROL)
    }

    fun buildExporting(playlistInfo: PlaylistInfo) {
        with(NotificationCompat.Builder(context, notificationChannelID)) {
            setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
            setContentTitle(context.getString(R.string.notify_exporting))
            setContentText(playlistInfo.title)
            notificationManager.notify(ID_EXPORTING, build())
        }
    }

    fun onExportFinished(playlist: Playlist?, success: Boolean, uri: String? = null) {
        notificationManager.cancel(ID_EXPORTING)
        with(NotificationCompat.Builder(context, notificationChannelID)) {
            setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
            setContentTitle(
                    if (success) {
                        if (uri != null) {
                            val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                            intent.data = Uri.parse(uri)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            setContentIntent(PendingIntent.getActivity(context, 2, intent, FLAG_ONE_SHOT))
                        }
                        context.getString(R.string.notify_export_success)
                    } else context.getString(R.string.notify_export_failed)
            )
            setContentText(playlist?.info?.title)
            notificationManager.notify(ID_EXPORT_SUCCESS, build())
        }
    }

    fun build(style: NotificationStyle, playlistInfo: PlaylistInfo?, track: Track?) {

        if (track == null || playlistInfo == null) return

        with(NotificationCompat.Builder(context, notificationChannelID)) {
            setSmallIcon(R.drawable.ic_play_arrow_black_24dp)
            setSubText(playlistInfo.title)
            setContentTitle(track.name)
            setContentText(track.album.artist.name)
            setShowWhen(false)

            setContentIntent(clickPendingIntent)
            setDeleteIntent(stopPendingIntent)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                color = ContextCompat.getColor(context, R.color.colorAccent)
            }

            val mediaStyle = android.support.v4.media.app.NotificationCompat.MediaStyle()
            mediaStyle.setMediaSession(mediaSession.sessionToken)
            mediaStyle.setShowActionsInCompactView(0, 1, 2)
            setStyle(mediaStyle)

            addAction(generateAction(R.drawable.ic_skip_previous_40dp, "Previous", KubeService.ACTION_PREVIOUS))
            addAction(when (style) {
                NotificationStyle.PLAYING -> generateAction(R.drawable.ic_pause_48dp, "Pause", KubeService.ACTION_PAUSE)
                NotificationStyle.PAUSED -> generateAction(R.drawable.ic_play_arrow_48dp, "Play", KubeService.ACTION_PLAY)
            })
            addAction(generateAction(R.drawable.ic_skip_next_40dp, "Next", KubeService.ACTION_NEXT))

            val notifyRunnable = {
                service.startForeground(ID_PLAYER_CONTROL, build())
                if (style == NotificationStyle.PAUSED) {
                    service.stopForeground(false)
                }
            }

            context.runOnUiThread {
                val coverUrl = track.album.covers[1].url
                if (!coverUrl.isEmpty()) {
                    Glide.with(context)
                            .asBitmap()
                            .load(coverUrl)
                            .into(object : SimpleTarget<Bitmap>() {
                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    notifyRunnable()
                                }

                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    setLargeIcon(resource)
                                    notifyRunnable()
                                }
                            })
                } else {
                    notifyRunnable()
                }
            }
        }
    }

}