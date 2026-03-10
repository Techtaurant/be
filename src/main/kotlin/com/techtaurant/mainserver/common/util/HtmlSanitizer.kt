package com.techtaurant.mainserver.common.util

import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist

/**
 * HTML sanitization 유틸리티
 * GitHub에서 허용하는 HTML 태그/속성만 통과시키고, 나머지는 제거합니다.
 */
object HtmlSanitizer {
    private val GITHUB_SAFELIST: Safelist =
        Safelist()
            .addTags(
                "h1",
                "h2",
                "h3",
                "h4",
                "h5",
                "h6",
                "p",
                "div",
                "span",
                "br",
                "hr",
                "b",
                "i",
                "em",
                "strong",
                "del",
                "s",
                "strike",
                "sup",
                "sub",
                "u",
                "mark",
                "ul",
                "ol",
                "li",
                "dl",
                "dt",
                "dd",
                "a",
                "img",
                "pre",
                "code",
                "blockquote",
                "table",
                "thead",
                "tbody",
                "tfoot",
                "tr",
                "th",
                "td",
                "details",
                "summary",
                "kbd",
                "abbr",
                "ruby",
                "rt",
                "var",
                "samp",
                "figure",
                "figcaption",
                "picture",
                "source",
            )
            .addAttributes("a", "href", "title")
            .addAttributes("img", "src", "alt", "width", "height", "title")
            .addAttributes("td", "align")
            .addAttributes("th", "align")
            .addAttributes("details", "open")
            .addAttributes("source", "srcset", "type", "media")
            .addAttributes("ol", "start", "type")
            .addAttributes("li", "value")
            .addProtocols("a", "href", "http", "https", "mailto")

    /**
     * HTML 본문을 sanitize합니다.
     * GitHub에서 허용하는 태그와 속성만 유지하고, 나머지는 제거합니다.
     *
     * @param html sanitize할 HTML 문자열
     * @return sanitize된 HTML 문자열
     */
    fun sanitizeContent(html: String): String {
        val dirty = Jsoup.parseBodyFragment(html)
        val clean = Cleaner(GITHUB_SAFELIST).clean(dirty)
        clean.outputSettings().prettyPrint(false)
        return clean.body().html()
    }

    /**
     * 제목에서 모든 HTML 태그를 제거합니다.
     *
     * @param text sanitize할 제목 문자열
     * @return 태그가 제거된 plain text
     */
    fun sanitizeTitle(text: String): String {
        return Jsoup.clean(text, Safelist.none())
    }

    /**
     * HTML 본문에서 tmp/ 경로로 시작하는 S3 objectKey를 모두 추출합니다.
     * HTML 태그(img src, a href)와 Markdown 패턴(![...](key), [...](key))을 모두 지원합니다.
     *
     * @param html HTML 또는 Markdown 문자열
     * @return 추출된 tmp/ 경로 리스트
     */
    fun extractTmpObjectKeys(html: String): List<String> {
        val keys = mutableSetOf<String>()

        // 1. HTML 태그 추출 (Jsoup)
        val doc = Jsoup.parseBodyFragment(html)
        doc.select("img[src]").forEach { img ->
            val src = img.attr("src")
            if (src.startsWith("tmp/")) {
                keys.add(src)
            }
        }
        doc.select("a[href]").forEach { a ->
            val href = a.attr("href")
            if (href.startsWith("tmp/")) {
                keys.add(href)
            }
        }

        // 2. Markdown 패턴 추출 (Regex)
        // ![alt](tmp/...) 또는 [text](tmp/...) 패턴 매칭
        val markdownRegex = Regex("""!?\[[^\]]*\]\((tmp/[^)]+)\)""")
        markdownRegex.findAll(html).forEach { matchResult ->
            keys.add(matchResult.groupValues[1])
        }

        return keys.toList()
    }
}
