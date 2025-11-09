package com.verpjae.booksearch

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class MainRvAdapter(val context: Context, val bookList: ArrayList<Book>) :
    RecyclerView.Adapter<MainRvAdapter.Holder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(context).inflate(R.layout.main_rv_item, parent, false)
        return Holder(view)
    }

    override fun getItemCount(): Int {
        return bookList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(bookList[position])
        holder.itemView.setOnClickListener {

            //Toast.makeText(context, "Clicked: ${bookList.get(position).bName}", Toast.LENGTH_SHORT).show()
            //새 액티비티를 열고 웹뷰를 이용해서 상세보기 페이지를 보여 준다.

        }
    }
    inner class Holder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
        val bookPhoto = itemView!!.findViewById<ImageView>(R.id.bookPhotoImg)
        val bookName = itemView?.findViewById<TextView>(R.id.bookNameTv)
        val bookNumb = itemView?.findViewById<TextView>(R.id.bookNumbTv)
        val bookRental = itemView?.findViewById<TextView>(R.id.bookRentalTv)
        val bookWriter = itemView?.findViewById<TextView>(R.id.bookWriterTv)
        val bookPublisher = itemView?.findViewById<TextView>(R.id.bookPublisherTv)

        fun bind (book: Book) {
            //context: Context
            /* dogPhoto의 setImageResource에 들어갈 이미지의 id를 파일명(String)으로 찾고,
            이미지가 없는 경우 안드로이드 기본 아이콘을 표시한다.*/

            if (book.bPhoto == "example"){
                Glide.with(itemView).load(R.drawable.example).override(1024, 1365).into(bookPhoto)
            }
            else if (book.bPhoto != "" && book.bPhoto[book.bPhoto.length -1] != '=') {
                //val resourceId = context.resources.getIdentifier(book.bPhoto, "drawable", context.packageName)
                Glide.with(itemView).load(book.bPhoto).override(1024, 1365).into(bookPhoto)
            }
            else {
                Glide.with(itemView).load(R.drawable.nopicture).override(1024, 1365)
                .into(bookPhoto)
            }
            /* 나머지 TextView와 String 데이터를 연결한다. */
            bookName?.text = book.bName
            bookNumb?.text = book.bNumb
            bookRental?.text = if(book.bRental != "대출가능") "대출불가" else book.bRental
            bookWriter?.text = book.bWriter
            bookPublisher?.text = book.bPublisher
        }
    }
    }