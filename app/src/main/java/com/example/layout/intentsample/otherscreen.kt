package com.example.layout.intentsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.example.layout.R

class otherscreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)
        val backbutton = findViewById<Button>(R.id.button8)
        backbutton.setOnClickListener{
            finish()
        }
        fun onButtonClick(view: View){
            finish()
        }
    }
}