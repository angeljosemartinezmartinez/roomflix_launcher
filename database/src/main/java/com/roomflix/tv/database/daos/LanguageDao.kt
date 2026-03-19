package com.roomflix.tv.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.roomflix.tv.database.tables.Languages

@Dao
interface LanguageDao {
    @Query("SELECT * from languages")
    fun getAll():List<Languages>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(languages: ArrayList<Languages>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(language: Languages)

    @Query("DELETE from languages")
    fun deleteAll()

    @Query("SELECT * FROM languages WHERE code= :id LIMIT 1")
    fun getOne(id: String): Languages
}