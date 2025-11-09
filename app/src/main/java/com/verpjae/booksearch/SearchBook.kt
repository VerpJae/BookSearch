package com.verpjae.booksearch

import org.jsoup.Connection
import org.jsoup.Jsoup
import java.net.URLEncoder

object SearchBook {

    val area = mapOf(
        "서울" to "https://reading.ssem.or.kr/",
        "부산" to "https://reading.pen.go.kr/",
        "대구" to "https://reading.edunavi.kr/",
        "인천" to "https://book.ice.go.kr/",
        "광주" to "https://book.gen.go.kr/",
        "대전" to "https://reading.edurang.net/",
        "울산" to "https://reading.ulsanedu.kr/",
        "세종" to "https://reading.sje.go.kr/",
        "경기" to "https://reading.gglec.go.kr/",
        "강원" to "https://reading.gweduone.net/",
        "충북" to "https://reading.cbe.go.kr/",
        "충남" to "https://reading.edus.or.kr/",
        "전북" to "https://reading.jbedu.kr/",
        "전남" to "https://reading.jnei.go.kr/",
        "경북" to "https://reading.gyo6.net/",
        "경남" to "https://reading.gne.go.kr/",
        "제주" to "https://reading.jje.go.kr/"
    )

    //val NO_IMAGE = "https://www.epasskorea.com/Public_html/Images/common/noimage.jpg"
    val NO_IMAGE = "r/html/newreading/image/search/data/imgNo.jpg"
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

    private fun getSchoolFromName(local: String, name: String): Map<String, String> {
        var url = area[local]
        url += "r/newReading/search/schoolListData.jsp"

        val response = Jsoup.connect(url!!)
            .method(Connection.Method.POST)
            .data("currentPage", "1")
            .data("kind", "1")
            .data("returnUrl", "")
            .data("txtSearchWord", "%EB%8F%84%EC%84%9C%EA%B2%80%EC%83%89") // "도서검색"
            .data("searchGbn", "")
            .data("selEducation", "all")
            .data("selSchool", "all")
            .data("schoolSearch", URLEncoder.encode(name, "UTF-8"))
            .execute()

        val cookies = response.cookies()
        val cookie = cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }

        val body = response.parse().body()
        if (body.text().contains("총 0개의 학교가 검색되었습니다"))
            throw Error("학교 \"${name}\"을(를) 찾을 수 없습니다.")

        val schDiv = body.select("div.school_name > a")

        println(schDiv)

        val schCode = schDiv.attr("onclick").split("('")[1].split("',")[0]
        val schName = schDiv.text()

        return mapOf("name" to schName, "code" to schCode, "cookie" to cookie)
    }

    private fun setSchoolCodeSetting(local: String, code: String, cookie:String) {
        var url = area[local]
        url += "r/newReading/search/schoolCodeSetting.jsp"

        Jsoup.connect(url!!)
            .method(Connection.Method.POST)
            .data("schoolCode", code)
            .data("returnUrl", "")
            .data("kind", "1")
            .data("txtSearchWord", "도서검색")
            .data("searchGbn", "")
            .header("Cookie", cookie)
            .execute()
            .parse()
    }

    fun searchBookFromSchoolName(local: String, book: String, school: String): MutableMap<String, Any> {
        val result = mutableMapOf<String, Any>()
        try {
            /* TODO:
                지원하는 지역을 구별, 미지원시 오류
             */
            val schoolInfo = getSchoolFromName(local, school)

            val code = schoolInfo["code"]!!
            val name = schoolInfo["name"]!!
            val cookie = schoolInfo["cookie"]!!

            result["schoolCode"] = code
            result["schoolName"] = name

            setSchoolCodeSetting(local, code, cookie)

            var url = area[local]
            url += "r/newReading/search/schoolSearchResult.jsp"

            val response = Jsoup.connect(url!!)
                .method(Connection.Method.POST)
                .data("currentPage", "1")
                .data("controlNo", "")
                .data("memberSerial", "")
                .data("bookInfo", "")
                .data("boxCmd", "")
                .data("printCmd", "")
                .data("pageParamInfo", "")
                .data("prevPageInfo", "")
                .data("searchPageName", "schoolSearchForm")
                .data("schSchoolCode", code)
                .data("division1", "ALL")
                .data("searchCon1", URLEncoder.encode(book, "UTF-8"))
                .data("connect1", "A")
                .data("division2", "TITL")
                .data("searchCon2", "")
                .data("connect2", "A")
                .data("division3", "PUBL")
                .data("searchCon3", "")
                .data("dataType", "ALL")
                .data("lineSize", "100")
                .data("cbSort", "STIT")
                .header("Cookie", cookie)
                .execute()
                .parse()

            //result.result = [];

            val body = response.body()
            if (body.text().contains("검색결과가 없습니다."))
                throw Error("책 ${decidePostposition(book)} 찾을 수 없습니다.")

            val bookList = mutableListOf<Map<String, String>>()

            val booksData = body.select("div.bd_list.bd_book_list.school_lib > ul > li")

            for (bookData in booksData) {
                val title = bookData.select("div.bd_list_title > a > span").text().trim()
                val writer = bookData.select("div.bd_list_writer > span.dd")
                    .text()
                    .trim()
                val company = bookData.select("div.bd_list_company > span.dd").text().trim()
                val callNumber = bookData.select("div.bd_list_year > span.dd").text().trim()
                val canRental = bookData.select("div.book_save > div > div").text()
                val imgUrl = bookData.select("div.book_image > img").attr("src")
                val preview = if(imgUrl.contains(NO_IMAGE)) "" else area[local] + imgUrl.substring(1)

                println(title)

                bookList.add(
                    mapOf(
                        "title" to title,
                        "writer" to writer,
                        "company" to company,
                        "callNumber" to callNumber,
                        "canRental" to canRental.toString(),
                        "previewImage" to preview
                    )
                )
            }

            result["result"] = bookList

            result["status"] = "success"
        } catch (e: Error) {
            result["status"] = "fail"
            result["result"] = e.message!!
        }
        return result
    }
}