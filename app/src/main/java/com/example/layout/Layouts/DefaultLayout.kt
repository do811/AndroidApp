package com.example.layout.Layouts

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.EchonetLiteManager
import com.example.layout.EchonetLiteObject
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R
import kotlinx.coroutines.launch

open class DefaultLayout : AppCompatActivity(), ListAdapter.OnSwitchClickListener {
    open fun isTargetEoj(obj: EchonetLiteObject<Number>): Boolean {
        return true
    }

    private lateinit var recycView: RecyclerView
    private lateinit var adapter: ListAdapter

    private var data = mutableListOf<ListItem>()
//    private var data = mutableListOf(
//        ListItem(1, "MainText1", "SubText1", true, "ON"),
//        ListItem(2, "MainText2", "SubText2", false, "OFF"),
//        ListItem(3, "MainText3", "SubText3", true, "ON")
//    )

    private fun addData(id: Int, mainText: String, subText: String, isOn: Boolean) {
        data.add(ListItem(id, mainText, subText, isOn, if (isOn) "ON" else "OFF"))
        recycView.post {//adapterをいじる際にpostを使用
            println("size:${data.size - 1}")
            adapter.notifyItemInserted(data.size - 1)
        }
    }

    private fun removeData(id: Int) {
        val position = data.indexOfFirst { it.id == id }
        if (position == -1) return
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
        recycView.post { // ここでも変更するためpostを使用
            adapter.notifyItemChanged(position) //
        }
    }

    private fun updateData(position: Int, isOn: Boolean) {
        data[position].Switch = isOn
        data[position].SwitchState = if (isOn) "ON" else "OFF"
        recycView.post { // ここでも変更するためpostを使用
            adapter.notifyItemChanged(position) //
        }
    }

    private fun showLightData() {
        for (i in 0..<data.size) {
            data.removeAt(0)
//            recycView.post {
//                adapter.notifyItemRemoved(0)
//            }
        }
        for (i in 0..<EchonetLiteManager.deviceList.size) {
            if (!(isTargetEoj(EchonetLiteManager.deviceList[i]))) {
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

    private fun checkLightData() {
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

//        for (i in 1..10) {
//            addData(i, "MainText$i", "SubText$i", i % 2 == 0)
//        }
//        addData(1, "", "", false)
        lifecycleScope.launch {
            EchonetLiteManager.asyncGetDeviceList()
            showLightData()
            println("data:${data.size}件")
            println(data)
            checkLightData()
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

//        data[position].Switch = isChecked
//        data[position].MainText = "Init"
//        if (isChecked) {
//            data[position].SwitchState = "ON"
//        } else {
//            data[position].SwitchState = "OFF"
//        }
//        recycView.post { // ここでも変更するためpostを使用
//            adapter.notifyItemChanged(position) //
//        }
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