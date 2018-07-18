package com.kube.app.service.player.youtube

import android.net.Uri
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.util.ArrayMap
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.TransferListener
import com.google.android.exoplayer2.util.Util
import com.kkbox.openapi.model.Playlist
import com.kkbox.openapi.model.Track
import com.kkbox.openapi.tools.Logger
import com.kubeapp.flutterkube.R
import com.kubeapp.flutterkube.service.KubePlayer
import com.kubeapp.flutterkube.service.KubeService
import com.kubeapp.flutterkube.service.NotificationController
import com.kubeapp.flutterkube.service.player.ExternalMusicServicePlayer
import com.kubeapp.flutterkube.service.player.PlayStrategy
import com.kubeapp.flutterkube.service.player.Queue
import com.kubeapp.flutterkube.service.player.SeekHandler
import com.kubeapp.flutterkube.service.player.youtube.KubeYoutubeTrackApi
import com.kubeapp.flutterkube.service.player.youtube.MyYouTubeExtractor
import org.jetbrains.anko.runOnUiThread

class YoutubePlayer(
        service: KubeService,
        mediaSession: MediaSessionCompat,
        notificationController: NotificationController,
        playStrategy: PlayStrategy
) : ExternalMusicServicePlayer(service, mediaSession, notificationController, playStrategy) {

    override var state = KubePlayer.PlayerState.STOP
    val exoPlayer: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context,
            DefaultTrackSelector(
                    AdaptiveTrackSelection.Factory(DefaultBandwidthMeter())
            )
    ).apply { addListener(YouTubeExoPlayerListener(this@YoutubePlayer)) }
    var youtubeUrlCacheMap = ArrayMap<String, YoutubePlayableItem>()
    override val musicServiceName: String
        get() = context.getString(R.string.player_name_youtube)

    override val currentPosition: Long
        get() = exoPlayer.currentPosition
    override val bufferedPosition: Long
        get() = exoPlayer.bufferedPosition
    override val duration: Long
        get() = exoPlayer.duration

    override fun searchInQueue(searchQueue: Queue<Track>, forExport: Boolean) {
        val track = searchQueue.dequeue()
        if (track == null) {
            cancelSearch()
            return
        }
        val playableItem = youtubeUrlCacheMap[track.id]
        if (playableItem == null || !playableItem.isAvailable) {
            KubeYoutubeTrackApi(track).startRequest({
                onSearchComplete(YoutubePlayableItem(track), searchQueue)
            }, {
                val resultItem = it
                MyYouTubeExtractor(context) {
                    if (it != null) {
                        resultItem.sourceUrl = it.url
                    }
                    onSearchComplete(resultItem, searchQueue)
                }.execute(resultItem.videoUrl)
            })
        } else {
            onSearchComplete(playableItem, searchQueue)
        }
    }

    private fun onSearchComplete(item: YoutubePlayableItem, searchQueue: Queue<Track>) {
        youtubeUrlCacheMap[item.track.id] = item

        if (currentPlaylist == null) return
        kubePlayerDelegate?.onSearchFinished(item, currentPlaylist!!.tracks.indexOfFirst {
            it.id == item.track.id
        })
        checkToPlay(playStrategy.currentTrackIndex, 0)
        startSearchTask(searchQueue)
    }

    override fun checkToPlay(trackIndex: Int, counter: Int) {
        if (state != KubePlayer.PlayerState.LOADING) return
        val tracks = currentPlaylist?.tracks ?: return

        val track = tracks[trackIndex]
        val playableItem = youtubeUrlCacheMap[track.id]
        if (playableItem == null || !playableItem.isAvailable) {
            if (!isSearching) {
                Handler().post {
                    if (counter < tracks.size) {
                        checkToPlay(playStrategy.performNext(), counter + 1)
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
            Logger.e(javaClass.simpleName, "Start play: ${playableItem.track.name}")
            playUrl(playableItem.sourceUrl!!)
        }
    }

    private fun playUrl(url: String) {
        val bandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(context, Util.getUserAgent(context, "Kube"), (bandwidthMeter as TransferListener<in DataSource>))
        val extractorsFactory = DefaultExtractorsFactory()

        val mediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
                .setExtractorsFactory(extractorsFactory)
                .createMediaSource(Uri.parse(url))
        context.runOnUiThread {
            exoPlayer.prepare(mediaSource)
            play()
            mediaSession.setMetadata(
                    with(MediaMetadataCompat.Builder())
                    {
                        putString(MediaMetadataCompat.METADATA_KEY_TITLE, "${currentTrack?.name}")
                        putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "${currentTrack?.album?.artist?.name}")
                        putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "${currentPlaylist?.info?.title}")
                        putLong(MediaMetadataCompat.METADATA_KEY_DURATION, exoPlayer.duration)
                        build()
                    }
            )
            mediaSession.isActive = true
        }
    }

    override fun play() {
        seekHandler.sendEmptyMessage(SeekHandler.ACTION_START)
        state = KubePlayer.PlayerState.PLAYING
        exoPlayer.playWhenReady = true
        kubePlayerDelegate?.onPlayerPlay()

        buildNotification()
    }

    override fun playAt(trackIndex: Int) {
        playStrategy.currentTrackIndex = trackIndex
        state = KubePlayer.PlayerState.LOADING
        checkToPlay(trackIndex)
    }

    override fun pause() {
        if (state != KubePlayer.PlayerState.PLAYING && state != KubePlayer.PlayerState.LOADING) return
        seekHandler.sendEmptyMessage(SeekHandler.ACTION_STOP)
        state = KubePlayer.PlayerState.PAUSE
        exoPlayer.playWhenReady = false
        kubePlayerDelegate?.onPlayerPause()

        buildNotification()
    }

    override fun stop() {
        cancelSearch()
        seekHandler.sendEmptyMessage(SeekHandler.ACTION_STOP)
        exoPlayer.stop(true)
        state = KubePlayer.PlayerState.STOP
    }

    override fun next() {
        val tracks = currentPlaylist?.tracks ?: return
        state = KubePlayer.PlayerState.LOADING
        exoPlayer.stop(true)

        var counter = 0
        var nextIndex = playStrategy.performNext()
        var playableItem = youtubeUrlCacheMap[tracks[nextIndex].id]
        while (playableItem?.isAvailable == false || playableItem?.ignore == true) {
            nextIndex = playStrategy.performNext()
            counter++
            if (counter == tracks.size) {
                stop()
                return
            }
            playableItem = youtubeUrlCacheMap[tracks[nextIndex].id]
        }

        checkToPlay(nextIndex)
        buildNotification()
    }

    override fun previous() {
        val tracks = currentPlaylist?.tracks ?: return
        state = KubePlayer.PlayerState.LOADING
        exoPlayer.stop(true)
        var counter = 0
        var nextIndex = playStrategy.performPrevious()
        var playableItem = youtubeUrlCacheMap[tracks[nextIndex].id]
        while (playableItem?.isAvailable == false || playableItem?.ignore == true) {
            nextIndex = playStrategy.performPrevious()
            counter++
            if (counter == tracks.size) {
                stop()
                return
            }
            playableItem = youtubeUrlCacheMap[tracks[nextIndex].id]
        }
        checkToPlay(nextIndex)

        buildNotification()
    }

    override fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
    }

    override fun release() {
        cancelSearch()
        exoPlayer.release()
    }

    override fun export(playlist: Playlist) {
        service.onPlayerServiceEvent(context.getString(R.string.message_something_not_support, context.getString(R.string.player_name_youtube)), false)
        notificationController.onExportFinished(playlist, false)
    }

    class YouTubeExoPlayerListener(private val youtubePlayer: YoutubePlayer) : Player.DefaultEventListener() {

        private var retryCount = 0
        private var isBuffering = false

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Logger.e("ExoPlayer", when (playbackState) {
                Player.STATE_ENDED -> {
                    youtubePlayer.state = KubePlayer.PlayerState.LOADING
                    youtubePlayer.checkToPlay(youtubePlayer.playStrategy.performNext())
                    "STATE_ENDED"
                }
                Player.STATE_BUFFERING -> {
                    isBuffering = true
                    youtubePlayer.kubePlayerDelegate?.onBuffering(true)
                    "STATE_BUFFERING"
                }
                Player.STATE_IDLE -> {
                    //Workaround for exoPlayer load source video fail, retry until 3 times.
                    Logger.e("ExoPlayer", "Source: ${youtubePlayer.youtubeUrlCacheMap[youtubePlayer.currentTrack?.id]?.sourceUrl}")
                    Logger.e("ExoPlayer", "Response code: 403")
                    if (youtubePlayer.state == KubePlayer.PlayerState.PLAYING) {
                        val nextIndex = if (retryCount < 3) {
                            retryCount++
                            youtubePlayer.youtubeUrlCacheMap.remove(youtubePlayer.currentTrack?.id)
                            youtubePlayer.state = KubePlayer.PlayerState.LOADING
                            youtubePlayer.startSearchTask(Queue(arrayListOf(youtubePlayer.currentTrack!!)))
                            return
                        } else {
                            retryCount = 0
                            youtubePlayer.playStrategy.performNext()
                        }
                        Logger.e("ExoPlayer", "Retry to play index $nextIndex")
                        youtubePlayer.state = KubePlayer.PlayerState.LOADING
                        youtubePlayer.checkToPlay(nextIndex)
                    }
                    "STATE_IDLE"
                }
                Player.STATE_READY -> {
                    if (isBuffering) {
                        isBuffering = false
                        youtubePlayer.kubePlayerDelegate?.onBuffering(false)
                    }
                    "STATE_READY"
                }
                else -> "STATE_UNKNOWN"
            })
        }
    }

}