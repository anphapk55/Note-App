package com.example.noteapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.noteapp.R
import com.example.noteapp.model.Album

class AlbumAdapter(private val context: Context, private val albumItemList:MutableList<Album>, private val listener:AlbumAdapterListener):
    RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder>(){

    inner class AlbumViewHolder(itemview : View): RecyclerView.ViewHolder(itemview){
        internal val img_album = itemview.findViewById(R.id.img_album) as ImageView
        internal val txt_namealbum = itemview.findViewById<TextView>(R.id.txt_namealbum)
        internal val txt_notealbum = itemview.findViewById<TextView>(R.id.txt_notealbum)

        init {
            itemview.setOnClickListener {
                listener.onAlbumItemSelected(albumItemList[adapterPosition])
            }
        }
    }

    interface AlbumAdapterListener {
        fun onAlbumItemSelected(album :Album)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        val itemview = LayoutInflater.from(context).inflate(R.layout.item_recyclehome,parent,false)
        return AlbumViewHolder(itemview)
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.img_album.setImageBitmap(albumItemList[position].image)
        holder.txt_namealbum.setText(albumItemList[position].nameab)
        holder.txt_notealbum.setText(albumItemList[position].note)
    }

    override fun getItemCount(): Int {
        return albumItemList.size
    }
}