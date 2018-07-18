package com.kubeapp.flutterkube.service.player.spotify

import android.support.v4.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase

class SpotifyCreatePlaylistApi(
        private val auth: String,
        private val userId: String,
        private val name: String,
        private val description: String) : OpenApiBase<String>() {

    override fun parse(result: ByteArray): String {
        return Gson().fromJson(String(result), RootEntity::class.java).id
    }

    override val url: String
        get() = "https://api.spotify.com/v1/users/$userId/playlists"
    override val httpMethod: ApiSpec.HttpMethod
        get() = ApiSpec.HttpMethod.POST
    override val headers: Map<String, String>
        get() = ArrayMap<String, String>().apply {
            this["Authorization"] = "Bearer $auth"
        }
    override val body: ByteArray
        get() = Gson().toJson(PostBody("[KKBOX] $name", description)).toByteArray()

    private data class PostBody(
            @SerializedName("name") val name: String,
            @SerializedName("description") val description: String,
            @SerializedName("public") val isPublic: Boolean = true
    )

    private data class RootEntity(
            @SerializedName("id") val id: String
    )

}