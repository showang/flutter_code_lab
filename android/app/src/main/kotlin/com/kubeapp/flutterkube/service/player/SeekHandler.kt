package com.kubeapp.flutterkube.service.player

import android.os.Handler
import android.os.Message
import com.kubeapp.flutterkube.service.KubePlayer
import com.kubeapp.flutterkube.service.player.youtube.KubePlayerDelegate

class SeekHandler : Handler() {

    companion object {
        const val ACTION_START = 1
        const val ACTION_STOP = 0
    }

    var delegate: KubePlayerDelegate? = null
    var player: KubePlayer? = null
    private var mLastPosition: Long = 0

    override fun handleMessage(msg: Message?) {
        when (msg?.what) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
    }

    private fun start() {
        synchronized(this) {
            sendEmptyMessageDelayed(ACTION_START, 500)
            val player = this.player ?: return
            mLastPosition = player.currentPosition
            delegate?.updateSeekPosition(mLastPosition, player.bufferedPosition, player.duration)
        }
    }

    private fun stop() {
        synchronized(this) {
            this.removeMessages(ACTION_START)
        }
    }

}