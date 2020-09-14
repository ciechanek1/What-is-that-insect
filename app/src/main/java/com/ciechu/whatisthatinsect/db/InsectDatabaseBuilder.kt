package com.ciechu.whatisthatinsect.db

import android.content.Context
import androidx.room.Room

object InsectDatabaseBuilder {

        private var instance: InsectDatabase? = null

        fun getInstance(context: Context): InsectDatabase {
            if (instance == null){
                synchronized(InsectDatabase::class){
                    instance = roomBuild(context)
                }
            }
            return instance!!
        }

    private fun roomBuild(context: Context) =
        Room.databaseBuilder(context,
            InsectDatabase::class.java,
            "insect_database")
            .fallbackToDestructiveMigration()
            .build()
}