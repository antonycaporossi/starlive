package com.lagradost

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

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
                LiveSearchResponse(
                    name,
                    href,
                    this@StarliveProvider.name,
                    TvType.Live
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
        // Questa è la pagina con l'iframe di starlive
        // Nell'app qui siamo nella pagina dettaglio
        val document = app.get(url).document
        val truelink = document.selectFirst("iframe")!!.attr("src").replace("//", "https://")
        val newpage = app.get(truelink).document
        return LiveStreamLoadResponse(
            "test",
            url,
            this.name,
            url,
            plot = newpage.select("script")[6].childNode(0).toString()
        )


    }
    private suspend fun extractVideoLinks(
        url: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document
        val truelink = document.selectFirst("iframe")!!.attr("src").replace("//", "https://")
        //val link1 = button.attr("data-link")
        //val doc2 = app.get(link1).document
        //val truelink = doc2.selectFirst("iframe")!!.attr("src")
        val newpage = app.get(truelink).document
        val streamurl = Regex(""""((.|\n)*?).";""").find(
            getAndUnpack(
                newpage.select("script")[6].childNode(0).toString()
            ))!!.value.replace("""src="""", "").replace(""""""", "").replace(";", "")

        callback(
            ExtractorLink(
                this.name,
                "test2",
                streamurl,
                truelink,
                quality = 0,
                true
            )
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        extractVideoLinks(data, callback)

        return true
    }



    // this function gets called when you search for something
    override suspend fun search(query: String): List<SearchResponse> {
        return listOf<SearchResponse>()
    }
}