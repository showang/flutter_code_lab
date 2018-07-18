package com.kubeapp.flutterkube.service.player.spotify

import android.support.v4.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase

class RefreshTokenApi(private val oldToken: String) : OpenApiBase<String>() {

    override fun parse(result: ByteArray): String {
        return Gson().fromJson(String(result), ResultEntity::class.java).accessToken
    }

    override val url: String
        get() = "https://api.kube-app.com/v1/spotify/refresh_token"
    override val httpMethod: ApiSpec.HttpMethod
        get() = ApiSpec.HttpMethod.POST
    override val headers: Map<String, String>
        get() = ArrayMap<String, String>()
    override val parameters: Map<String, String>
        get() = ArrayMap<String, String>()

    override val body: ByteArray
        get() = "{\"refresh_token\":\"$oldToken\"}".toByteArray()

    private data class ResultEntity(
            @SerializedName("access_token") val accessToken: String
    )
}