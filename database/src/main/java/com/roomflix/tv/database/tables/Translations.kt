package com.roomflix.tv.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pictures")
data class Translations(
        @PrimaryKey(autoGenerate = true) var id: Int,
        var locale: String?,
        var picture: String,
        var pictureFocused: String,
        var functionType: Int? = 0,
        var functionTarget: String? = ""
)
