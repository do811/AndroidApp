package com.example.layout.Layouts

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R

class ElectLayout : AppCompatActivity(), ListAdapter.OnSwitchClickListener {
    private val data = mutableListOf(
        ListItem(1, "MainText1", "SubText1", true, "ON"),
        ListItem(2, "MainText2", "SubText2", false, "OFF"),
        ListItem(3, "MainText3", "SubText3", true, "ON")
    )
    private lateinit var recycView: RecyclerView
    private lateinit var adapter: ListAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elect)
        val genebutton = findViewById<Button>(R.id.inc_button)
        recycView = findViewById<RecyclerView>(R.id.rccv)
        recycView.setHasFixedSize(true)
        recycView.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        adapter = ListAdapter(data, this)
        recycView.adapter = adapter


        genebutton.setOnClickListener {
            data.add(ListItem(4, "MainText4", "SubText4", false, "OFF"))
            recycView.post {//adapterをいじる際にpostを使用
                adapter.notifyItemInserted(data.size - 1)
            }
        }

        fun onButtonClick(view: View) {
            finish()
        }
    }

    override fun onSwitchClick(position: Int, isChecked: Boolean) {
        data[position].Switch = isChecked
        data[position].MainText = "Init"
        if (isChecked) {
            data[position].SwitchState = "ON"
        } else {
            data[position].SwitchState = "OFF"
        }
        recycView.post { // ここでも変更するためpostを使用
            adapter.notifyItemChanged(position) //
        }
    }
}