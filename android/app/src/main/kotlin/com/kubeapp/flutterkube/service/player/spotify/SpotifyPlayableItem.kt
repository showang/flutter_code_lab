package com.kubeapp.flutterkube.service.player.spotify

import com.kkbox.openapi.model.Track
import com.kubeapp.flutterkube.service.player.PlayableItem

class SpotifyPlayableItem(
        override val track: Track,
        val playUri: String? = null,
        val duration: Long = 0
) : PlayableItem {

    override val isAvailable get() = playUri != null

}