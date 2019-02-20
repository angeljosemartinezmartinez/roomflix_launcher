package verion.desing.launcher.database.daos

import androidx.room.*
import verion.desing.launcher.database.tables.Button

@Dao
interface ButtonDao {
    @Query("SELECT * from button")
    fun getAll(): List<Button>

    @Insert(onConflict = OnConflictStrategy.ROLLBACK)
    fun insert(button: Button)

    @Query("DELETE from button")
    fun deleteAll()

    @Update
    fun updateButton(b: Button): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(childbuttons: ArrayList<Button>)

    /*@Query("SELECT * FROM button WHERE picture_code= :pic AND focus_code = :focusPic")
    fun getOneFromLanguage(pic: String,focusPic: String): List<Button>*/
}