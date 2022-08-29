package com.lagradost

import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse

class StarliveProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var lang = "it"
    override var mainUrl = "https://starlive.xyz/"
    override var name = "Starlive"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(
        TvType.Live,

        )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val sections = document.select("div.panel-group .panel.panel-default")
        
        if (sections.isEmpty()) throw ErrorLoadingException()

        return HomePageResponse(sections.map { it ->
            val categoryname = it.selectFirst("h4 a")!!.text()
            val shows = it.select("tr.ita").map {
                val href = it.selectFirst("a")!!.attr("href")
                val name = it.selectFirst("a")!!.text()
                val posterurl = fixUrl(it.selectFirst("a > img")!!.attr("src"))
                LiveSearchResponse(
                    name,
                    href,
                    this@StarliveProvider.name,
                    TvType.Live,
                    posterurl,
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
        return LiveStreamLoadResponse(
            "test",
            url,
            this.name,
            url
        )


    }



    // this function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf<SearchResponse>()
    }
}