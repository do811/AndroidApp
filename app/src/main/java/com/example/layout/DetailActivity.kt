package com.example.layout

import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val stub = StubEchonetObject("192.168.2.52", listOf(0x02, 0x90, 0x01))
        stub.setI("power", "on")
        stub.setC("lightLevel", "100")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val genebutton = findViewById<Button>(R.id.button0)
        val detail_text = findViewById<TextView>(R.id.textview0)

        val context: Context = this
        val linearLayout = LinearLayout(context)
        val textView = TextView(context)
        val button = Button(context)
        textView.text = stub.ipAddress
        button.text = stub.get("power").joinToString { "%02x".format(it) }
        linearLayout.orientation = LinearLayout.VERTICAL
        linearLayout.addView(textView)
        linearLayout.addView(button)
        setContentView(linearLayout)
        fun onButtonClick(view: View) {
            finish()
        }
    }
}