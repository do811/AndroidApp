package com.example.layout.Layouts

import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.layout.EchonetLiteManager
import com.example.layout.EchonetLiteObject
import com.example.layout.ListAdapter
import com.example.layout.ListItem
import com.example.layout.R
import kotlinx.coroutines.launch

class AirConditionerLayout : DefaultLayout() {
    override fun isTargetEoj(obj: EchonetLiteObject<Number>): Boolean {
        val eojList = listOf((listOf(0x01, 0x30)))
        for (eoj in eojList) {
            if (obj.compareEoj(eoj)) {
                return true
            }
        }
        return false
    }
}