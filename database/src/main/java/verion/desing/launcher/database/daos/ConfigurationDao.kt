package verion.desing.launcher.database.daos

import androidx.room.*
import verion.desing.launcher.database.tables.Configuration

@Dao
interface ConfigurationDao {
    @Query("SELECT * from configuration")
    fun getAll(): List<Configuration>

    @Insert(onConflict = OnConflictStrategy.ROLLBACK)
    fun insert(button: Configuration)

    @Query("DELETE from configuration")
    fun deleteAll()

    @Update
    fun updateConfiguration(c: Configuration): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(c: ArrayList<Configuration>)
}