package com.example.layout.Layouts

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.EchonetLiteManager
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R
import kotlinx.coroutines.launch

class ElseLayout : AppCompatActivity(), ListAdapter.OnSwitchClickListener {

    private var data = mutableListOf<ListItem>()
    private lateinit var recycView: RecyclerView
    private lateinit var adapter: ListAdapter


    private fun updateData(position: Int, isOn: Boolean) {
        data[position].Switch = isOn
        data[position].SwitchState = if (isOn) "ON" else "OFF"
        recycView.post { // ここでも変更するためpostを使用
            adapter.notifyItemChanged(position) //
        }
    }

    private fun showData() {
        for (i in 0..<data.size) {
            data.removeAt(0)
//            recycView.post {
//                adapter.notifyItemRemoved(0)
//            }
        }
        for (i in 0..<EchonetLiteManager.deviceList.size) {
            // 照明以外はいらない
            if (EchonetLiteManager.deviceList[i].compareEoj(
                    listOf(0x01, 0x30)
                ) || EchonetLiteManager.deviceList[i].compareEoj(
                    listOf(0x02, 0x90)
                ) || EchonetLiteManager.deviceList[i].compareEoj(listOf(0x02, 0x91))
            ) {
                continue
            }
            val mainText = EchonetLiteManager.deviceList[i].name["en"].toString()
            val subText = EchonetLiteManager.deviceList[i].ipAddress.toString()
            val isOn = EchonetLiteManager.deviceList[i].status[0x80.toByte()] == 0x30.toByte()
//            addData(i, mainText, subText, isOn)
            data.add(ListItem(i, mainText, subText, isOn, if (isOn) "ON" else "OFF"))
        }
        println("bbbbb")
        recycView.post {
            adapter.notifyDataSetChanged()
        }
    }


    private fun checkData() {
        lifecycleScope.launch {
            for (i in 0..<data.size) {
                val obj = EchonetLiteManager.deviceList[data[i].id]
                val sent = obj.asyncGet("動作状態")
                if (sent == null) {
                    println("sent is null")
                    continue
                }
                val ret = EchonetLiteManager.asyncWaitPacket(sent)
                if (ret == null) {
                    println("ret is null")
                    continue
                }
                if (ret.edt.isNullOrEmpty()) {
                    println("edt is null")
                    continue
                }
                val edt = obj.edtToString(ret.epc, ret.edt)
                updateData(i, edt == "true")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_elect)
//        val genebutton = findViewById<Button>(R.id.inc_button)
        recycView = findViewById<RecyclerView>(R.id.rccv)
        recycView.setHasFixedSize(true)
        recycView.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        adapter = ListAdapter(data, this)
        recycView.adapter = adapter


        lifecycleScope.launch {
            EchonetLiteManager.asyncGetDeviceList()
            showData()
            println("data:${data.size}件")
            println(data)
            checkData()
        }

    }

    override fun onSwitchClick(position: Int, isChecked: Boolean) {
        println("on press")
        fun reset() {
            updateData(position, data[position].Switch)
        }

        val id = data[position].id
        val obj = EchonetLiteManager.deviceList[id]
        lifecycleScope.launch {
            // edt:"true" or "false"
            val set = obj.asyncSetC("動作状態", isChecked.toString())
            if (set == null) {
                reset()
                return@launch
            }
            EchonetLiteManager.asyncWaitPacket(set)

            val get = obj.asyncGet("動作状態")
            if (get == null) {
                reset()
                return@launch
            }

            val ret = EchonetLiteManager.asyncWaitPacket(get)
            if (ret?.edt == null || ret.edt.isEmpty()) {
                reset()
                return@launch
            }

            println("this is ${obj.edtToString(ret.epc, ret.edt)}")
            if (obj.edtToString(ret.epc, ret.edt) == isChecked.toString()) {
                // ちゃんと返答が返ってきてるなら変更
                updateData(position, isChecked)
            } else {
                reset()
            }
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