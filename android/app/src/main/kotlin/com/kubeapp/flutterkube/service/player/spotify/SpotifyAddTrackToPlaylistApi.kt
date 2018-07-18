package com.kubeapp.flutterkube.service.player.spotify

import android.support.v4.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase

class SpotifyAddTrackToPlaylistApi(
        private val auth: String,
        private val userId: String,
        private val playlistId: String,
        private val playableItems: List<SpotifyPlayableItem>) : OpenApiBase<String>() {

    override fun parse(result: ByteArray): String {
        return String(result)
    }

    override val url: String
        get() = "https://api.spotify.com/v1/users/$userId/playlists/$playlistId/tracks"
    override val httpMethod: ApiSpec.HttpMethod
        get() = ApiSpec.HttpMethod.POST
    override val headers: Map<String, String>
        get() = ArrayMap<String, String>().apply {
            this["Authorization"] = "Bearer $auth"
        }

    override val body: ByteArray
        get() {
            val bodyEntity = BodyEntity(playableItems.mapNotNull {
                it.playUri
            })
            val jsonBody = Gson().toJson(bodyEntity)
            return jsonBody.toByteArray()
        }

    private data class BodyEntity(
            @SerializedName("uris") val uris: List<String>,
            @SerializedName("position") val position: Int = 0
    )

}