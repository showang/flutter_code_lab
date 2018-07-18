package com.kubeapp.flutterkube.service.player.youtube

import android.support.v4.util.ArrayMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kkbox.openapi.infrastructure.ApiSpec
import com.kkbox.openapi.infrastructure.implementation.OpenApiBase
import com.kkbox.openapi.model.ImageInfo
import com.kkbox.openapi.model.Track
import com.kube.app.service.player.youtube.YoutubePlayableItem

class KubeYoutubeTrackApi(private val track: Track) : OpenApiBase<YoutubePlayableItem>() {
    override fun parse(result: ByteArray): YoutubePlayableItem {
        return Gson().fromJson(String(result), RootEntity::class.java).parse(track)
    }

    override val url: String
        get() = "https://api.kube-app.com/v1/youtube/fetch"
    override val httpMethod: ApiSpec.HttpMethod
        get() = ApiSpec.HttpMethod.POST
    override val contentType: ApiSpec.ContentType
        get() = super.contentType
    override val headers: Map<String, String>
        get() = ArrayMap()
    override val parameters: Map<String, String>
        get() = ArrayMap()

    override val body: ByteArray
        get() {
            val entity = BodyEntity(
                    track.id,
                    "${track.name} ${track.album.artist.name.replace("Various Artists", "")}"
            )
            return Gson().toJson(entity).toByteArray()
        }

    private data class BodyEntity(
            @SerializedName("track_id") val trackId: String,
            @SerializedName("keyword") val keyWord: String
    )

    private data class RootEntity(
            @SerializedName("id") val id: String,
            @SerializedName("videos") val videos: List<ItemEntity>
    ) {
        fun parse(track: Track): YoutubePlayableItem {
            for (entity in videos) {
                if ("youtube#video".endsWith(entity.id.kind)) {
                    return YoutubePlayableItem(
                            track,
                            entity.id.videoId,
                            entity.snippet.title,
                            ImageInfo(ImageInfo.Type.SMALL, entity.snippet.thumbnails.medium.url)
                    )
                }
            }
            return YoutubePlayableItem(track)
        }
    }

    private data class ItemEntity(
            @SerializedName("id") val id: IdEntity,
            @SerializedName("snippet") val snippet: SnippetEntity
    )

    private data class IdEntity(
            @SerializedName("kind") val kind: String,
            @SerializedName("videoId") val videoId: String?,
            @SerializedName("playlistId") val playlistId: String?
    )

    private data class SnippetEntity(
            @SerializedName("title") val title: String,
            @SerializedName("thumbnails") val thumbnails: ThumbnailsEntity
    )

    private data class ThumbnailsEntity(
            @SerializedName("medium") val medium: ImageEntity
    )

    private data class ImageEntity(
            @SerializedName("url") val url: String,
            @SerializedName("width") val width: Int,
            @SerializedName("height") val height: Int
    )

}