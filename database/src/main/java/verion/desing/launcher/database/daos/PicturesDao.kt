package verion.desing.launcher.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import verion.desing.launcher.database.tables.Translations

@Dao
interface PicturesDao {

    @Query("SELECT * from pictures")
    fun getAll():List<Translations>

    @Insert(onConflict = OnConflictStrategy.ROLLBACK)
    fun insertAll(pictures: ArrayList<Translations>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(translations: Translations)

    @Query("DELETE from pictures")
    fun deleteAll()

    @Query("SELECT * from pictures WHERE locale= :id ")
    fun getOne(id: String):List<Translations>
}