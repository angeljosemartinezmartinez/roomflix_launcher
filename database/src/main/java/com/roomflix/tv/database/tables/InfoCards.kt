package com.roomflix.tv.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.roomflix.tv.database.models.Child
import com.roomflix.tv.database.models.Translation
import java.io.Serializable

@Entity(tableName = "infoCards")
data class InfoCards (
        @PrimaryKey(autoGenerate = false) var id: Int,
        var translations: ArrayList<Translation>? = ArrayList(),
        var child: ArrayList<InfoCards>? = null
) : Serializable
