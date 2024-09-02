package com.example.layout

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        val genebutton = findViewById<Button>(R.id.button0)
        val detail_text = findViewById<TextView>(R.id.textview0)

        genebutton.setOnClickListener {
            detail_text.text = "Button_name"
        }
        fun onButtonClick(view: View) {
            finish()
        }
    }
}