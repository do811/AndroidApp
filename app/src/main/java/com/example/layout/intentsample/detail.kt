package com.example.layout.intentsample;
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.layout.R

class detail : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail)
        val genebutton = findViewById<Button>(R.id.button4)
        genebutton.setOnClickListener{
            setContentView(R.layout.activity_sub)
        }
        fun onButtonClick(view: View){
            finish()
        }
    }
}