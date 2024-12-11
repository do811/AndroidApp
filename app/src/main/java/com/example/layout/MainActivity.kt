package com.example.layout

import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.layout.Layouts.AirConditionerLayout
import com.example.layout.Layouts.ElectLayout
import com.example.layout.Layouts.AddMachineLayout
import com.example.layout.Layouts.ElseLayout
import com.example.layout.intentsample.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val button = findViewById<Button>(R.id.button0)//右上
        val button1 = findViewById<Button>(R.id.button1)//左上
        val button2 = findViewById<Button>(R.id.button2)//右下
        val button3 = findViewById<Button>(R.id.button3)//左下
        val button4 = findViewById<Button>(R.id.button4)//下の設定ボタン
        val displayMetrics = DisplayMetrics()

        val intent2secondsc = Intent(
            this@MainActivity,
            ElectLayout::class.java//真ん中左
        )
        val intent3thridsc = Intent(
            this@MainActivity,
            AirConditionerLayout::class.java//右下
        )
        val intent4fourthsc = Intent(
            this@MainActivity,
            AddMachineLayout::class.java
        )
        val intentElseSc = Intent(
            this@MainActivity,
            ElseLayout::class.java
        )
        button.setOnClickListener {
            startActivity(intent3thridsc)
        }
        button1.setOnClickListener {
            startActivity(intent2secondsc)
        }
        button2.setOnClickListener {
            startActivity(intentElseSc)
        }
        button3.setOnClickListener {
            startActivity(intent4fourthsc)
        }
        button4.setOnClickListener {
            startActivity(intent2secondsc)
        }

    }
}