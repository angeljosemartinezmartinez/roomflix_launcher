package verion.desing.launcher.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import verion.desing.launcher.database.daos.LanguageDao
import verion.desing.launcher.database.tables.Language

@TypeConverters(Converters::class)
@Database(entities = arrayOf(Language::class), version = 2)
abstract class AppDataBase : RoomDatabase(){
    abstract fun languageDao():LanguageDao

    companion object {
        private var INSTANCE: AppDataBase? = null

        fun getInstance(context: Context): AppDataBase? {
            if (INSTANCE == null) {
                synchronized(AppDataBase::class) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                            AppDataBase::class.java, "verionLauncher.db")
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