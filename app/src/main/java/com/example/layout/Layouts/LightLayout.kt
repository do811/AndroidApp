package com.example.layout.Layouts

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.EchonetLiteManager
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R

class LightLayout : AppCompatActivity(), ListAdapter.OnSwitchClickListener {

    private lateinit var recycView: RecyclerView
    private lateinit var adapter: ListAdapter

    private val data = mutableListOf<ListItem>()
//    private val data = mutableListOf(
//        ListItem(1, "MainText1", "SubText1", true, "ON"),
//        ListItem(2, "MainText2", "SubText2", false, "OFF"),
//        ListItem(3, "MainText3", "SubText3", true, "ON")
//    )

    private fun addData(mainText: String, subText: String, isOn: Boolean) {
        data.add(ListItem(data.size, mainText, subText, isOn, if (isOn) "ON" else "OFF"))
        recycView.post {//adapterをいじる際にpostを使用
            adapter.notifyItemInserted(data.size - 1)
        }
    }

    private fun removeData(position: Int) {
        data.removeAt(position)
        recycView.post {
            adapter.notifyItemRemoved(position)
        }
    }

    private fun updateData(position: Int, mainText: String, subText: String, isOn: Boolean) {
        data[position].MainText = mainText
        data[position].SubText = subText
        data[position].Switch = isOn
        data[position].SwitchState = if (isOn) "ON" else "OFF"
    }

    private fun getLightData(){
        for (echonetLiteObject in EchonetLiteManager.deviceList) {
            val mainText = echonetLiteObject.name["jp"].toString()
            val subText = echonetLiteObject.name["en"].toString()
            val isOn = echonetLiteObject.status[0x80.toByte()] == 0x30.toByte()
            addData(mainText, subText, isOn)
        }
    }

    private fun updateLightData() {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elect)
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

        for(i in 1..10){
            addData("MainText$i", "SubText$i", i % 2 == 0)
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