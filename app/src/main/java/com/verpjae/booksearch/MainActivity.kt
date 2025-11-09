package com.verpjae.booksearch


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

/*
DEPRECATED
ACTUALLY I CANT UNDERSTAND THESE CODE WELL .. LOL

fun sans(arr: JSONArray): ArrayList<ArrayList<String>> {
    val result: ArrayList<ArrayList<String>> = arrayListOf(arrayListOf("책이름", "대여", "책 번호", "example", "지은이", "출판사"))
    for (i in 0 until arr.length()) {
            var b =  arr.getJSONObject(i)
            result.add(arrayListOf(b.getString("title").toString(),  b.getString("canRental").toString(), b.getString("callNumber").toString(), b.getString("previewImage").toString(), b.getString("writer"), b.getString("company")))
        }
    return result
    //return "$result";
}

fun tost(Liist: ArrayList<Book>, re: ArrayList<ArrayList<String>>, adaptt: MainRvAdapter){
    Liist.clear()
    for (i in 0 until re.size) {
        Liist.add(Book(re[i][0], re[i][1], re[i][2], re[i][3], re[i][4], re[i][5]))
    }
    adaptt.notifyDataSetChanged()
}

*/


class MainActivity : AppCompatActivity() {

    val list_of_items = SearchBook.area.keys.toList() // 지역 이름(도 단위) 리스트

    var bookList = arrayListOf<Book>(
        Book("책이름", "대출", "책 번호", "example", "지은이", "출판사"),
        Book("어린왕자", "가능", "ㄱ 5678", "example", "생텍쥐페리", "출판사")
    )

    fun adaptWithInfo(list: MutableList<*>, adapter: MainRvAdapter){
        // 예시로 있는 두 권 삭제
        bookList.clear()

        for(book in list){
            book as Map<*,*>
            bookList.add(Book(book["title"] as String, book["canRental"] as String, book["callNumber"] as String, book["previewImage"] as String, book["writer"] as String, book["company"] as String))
        }
        adapter.notifyDataSetChanged()
    }

    // =============================
    // 캐쉬 저장 및 권한 확인

    private fun saveCache(key: String, data: String) {
        val shared = getSharedPreferences("datalol", Context.MODE_PRIVATE)
        val editor = shared.edit()
        editor.putString(key, data)
        editor.apply()
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false

            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            return connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
    private fun readCache(key: String): String {
        val shared = getSharedPreferences("datalol", Context.MODE_PRIVATE)
        return shared.getString(key, "")!!
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val imm: InputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        return super.dispatchTouchEvent(ev)
    }

    // =============================

    lateinit var mAdView : AdView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}

        // 2. 광고 띄우기
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)


        // bookList와 어댑터 연결
        val mRecyclerView = findViewById<RecyclerView>(R.id.recyclerv)
        val mAdapter = MainRvAdapter(this, bookList)
        mRecyclerView.adapter = mAdapter

        val lm = LinearLayoutManager(this)
        mRecyclerView.layoutManager = lm
        mRecyclerView.setHasFixedSize(true)

        val school = findViewById<EditText>(R.id.school)
        school.setText(if(readCache("school") == "null" || readCache("school") == "") "" else readCache("school"))
        val book = findViewById<EditText>(R.id.bookname)
        val local = findViewById<Spinner>(R.id.local)
        local.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, list_of_items)
        val pos = if(readCache("local") == "null" || readCache("local") == ""){
            0
        } else readCache("local").toInt()

        local.setSelection(pos)
        val button = findViewById<Button>(R.id.button)
        val textView = findViewById<TextView>(R.id.textview)

        book.setOnEditorActionListener{ v, action, event ->
            var handled = false

            if (action == EditorInfo.IME_ACTION_DONE) {
                // 키보드 내리기
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(book.windowToken, 0)
                button.performClick()
                handled = true
            }

            handled
        }

        button.setOnClickListener() {
            saveCache("school", school.text.toString())
            saveCache("local", local.selectedItemPosition.toString())
            if(!isNetworkAvailable(this)) {
                textView.setTextColor(Color.parseColor("#ff2424"))
                textView.text = "인터넷 연결 상태를 확인해주세요."
            }
            else {
                CoroutineScope(IO).launch {
                    //var res:String? = "BookList"
                    /*
                    val parse =
                        Jsoup.connect("http://vz.kro.kr/book/?book=" + book.getText() + "&school=" + school.getText() + "&local=" + local.selectedItem)
                            .ignoreContentType(true).get().text()
                    */
                    val parse = SearchBook.searchBookFromSchoolName(local.selectedItem.toString(), book.text.toString(), school.text.toString())

                    if (parse["status"] == "success") {
                        val schoolName = parse["schoolName"]
                        val bresult = parse["result"] as MutableList<*>
                        println(bresult)
                        CoroutineScope(Main).launch {
                            delay(50)
                            textView.setTextColor(Color.parseColor("#3fd467"))
                            textView.text = "성공 - $schoolName"
                            adaptWithInfo(bresult, mAdapter)
                        }
                    } else {
                        val ress = parse["result"] as String

                        CoroutineScope(Main).launch {
                            delay(50)
                            bookList.clear()
                            bookList.add(Book("책이름", "대출", "책 번호", "example", "지은이", "출판사"))
                            mAdapter.notifyDataSetChanged()

                            // textView 는 오류메시지 띄울 공간
                            textView.setTextColor(Color.parseColor("#ff2424"))
                            if (parse.contains("schoolName")) {
                                val schoolName = parse["schoolName"]
                                textView.text = "$schoolName - $ress"
                            } else { // 오류 내용
                                textView.text = ress
                            }
                        }
                    }
                }
            }
        }
    }
}