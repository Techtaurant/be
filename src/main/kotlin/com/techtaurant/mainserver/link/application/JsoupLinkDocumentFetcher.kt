package com.techtaurant.mainserver.link.application

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.springframework.stereotype.Component

@Component
class JsoupLinkDocumentFetcher : LinkDocumentFetcher {
    override fun fetch(url: String): Document {
        return Jsoup.connect(url).get()
    }
}
