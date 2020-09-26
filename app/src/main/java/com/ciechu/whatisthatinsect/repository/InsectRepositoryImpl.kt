package com.ciechu.whatisthatinsect.repository

import android.app.Application
import androidx.lifecycle.LiveData
import com.ciechu.whatisthatinsect.data.Insect
import com.ciechu.whatisthatinsect.db.InsectDatabaseBuilder

class InsectRepositoryImpl(app: Application) : InsectRepository {
    private val insectDao = InsectDatabaseBuilder.getInstance(app.applicationContext).insectDao()

   override suspend fun insert(insect: Insect){
        insectDao.insert(insect)
    }
   override suspend fun update(insect: Insect){
        insectDao.update(insect)
    }
   override suspend fun delete(list: List<Insect>){
        insectDao.delete(list)
    }
    override fun getAllInsects(): LiveData<List<Insect>> {
       return insectDao.getAllInsect()
    }
}