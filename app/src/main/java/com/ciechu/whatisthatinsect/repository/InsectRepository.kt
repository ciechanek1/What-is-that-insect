package com.ciechu.whatisthatinsect.repository

import androidx.lifecycle.LiveData
import com.ciechu.whatisthatinsect.data.Insect

interface InsectRepository {

    suspend fun insert(insect: Insect)
    suspend fun update(insect: Insect)
    suspend fun delete(list: List<Insect>)
    fun getAllInsects(): LiveData<List<Insect>>
}