package com.ciechu.whatisthatinsect.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ciechu.whatisthatinsect.data.Insect

@Database(entities = [Insect::class], version = 1)
abstract class InsectDatabase : RoomDatabase() {

    abstract fun insectDao(): InsectDao
}