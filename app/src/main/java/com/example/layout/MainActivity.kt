package com.example.layout

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.layout.intentsample.*
import kotlinx.coroutines.runBlocking
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.button0)
        val button1 = findViewById<Button>(R.id.button1)
        val button2 = findViewById<Button>(R.id.button2)
        val button3 = findViewById<Button>(R.id.button3)
        val button4 = findViewById<Button>(R.id.button4)
        val displayMetrics = DisplayMetrics()

        val intent2secondsc = Intent(
            this@MainActivity,
            DetailActivity::class.java//真ん中左
        )
        val intent3thridsc = Intent(
            this@MainActivity,
            otherscreen1::class.java//右下
        )
        val intent4fourthsc = Intent(
            this@MainActivity,
            detail::class.java
        )

        val echonet = EchonetLiteManager(resources.assets)

        button.setOnClickListener {
            startActivity(intent2secondsc)
        }
        button1.setOnClickListener {
            startActivity(intent2secondsc)
        }
        button2.setOnClickListener {
            startActivity(intent2secondsc)
        }
        button3.setOnClickListener {
            lifecycleScope.launch {
                val a = EchonetLiteObject(
                    InetAddress.getByName("192.168.2.118"),
                    listOf(0x02, 0x90, 0x01),
                    resources.assets
                )
                val ret =
                    a.asyncGet("動作状態")?.let { it1 -> echonet.asyncWaitPacket(it1) }
                if (ret == null) {
                    return@launch
                }
                println(ret)
                print(a.epcToString(ret.epc) + ":")
                println(ret.edt?.let { it1 -> a.edtToString(ret.epc, it1) })
            }
        }
        button4.setOnClickListener {
            lifecycleScope.launch {
                echonet.asyncGetDeviceList()
                echonet.deviceList.forEach { it ->
                    val ret = it.asyncGet("動作状態")
                    println("ret:$ret")
                }
                println("len:" + echonet.deviceList.size)

//                val a = EchonetLiteObject(
//                    InetAddress.getByName("192.168.2.50"),
//                    listOf(0x02, 0x91, 0x01),
//                    resources.assets
//                )
//                println(echonet.asyncWaitPacket(a.asyncSetC("動作状態","false")))

//                val b = EchonetLiteObject(
//                    InetAddress.getByName("224.0.23.0"),
//                    listOf(0x0E, 0xF0, 0x01), resources.assets
//                )
//                println(b.asyncGet("自ノードインスタンスリストS")
//                    ?.let { it1 -> echonet.asyncWaitPacket(it1) })
            }
        }

//        val interfaces = NetworkInterface.getNetworkInterfaces().toList()
//        for (networkInterface in interfaces) {
//            println("インターフェース名: ${networkInterface.name}")
//            println("ディスプレイ名: ${networkInterface.displayName}")
//            println("インターフェースの詳細: ${networkInterface.inetAddresses.toList()}")
//            println("-----------------------------------")
//        }

    }
}