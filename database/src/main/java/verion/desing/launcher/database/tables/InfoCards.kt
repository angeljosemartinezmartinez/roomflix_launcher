package verion.desing.launcher.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey
import verion.desing.launcher.database.models.Child
import verion.desing.launcher.database.models.Translation

@Entity(tableName = "infoCards")
data class InfoCards (
        @PrimaryKey(autoGenerate = false) var id: Int,
        var translations: ArrayList<Translation>? = ArrayList(),
        var child: ArrayList<Child>? = ArrayList()
)
