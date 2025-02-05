package com.zionhuang.innertube

import com.zionhuang.innertube.models.Artist
import com.zionhuang.innertube.models.PlaylistItem
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.YouTubeClient
import com.zionhuang.innertube.models.response.PlayerResponse
import com.zionhuang.innertube.pages.PlaylistPage
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.io.IOException
import java.net.Proxy

private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)
            .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        headers.forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body?.string()

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }

}

object NewPipeUtils {

    init {
        NewPipe.init(NewPipeDownloaderImpl(YouTube.proxy))
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    fun getStreamUrl(format: PlayerResponse.StreamingData.Format, videoId: String): Result<String> =
        runCatching {
            val url = format.url ?: format.signatureCipher?.let { signatureCipher ->
                val params = parseQueryString(signatureCipher)
                val obfuscatedSignature = params["s"]
                    ?: throw ParsingException("Could not parse cipher signature")
                val signatureParam = params["sp"]
                    ?: throw ParsingException("Could not parse cipher signature parameter")
                val url = params["url"]?.let { URLBuilder(it) }
                    ?: throw ParsingException("Could not parse cipher url")
                url.parameters[signatureParam] =
                    YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                        videoId,
                        obfuscatedSignature
                    )
                url.toString()
            } ?: throw ParsingException("Could not find format url")

            return@runCatching YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                videoId,
                url
            )
        }


//    public static final YoutubeService YouTube = new YoutubeService(0);
//    public static final SoundcloudService SoundCloud = new SoundcloudService(1);
//    public static final MediaCCCService MediaCCC = new MediaCCCService(2);
//    public static final PeertubeService PeerTube = new PeertubeService(3);
//    public static final BandcampService Bandcamp = new BandcampService(4);


    /**
     * Get playlist and it's first page of songs. Use getMorePlaylistItems() to get more songs
     */
    fun getPlaylistInfo(playlistId: String): Result<PlaylistPage> = runCatching {
        val url = "https://www.youtube.com/playlist?list=$playlistId"
        val info = PlaylistInfo.getInfo(NewPipe.getService(0), url)

        return@runCatching PlaylistPage(
            playlist = PlaylistItem(
                id = info.id.toString(),
                title = info.name,
                author = Artist(
                    name = info.uploaderName,
                    id = info.uploaderUrl
                ),
                songCountText = info.streamCount.toString(),
                thumbnail = info.thumbnails.firstOrNull()?.url,
                playEndpoint = null,
                shuffleEndpoint = null,
                radioEndpoint = null,
                isEditable = false
            ),
            songs = info.relatedItems.map { item ->
                SongItem(
                    id = item.url.substringAfterLast("?v="),
                    title = item.name,
                    artists = listOf(
                        Artist(
                            name = item.uploaderName,
                            id = item.uploaderUrl,
                        )
                    ),
                    thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                )
            },
            songsContinuation = null,
            continuation = null,
            nextPage = if (info.hasNextPage()) info.nextPage else null
        )
    }


    /**
     * Get more songs from the playlist
     */
    fun getMorePlaylistItems(playlistId: String, currentNextPage: Page): Result<Pair<List<SongItem>, Page?>> =
        runCatching {
            val url = "https://www.youtube.com/playlist?list=$playlistId"
            val result = PlaylistInfo.getMoreItems(NewPipe.getService(0), url, currentNextPage)

            val songs = result.items.map { item ->
                item.thumbnails
                SongItem(
                    id = item.url.substringAfterLast("?v="),
                    title = item.name,
                    artists = listOf(
                        Artist(
                            name = item.uploaderName,
                            id = item.uploaderUrl,
                        )
                    ),
                    thumbnail = item.thumbnails.firstOrNull()?.url ?: ""
                )
            }
            return@runCatching Pair(songs, if (result.hasNextPage()) result.nextPage else null)
        }

}