package verion.desing.launcher.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import verion.desing.launcher.database.tables.Descriptions
import verion.desing.launcher.database.tables.Titles

@Dao
interface DescriptionsDao {
    @Query("SELECT * from descriptions")
    fun getAll(): List<Descriptions>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(languages: ArrayList<Descriptions>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(language: Descriptions)

    @Query("DELETE from titles")
    fun deleteAll()

    @Query("SELECT * FROM descriptions WHERE language= :id LIMIT 1")
    fun getOne(id: String): List<Descriptions>
}