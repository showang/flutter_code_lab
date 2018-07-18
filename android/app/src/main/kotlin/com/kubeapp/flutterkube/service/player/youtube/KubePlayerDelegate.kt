package com.kubeapp.flutterkube.service.player.youtube

import com.kubeapp.flutterkube.service.player.PlayableItem

interface KubePlayerDelegate {

    fun onSearchFinished(item: PlayableItem, index: Int)

    fun updateCurrentPlayingTrack(index: Int)

    fun onPlayerPause()

    fun onPlayerPlay()

    fun onBuffering(isBegin: Boolean)

    fun updateSeekPosition(position: Long, bufferPosition: Long, duration: Long)
}