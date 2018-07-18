package com.kubeapp.flutterkube.service

import com.kkbox.openapi.api.TracksApi
import com.kkbox.openapi.model.Paging
import com.kkbox.openapi.model.Playlist
import com.kkbox.openapi.model.PlaylistInfo
import com.kkbox.openapi.model.Track

class PlaylistController {

    companion object {

        fun fetchAllTracks(playlistInfo: PlaylistInfo, tracks: ArrayList<Track> = arrayListOf(), paging: Paging? = null, errorCallback: (error: Error?) -> Unit = {}, callback: (playlist: Playlist) -> Unit) {
            val p = paging ?: Paging(true, 0)
            if (p.hasNextPage) {
                requestTrack(playlistInfo.id, p.offset, tracks) {
                    if (it != null) {
                        errorCallback(it)
                        return@requestTrack
                    }
                    callback(Playlist(playlistInfo, tracks))
                }
            } else {
                callback(Playlist(playlistInfo, tracks))
            }
        }

        private fun requestTrack(playlistId: String, offset: Int, tracks: MutableList<Track>, completeHandler: (e: Error?) -> Unit) {
            TracksApi(playlistId, offset).startRequest({
                completeHandler(it)
            }, {
                tracks.addAll(it.tracks)
                if (it.paging.hasNextPage) {
                    requestTrack(playlistId, it.paging.offset, tracks, completeHandler)
                } else {
                    completeHandler(null)
                }
            })

        }

    }

}