package com.verpjae.booksearch

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.verpjae.booksearch.databinding.MainRvItemBinding


class MainRvAdapter(val context: Context, val bookList: MutableList<Book>) :
    RecyclerView.Adapter<MainRvAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = MainRvItemBinding.inflate(LayoutInflater.from(context), parent, false)
            return Holder(binding)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(bookList[position])
        }

    override fun getItemCount(): Int = bookList.size

    inner class Holder(private val binding: MainRvItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            with(binding) {
                Glide.with(root)
                    .load(
                        when {
                            book.bPhoto == "example" -> R.drawable.example
                            book.bPhoto.isNotEmpty() && book.bPhoto.last() != '=' -> book.bPhoto
                            else -> R.drawable.nopicture
                        }
                    )
                    .override(1024, 1365)
                    .into(bookPhotoImg)

                bookNameTv.text = book.bName
                bookNumbTv.text = book.bNumb
                bookRentalTv.text = if (book.bRental != "대출가능") "대출불가" else book.bRental
                bookWriterTv.text = book.bWriter
                bookPublisherTv.text = book.bPublisher
            }
        }
    }
}