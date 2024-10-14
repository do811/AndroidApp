package com.example.layout.intentsample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModel.*
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class Execute : ViewModel() {
    fun OnTask(func: suspend () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                func()
            }
            withContext(Dispatchers.Main) {

            }
        }
        runBlocking {
            launch(Dispatchers.IO) {
                func()
            }
        }
    }

}