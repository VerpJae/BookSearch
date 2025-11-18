package com.verpjae.booksearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.URLEncoder

object SearchBook {

    val areaList = listOf("서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주")
    const val areaChar = "BCDEFGHIJKMNPQRSTV"
    const val SEARCH_SCHOOL_URL = "https://read365.edunet.net/dls/api/school/list"
    const val SEARCH_STATE_URL = "https://read365.edunet.net/alpasq/api/search/book/state"
    const val SEARCH_BOOK_URL = "https://read365.edunet.net/alpasq/api/search"

    /*
    response = Jsoup.connect(url)
        .method(Connection.Method.POST)
        .ignoreContentType(true)
        .header("Content-Type", "application/json")
        .requestBody(JSON.stringify(json))
        .execute();
    response.body();

    */

    private fun getSchoolFromName(local: Int, schName: String): Map<String, String> { // schoolName, neisCode
        val provCode = areaChar[local] + "10"

        val schoolDataResponse = Jsoup.connect(SEARCH_SCHOOL_URL)
            .method(Connection.Method.GET)
            .data("schoolCode", "")
            .data("provCode", provCode)
            .data("schoolLevel", "")
            .data("searchKeyword", schName)
            .ignoreContentType(true)
            .header("Content-Type", "application/json")
            .execute().body()

        /* schoolDataResponse Expectation
        {
            "status": "OK",
            "message": "SUCCESS",
            "data": [
                {
                    "libCode": "90379",
                    "schoolCode": "90379",
                    "neisCode": "K100000460",
                    "schoolName": "영월고등학교",
                    "addr": "강원도 영월군 영월읍 오무개길 51 영월고등학교 1층 도서관",
                    "provCode": "K10",
                    "schoolLevel": "고등학교",
                    "schoolType": "공립",
                    "tel": "0333705142",
                    "fax": "",
                    "belong": "영월교육지원청",
                    "masterName": "고진식",
                    "homepageURL": "www.youngwol.hs.kr/"
                }
            ]
        }
        */

        val schoolDataJSON = JSONObject(schoolDataResponse)
            .getJSONArray("data")
            .getJSONObject(0)

        val schoolName = schoolDataJSON.getString("schoolName")
        val neisCode = schoolDataJSON.getString("neisCode")
        return mapOf(Pair("schoolName", schoolName), Pair("neisCode", neisCode))
    }
    private fun decidePostposition(word: String): String  {
        val lastChar: Char = word[word.length - 1]

        // 한글의 시작(가)이나 끝(힣) 범위 초과시 오류
        if(lastChar.code < 0xAC00 || lastChar.code > 0xD7A3) {
            return "\"${word}\""+"을(를)"
        }
        val postposition: String =
            if ((lastChar.code - 0xAC00) % 28 > 0) "을"
            else "를"

        return "\"${word}\""+postposition
    }
    private fun getBookSearchData(local: Int, keyword: String, schName: String, searchType:String, page: Int): JSONObject {
        val provCode = areaChar[local] + "10" // B10부터 서울
        val schoolData = getSchoolFromName(local, schName)
        val neisCode = schoolData["neisCode"]
        val schoolName = schoolData["schoolName"]
        val json = """
        {
            "searchKeyword": "$keyword",
            "searchType": "$searchType",
            "neisCode": ["$neisCode"],
            "provCode": "$provCode",
            "schoolName": "$schoolName",
            "page": "$page",
            "coverYn": "N",
            "facet": "Y"
        }
        """.trimIndent()

        val schoolDataResponse = Jsoup.connect(SEARCH_BOOK_URL)
            .method(Connection.Method.POST)
            .ignoreContentType(true)
            .header("Content-Type", "application/json")
            .requestBody(json)
            .execute().body()
        println(schoolDataResponse)
        val searchData = JSONObject(schoolDataResponse)
            .getJSONObject("data")

        return searchData
    }


    fun searchBookFromSchoolName(local: Int, keyword: String, schName: String, searchType: String): MutableMap<String, Any> {
        val provCode = areaChar[local] + "10" // B10부터 서울
        val result = mutableMapOf<String, Any>()
        val resultBookList = mutableListOf<Map<String, String>>()

        try {
            val searchData = getBookSearchData(local, keyword, schName, searchType, 1)
            val totalPage = searchData.getInt("totalPage")

            if (totalPage == 0)
                throw Error("책 ${decidePostposition(keyword)} 찾을 수 없습니다.")

            for (page in 1..totalPage) {
                val searchData = getBookSearchData(local, keyword, schName, searchType, page)
                val bookList = searchData.getJSONArray("bookList")
                for (i in 0 until bookList.length()) {
                    val bookData = bookList.getJSONObject(i)

                    val title = bookData.getString("title")
                    val author = bookData.getString("author")
                    val publisher = bookData.getString("publisher")
                    val callNumber = bookData.getString("callNo")

                    val neisCode = bookData.getString("neisCode")
                    val bookKey = bookData.getString("bookKey")
                    val res = Jsoup.connect(SEARCH_STATE_URL)
                        .method(Connection.Method.GET)
                        .data("bookKey", bookKey)
                        .data("provCode", provCode)
                        .data("neisCode", neisCode)
                        .ignoreContentType(true)
                        .header("Content-Type", "application/json")
                        .execute().body()
                    /*
                {
                    "status": "OK",
                    "message": "SUCCESS",
                    "data": {
                        "coverYn": "Y",
                        "coverUrl": "https://image.aladin.co.kr/product/14131/83/cover500/k292532438_1.jpg",
                        "status": "대출중",
                        "rsvtCount": 0,
                        "returnPlanDate": "2025-11-19",
                        "rsvtYn": "Y",
                        "illYn": "N",
                        "locationName": "자료실"
                    }
                }
                */
                    val etcData = JSONObject(res).getJSONObject("data")
                    val canRental = etcData.getString("status")
                    val coverUrl = etcData.optString("coverUrl")
                    val imgUrl = coverUrl.takeUnless { it == "N" }
                        ?: "https://read365.edunet.net/img/no_book.805feab5.png"

                    println(title)

                    resultBookList.add(
                        mapOf(
                            "title" to title,
                            "author" to author,
                            "publisher" to publisher,
                            "callNumber" to callNumber,
                            "canRental" to canRental,
                            "previewImage" to imgUrl
                        )
                    )
                }
            }

            result["status"] = "success"
            result["result"] = resultBookList
        } catch (e: Error) {
            result["status"] = "fail"
            result["result"] = e.message!!
        }
        return result
    }

    suspend fun searchBooksPaged(
        local: Int,
        keyword: String,
        schName: String,
        searchType: String,
        onPageLoaded: (List<Book>) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {

        try {
            val firstPageData = getBookSearchData(local, keyword, schName, searchType, 1)
            val totalPage = firstPageData.getInt("totalPage")

            if (totalPage == 0) {
                onError("책 ${decidePostposition(keyword)} 찾을 수 없습니다.")
                return@withContext
            }

            for (page in 1..totalPage) {
                coroutineContext.ensureActive()

                val searchData = getBookSearchData(local, keyword, schName, searchType, page)
                val bookList = searchData.getJSONArray("bookList")

                val pageResult = mutableListOf<Book>()

                for (i in 0 until bookList.length()) {
                    coroutineContext.ensureActive()

                    val bookData = bookList.getJSONObject(i)

                    val title = bookData.getString("title")
                    val author = bookData.getString("author")
                    val publisher = bookData.getString("publisher")
                    val callNumber = bookData.getString("callNo")
                    val neisCode = bookData.getString("neisCode")
                    val bookKey = bookData.getString("bookKey")

                    val res = Jsoup.connect(SEARCH_STATE_URL)
                        .method(Connection.Method.GET)
                        .data("bookKey", bookKey)
                        .data("provCode", areaChar[local] + "10")
                        .data("neisCode", neisCode)
                        .ignoreContentType(true)
                        .header("Content-Type", "application/json")
                        .execute().body()

                    val etcData = JSONObject(res).getJSONObject("data")
                    val canRental = etcData.getString("status")
                    val imgUrl = etcData.optString("coverUrl").takeUnless { it == "N" }
                        ?: "https://read365.edunet.net/img/no_book.805feab5.png"

                    pageResult.add(
                        Book(
                            bName = title,
                            bRental = canRental,
                            bNumb = callNumber,
                            bPhoto = imgUrl,
                            bWriter = author,
                            bPublisher = publisher
                        )
                    )
                }

                // 페이지 1개가 완성될 때마다 UI에게 전달
                withContext(Dispatchers.Main) {
                    coroutineContext.ensureActive()
                    onPageLoaded(pageResult)
                }
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "오류 발생")
            }
        }
    }
}