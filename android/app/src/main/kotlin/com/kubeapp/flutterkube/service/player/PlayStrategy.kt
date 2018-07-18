package com.kubeapp.flutterkube.service.player

import java.util.*
import kotlin.collections.ArrayList

class PlayStrategy(
        s: Sequence = Sequence.DEFAULT
) {

    private var playingIndex = 0
    private var playingTrackIndexList = ArrayList<Int>() as MutableList<Int>
    private val playingIndexCounter = ArrayList<Int>()

    var currentTrackIndex: Int
        get() = if (playingTrackIndexList.size > playingIndex) playingTrackIndexList[playingIndex] else 0
        set(value) {
            playingIndex = playingTrackIndexList.indexOf(value)
        }

    fun performNext(): Int {
        return processTrackIndex(playingIndex + 1)
    }

    fun performPrevious(): Int {
        return processTrackIndex(playingIndex - 1)
    }

    private fun processTrackIndex(newIndex: Int): Int {
        playingIndexCounter.remove(newIndex)
        if (playingIndexCounter.size == 0) {
            init(playingTrackIndexList.size, playingTrackIndexList[newIndex])
            return playingTrackIndexList[currentTrackIndex]
        }
        return playingTrackIndexList[checkIndexChanged(newIndex)]
    }

    val playingTrackIndexSequence: List<Int>
        get() = playingTrackIndexList
    var sequence: Sequence = s
        set(value) {
            field = value
            if (playingTrackIndexList.size > 0) {
                init(playingTrackIndexList.size, playingTrackIndexList[playingIndex])
            }
        }

    fun init(trackSize: Int, startIndex: Int? = null) {
        playingIndex = 0
        playingTrackIndexList = sequence.sort(IntArray(trackSize) { i -> i }.toMutableList(), startIndex)
        playingIndexCounter.clear()
        playingIndexCounter.addAll(playingTrackIndexList)
        playingIndexCounter.remove(playingTrackIndexList[playingIndex])
    }

    private fun checkIndexChanged(index: Int): Int {
        playingIndex = when {
            index >= playingTrackIndexList.size -> 0
            index < 0 -> playingTrackIndexList.size - 1
            else -> index
        }
        return playingIndex
    }

    enum class Sequence {
        DEFAULT {
            override fun sort(sequenceList: MutableList<Int>, startIndex: Int?): MutableList<Int> {
                val start = startIndex ?: 0
                val result = ArrayList<Int>()
                val startPart = sequenceList.subList(start, sequenceList.size)
                val endPart = sequenceList.subList(0, start)
                result.addAll(startPart)
                result.addAll(endPart)
                return result
            }
        },
        RANDOM {
            override fun sort(sequenceList: MutableList<Int>, startIndex: Int?): MutableList<Int> {
                val start = startIndex ?: Random().nextInt(sequenceList.size)
                val startTrackIndex = sequenceList[start]
                sequenceList.removeAt(start)
                sequenceList.shuffle()
                sequenceList.add(0, startTrackIndex)
                return sequenceList
            }
        };

        abstract fun sort(sequenceList: MutableList<Int>, startIndex: Int? = null): MutableList<Int>
    }
}
