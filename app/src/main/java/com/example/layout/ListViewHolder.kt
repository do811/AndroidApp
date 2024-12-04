package com.example.layout

import android.view.View
import android.widget.TextView
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView

class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val MainText: TextView = itemView.findViewById(R.id.maintext)
    val SubText: TextView = itemView.findViewById(R.id.subtext)
    val Switch: Switch = itemView.findViewById(R.id.onoffS)
}