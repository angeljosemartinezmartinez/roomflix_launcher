package verion.desing.launcher.database.tables


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "templates")
data class Templates (
        @PrimaryKey(autoGenerate = false) var code :Int = 0,
        var logo: String? = "",
        var miniLogo: String? = "",
        var background: String? = "",
        var buttons: ArrayList<Button>?
): Serializable
