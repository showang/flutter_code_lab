package com.kubeapp.flutterkube.service.player.spotify

import android.support.v4.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase
import com.kkbox.openapi.model.Track
import com.kkbox.openapi.tools.Logger

class SpotifySearchApi(private val track: Track, private val auth: String) : OpenApiBase<SpotifyPlayableItem>() {

    override fun parse(result: ByteArray): SpotifyPlayableItem {
        val json = Gson().fromJson(String(result), RootEntity::class.java)
        val trackArray = json.tracks.items
        return if (trackArray.isNotEmpty()) SpotifyPlayableItem(track, trackArray[0].uri, trackArray[0].duration)
        else SpotifyPlayableItem(track)
    }

    override val url: String
        get() = "https://api.spotify.com/v1/search"
    override val httpMethod: ApiSpec.HttpMethod
        get() = ApiSpec.HttpMethod.GET

    override val headers: Map<String, String>
        get() = ArrayMap<String, String>().apply {
            this["Authorization"] = "Bearer $auth"
        }

    override val parameters: Map<String, String>
        get() = ArrayMap<String, String>().apply {
            val parenthesesRegex = Regex("\\(.+?\\)")
            val chineseParenthesesRegex = Regex("（.+?）")
            val trackKeywords = track.name
                    .replace("feat.", "")
                    .replace(parenthesesRegex, "")
                    .replace(chineseParenthesesRegex, "")
            val artistKeywords = track.album.artist.name
                    .replace("Various Artists", "")
                    .replace("(", "")
                    .replace(")", "")
                    .replace("&", " ")
                    .replace("with", "")
            this["q"] = "$trackKeywords $artistKeywords"
            this["type"] = "track"
            this["market"] = "from_token"
            Logger.d(this@SpotifySearchApi.javaClass.simpleName, "keyword: ${this["q"]}")
        }

    private data class RootEntity(
            @SerializedName("tracks") val tracks: TracksEntity
    )

    private data class TracksEntity(
            @SerializedName("items") val items: List<ItemEntity>
    )

    private data class ItemEntity(
            @SerializedName("uri") val uri: String,
            @SerializedName("duration_ms") val duration: Long
    )

}