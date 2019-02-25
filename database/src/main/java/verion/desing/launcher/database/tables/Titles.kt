package verion.desing.launcher.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "titles")
data class Titles (
        @PrimaryKey(autoGenerate = true) var id:Int,
        var language:String,
        var text:String
)