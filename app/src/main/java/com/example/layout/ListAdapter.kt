package com.example.layout


import com.example.layout.ListViewHolder;
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


class ListAdapter(
    private val data: MutableList<ListItem>,
    private val listener: OnSwitchClickListener
) :
    RecyclerView.Adapter<ListViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        return ListViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item, parent, false)
        )
    }

    override fun getItemCount(): Int {

        return data.size
    }

    interface OnSwitchClickListener {
        fun onSwitchClick(position: Int, isChecked: Boolean)
    }


    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.MainText.text = data[position].MainText
        holder.SubText.text = data[position].SubText
        holder.Switch.isChecked = data[position].Switch
        holder.Switch.setOnCheckedChangeListener { _, isChecked ->
            listener.onSwitchClick(holder.adapterPosition, isChecked)
        }
    }


}