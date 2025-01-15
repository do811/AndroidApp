package com.example.layout.Layouts

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.ELObject
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R
import kotlinx.coroutines.launch

class ElseLayout : DefaultLayout() {
    override fun isTargetEoj(obj: ELObject<Number>): Boolean {
        val eojList =
            listOf(listOf(0x01, 0x30), listOf(0x02, 0x90), listOf(0x02, 0x91), listOf(0x0E, 0xF0))
        for (eoj in eojList) {
            if (obj.compareEoj(eoj)) {
                return false
            }
        }
        return true
    }
}