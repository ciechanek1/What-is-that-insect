package com.ciechu.whatisthatinsect.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Insect_exp")
data class InsectForExp(
    @PrimaryKey(autoGenerate = false)
    val name: String
) {
}