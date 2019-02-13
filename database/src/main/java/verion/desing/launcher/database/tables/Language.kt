package verion.desing.launcher.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import verion.desing.launcher.database.models.Data

@Entity(tableName = "languages")
data class Language (
        @PrimaryKey(autoGenerate = false)
        var code: Int? = 0,
        var baseUrl: String? = "",
        var data: ArrayList<Data>? = ArrayList()

)
