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
            startActivity(intent2secondsc)
        }
        button4.setOnClickListener {
            startActivity(intent2secondsc)
        }

    }
}