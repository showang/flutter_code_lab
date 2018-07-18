package com.kubeapp.flutterkube.service

import com.kkbox.openapi.model.Playlist
import com.kubeapp.flutterkube.service.player.PlayStrategy

interface KubePlayer {

    val musicServiceName: String
    val state: PlayerState

    enum class PlayerState {
        LOADING, PLAYING, PAUSE, STOP
    }

    val currentPosition: Long
    val bufferedPosition: Long
    val duration: Long
    val playStrategy: PlayStrategy

    val isPlaying: Boolean
        get() = state == PlayerState.PLAYING
    val isPause: Boolean
        get() = state == PlayerState.PAUSE
    val isLoading: Boolean
        get() = state == PlayerState.LOADING
    val isStop: Boolean
        get() = state == PlayerState.STOP

    fun startPlay(playlist: Playlist, atIndex: Int?)
    fun play()
    fun playAt(trackIndex: Int)
    fun pause()
    fun stop()
    fun next()
    fun previous()

    fun seekTo(position: Long)

    fun release()

    fun export(playlist: Playlist)
}