package verion.desing.launcher.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import verion.desing.launcher.database.tables.Language

@Dao
interface LanguageDao {
    @Query("SELECT * from languages")
    fun getAll():Language

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(languages: Language)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(language: Language)

    @Query("DELETE from languages")
    fun deleteAll()

    @Query("SELECT * FROM languages WHERE code= :id LIMIT 1")
    fun getOne(id: String): Language
}