package com.ciechu.whatisthatinsect.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ciechu.whatisthatinsect.data.Insect
import com.ciechu.whatisthatinsect.data.InsectForExp

@Database(entities = [Insect::class, InsectForExp::class], version = 1)
abstract class InsectDatabase : RoomDatabase() {

    abstract fun insectDao(): InsectDao
    abstract fun insectExpDao(): InsectExpDao

}