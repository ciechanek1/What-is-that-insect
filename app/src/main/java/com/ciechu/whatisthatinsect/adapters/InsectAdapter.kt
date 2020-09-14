package com.ciechu.whatisthatinsect.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_insect.view.*
import com.bumptech.glide.Glide
import com.ciechu.whatisthatinsect.R
import com.ciechu.whatisthatinsect.data.Insect

class InsectAdapter(private val insectsList: List<Insect>,
                    private val listener: OnItemClickListener
) : RecyclerView.Adapter<InsectAdapter.MyViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_insect, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return insectsList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.itemView.setBackgroundColor(Color.WHITE)
    //Change the background color of the selected item
        if (insectsList[position].isSelected) holder.itemView.setBackgroundColor(Color.LTGRAY)
        holder.bind(insectsList[position])
    }

   inner class MyViewHolder(view: View): RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener { listener.onItemClick(insectsList[adapterPosition], adapterPosition) }
            view.setOnLongClickListener { listener.onItemLongClick(insectsList[adapterPosition], adapterPosition)
            true
            }
        }
       fun bind(insect: Insect){
           with(itemView){
               name_insect_item.text = insect.name
               dateOfCapture_item.text = insect.date
              Glide.with(this)
                   .load(insect.image)
                   .into(this.image_insect_item)
           }
       }
    }
}

interface OnItemClickListener{
    fun onItemClick(insect: Insect, position: Int)
    fun onItemLongClick(insect: Insect, position: Int)
}


