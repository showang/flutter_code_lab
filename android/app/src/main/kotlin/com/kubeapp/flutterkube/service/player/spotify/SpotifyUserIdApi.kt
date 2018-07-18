package com.kubeapp.flutterkube.service.player.spotify

import android.support.v4.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase

class SpotifyUserIdApi(private val auth: String) : OpenApiBase<String>() {

    override fun parse(result: ByteArray): String {
        return Gson().fromJson(String(result), RootEntity::class.java).id
    }

    override val url: String
        get() = "https://api.spotify.com/v1/me"
    override val httpMethod: ApiSpec.HttpMethod
        get() = ApiSpec.HttpMethod.GET
    override val headers: Map<String, String>
        get() = ArrayMap<String, String>().apply {
            this["Authorization"] = "Bearer $auth"
        }

    private data class RootEntity(
            @SerializedName("id") val id: String
    )
}