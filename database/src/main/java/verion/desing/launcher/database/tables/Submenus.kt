package verion.desing.launcher.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "submenus")
data class Submenus (
        @PrimaryKey(autoGenerate = false) var id :Int? = 0,
        @ColumnInfo(name = "button") var buttons :ArrayList<Button>?
):Serializable
