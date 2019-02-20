package verion.desing.launcher.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pictures")
data class Pictures(
        @PrimaryKey(autoGenerate = true) var id: Int,
        var locale: String?,
        var picture: String,
        var pictureFocused: String
)
