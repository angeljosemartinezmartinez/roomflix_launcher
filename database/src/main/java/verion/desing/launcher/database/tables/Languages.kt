package verion.desing.launcher.database.tables

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "languages")
data class Languages(
        @PrimaryKey(autoGenerate = false) var id: Int? = 0,
        @ColumnInfo(name = "nativeName") var nativeName: String?,
        @ColumnInfo(name = "code") var code: String?,
        @ColumnInfo(name = "picture") var picture: String?,
        @ColumnInfo(name = "isDefault") var isDefault: Boolean?,
        @ColumnInfo(name = "channel") var channel:String?

):Serializable