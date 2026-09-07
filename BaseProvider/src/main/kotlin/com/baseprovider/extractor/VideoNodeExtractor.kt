package com.baseprovider.extractor

import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.extractors.*
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.*

class VideoNodePage : ExtractorApi() {
    override var name = "VideoNodePage"
    override var mainUrl = "https://videonode.de"
    override val requiresReferer = true

    private val resolver by lazy {
        WebViewResolver(
            interceptUrl = Regex("(m3u8|mp4|master\\.txt)"),
            additionalUrls = listOf(
                Regex("(m3u8|mp4|master\\.txt)")
            ),
            useOkhttp = false,
            timeout = 20_000L
        )
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val doc = app.get(url, referer = referer).document

            doc.select("iframe").forEach { iframe ->
                val src = iframe.attr("src")

                if (src.isNotBlank()) {
                    loadExtractorWithFallbackCustom(
                        src,
                        url,
                        subtitleCallback,
                        callback = callback,
                        providerTag = "VideoNodePage",
                        callChain = "VideoNodePage"
                    )
                }
            }
        } catch (e: Exception) {
            Log.d("VideoNodePage", "Direct request failed: ${e.message}")
        }

        try {
            val interceptedUrl = app.get(
                url,
                referer = referer,
                interceptor = resolver
            ).url

            if (interceptedUrl.isNotBlank()) {
                MasterLinkGenerator.createSmartLink(
                    this.name,
                    interceptedUrl,
                    referer,
                    headers = MasterLinkGenerator.minimalVideoHeaders,
                    bareHeaders = true,
                    callback = callback
                )
            }
        } catch (e: Exception) {
            Log.d("VideoNodePage", "WebViewResolver failed: ${e.message}")
        }
    }
}
