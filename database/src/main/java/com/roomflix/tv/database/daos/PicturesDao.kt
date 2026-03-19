package com.roomflix.tv.database.daos

import androidx.room.*
import com.roomflix.tv.database.tables.Translations

@Dao
interface PicturesDao {

    @Query("SELECT * from pictures")
    fun getAll():List<Translations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(pictures: ArrayList<Translations>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(translations: Translations)

    @Update
    fun updateAll(pictures: List<Translations>):Int

    @Query("DELETE from pictures")
    fun deleteAll()

    @Query("SELECT * from pictures WHERE locale= :id ")
    fun getOne(id: String):List<Translations>
}