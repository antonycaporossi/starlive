package com.lagradost

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson

class StarliveProvider : MainAPI() { // all providers must be an instance of MainAPI
    override var lang = "it"
    override var mainUrl = "https://starlive.xyz"
    override var name = "Starlive"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val supportedTypes = setOf(
        TvType.Live,

        )

    //val map = hashMapOf("eng" to 🏴󠁧󠁢󠁥󠁮󠁧󠁿, "usa" to 🇺🇸, "ita" to 🇮🇹)
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val sections = document.select("div.panel-group .panel.panel-default")
       
        if (sections.isEmpty()) throw ErrorLoadingException()

        return HomePageResponse(sections.map { it ->
            val poster = it.selectFirst("h4")!!.attr("style").replace("background-image: url(", "").replace(");", "")
            val categoryname = it.selectFirst("h4 a")!!.text()
            val shows = it.select("table tr").not("tr[class='']").not(".audio").map {
                val url = it.selectFirst("a")!!.attr("href")
                val evento = it.selectFirst("a")!!.text()
                val channelName = it.selectFirst(".emd")!!.text()
                LiveSearchResponse(
                    evento+" "+channelName,
                    LoadData(
                        fixUrl(url),
                        evento,
                        channelName
                    ).toJson(),
                    this@StarliveProvider.name,
                    TvType.Live,
                    mainUrl + poster
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
        val loadData = parseJson<LoadData>(url)

        return LiveStreamLoadResponse(
            loadData.eventoName,
            loadData.url,
            this.name,
            loadData.url,
            plot = loadData.url
        )
    }
    data class LoadData(
        val url: String,
        val eventoName: String,
        val channelName: String = "",
        val orario: String = ""
    )

    

    private suspend fun extractVideoLinks(
        url: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val document = app.get(url).document
        val truelink = document.selectFirst("iframe")!!.attr("src").replace("//", "https://")
        val newpage = app.get(truelink, referer = url).document.toString()
        // I HATE YOU, REGEX.
        val streamurl = Regex("""var src="(?:[^"]|"")*""").find( getAndUnpack(getPacked(newpage).toString()) )!!.value.replace("""var src="""", "")

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


/*    private suspend fun parseIframeUrl(url: String, referer: String): String{
        val newpage = app.get(url, referer = referer).document.toString()

        return when{
            url.contains("tutele.nl") -> {
                url
            }
            url.contains("smokelearned.net") -> {
                url
            }
            url.contains("deliriousholistic.net") -> {
                // I HATE YOU, REGEX.
                Regex("""var src="(?:[^"]|"")*""").find( getAndUnpack(getPacked(newpage).toString()) )!!.value.replace("""var src="""", "")
            } else -> url
        }
    } */