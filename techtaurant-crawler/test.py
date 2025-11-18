from urllib.parse import urljoin

from bs4 import BeautifulSoup
from playwright.sync_api import sync_playwright

# 블로그별 설정
BLOG_CONFIGS = {
    "toss": {
        "base_url": "https://toss.tech/",
        "article_pattern": "/article/",
        "title_selector": "span.e1sck7qg7",
    },
    "kakao": {
        "base_url": "https://tech.kakao.com/blog",
        "article_pattern": "/posts/",
        "title_selector": "h4.tit_post",
    },
}


def extract_articles(soup: BeautifulSoup, config: dict) -> list[dict[str, str]]:
    """게시글 링크와 제목을 추출"""
    articles = []
    a_tags = soup.find_all("a", href=True)

    for a_tag in a_tags:
        href = a_tag.get("href")

        # article 링크만 필터링
        if not href or config["article_pattern"] not in href:
            continue

        # 제목 추출 (선택자로 찾기 시도)
        title_element = a_tag.select_one(config["title_selector"])
        title = title_element.get_text(strip=True) if title_element else a_tag.get_text(strip=True)

        # 절대 URL 변환
        absolute_url = urljoin(config["base_url"], href)

        articles.append({"title": title, "url": absolute_url})

    return articles


def crawl_blog(blog_name: str, page: int = 1) -> list[dict[str, str]]:
    """블로그 크롤링"""
    config = BLOG_CONFIGS.get(blog_name)
    if not config:
        raise ValueError(f"지원하지 않는 블로그: {blog_name}")

    # 페이지 URL 구성
    request_url = f"{config['base_url']}?page={page}"

    with sync_playwright() as p:
        browser = p.chromium.launch()
        page_ = browser.new_page()
        page_.goto(request_url, wait_until="networkidle")
        html_content = page_.content()
        browser.close()

    soup = BeautifulSoup(html_content, "html.parser")

    return extract_articles(soup, config)


if __name__ == "__main__":
    # Toss Tech Blog 크롤링
    # print("=== Toss Tech Blog ===")
    # toss_articles = crawl_blog("toss", page=1)
    # for article in toss_articles:
    #     print(f"제목: {article['title']}")
    #     print(f"URL: {article['url']}\n")

    print("\n=== Kakao Tech Blog ===")
    # Kakao Tech Blog 크롤링 (필요시)
    kakao_articles = crawl_blog("kakao", page=1)
    for article in kakao_articles:
        print(f"제목: {article['title']}")
        print(f"URL: {article['url']}\n")
