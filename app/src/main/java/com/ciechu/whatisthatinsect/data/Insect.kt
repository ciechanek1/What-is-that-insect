package com.ciechu.whatisthatinsect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "insect_table")
data class Insect(
    val name: String,
    val image: String,
    val date: String,
    var isSelected: Boolean = false,
    var hadCongrats: Boolean = false) {

    @PrimaryKey(autoGenerate = false)
    var rowId = name
}