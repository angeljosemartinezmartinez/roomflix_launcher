package verion.desing.launcher.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import verion.desing.launcher.database.tables.InfoCards

@Dao
interface InfoCardDao {
    @Query("SELECT * from infoCards")
    fun getAll(): List<InfoCards>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(languages: ArrayList<InfoCards>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(language: InfoCards)

    @Query("DELETE from languages")
    fun deleteAll()

    @Query("SELECT * FROM infoCards WHERE id= :id LIMIT 1")
    fun getOne(id: Int): InfoCards
}