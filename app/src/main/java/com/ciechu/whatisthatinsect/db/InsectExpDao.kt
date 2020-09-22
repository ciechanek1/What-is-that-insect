package com.ciechu.whatisthatinsect.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.ciechu.whatisthatinsect.data.InsectForExp

@Dao
interface InsectExpDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(insectNameForExp: InsectForExp)

}