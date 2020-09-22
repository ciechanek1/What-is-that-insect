package com.ciechu.whatisthatinsect.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ciechu.whatisthatinsect.data.InsectForExp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpViewModel(app: Application): AndroidViewModel(app) {

    private val repository = ExpRepository(app)

    fun insert(insectNameForExp: InsectForExp){
        CoroutineScope(Dispatchers.IO).launch { repository.insert(insectNameForExp) }
    }
}