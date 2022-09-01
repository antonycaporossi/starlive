package com.lagradost

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson

class FullMatchTVProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var lang = "it"
    override var mainUrl = "https://fullmatchtv.com"
    override var name = "FullMatchTV"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(
        TvType.Live,

        )
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val sections = document.select(".tdb-category-loop-posts ")
       
        if (sections.isEmpty()) throw ErrorLoadingException()

        return HomePageResponse(sections.map { it ->
            
            val categoryname = "categoria test"
            val shows = it.select(".tdb_module_loop").map {

                val poster = fixUrl(it.selectFirst("span.entry-thumb")!!.attr("style").replace("background-image: url(", "").replace(");", ""))
                val url = it.selectFirst(".td-module-thumb a")!!.attr("href")
                val name = it.selectFirst(".td-module-meta-info h3")!!.text()
                LiveSearchResponse(
                    name,
                    url,
                    this.name,
                    TvType.Live,
                    poster
                )
            }
            HomePageList(
                categoryname,
                shows,
                isHorizontalImages = true
            )

        })
    }
    override suspend fun load(url: String): LoadResponse {

        val document = app.get(url).document
        val name = document.select("header.td-post-title h1")!!.text()
        
        val plot = document.selectFirst(".td-post-content p")!!.text()
        val poster = document.select("meta[property='og:image']")!!.attr("content")
        return newMovieLoadResponse(
            name,
            url,
            TvType.Movie,
            url
        ){
            posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query"

        val document = app.get(url).document
        return document.select(".td-ss-main-content .td_module_wrap").map { it ->
            val href = it.selectFirst("a")!!.attr("href")
            val poster = it.selectFirst("img")!!.attr("src")
            val name = it.selectFirst(".item-details h3")!!.text()
            MovieSearchResponse(
                name,
                href,
                this.name,
                TvType.Movie,
                poster
            )

        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select(".td-post-content iframe").forEach { iframe ->
            // NOT THE CLEANEST, BUT THE SMARTEST
            val link = httpsify(iframe.attr("src")).replace("sbspeed", "sbthe").replace("sbfast", "sbthe")
            loadExtractor(link, data, subtitleCallback, callback)
        }
        

        return true
    }
}
