package com.roomflix.tv.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roomflix.tv.database.models.TranslationSubmenu
import java.io.Serializable

@Entity(tableName = "submenus")
data class Submenus (
        @PrimaryKey(autoGenerate = false) var id :Int? = 0,
        @ColumnInfo(name = "translation") var translations: ArrayList<TranslationSubmenu>?,
        @ColumnInfo(name = "button") var buttons :ArrayList<Button>?
):Serializable
