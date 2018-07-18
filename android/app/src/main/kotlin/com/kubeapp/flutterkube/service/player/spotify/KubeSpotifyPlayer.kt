package com.kubeapp.flutterkube.service.player.spotify

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.util.ArrayMap
import android.support.v7.preference.PreferenceManager
import android.util.Base64
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.kkbox.openapi.infrastructure.implementation.OkhttpRequestExecutor
import com.kkbox.openapi.model.Playlist
import com.kkbox.openapi.model.Track
import com.kkbox.openapi.tools.Logger
import com.kubeapp.flutterkube.R
import com.kubeapp.flutterkube.service.KubePlayer
import com.kubeapp.flutterkube.service.KubeService
import com.kubeapp.flutterkube.service.NotificationController
import com.kubeapp.flutterkube.service.Settings.Player.SPOTIFY
import com.kubeapp.flutterkube.service.player.ExternalMusicServicePlayer
import com.kubeapp.flutterkube.service.player.PlayStrategy
import com.kubeapp.flutterkube.service.player.Queue
import com.kubeapp.flutterkube.service.player.SeekHandler
import com.spotify.sdk.android.player.*
import com.spotify.sdk.android.player.SpotifyPlayer.InitializationObserver
import java.io.ByteArrayOutputStream

class KubeSpotifyPlayer(
        service: KubeService,
        mediaSession: MediaSessionCompat,
        notificationController: NotificationController,
        playStrategy: PlayStrategy
) : ExternalMusicServicePlayer(service, mediaSession, notificationController, playStrategy), ConnectionStateCallback, Player.NotificationCallback {

    companion object {
        const val CLIENT_ID = "6d665447ee094bddb63baa3f76f937e0"
        const val PREFERENCE_AUTH_TOKEN = "spotify_auth"
        const val PREFERENCE_REFRESH_TOKEN = "spotify_refresh"
    }

    private var player: SpotifyPlayer? = null
    private var authToken: String
    private var exportingPlaylist: Playlist? = null
    var playableItemCacheMap = ArrayMap<String, SpotifyPlayableItem>()
    override val musicServiceName: String
        get() = context.getString(R.string.player_name_spotify)

    init {
        authToken = PreferenceManager.getDefaultSharedPreferences(context).getString(PREFERENCE_AUTH_TOKEN, "")
        val config = Config(context, authToken, CLIENT_ID)
        Logger.e(javaClass.simpleName, "init player")
        Spotify.getPlayer(config, this, object : InitializationObserver {
            override fun onInitialized(p: SpotifyPlayer) {
                player = p
                player?.addConnectionStateCallback(this@KubeSpotifyPlayer)
                player?.addNotificationCallback(this@KubeSpotifyPlayer)
            }

            override fun onError(p0: Throwable?) {
                Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "onError: ${p0?.message}")
                onPlayerServiceEvent("Spotify player initialize error", true)
            }

        })
    }

    override val currentPosition: Long
        get() = player?.playbackState?.positionMs ?: 0
    override val bufferedPosition: Long
        get() = player?.metadata?.currentTrack?.durationMs ?: 0
    override val duration: Long
        get() = player?.metadata?.currentTrack?.durationMs ?: 0

    override fun startPlay(playlist: Playlist, atIndex: Int?) {
        val superCall = { super.startPlay(playlist, atIndex) }
        if (player == null || player?.isShutdown == true) {
            val config = Config(context, authToken, CLIENT_ID)
            Spotify.getPlayer(config, this, object : InitializationObserver {
                override fun onInitialized(p: SpotifyPlayer) {
                    player = p
                    player?.addConnectionStateCallback(this@KubeSpotifyPlayer)
                    player?.addNotificationCallback(this@KubeSpotifyPlayer)
                    superCall()
                }

                override fun onError(p0: Throwable?) {
                    Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "onError: ${p0?.message}")
                    onPlayerServiceEvent("Spotify player initialize error", true)
                }
            })
        } else {
            superCall()
        }
    }

    override fun searchInQueue(searchQueue: Queue<Track>, forExport: Boolean) {
        val track = searchQueue.dequeue()
        if (track == null) {
            cancelSearch()
            if (forExport) {
                if (exportingPlaylist != null) {
                    startSpotifyExportProcess(exportingPlaylist!!)
                } else {
                    onExportError().invoke(Error("Playlist not found."))
                }
            }
            return
        }
        val playableItem = playableItemCacheMap[track.id]
        if (playableItem == null || !playableItem.isAvailable) {
            SpotifySearchApi(track, authToken)
                    .startRequest({
                        if (it is OkhttpRequestExecutor.AuthError) {
                            Logger.e("SpotifySearchApi", "Token expired, refresh.")
                            refreshToken {
                                if (it != null) {
                                    onPlayerServiceEvent("Refresh token error, plz retry later.", false)
                                } else {
                                    searchQueue.items.add(0, track)
                                    searchInQueue(searchQueue, forExport)
                                }
                            }
                        } else {
                            Logger.e("SpotifySearchApi", "ERROR: $it")
                            onSearchComplete(SpotifyPlayableItem(track), searchQueue, forExport)
                        }
                    }, {
                        onSearchComplete(it, searchQueue, forExport)
                    })
        } else {
            onSearchComplete(playableItem, searchQueue, forExport)
        }
    }

    private fun onSearchComplete(item: SpotifyPlayableItem, searchQueue: Queue<Track>, forExport: Boolean) {
        playableItemCacheMap[item.track.id] = item

        if (forExport) {
            startSearchTask(searchQueue, forExport)
            return
        }
        if (currentPlaylist == null) return
        kubePlayerDelegate?.onSearchFinished(item, currentPlaylist!!.tracks.indexOfFirst {
            it.id == item.track.id
        })
        checkToPlay(playStrategy.currentTrackIndex)
        startSearchTask(searchQueue)
    }

    override fun checkToPlay(trackIndex: Int, counter: Int) {
        if (state != KubePlayer.PlayerState.LOADING) return
        val tracks = currentPlaylist?.tracks ?: return
        val track = tracks[trackIndex]
        val playableItem = playableItemCacheMap[track.id]
        if (playableItem == null || !playableItem.isAvailable) {
            if (!isSearching) {
                Handler().post {
                    if (counter < tracks.size) {
                        checkToPlay(playStrategy.performNext())
                    } else {
                        service.stop()
                        service.onPlayerServiceEvent("All playlist tracks is not available.", false)
                    }
                }
            } else {
                //Wait for search complete
                kubePlayerDelegate?.onBuffering(true)
                if (playableItem?.isAvailable == false) {
                    playStrategy.performNext()
                } else {
                    kubePlayerDelegate?.updateCurrentPlayingTrack(trackIndex)
                }
            }
        } else {
            kubePlayerDelegate?.updateCurrentPlayingTrack(trackIndex)

            mediaSession.setMetadata(with(MediaMetadataCompat.Builder()) {
                putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.name)
                putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.album.artist.name)
                putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "${currentPlaylist?.info?.title}")
                putLong(MediaMetadataCompat.METADATA_KEY_DURATION, playableItem.duration)
                build()
            })
            play()
            mediaSession.isActive = true
        }
    }

    override fun play() {
        if (isPause) {
            player?.resume(object : Player.OperationCallback {
                override fun onSuccess() {
                    Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Resume success.")
                }

                override fun onError(p0: Error?) {
                    Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Resume error: $p0")
                    Handler().postDelayed({
                        Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Retry resume.")
                        state = KubePlayer.PlayerState.LOADING
                        play()
                    }, 1000)
                }

            })
        } else {
            val track = currentTrack ?: return
            val uri = playableItemCacheMap[track.id]?.playUri
            player?.playUri(object : Player.OperationCallback {
                override fun onSuccess() {
                    Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Play uri success: $uri")
                }

                override fun onError(p0: Error?) {
                    Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Play uri error: $p0")
                    Handler().postDelayed({
                        Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Retry play.")
                        state = KubePlayer.PlayerState.LOADING
                        play()
                    }, 1000)
                }

            }, uri, 0, 0)
        }
        state = KubePlayer.PlayerState.PLAYING
        kubePlayerDelegate?.onPlayerPlay()
        seekHandler.sendEmptyMessage(SeekHandler.ACTION_START)
        buildNotification()
    }

    override fun playAt(trackIndex: Int) {
        playStrategy.currentTrackIndex = trackIndex
        state = KubePlayer.PlayerState.LOADING
        checkToPlay(trackIndex)
    }

    override fun pause() {
        seekHandler.sendEmptyMessage(SeekHandler.ACTION_STOP)
        state = KubePlayer.PlayerState.PAUSE
        player?.pause(object : Player.OperationCallback {
            override fun onSuccess() {
                Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Pause success.")
            }

            override fun onError(p0: Error?) {
                Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Pause error: $p0")
            }
        })

        kubePlayerDelegate?.onPlayerPause()
        buildNotification()
    }

    private fun simulationStop() {
        player?.pause(object : Player.OperationCallback {
            override fun onSuccess() {
                Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "simulationStop success.")
            }

            override fun onError(p0: Error?) {
                Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "simulationStop error: $p0")
            }
        })
    }

    override fun stop() {
        seekHandler.sendEmptyMessage(SeekHandler.ACTION_STOP)
        simulationStop()
        cancelSearch()
        state = KubePlayer.PlayerState.STOP
    }

    override fun next() {
        simulationStop()
        state = KubePlayer.PlayerState.LOADING
        checkToPlay(playStrategy.performNext())

        buildNotification()
    }

    override fun previous() {
        simulationStop()
        val tracks = currentPlaylist?.tracks ?: return
        state = KubePlayer.PlayerState.LOADING
        var counter = 0
        var nextIndex = playStrategy.performPrevious()
        while (playableItemCacheMap[tracks[nextIndex].id]?.isAvailable == false) {
            nextIndex = playStrategy.performPrevious()
            counter++
            if (counter == tracks.size) {
                stop()
                return
            }
        }
        checkToPlay(nextIndex)

        buildNotification()
    }

    override fun seekTo(position: Long) {
        player?.seekToPosition(object : Player.OperationCallback {
            override fun onSuccess() {
                Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Seek success.")
            }

            override fun onError(p0: Error?) {
                Logger.e(this@KubeSpotifyPlayer.javaClass.simpleName, "Seek error: $p0")
            }

        }, position.toInt())
    }

    override fun release() {
        Logger.e(javaClass.simpleName, "Release player.")
        stop()
        player?.removeNotificationCallback(this)
        player?.removeConnectionStateCallback(this)
        player = null
        Spotify.destroyPlayer(this)
        playableItemCacheMap.clear()
    }

    //Spotify callbacks
    override fun onPlaybackError(p0: Error?) {
        Logger.e(javaClass.simpleName, "onPlaybackError: $p0")
        next()
    }

    override fun onPlaybackEvent(p0: PlayerEvent?) {
        when (p0) {
            PlayerEvent.kSpPlaybackNotifyAudioDeliveryDone -> next()
            else -> {
            }
        }

        Logger.e(javaClass.simpleName, "playback event: ${p0?.name}")
    }

    override fun onLoggedOut() {
        Logger.e(javaClass.simpleName, "onLoggedOut")
    }

    override fun onLoggedIn() {
        Logger.e(javaClass.simpleName, "onLoggedIn")
    }

    override fun onConnectionMessage(p0: String?) {
        Logger.e(javaClass.simpleName, "onConnectionMessage: $p0")
    }

    @SuppressLint("ApplySharedPref")
    override fun onLoginFailed(p0: Error?) {
        Logger.e(javaClass.simpleName, "onLoginFailed: $p0")
        when (p0) {
            Error.kSpErrorNeedsPremium -> {
                onPlayerServiceEvent("Needs Spotify premium account to playing songs.", true)
            }
            Error.kSpErrorLoginBadCredentials -> refreshToken { error ->
                //Token expired
                if (error != null) {
                    onPlayerServiceEvent("Spotify refresh permission error.")
                    return@refreshToken
                }
                val intent = Intent(context, KubeService::class.java)
                intent.action = KubeService.CONFIG_CHANGE_PLAYER
                intent.putExtra(KubeService.EXTRA_STRING_CHANGE_PLAYER_NAME, SPOTIFY)
                context.startService(intent)
            }
            else -> {
                onPlayerServiceEvent("Spotify player unknown error.", true)
            }
        }

    }

    @SuppressLint("ApplySharedPref")
    private fun refreshToken(callback: (error: kotlin.Error?) -> Unit = {}) {
        val preference = PreferenceManager.getDefaultSharedPreferences(context)
        val refreshToken = preference.getString(PREFERENCE_REFRESH_TOKEN, "")
        RefreshTokenApi(refreshToken).startRequest({ error: kotlin.Error ->
            callback(error)
        }, { newToken ->
            authToken = newToken
            preference.edit().putString(PREFERENCE_AUTH_TOKEN, newToken).commit()
            callback(null)
        })
    }

    private fun onPlayerServiceEvent(message: String, resetPlayer: Boolean = false) {
        service.onPlayerServiceEvent(message, resetPlayer)
    }

    override fun onTemporaryError() {
        Logger.e(javaClass.simpleName, "onTemporaryError")
    }

    override fun export(playlist: Playlist) {
        exportingPlaylist = playlist
        searchInQueue(Queue(playlist.tracks.toMutableList()), true)
    }

    private fun startSpotifyExportProcess(playlist: Playlist) {
        SpotifyUserIdApi(authToken).startRequest(onExportError()) { userId ->
            SpotifyCreatePlaylistApi(authToken, userId, playlist.info.title, playlist.info.description)
                    .startRequest(onExportError()) { playlistId ->
                        SpotifyAddTrackToPlaylistApi(authToken, userId, playlistId, playlist.tracks.map {
                            playableItemCacheMap[it.id] ?: SpotifyPlayableItem(it, null)
                        }).startRequest(onExportError()) {
                            onExportSuccess("spotify:user:$userId:playlist:$playlistId")
                        }
                        Glide.with(context)
                                .asBitmap()
                                .load(playlist.info.covers[1].url)
                                .into(UpdatePlaylistCoverTarget(authToken, userId, playlistId))
                    }
        }
    }

    private class UpdatePlaylistCoverTarget(private val auth: String,
                                            private val userId: String,
                                            private val playlistId: String) : SimpleTarget<Bitmap>(300, 300) {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            val outputStream = ByteArrayOutputStream()
            resource.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) //bm is the bitmap object
            val base64Image = Base64.encode(outputStream.toByteArray(), Base64.NO_WRAP)
            SpotifyUpdateCoverImageApi(auth, userId, playlistId, base64Image)
                    .startRequest {
                        Logger.e(this@UpdatePlaylistCoverTarget.javaClass.simpleName, "Update cover success.")
                    }
        }
    }

    private fun onExportSuccess(uri: String?) {
        service.onExportFinished(exportingPlaylist, true, uri)
        exportingPlaylist = null
    }

    private fun onExportError(): (kotlin.Error) -> Unit {
        return {
            service.onExportFinished(exportingPlaylist, false)
            exportingPlaylist = null
        }
    }

}