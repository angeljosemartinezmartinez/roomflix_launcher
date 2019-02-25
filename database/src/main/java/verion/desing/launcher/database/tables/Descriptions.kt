package verion.desing.launcher.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "descriptions")
data class Descriptions (
        @PrimaryKey(autoGenerate = true) var id:Int,
        var language:String,
        var text:String
)