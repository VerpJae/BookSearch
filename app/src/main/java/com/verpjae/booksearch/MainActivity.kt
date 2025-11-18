package com.verpjae.booksearch

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdView
import com.verpjae.booksearch.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import androidx.core.content.edit
import kotlinx.coroutines.Job

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mAdapter: MainRvAdapter
    private lateinit var mAdView: AdView

    private var searchJob: Job? = null

    private val bookList = mutableListOf<Book>(
        Book("책이름", "대출불가", "책 번호", "example", "지은이", "출판사")
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 광고 초기화
        MobileAds.initialize(this)
        mAdView = binding.adView
        mAdView.loadAd(AdRequest.Builder().build())

        // RecyclerView 설정
        mAdapter = MainRvAdapter(this, bookList)
        binding.recyclerv.apply {
            adapter = mAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            setHasFixedSize(true)
        }


        // 전체,서명,저자,발행자
        val categoryList = listOf("", "TITLE", "AUTHOR", "PUBLISHER")

        // Spinner 설정
        with(binding.local) {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                SearchBook.areaList
            )
            setSelection(readCache("local").toIntOrNull() ?: 0)
        }
        with(binding.category) {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                categoryList
            )
            setSelection(0)
        }

        // 캐시 불러오기
        binding.school.setText(readCache("school").ifBlank { "" })

        // 검색 버튼 클릭
        binding.button.setOnClickListener { onSearchClicked() }

        // 엔터로 검색 실행
        binding.bookname.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                binding.button.performClick()
                true
            } else false
        }
    }

    // 최신식 키보드 숨김
    private fun hideKeyboard() {
        currentFocus?.let { view ->
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onSearchClicked() {
        val school = binding.school.text.toString()
        //val local = binding.local.selectedItem.toString()
        val local = binding.local.selectedItemPosition
        val book = binding.bookname.text.toString()
        val category = binding.category.selectedItem.toString()

        saveCache("school", school)
        saveCache("local", local.toString())

        if (!isNetworkAvailable()) {
            binding.textview.apply {
                setTextColor(Color.RED)
                text = "인터넷 연결 상태를 확인해주세요."
            }
            return
        }
        searchJob?.cancel()

        searchJob = lifecycleScope.launch {
            bookList.clear()
            bookList.add(Book("책이름", "대출불가", "책 번호", "example", "지은이", "출판사"))
            SearchBook.searchBooksPaged(
                local = local,
                keyword = book,
                schName = school,
                searchType = category,
                onPageLoaded = { pageList ->
                    // 페이지가 하나씩 로딩될 때마다 호출됨
                    bookList.addAll(pageList)
                    mAdapter.notifyDataSetChanged()
                },
                onError = { msg ->
                    binding.textview.apply {
                        setTextColor(Color.RED)
                        text = msg
                    }
                }
            )
        }
    }

    private fun saveCache(key: String, data: String) {
        getSharedPreferences("datalol", MODE_PRIVATE)
            .edit { putString(key, data) }
    }

    private fun readCache(key: String): String =
        getSharedPreferences("datalol", MODE_PRIVATE)
            .getString(key, "") ?: ""

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)

    }
}
