package com.ciechu.whatisthatinsect.viewmodels

import android.app.Application
import androidx.lifecycle.LiveData
import com.ciechu.whatisthatinsect.data.Insect
import com.ciechu.whatisthatinsect.db.InsectDatabaseBuilder

class InsectRepository(app: Application) {
    private val insectDao = InsectDatabaseBuilder.getInstance(app.applicationContext).insectDao()

    suspend fun insert(insect: Insect){
        insectDao.insert(insect)
    }
    suspend fun update(insect: Insect){
        insectDao.update(insect)
    }
    suspend fun delete(list: List<Insect>){
        insectDao.delete(list)
    }
     fun getAllInsects(): LiveData<List<Insect>> {
       return insectDao.getAllInsect()
    }
}