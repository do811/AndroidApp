package com.example.layout

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.os.Bundle
import android.os.NetworkOnMainThreadException
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import androidx.lifecycle.ViewModelProvider
import com.example.layout.intentsample.Execute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.net.InetAddress

//import java.util.FormatProcessor as FMT

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val genebutton = findViewById<Button>(R.id.button0)
        val detail_text = findViewById<TextView>(R.id.textview0)
        val context: Context = this
        val linearLayout = LinearLayout(context)
        val textView = TextView(detail_text.context)
        val button = Button(genebutton.context)

        //
//        textView.text = stub.ipAddress.address.joinToString { "%02x".format(it) }
//        button.text = stub.get("power").joinToString { "%02x".format(it) }
//        linearLayout.orientation = LinearLayout.VERTICAL
//        linearLayout.addView(textView)
//        linearLayout.addView(button)
//        setContentView(linearLayout)
        //        stub.setI("power", "on")
//        stub.setC("liteLevel", "100")
        fun on() {
            val stub =
                EchonetObject(
                    InetAddress.getByName("192.168.2.52"),
                    listOf(0x02, 0x90.toByte(), 0x01)
                )
            stub.setI("power", "on");
        }
        button.setOnClickListener {
            try {
                Execute.OnTask {
                    on()
                }
            } catch (e: java.lang.RuntimeException) {
                textView.text = ""
            }
        }
    }
}