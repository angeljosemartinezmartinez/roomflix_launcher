package verion.desing.launcher.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import verion.desing.launcher.database.tables.Languages
import verion.desing.launcher.database.tables.Templates

@Dao
interface TemplateDao {

    @Query("SELECT * from templates")
    fun getAll(): Templates

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(languages: Templates)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(language: Templates)

    @Query("DELETE from templates")
    fun deleteAll()

    @Query("SELECT * FROM templates WHERE code= :id LIMIT 1")
    fun getOne(id: String): Templates
}