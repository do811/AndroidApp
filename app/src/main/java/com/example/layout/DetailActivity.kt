package com.example.layout

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DetailActivity : AppCompatActivity(), ListAdapter.OnSwitchClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        val genebutton = findViewById<Button>(R.id.inc_button)
        val data = mutableListOf<ListItem>(
            ListItem(1, "MainText1", "SubText1", true),
            ListItem(2, "MainText2", "SubText2", false),
            ListItem(3, "MainText3", "SubText3", true)
        )
        val recycView = findViewById<RecyclerView>(R.id.rccv)
        recycView.setHasFixedSize(true)
        recycView.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }

        genebutton.setOnClickListener {
            data.add(ListItem(4, "MainText4", "SubText4", false))
            recycView.adapter = ListAdapter(data)
        }
        fun onButtonClick(view: View) {
            finish()
        }
    }
}