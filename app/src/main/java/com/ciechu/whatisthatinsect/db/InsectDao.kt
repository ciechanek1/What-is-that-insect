package com.ciechu.whatisthatinsect.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ciechu.whatisthatinsect.data.Insect

@Dao
interface InsectDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert (insect: Insect)

    @Update
    suspend fun update(insect: Insect)

    @Delete
    suspend fun delete (insect: List<Insect>)

    @Query("SELECT * FROM insect_table")
    fun getAllInsect (): LiveData<List<Insect>>
}