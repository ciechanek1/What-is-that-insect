package com.ciechu.whatisthatinsect.viewmodels

import android.app.Application
import com.ciechu.whatisthatinsect.data.InsectForExp
import com.ciechu.whatisthatinsect.db.InsectDatabaseBuilder

class ExpRepository(app: Application) {

    private val insectForExp = InsectDatabaseBuilder.getInstance(app).insectExpDao()

    suspend fun insert(insectNameForExp: InsectForExp){
        insectForExp.insert(insectNameForExp)
    }
}