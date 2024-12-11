package com.example.layout.Layouts

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.MainActivity
import com.example.layout.R

class ElectLayout : AppCompatActivity(), ListAdapter.OnSwitchClickListener {

    private lateinit var recycView: RecyclerView
    private lateinit var adapter: ListAdapter

    private val data = mutableListOf(
        ListItem(1, "MainText1", "SubText1", true, "ON"),
        ListItem(2, "MainText2", "SubText2", false, "OFF"),
        ListItem(3, "MainText3", "SubText3", true, "ON")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elect)
        val genebutton = findViewById<Button>(R.id.inc_button)
//        if (savedInstanceState == null) { // savedInstanceState が null の場合のみ Intent から取得
//            data.clear()
//            data.addAll(intent.getParcelableArrayListExtra<ListItem>("data") ?: arrayListOf())
//        }
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

    }

//    override fun onDestroy() {
//        super.onDestroy()
//        val intent = Intent(this, MainActivity::class.java)
//        val bundle = Bundle()
//        bundle.putParcelableArrayList("data", ArrayList<Parcelable>(data))
//        intent.putExtras(bundle)
////            finish()
//    }

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

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putParcelableArrayList("data", ArrayList<Parcelable>(data))
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        data.clear()
        data.addAll(savedInstanceState.getParcelableArrayList<ListItem>("data")!!)
        adapter.notifyDataSetChanged()
    }
}