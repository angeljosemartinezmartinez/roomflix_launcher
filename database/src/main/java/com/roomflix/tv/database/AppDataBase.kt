package com.roomflix.tv.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.roomflix.tv.database.daos.*
import com.roomflix.tv.database.tables.*

@TypeConverters(Converters::class)
@Database(entities = arrayOf(Languages::class,
        Submenus::class,
        Templates::class,
        Button::class,
        Translations::class,
        InfoCards::class,
        Update::class,
        Configuration::class
        ), version = 31, exportSchema = false)
abstract class AppDataBase : RoomDatabase(){
    abstract fun languageDao():LanguageDao
    abstract fun templateDao(): TemplateDao
    abstract fun submenuDao(): SubmenuDao
    abstract fun buttonDao(): ButtonDao
    abstract fun picturesDao(): PicturesDao
    abstract fun infoCardDao(): InfoCardDao
    abstract fun updateDao(): UpdateDao
    abstract fun configurationDao() : ConfigurationDao


    companion object {
        private var INSTANCE: AppDataBase? = null

        fun getInstance(context: Context): AppDataBase? {
            if (INSTANCE == null) {
                synchronized(AppDataBase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            AppDataBase::class.java, "roomflix.db")
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build()
                }
            }
            return INSTANCE
        }

        fun destroyInstance() {
            INSTANCE = null
        }
    }

}