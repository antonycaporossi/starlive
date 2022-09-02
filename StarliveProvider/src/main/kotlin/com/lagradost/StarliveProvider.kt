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

    val flags = mapOf(
        "eng" to "\uD83C\uDFF4\uDB40\uDC67\uDB40\uDC62\uDB40\uDC65\uDB40\uDC6E\uDB40\uDC67\uDB40\uDC7F",
        "usa" to "\uD83C\uDDFA\uD83C\uDDF8",
        "ita" to "\uD83C\uDDEE\uD83C\uDDF9",
        "esp" to "\uD83C\uDDEA\uD83C\uDDF8",
        "ara" to "\uD83C\uDDF8\uD83C\uDDE6",
        "alb" to "\uD83C\uDDE6\uD83C\uDDF1",
        "pol" to "\uD83C\uDDF5\uD83C\uDDF1",
        "fra" to "\uD83C\uDDEB\uD83C\uDDF7",
        "por" to "\uD83C\uDDF5\uD83C\uDDF9",
        "ger" to "\uD83C\uDDE9\uD83C\uDDEA",
        "ned" to "\uD83C\uDDF3\uD83C\uDDF1",
        "tur" to "\uD83C\uDDF9\uD83C\uDDF7",
        "gre" to "\uD83C\uDDEC\uD83C\uDDF7",
        "arg" to "\uD83C\uDDE6\uD83C\uDDF7",
        "uru" to "\uD83C\uDDFA\uD83C\uDDFE",
        "rom" to "\uD83C\uDDF7\uD83C\uDDF4",
        "aus" to "\uD83C\uDDE6\uD83C\uDDFA",
        "swe" to "\uD83C\uDDF8\uD83C\uDDEA",
        "bul" to "\uD83C\uDDE7\uD83C\uDDEC",
        "can" to "\uD83C\uDDE8\uD83C\uDDE6",
        "bra" to "\uD83C\uDDE7\uD83C\uDDF7",
        "nzl" to "\uD83C\uDDF3\uD83C\uDDFF",
        "mex" to "\uD83C\uDDF2\uD83C\uDDFD",
        "nor" to "\uD83C\uDDF3\uD83C\uDDF4",
        "srb" to "\uD83C\uDDF7\uD83C\uDDF8",
        "bel" to "\uD83C\uDDE7\uD83C\uDDEA",
        "fin" to "\uD83C\uDDEB\uD83C\uDDEE"
    )
    
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(mainUrl).document
        val sections = document.select("div.panel-group .panel.panel-default")
        val sections_dates = document.select("div.panel-group .panel.panel-default")
       
        if (sections.isEmpty()) throw ErrorLoadingException()

        return HomePageResponse(sections.mapIndexed { idx, it ->
            val poster = fixUrl(it.selectFirst("h4")!!.attr("style").replace("background-image: url(", "").replace(");", ""))
            val categoryname = it.selectFirst("h4 a")!!.text()
            val shows = it.select("table tr").not("tr[class='']").not(".audio").filter{it -> it.select("a").isNotEmpty()}.map {
                val lang = flags[it!!.attr("class")].toString()
                val url = it.selectFirst("a")!!.attr("href")
                val evento = it.selectFirst("a")!!.text().split(" ", limit = 2)
                val channelName = it.selectFirst(".emd")!!.text()
                LiveSearchResponse(
                    lang + " " +evento.last() +" - Ore: "+ evento.first(),
                    LoadData(
                        fixUrl(url),
                        evento.last(),
                        poster,
                        channelName,
                        orario = evento.first(),
                        lang = lang
                    ).toJson(),
                    this@StarliveProvider.name,
                    TvType.Live,
                    poster
                )
            }
            HomePageList(
                idx.toString() + " - "+ categoryname,
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
            loadData.lang + " " +loadData.eventoName,
            loadData.url,
            this.name,
            loadData.url,
            plot = "Ore: "+loadData.orario+" - Canale: "+loadData.channelName,
            posterUrl = loadData.poster
        )
    }
    data class LoadData(
        val url: String,
        val eventoName: String,
        val poster: String = "",
        val channelName: String = "",
        val orario: String = "",
        val lang: String = ""
    )

    private suspend fun parseIframeUrl(url: String, ref_truelink: String): Pair<String, String>{

        return when{
            ref_truelink.contains("tutele.nl") -> {
                val newpage = app.get(ref_truelink, referer = url).document.toString()
                Pair(
                    Regex("""var src="(?:[^"]|"")*""").find( getAndUnpack(getPacked(newpage).toString()) )!!.value.replace("""var src="""", ""), 
                    ref_truelink
                )
            }
            ref_truelink.contains("pepperlive") -> {
                val document = app.get(ref_truelink, referer = mainUrl).document
                
                val truelink_inner = httpsify(document.selectFirst("iframe")!!.attr("src"))
                val newpage_inner = app.get(truelink_inner, referer = ref_truelink).document.toString()
                Pair(
                    Regex("""var src="(?:[^"]|"")*""").find( getAndUnpack(getPacked(newpage_inner).toString()) )!!.value.replace("""var src="""", ""),
                    truelink_inner
                )
            }
            ref_truelink.contains("castmax") -> {
                Pair(
                    "https://cdn.castmax.live/hls/" + ref_truelink.substringAfter("embed/").replace(".html", ".m3u8"), 
                    "https://castmax.live/"
                )
            }
            ref_truelink.contains("starlive.stream") -> {
                // this page activate with referer = starlive.xyz
                val document = app.get(ref_truelink, referer = mainUrl).document
                
                val truelink_inner = httpsify(document.selectFirst("iframe")!!.attr("src"))
                // this page activate with referer = starlive.stream
                val newpage_inner = app.get(truelink_inner, referer = "https://starlive.stream/").document.toString()
                Pair(
                    Regex("""var src="(?:[^"]|"")*""").find( getAndUnpack(getPacked(newpage_inner).toString()) )!!.value.replace("""var src="""", ""),
                    truelink_inner
                )
            }
            ref_truelink.contains("deliriousholistic.net") -> {
                val newpage = app.get(ref_truelink, referer = url).document.toString()
                Pair(
                    Regex("""var src="(?:[^"]|"")*""").find( getAndUnpack(getPacked(newpage).toString()) )!!.value.replace("""var src="""", ""),
                    ref_truelink
                )
            } else -> Pair(url, ref_truelink)
        }
    }

    private suspend fun extractVideoLinks(
        url: String,
        callback: (ExtractorLink) -> Unit
    ) {
        
        val document = app.get(url).document
        val referer = httpsify(document.selectFirst("iframe")!!.attr("src"))

        val (sourceStream, refererStream) = parseIframeUrl(url = url, ref_truelink = referer)
        callback(
            ExtractorLink(
                source = this.name,
                name = sourceStream,
                url = sourceStream,
                referer = refererStream,
                quality = 0,
                isM3u8 = true
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
}
