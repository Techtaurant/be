package com.techtaurant.mainserver.link.application

import org.jsoup.nodes.Document

interface LinkDocumentFetcher {
    fun fetch(url: String): Document
}
