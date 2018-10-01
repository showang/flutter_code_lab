package com.kubeapp.flutterkube

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import io.flutter.app.FlutterActivity
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugins.GeneratedPluginRegistrant

class MainActivity : FlutterActivity(), ServiceConnection {

    companion object {
        private const val CHANNEL_PLAYER_METHOD = "com.kube.flutter/player.method"
        private const val CHANNEL_PLAYER_EVENT = "com.kube.flutter/player.event"

        private const val METHOD_START_PLAY = "startPlay"
        private const val ARGUMENT_KEY_PLAYLIST_INFO = "info"
        private const val ARGUMENT_KEY_START_INDEX = "position"
    }

//    var service: KubeService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        KKBOXOpenApi.install("fc87971f683fd619ba46be6e3aa2cbc2", "5b70cd567551d03d4c43c5cec9e02d1a")
//        KKBOXOpenApi.fetchAuthToken { }
//        bindService(Intent(this, KubeService::class.java), this, Context.BIND_AUTO_CREATE)
        GeneratedPluginRegistrant.registerWith(this)
        EventChannel(flutterView, CHANNEL_PLAYER_EVENT).setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(p0: Any?, p1: EventChannel.EventSink?) {

            }

            override fun onCancel(p0: Any?) {

            }

        })



        MethodChannel(flutterView, CHANNEL_PLAYER_METHOD).setMethodCallHandler { methodCall, result ->
            when (methodCall.method) {
                METHOD_START_PLAY -> {
//                    val playlistInfoJson = methodCall.argument<String>(ARGUMENT_KEY_PLAYLIST_INFO)
//                    val position = methodCall.argument<Int>(ARGUMENT_KEY_START_INDEX) ?: 0
//                    val playlistInfo = PlaylistInfoEntity.parse(Gson().fromJson(playlistInfoJson, PlaylistInfoEntity::class.java))
//                    PlaylistController.fetchAllTracks(playlistInfo, errorCallback = {
//                        Log.e("fetchAllTracks", "error: $it")
//                        result.error("what?", "what?", it)
//                    }) {
//                        Log.e("fetchAllTracks", "Success")
//                        val intent = Intent(this, KubeService::class.java)
//                        intent.action = KubeService.ACTION_START_PLAYLIST
//                        intent.putExtra(KubeService.EXTRA_PLAYLIST, it)
//                        intent.putExtra(KubeService.EXTRA_START_INDEX, position)
//                        this@MainActivity.startService(intent)
//                        result.success(true)
//                    }
                }
                else -> result.notImplemented()
            }
        }
    }

//    override fun onDestroy() {
//        unbindService(this)
//        super.onDestroy()
//    }

    override fun onServiceDisconnected(name: ComponentName?) {
//        service = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//        val binder = service as KubeBinder
//        val kubeService = binder.service
//        this.service = kubeService

//        kubeService.delegate = this
//        if (kubeService.player?.isStop == false && kubeService.currentPlaylist != null) {
//            onStartPlay(kubeService.currentPlaylist!!)
//        }
    }
}

@GlideModule
class MyAppGlideModule : AppGlideModule() {
    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val calculator = MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(2f)
                .build()
        builder.setMemoryCache(LruResourceCache(calculator.memoryCacheSize.toLong()))
    }
}
