package verion.desing.launcher.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import verion.desing.launcher.database.tables.Submenus

@Dao
interface SubmenuDao {
    @Query("SELECT * from submenus")
    fun getAll(): List<Submenus>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(submenus: ArrayList<Submenus>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(submenus: Submenus)

    @Query("DELETE from submenus")
    fun deleteAll()

    @Query("SELECT * FROM submenus WHERE id= :id LIMIT 1")
    fun getOne(id: String): Submenus
}