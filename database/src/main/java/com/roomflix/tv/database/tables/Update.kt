package com.roomflix.tv.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "updateTable")
data class Update (
        @PrimaryKey(autoGenerate = false) var baseUrl: String = "",
        var date: String? = "",
        var pkg: String? = "",
        var apk: String? = ""
)
