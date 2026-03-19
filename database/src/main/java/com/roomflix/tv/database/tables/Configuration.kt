package com.roomflix.tv.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuration")
data class Configuration (
        @PrimaryKey(autoGenerate = false)var timeZone: Int
)
