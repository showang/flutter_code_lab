package com.kubeapp.flutterkube.service.player.spotify

import android.support.v4.util.ArrayMap
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase

class SpotifyUpdateCoverImageApi(
        private val auth: String,
        private val userId: String,
        private val playlistId: String,
        private val base64Image: ByteArray) : OpenApiBase<Boolean>() {
    override fun parse(result: ByteArray): Boolean {
        return true
    }

    override val url: String
        get() = "https://api.spotify.com/v1/users/$userId/playlists/$playlistId/images"
    override val httpMethod: ApiSpec.HttpMethod
        get() = ApiSpec.HttpMethod.PUT
    override val headers: Map<String, String>
        get() = ArrayMap<String, String>().apply {
            this["Authorization"] = "Bearer $auth"
        }
    override val contentType: ApiSpec.ContentType
        get() = ApiSpec.ContentType.IMAGE_JPEG

    override val body: ByteArray
        get() = base64Image
}