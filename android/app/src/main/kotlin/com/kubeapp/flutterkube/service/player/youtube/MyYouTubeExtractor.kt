package com.kubeapp.flutterkube.service.player.youtube

import android.content.Context
import android.util.SparseArray
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
//import com.facebook.network.connectionclass.ConnectionClassManager
//import com.facebook.network.connectionclass.ConnectionQuality
//import com.facebook.network.connectionclass.DeviceBandwidthSampler
import com.kkbox.openapi.tools.Logger

class MyYouTubeExtractor(context: Context, private val callback: (YtFile?) -> Unit) : YouTubeExtractor(context) {

//    private var connectionQuality = ConnectionQuality.MODERATE
//    private val deviceBandwidthSampler = DeviceBandwidthSampler.getInstance()

    override fun extract(youtubeLink: String?, parseDashManifest: Boolean, includeWebM: Boolean) {
//        deviceBandwidthSampler.startSampling()
        super.extract(youtubeLink, parseDashManifest, includeWebM)
    }

    override fun onExtractionComplete(ytFiles: SparseArray<YtFile>?, videoMeta: VideoMeta?) {
        if (ytFiles == null) {
            Logger.e("MyYouTubeExtractor", "ytFile not found.")
            callback(null)
            return
        }
//        deviceBandwidthSampler.stopSampling()
        callback(getBestStream(ytFiles))
    }

    private fun getBestStream(ytFiles: SparseArray<YtFile>): YtFile? {
//        connectionQuality = ConnectionClassManager.getInstance().currentBandwidthQuality
        val iTags: IntArray =
//                when (connectionQuality) {
//            ConnectionQuality.POOR -> intArrayOf(18, 17, 140, 251, 141)
//            ConnectionQuality.GOOD, ConnectionQuality.EXCELLENT -> intArrayOf(18, 141, 251, 140, 17)
//            else ->
                intArrayOf(18, 251, 141, 140, 17)
//        }

        return when {
            ytFiles.get(iTags[0]) != null -> ytFiles.get(iTags[0])
            ytFiles.get(iTags[1]) != null -> ytFiles.get(iTags[1])
            ytFiles.get(iTags[2]) != null -> ytFiles.get(iTags[2])
            else -> ytFiles.get(iTags[3])
        }
    }

}