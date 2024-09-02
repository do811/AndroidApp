package com.example.layout.intentsample;

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.layout.R


class Sub : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)
        val backbutton = findViewById<Button>(R.id.button8)
        backbutton.setOnClickListener {
            finish()
        }
    }
}