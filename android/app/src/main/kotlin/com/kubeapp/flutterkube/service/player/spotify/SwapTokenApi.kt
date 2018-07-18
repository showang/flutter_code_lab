package com.kubeapp.flutterkube.service.player.spotify

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase

class SwapTokenApi(private val code: String, private val redirectUri: String) : OpenApiBase<SwapTokenApi.SwapTokenApiResult>() {

    override fun parse(result: ByteArray): SwapTokenApiResult {
        return Gson().fromJson(String(result), SwapTokenApiResult::class.java)
    }

    override val url: String = "https://api.kube-app.com/v1/spotify/swap_token"
    override val httpMethod: ApiSpec.HttpMethod = ApiSpec.HttpMethod.POST
    override val body: ByteArray
        get() = Gson().toJson(BodyEntity(code, redirectUri)).toByteArray()


    private data class BodyEntity(
            @SerializedName("code") val code: String,
            @SerializedName("redirect_uri") val redirectUri: String
    )

    data class SwapTokenApiResult(
            @SerializedName("access_token") val accessToken: String,
            @SerializedName("refresh_token") val refreshToken: String
    )
}