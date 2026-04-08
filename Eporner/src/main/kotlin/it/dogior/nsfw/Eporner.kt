package it.dogior.nsfw

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import org.json.JSONObject

open class Eporner : MainAPI() {
    override var mainUrl = "https://www.eporner.com/api/v2/video"
    override var name = "Eporner"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(TvType.NSFW)
    override val vpnStatus = VPNStatus.MightBeNeeded
//    override var sequentialMainPage = true
//    override var sequentialMainPageDelay = 200L
//    override var sequentialMainPageScrollDelay = 200L


    override val mainPage = mainPageOf(
        "search/?order=latest" to "Latest",
        "search/?order=top-rated" to "Top Rated",
        "search/?order=top-weekly" to "Most Viewed - Weekly",
        "search/?order=top-monthly" to "Most Viewed - Top Monthly",
        "search/?order=most-popular" to "Most Popular",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val response = app.get("$mainUrl/${request.data}&page=$page/").text
        val results = parseJson<SearchResult>(response)
        val section = results.videos.map { videoToSearchRsponse(it) }
        val hasNext = page < results.totalPages
        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = section,
                isHorizontalImages = true
            ),
            hasNext = hasNext
        )
    }

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val response = app.get("$mainUrl/?query=$query&page=$page/").text
        val results = parseJson<SearchResult>(response)
        val section = results.videos.map { videoToSearchRsponse(it) }
        val hasNext = page < results.totalPages
        return newSearchResponseList(section, hasNext)
    }

    override suspend fun load(url: String): LoadResponse? {
        val videoId = if (url.contains("hd-porn/")) {
            url.substringAfter("hd-porn/").substringBefore("/")
        } else {
            url.substringAfter("/video-").substringBefore("/")
        }
        val response =
            app.get("https://www.eporner.com/api/v2/video/id/?id=$videoId&thumbsize=big").text
        val video = tryParseJson<Video>(response)
        if (video == null) return null

        val title = video.title
        val poster = video.defaultThumb.src

        val durationParts = try {
            video.lengthMin.split(":").reversed().map { it.toInt() }
        } catch (_: NumberFormatException) {
            null
        }
        val duration = if (durationParts == null) null else {
            var dur = 0
            for (i in 0..durationParts.size-1) {
                dur = durationParts[i] * (i * 60)
            }
            dur / 60
        }
        val description = "Added ${video.added}"

        val tags = video.keywords.split(",")

        /*val relatedDiv = document.select("#relateddiv")
        val relatedVideos = relatedDiv.select(".mb").map {
            val a = it.select(".mbcontent > a")
            val img = a.select("img")
            val relatedPoster = img.attr("data-src")
            val relatedTitle = img.attr("alt")
            val relatedLink = a.attr("href")
            newMovieSearchResponse(relatedTitle, relatedLink) {
                this.posterUrl = relatedPoster
            }
        }*/


        return newMovieLoadResponse(title, url, TvType.NSFW, video.url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
//            this.recommendations = relatedVideos
            this.duration = duration
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(
            data, interceptor = WebViewResolver(Regex("""https://www\.eporner\.com/xhr/video"""))
        )
        val json = response.text
        Log.d("BANANA", "$data\n$json")


        val jsonObject = JSONObject(json)
        val sources = jsonObject.getJSONObject("sources")
        val mp4Sources = sources.getJSONObject("mp4")
        val qualities = mp4Sources.keys()
        while (qualities.hasNext()) {
            val quality = qualities.next() as String
            val sourceObject = mp4Sources.getJSONObject(quality)
            val src = sourceObject.getString("src")
            val labelShort = sourceObject.getString("labelShort") ?: ""
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = src,
                    type = INFER_TYPE
                ) {
                    this.referer = ""
                    this.quality = getIndexQuality(labelShort)
                }
            )
        }
        return true
    }

    private fun getIndexQuality(str: String?): Int {
        return Regex("(\\d{3,4})[pP]").find(str ?: "")?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: Qualities.Unknown.value
    }

    private fun videoToSearchRsponse(video: Video): MovieSearchResponse {
        return newMovieSearchResponse(name = video.title, url = video.url, TvType.NSFW) {
            this.posterUrl = video.defaultThumb.src
        }
    }

    data class SearchResult(
        @JsonProperty("count")
        val count: Int,
        @JsonProperty("page")
        val page: Int,
        @JsonProperty("per_page")
        val perPage: Int,
        @JsonProperty("start")
        val start: Int,
        @JsonProperty("time_ms")
        val timeMs: Int,
        @JsonProperty("total_count")
        val totalCount: Int,
        @JsonProperty("total_pages")
        val totalPages: Int,
        @JsonProperty("videos")
        val videos: List<Video>
    )

    data class Video(
        @JsonProperty("added")
        val added: String,
        @JsonProperty("default_thumb")
        val defaultThumb: Thumb,
        @JsonProperty("embed")
        val embed: String,
        @JsonProperty("id")
        val id: String,
        @JsonProperty("keywords")
        val keywords: String,
        @JsonProperty("length_min")
        val lengthMin: String,
        @JsonProperty("length_sec")
        val lengthSec: Int,
        @JsonProperty("rate")
        val rate: String,
        @JsonProperty("thumbs")
        val thumbs: List<Thumb>,
        @JsonProperty("title")
        val title: String,
        @JsonProperty("url")
        val url: String,
        @JsonProperty("views")
        val views: Int
    )

    data class Thumb(
        @JsonProperty("height")
        val height: Int,
        @JsonProperty("size")
        val size: String,
        @JsonProperty("src")
        val src: String,
        @JsonProperty("width")
        val width: Int
    )
}
