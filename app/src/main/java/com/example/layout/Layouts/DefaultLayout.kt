package com.example.layout.Layouts

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.ELManager
import com.example.layout.ELObject
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R
import kotlinx.coroutines.launch

open class DefaultLayout : AppCompatActivity(), ListAdapter.OnSwitchClickListener {
    /**
     * 引数のEOJが、このレイアウトで対象としているEOJかどうか判断する
     * 子クラスでオーバーライドする想定
     */
    open fun isTargetEoj(obj: ELObject<Number>): Boolean {
        return true
    }

    private lateinit var recycView: RecyclerView
    private lateinit var adapter: ListAdapter

    private var data = mutableListOf<ListItem>()

    private fun addData(id: Int, mainText: String, subText: String, isOn: Boolean) {
        data.add(ListItem(id, mainText, subText, isOn, if (isOn) "ON" else "OFF"))
        recycView.post {//adapterをいじる際にpostを使用
            println("data added. size:${data.size - 1}")
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
        }
        for (i in 0..<ELManager.deviceList.size) {
            if (!(isTargetEoj(ELManager.deviceList[i]))) {
                continue
            }
            val mainText = ELManager.deviceList[i].name["en"].toString()
            val subText = ELManager.deviceList[i].ipAddress.toString()
            val isOn = ELManager.deviceList[i].status[0x80.toByte()] == 0x30.toByte()
            // addDataだと毎回通知されるが、今回は不必要
            // addData(i, mainText, subText, isOn)
            data.add(ListItem(i, mainText, subText, isOn, if (isOn) "ON" else "OFF"))
        }
        recycView.post {
            // いろいろ変わったことを通知（重いので多用しないこと）
            adapter.notifyDataSetChanged()
        }
    }

    private fun checkLightData() {
        lifecycleScope.launch {
            for (i in 0..<data.size) {
                val obj = ELManager.deviceList[data[i].id]
                val sent = obj.asyncGet("動作状態")
                if (sent == null) {
                    println("checkLightData:sent is null")
                    continue
                }
                val ret = ELManager.asyncWaitPacket(sent)
                if (ret == null) {
                    println("checkLightData:ret is null")
                    continue
                }
                if (ret.edt.isNullOrEmpty()) {
                    println("checkLightData:edt is null")
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

        lifecycleScope.launch {
            println("onCreate start")
            ELManager.asyncGetDeviceList()
            lifecycleScope.launch {
                ELManager.asyncReadPacket()
            }
            showLightData()
            println("data:${data.size}件")
            println(data)
            checkLightData()
            println("onCreate end ")
        }
    }

    override fun onStop() {
        super.onStop()
        ELManager.stopReadPacket()
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
        println("onSwitchClick")
        val id = data[position].id
        val obj = ELManager.deviceList[id]
        lifecycleScope.launch {
            // edt:"true" or "false"
            val set = obj.asyncSetC("動作状態", isChecked.toString())
            if (set == null) {
                updateData(position, data[position].Switch)
                println("onSwitchClick:set is null")
                return@launch
            }
            ELManager.asyncWaitPacket(set)

            val get = obj.asyncGet("動作状態")
            if (get == null) {
                updateData(position, data[position].Switch)
                println("onSwitchClick:get is null")
                return@launch
            }

            val ret = ELManager.asyncWaitPacket(get)
            if (ret?.edt == null || ret.edt.isEmpty()) {
                updateData(position, data[position].Switch)
                println("onSwitchClick:ret is null")
                return@launch
            }

            println("this is ${obj.edtToString(ret.epc, ret.edt)}")
            if (obj.edtToString(ret.epc, ret.edt) == isChecked.toString()) {
                // ちゃんと返答が返ってきてるなら変更
                updateData(position, isChecked)
                println(isChecked)
            } else {
                println("onSwitchClick:変更失敗")
                updateData(position, data[position].Switch)
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