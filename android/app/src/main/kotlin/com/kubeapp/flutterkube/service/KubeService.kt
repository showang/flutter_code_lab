package com.kubeapp.flutterkube.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.*
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.preference.PreferenceManager
import android.telephony.TelephonyManager
import android.util.Log
import com.kkbox.openapi.model.Playlist
import com.kkbox.openapi.tools.Logger
import com.kubeapp.flutterkube.service.player.KubePlayerBehavior
import com.kubeapp.flutterkube.service.player.PlayStrategy
import com.kubeapp.flutterkube.service.player.spotify.KubeSpotifyPlayer
import com.kube.app.service.player.youtube.YoutubePlayer
import com.kubeapp.flutterkube.R
import com.kubeapp.flutterkube.service.Settings.Companion.PREFERENCE_PLAYER
import com.kubeapp.flutterkube.service.Settings.Player.SPOTIFY
import com.kubeapp.flutterkube.service.Settings.Player.YOUTUBE

class KubeService : Service() {

    companion object {
        const val SP_CONFIG_PLAY_STRATEGY = "SP_CONFIG_PLAY_STRATEGY"

        const val EXTRA_PLAYLIST = "0"
        const val EXTRA_START_INDEX = "1"
        const val EXTRA_STRING_PLAYER_ERROR_MESSAGE = "3"
        const val EXTRA_BOOL_ERROR_PLAYER_RESET = "4"
        const val EXTRA_STRING_CHANGE_PLAYER_NAME = "5"
        const val EXTRA_STRING_PLAY_SEQUENCE_NAME = "6"

        const val ACTION_START_PLAYLIST = "0"
        const val ACTION_PLAY = "1"
        const val ACTION_PLAY_AT = "2"
        const val ACTION_PAUSE = "3"
        const val ACTION_STOP = "4"
        const val ACTION_NEXT = "5"
        const val ACTION_PREVIOUS = "6"
        const val ACTION_EXPORT_PLAYLIST = "7"
        const val ACTION_CHANGE_PLAY_SEQUENCE = "8"

        const val CONFIG_CHANGE_PLAYER = "101"

        const val ERROR_PLAYER_SERVICE = "102"
    }

    var delegate: KubeServiceDelegate? = null

    var currentPlaylist: Playlist? = null

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var kubePlayerBehavior: KubePlayerBehavior? = null

    private val bluetoothReceiver = ReceiverBluetooth(this)

    private val binder: KubeBinder = KubeBinder(this)

    private var audioFocusRequest: AudioFocusRequest? = null
    private lateinit var audioAttributes: AudioAttributes
    private lateinit var audioFocusCallback: (Int) -> Unit
    private var userPaused = false
    private lateinit var playStrategy: PlayStrategy

    var player: KubePlayer? = null
    private lateinit var notificationController: NotificationController

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        Logger.d(javaClass.simpleName, "onCreate")
//        kubePlayerBehavior = KubePlayerTracker(KubeApplication.firebaseAnalytics)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        initPlayStrategy()
        initMediaSession()
        initNotificationController(mediaSession)
        initKubePlayer()
        initBluetoothConfigs()
        initAudioManager()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        if (intent == null) return START_STICKY
        when (intent.action) {
            ACTION_START_PLAYLIST -> prepareToStart(intent)
            ACTION_PLAY -> play()
            ACTION_PLAY_AT -> playAtCurrentPlaylist(intent.getIntExtra(EXTRA_START_INDEX, 0))
            ACTION_PAUSE -> pause()
            ACTION_STOP -> stop()
            ACTION_NEXT -> next()
            ACTION_PREVIOUS -> previous()
            ACTION_EXPORT_PLAYLIST -> exportPlaylistToMusicService(intent)
            ACTION_CHANGE_PLAY_SEQUENCE -> updatePlaySequence(intent)
            CONFIG_CHANGE_PLAYER -> changePlayer(intent.getStringExtra(EXTRA_STRING_CHANGE_PLAYER_NAME))
            ERROR_PLAYER_SERVICE -> onPlayerServiceEvent(
                    intent.getStringExtra(EXTRA_STRING_PLAYER_ERROR_MESSAGE),
                    intent.getBooleanExtra(EXTRA_BOOL_ERROR_PLAYER_RESET, false)
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Logger.e(javaClass.simpleName, "onDestroy")
        this.unregisterReceiver(bluetoothReceiver)
        stop()
        mediaSession.release()
        notificationController.cancel()
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun prepareToStart(intent: Intent) {
        Log.e(javaClass.simpleName, "prepareToStart")
        val playlist = intent.getSerializableExtra(EXTRA_PLAYLIST) as Playlist
        var startIndex: Int? = intent.getIntExtra(EXTRA_START_INDEX, -1)
        if (startIndex == -1) {
            startIndex = null
        }
        if (playlist.tracks.isEmpty()) return
        stop(true)
        if (playlist.info.id == currentPlaylist?.info?.id) {
            playAtCurrentPlaylist(startIndex ?: 0)
        } else {
            startPlay(playlist, startIndex)
        }
    }

    fun onPlayerServiceEvent(message: String, resetPlayer: Boolean) {
        stop(false)
        delegate?.onPlayerServiceError(message)
        val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        if (resetPlayer) {
            preference.edit().putString(PREFERENCE_PLAYER, YOUTUBE).apply()
            player?.release()
            player = null
            initKubePlayer(YOUTUBE)
        }
    }

    private fun exportPlaylistToMusicService(intent: Intent) {
        val playlist = intent.getSerializableExtra(EXTRA_PLAYLIST) as? Playlist
        if (playlist == null) {
            onExportFinished(playlist, false)
            return
        }
        notificationController.buildExporting(playlist.info)
        player?.export(playlist)
    }

    private fun updatePlaySequence(intent: Intent) {
        val playSequenceConfig = intent.getStringExtra(EXTRA_STRING_PLAY_SEQUENCE_NAME)
                ?: PlayStrategy.Sequence.DEFAULT.name
        playStrategy.sequence = when (playSequenceConfig) {
            PlayStrategy.Sequence.RANDOM.name -> PlayStrategy.Sequence.RANDOM
            else -> PlayStrategy.Sequence.DEFAULT
        }
        sharedPreferences.edit().putString(SP_CONFIG_PLAY_STRATEGY, playSequenceConfig).apply()
    }

    fun onExportFinished(playlist: Playlist?, success: Boolean, uri: String? = null) {
        notificationController.onExportFinished(playlist, success, uri)
    }

    private fun changePlayer(playerName: String?) {
        val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val playerType = playerName ?: preference.getString(PREFERENCE_PLAYER, YOUTUBE)
        stop(false)
        player?.release()
        player = null
        Handler().postDelayed({
            initKubePlayer(playerType)
        }, 300)
    }

    private fun startPlay(playlist: Playlist, index: Int?) {
        currentPlaylist = playlist

        delegate?.onStartPlay(playlist)
        requestAudioFocus()
        player?.startPlay(playlist, index)
    }

    private fun initPlayStrategy() {
        val strategyConfig = sharedPreferences.getString(SP_CONFIG_PLAY_STRATEGY, PlayStrategy.Sequence.DEFAULT.name)
        playStrategy = when (strategyConfig) {
            PlayStrategy.Sequence.RANDOM.name -> PlayStrategy(PlayStrategy.Sequence.RANDOM)
            else -> PlayStrategy()
        }
    }

    private fun initNotificationController(mediaSession: MediaSessionCompat) {
        notificationController = NotificationController(this, mediaSession)
    }

    private fun initKubePlayer(playerType: String? = null) {
        kubePlayerBehavior?.onInitPlayer(player?.musicServiceName, when (playerType) {
            SPOTIFY -> getString(R.string.player_name_spotify)
            YOUTUBE -> getString(R.string.player_name_youtube)
            else -> getString(R.string.player_name_youtube)
        })
        val playerName = playerType
                ?: PreferenceManager.getDefaultSharedPreferences(applicationContext).getString(Settings.PREFERENCE_PLAYER, YOUTUBE)
        player = when (playerName) {
            SPOTIFY -> KubeSpotifyPlayer(this, mediaSession, notificationController, playStrategy)
            else -> YoutubePlayer(this, mediaSession, notificationController, playStrategy)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, packageName)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setPlaybackState(
                with(PlaybackStateCompat.Builder()) {
                    setActions(PlaybackStateCompat.ACTION_PLAY
                            or PlaybackStateCompat.ACTION_PAUSE
                            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                    build()
                }
        )

        mediaSession.setCallback(object : MediaSessionCompat.Callback() {

            override fun onPlay() {
                play()
            }

            override fun onPause() {
                pause()
            }

            override fun onSkipToNext() {
                next()
            }

            override fun onSkipToPrevious() {
                previous()
            }

            override fun onStop() {
                super.onStop()
                stop(false)
            }
        })
        mediaSession.isActive = true
    }

    private fun initBluetoothConfigs() {
        val bluetoothFilter = IntentFilter()
        bluetoothFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        this.registerReceiver(bluetoothReceiver, bluetoothFilter)
    }

    private fun initAudioManager() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            audioAttributes = with(AudioAttributes.Builder()) {
                setUsage(AudioAttributes.USAGE_MEDIA)
                setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                build()
            }
        }
        audioFocusCallback = {
            when (it) {
                AUDIOFOCUS_LOSS,
                AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        pause()
                    } else {
                        if (!userPaused) {
                            pause()
                        }
                        userPaused = false
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                audioFocusRequest = with(AudioFocusRequest.Builder(AUDIOFOCUS_GAIN)) {
                    setAudioAttributes(audioAttributes)
                    setOnAudioFocusChangeListener(audioFocusCallback)
                    build()
                }
            }
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(audioFocusCallback, STREAM_MUSIC, AUDIOFOCUS_GAIN)
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
                audioFocusRequest = null
            }
        } else {
            audioManager.abandonAudioFocus(audioFocusCallback)
        }
    }

    private fun play() {
        if (currentPlaylist == null) return
        requestAudioFocus()
        player?.play()
    }

    private fun playAtCurrentPlaylist(index: Int) {
        player?.playAt(index)
    }

    private fun pause() {
        userPaused = true
        player?.pause()
        abandonAudioFocusRequest()
    }

    fun stop(continuePlay: Boolean = false) {
        mediaSession.isActive = false
        player?.stop()

        if (!continuePlay) {
            delegate?.onStopPlay(continuePlay)
            abandonAudioFocusRequest()
        }
//        currentTrack = null
        currentPlaylist = null
        stopForeground(true)
    }

    private fun next() {
        player?.next()
    }

    private fun previous() {
        player?.previous()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

}

interface KubeServiceDelegate {
    fun onStartPlay(playlist: Playlist)

    fun onStopPlay(continuePlay: Boolean)

    fun onNetworkError()

    fun onPlayerServiceError(message: String)
}

class KubeBinder(val service: KubeService) : Binder()

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.getStringExtra(TelephonyManager.EXTRA_STATE) == TelephonyManager.EXTRA_STATE_RINGING) {
            val serviceIntent = Intent(context, KubeService::class.java)
            serviceIntent.action = KubeService.ACTION_PAUSE
            context?.startService(serviceIntent)
        }
    }

}