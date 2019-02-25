package verion.desing.launcher.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import verion.desing.launcher.database.tables.InfoCards
import verion.desing.launcher.database.tables.Titles

@Dao
interface TitlesDao {

    @Query("SELECT * from titles")
    fun getAll(): List<Titles>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(languages: ArrayList<Titles>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(language: Titles)

    @Query("DELETE from titles")
    fun deleteAll()

    @Query("SELECT * FROM titles WHERE language= :id LIMIT 1")
    fun getOne(id: String): List<Titles>
}