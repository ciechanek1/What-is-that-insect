package com.ciechu.whatisthatinsect.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ciechu.whatisthatinsect.data.Insect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList

class InsectViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = Repository(app)
    val allInsects = repository.getAllInsects()

// Multi Select
    var multiSelectMode = false
    val selectedInsects = ArrayList<Insect>()

    fun insert(insect: Insect){
        CoroutineScope(Dispatchers.IO).launch { repository.insert(insect) }
    }
    fun delete(listInsects: List<Insect>){
        CoroutineScope(Dispatchers.IO).launch { repository.delete(listInsects) }
    }
}