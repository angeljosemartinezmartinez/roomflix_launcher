package verion.desing.launcher.database.daos

import androidx.room.*
import verion.desing.launcher.database.tables.Update

@Dao
interface UpdateDao {
    @Query("SELECT * from updateTable")
    fun getAll(): List<Update>

    @Insert(onConflict = OnConflictStrategy.ROLLBACK)
    fun insert(button: Update)

}