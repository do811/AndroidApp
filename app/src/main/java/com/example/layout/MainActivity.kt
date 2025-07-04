package com.example.layout

import com.example.layout.intentsample.otherscreen1
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.layout.Layouts.AirConditionerLayout
import com.example.layout.Layouts.LightLayout
import com.example.layout.Layouts.AddMachineLayout
import com.example.layout.Layouts.ElseLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // init
        if (!ELManager.isAssetManagerInitialized()) {
            ELManager.assetManager = resources.assets
        }

        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.button0)//右上
        val button1 = findViewById<Button>(R.id.button1)//左上
        val button2 = findViewById<Button>(R.id.button2)//右下
        val button4 = findViewById<Button>(R.id.button4)//設定ボタン
        val constraint = findViewById<ConstraintLayout>(R.id.background)
        val Allral = constraint.background
        val displayMetrics = DisplayMetrics()
        var color: Int = 0;
        val alpha: Double
        run {
            if (Allral is ColorDrawable) {
                // ColorDrawable の場合、getColor() メソッドで色を取得できる
                color = Allral.color
                val setAlpha = intent.getIntExtra("ColorIntData", 100);
                // color には色情報が int 型で格納されている (例: 0xFFFF0000 - 赤)
                println("Background color: $color")
                val red = Color.red(color)
                val green = Color.green(color)
                val blue = Color.blue(color)
                alpha = 0xFF * setAlpha / 100.0
                println("red: $red")
                println("green: $green")
                println("blue: $blue")
                println("alpha: $alpha")
                constraint.setBackgroundColor(Color.argb(setAlpha, red, green, blue))
            }
            //Allral.setBackgroundColor(color.toInt())
        }
        val intent2secondsc = Intent(
            this@MainActivity,
            LightLayout::class.java//真ん中左
        )
        val intent3thridsc = Intent(
            this@MainActivity,
            AirConditionerLayout::class.java//右下
        )
        val intent4fourthsc = Intent(
            this@MainActivity,
            otherscreen1::class.java
        )
        val intentElseSc = Intent(
            this@MainActivity,
            ElseLayout::class.java
        )

        intent4fourthsc.putExtra("colorInt", color)

//        val echonet = EchonetLiteManager(resources.assets)

        button.setOnClickListener {
            startActivity(intent3thridsc)
        }
        button1.setOnClickListener {
            startActivity(intent2secondsc)
        }
        button2.setOnClickListener {
            startActivity(intentElseSc)
        }
//        button3.setOnClickListener {
//            startActivity(intent4fourthsc)
////            lifecycleScope.launch {
////                val a = EchonetLiteObject(
////                    InetAddress.getByName("192.168.2.118"),
////                    listOf(0x02, 0x90, 0x01),
////                    resources.assets
////                )
////                val ret =
////                    a.asyncGet("動作状態")?.let { it1 -> echonet.asyncWaitPacket(it1) }
////                if (ret == null) {
////                    return@launch
////                }
////                println(ret)
////                print(a.epcToString(ret.epc) + ":")
////                println(ret.edt?.let { it1 -> a.edtToString(ret.epc, it1) })
////            }
//        }
        button4.setOnClickListener {
            startActivity(intent4fourthsc)
//            lifecycleScope.launch {
//                echonet.asyncGetDeviceList()
//                echonet.deviceList.forEach { it ->
//                    val ret = it.asyncGet("動作状態")
//                    println("ret:$ret")
//                }
//                println("len:" + echonet.deviceList.size)

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
//            }
        }


    }
}