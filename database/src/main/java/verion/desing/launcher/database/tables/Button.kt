package verion.desing.launcher.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import verion.desing.launcher.database.models.FocusPictures
import verion.desing.launcher.database.models.Pictures
import java.io.Serializable

@Entity(tableName =  "button"
)
data class Button(
        @PrimaryKey(autoGenerate = false)
        var id: Int,
        var postion: Int? = 0,
        var pictures: Pictures?,
        var focusPictures: FocusPictures?,
        var functionType: Int? = 0,
        var functionTarget: String? = "",
        var codeLanguage: String? = ""
):Serializable