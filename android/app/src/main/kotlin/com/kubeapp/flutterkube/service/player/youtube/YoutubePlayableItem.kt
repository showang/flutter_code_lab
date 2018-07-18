package com.kube.app.service.player.youtube

import com.kkbox.openapi.model.ImageInfo
import com.kkbox.openapi.model.Track
import com.kubeapp.flutterkube.service.player.PlayableItem

class YoutubePlayableItem(
        override val track: Track,
        val videoId: String? = null,
        val videoTitle: String? = null,
        val coverImage: ImageInfo? = null
) : PlayableItem {
    companion object {
        private const val youtubeBaseUrl = "https://www.youtube.com/watch?v="
    }

    var ignore: Boolean = false
    val videoUrl: String
        get() = "$youtubeBaseUrl$videoId"

    override val isAvailable: Boolean
        get() = videoId != null && !videoId.isEmpty() && sourceUrl != null

    var sourceUrl: String? = null
}