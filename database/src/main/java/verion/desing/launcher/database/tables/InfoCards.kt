package verion.desing.launcher.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "infoCards")
data class InfoCards (
        @PrimaryKey(autoGenerate = false) var id: Int,
        var titles: ArrayList<Titles>,
        var descriptions: ArrayList<Descriptions>,
        var picture: String
)
