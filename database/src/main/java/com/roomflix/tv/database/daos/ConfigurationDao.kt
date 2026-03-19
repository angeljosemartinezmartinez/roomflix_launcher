package com.roomflix.tv.database.daos

import androidx.room.*
import com.roomflix.tv.database.tables.Configuration

@Dao
interface ConfigurationDao {
    @Query("SELECT * from configuration")
    fun getAll(): List<Configuration>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(button: Configuration)

    @Query("DELETE from configuration")
    fun deleteAll()

    @Update
    fun updateConfiguration(c: Configuration): Int


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(c: ArrayList<Configuration>)
}