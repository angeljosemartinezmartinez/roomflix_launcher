package com.roomflix.tv.database.daos

import androidx.room.*
import com.roomflix.tv.database.tables.Update

@Dao
interface UpdateDao {
    @Query("SELECT * from updateTable")
    fun getAll(): List<Update>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(button: Update)

}