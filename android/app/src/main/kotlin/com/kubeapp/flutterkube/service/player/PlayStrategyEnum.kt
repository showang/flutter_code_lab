package com.kubeapp.flutterkube.service.player

import com.kkbox.openapi.model.Playlist
import com.kkbox.openapi.model.Track

enum class PlayStrategyEnum {

    SEQUENCE {
        override fun sort(startTrackIndex: Int) {
            val startPart = playSequence.subList(startTrackIndex, playSequence.size)
            val endPart = playSequence.subList(0, startTrackIndex)
            startPart.addAll(endPart)
            playSequence = startPart
        }

        override fun tracksOf(playlist: Playlist, startIndex: Int): List<Track> {
            val sortedTracks = playlist.tracks.subList(startIndex, playlist.tracks.size).toMutableList()

            if (startIndex > 0) {
                sortedTracks.addAll(playlist.tracks.subList(0, startIndex))
            }
            return sortedTracks
        }
    },
    RANDOM {
        override fun sort(startTrackIndex: Int) {
            playSequence.removeAt(startTrackIndex)
            playSequence.shuffle()
            playSequence.add(0, startTrackIndex)
        }

        override fun tracksOf(playlist: Playlist, startIndex: Int): List<Track> {
            val sortedTracks = playlist.tracks.toMutableList()
            val startTrack = sortedTracks[startIndex]
            sortedTracks.removeAt(startIndex)
            sortedTracks.shuffle()
            sortedTracks.add(0, startTrack)
            return sortedTracks
        }
    };


    fun initTracksOf(playlist: Playlist, startTrackIndex: Int = 0): List<Track> {
        playSequence = IntArray(playlist.tracks.size) { i -> i }.toMutableList()
        sort(startTrackIndex)
        return tracksOf(playlist, startTrackIndex)
    }

    protected abstract fun tracksOf(playlist: Playlist, startIndex: Int = 0): List<Track>

    abstract fun sort(startTrackIndex: Int)

    fun nextTrackIndex(): Int {
        return playSequence[++playingIndex]
    }

    fun preTrackIndex(): Int {
        return playSequence[--playingIndex]
    }

    private var playingIndex = 0

    val currentTrackIndex: Int
        get() = playSequence[playingIndex]

//    private var trackList = ArrayList<Track>()

    protected var playSequence = ArrayList<Int>() as MutableList<Int>
}