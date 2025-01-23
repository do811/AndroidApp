//package com.example.layout.intentsample
//import androidx.appcompat.app.AppCompatActivit
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModel.*
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import com.example.layout.EchonetObject
//import java.net.InetAddress
//
//class Execute : AppCompatActivity() {
//    fun OnTask() {
//        lifecycleScope.launch {
//            withContext(Dispatchers.IO) {
//                var manager = EchonetManager()
//                val monoLite = EchonetObject(
//                    InetAddress.getByName("192,168.2.50")
//                    listOf(0x02, 0x90, 0x01)
//                )
//                func()
//            }
//            withContext(Dispatchers.Main) {
//
//            }
//        }
//        runBlocking {
//
//        }
//    }
//
//}