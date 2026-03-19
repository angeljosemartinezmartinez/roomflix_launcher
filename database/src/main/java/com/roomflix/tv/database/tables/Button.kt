package com.roomflix.tv.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "button", foreignKeys = arrayOf(
)
)
data class Button(
        @PrimaryKey(autoGenerate = false)
        var id: Int,
        var postion: Int? = 0,
        var pictures: ArrayList<Translations>?
) : Serializable