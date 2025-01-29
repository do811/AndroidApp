package com.example.layout.intentsample

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import android.view.View
import android.widget.Button
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.example.layout.MainActivity
import com.example.layout.R

class otherscreen1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otherscreen1)
        val backbutton = findViewById<Button>(R.id.button3)
        val bar = findViewById<SeekBar>(R.id.seekBar)
        var text = findViewById<TextView>(R.id.textView)
        val mainIntent = Intent(this, MainActivity::class.java)
        backbutton.setOnClickListener {
            finish()
        }
        bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                text.text = "${progress}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // ユーザーがシークバーをタッチし始めたときに呼び出されます。
                // 今回は何もする必要がないので、空のままにしておきます。
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // ユーザーがシークバーのタッチを離したときに呼び出されます。
                if (seekBar != null) {
                    mainIntent.putExtra("ColorIntData", seekBar.progress)
                }
            }
        }
        )
        fun onButtonClick(view: View) {
            startActivity(mainIntent)
        }
    }
}