package com.example.layout.intentsample

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.SeekBar
import android.view.View
import android.widget.Button
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.layout.MainActivity
import com.example.layout.R

class otherscreen1 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otherscreen1)
        val backbutton = findViewById<Button>(R.id.button3)
        val bar = findViewById<SeekBar>(R.id.seekBar)
        var text = findViewById<TextView>(R.id.textView)
        val constraint = findViewById<ConstraintLayout>(R.id.background)
        val Allral = constraint.background
        val mainIntent = Intent(this, MainActivity::class.java)
        var maincolor = intent.getIntExtra("colorInt", 0xFFFFFF)
        bar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                text.text = "${progress}%"
                if (Allral is ColorDrawable) {
                    // ColorDrawable の場合、getColor() メソッドで色を取得できる
                    // color には色情報が int 型で格納されている (例: 0xFFFF0000 - 赤)
                    println("Background color: $maincolor")
                    val red = Color.red(maincolor)
                    val green = Color.green(maincolor)
                    val blue = Color.blue(maincolor)
                    val alpha: Int = progress//(progress / 100.0).toInt()
                    println("red: $red")
                    println(" green: $green")
                    println("blue: $blue")
                    println("alpha: $alpha")
                    constraint.setBackgroundColor(Color.argb(alpha, red, green, blue))
                }
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
        backbutton.setOnClickListener {
            startActivity(mainIntent)
        }
        fun onButtonClick(view: View) {
            startActivity(mainIntent)
        }
    }
}
