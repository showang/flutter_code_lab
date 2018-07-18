package com.kubeapp.flutterkube.service.player

interface KubePlayerBehavior {

    fun onInitPlayer(oldPlayerName: String? = null, newPlayerName: String)

}