package com.example.layout.Layouts

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.ELManager
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R
import kotlinx.coroutines.launch
import com.example.layout.ELObject

class LightLayout : DefaultLayout() {
    override fun isTargetEoj(obj: ELObject<Number>): Boolean {
        val eojList = listOf((listOf(0x02, 0x90)), (listOf(0x02, 0x91)))
        for (eoj in eojList) {
            if (obj.compareEoj(eoj)) {
                return true
            }
        }
        return false
    }
}