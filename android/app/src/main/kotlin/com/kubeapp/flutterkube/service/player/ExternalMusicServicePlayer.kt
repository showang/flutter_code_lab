package com.kubeapp.flutterkube.service.player

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import com.kkbox.openapi.model.Playlist
import com.kkbox.openapi.model.Track
import com.kkbox.openapi.tools.Logger
import com.kubeapp.flutterkube.service.KubePlayer
import com.kubeapp.flutterkube.service.KubeService
import com.kubeapp.flutterkube.service.NotificationController
import com.kubeapp.flutterkube.service.player.youtube.KubePlayerDelegate
import java.util.concurrent.Executors
import java.util.concurrent.Future

abstract class ExternalMusicServicePlayer(
        protected val service: KubeService,
        protected val mediaSession: MediaSessionCompat,
        protected val notificationController: NotificationController,
        override val playStrategy: PlayStrategy
) : KubePlayer {

    override var state = KubePlayer.PlayerState.STOP

    var kubePlayerDelegate: KubePlayerDelegate? = null
        set(value) {
            field = value
            seekHandler.delegate = value
        }

    protected val seekHandler = SeekHandler().apply { player = this@ExternalMusicServicePlayer }
    protected val context: Context get() = service.applicationContext
    protected val currentPlaylist get() = service.currentPlaylist

    protected val currentTrack
        get() = service.currentPlaylist?.tracks?.get(playStrategy.currentTrackIndex)

    private val searchThreadExecutor = Executors.newSingleThreadExecutor()
    private var searchFuture: Future<*>? = null
    protected val isSearching get() = searchFuture != null

    override fun startPlay(playlist: Playlist, atIndex: Int?) {
        playStrategy.init(playlist.tracks.size, atIndex)
        val searchQueue = Queue(
                playStrategy.playingTrackIndexSequence.map { playlist.tracks[it] }.toMutableList()
        )
        Logger.e(javaClass.simpleName, "startPlay Loading")
        state = KubePlayer.PlayerState.LOADING
        startSearchTask(searchQueue)
    }


    protected fun startSearchTask(searchQueue: Queue<Track>, forExport: Boolean = false) {
        this.searchFuture = searchThreadExecutor.submit {
            searchInQueue(searchQueue, forExport)
        }
    }

    protected fun cancelSearch() {
        searchFuture?.cancel(true)
        searchFuture = null
    }

    protected abstract fun searchInQueue(searchQueue: Queue<Track>, forExport: Boolean)

    protected abstract fun checkToPlay(trackIndex: Int, counter: Int = 0)

    protected fun buildNotification() {
        val style = if (isPause) NotificationController.NotificationStyle.PAUSED else NotificationController.NotificationStyle.PLAYING
        notificationController.build(style, currentPlaylist?.info, currentTrack)
    }

    override fun export(playlist: Playlist) {
        service.onPlayerServiceEvent("Not support yet.", false)
    }

}