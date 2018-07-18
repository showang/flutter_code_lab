package com.kubeapp.flutterkube.service.player

import com.kkbox.openapi.model.Track

interface PlayableItem {

    val track: Track
    val isAvailable: Boolean

}