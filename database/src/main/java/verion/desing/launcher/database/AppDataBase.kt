package verion.desing.launcher.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import verion.desing.launcher.database.daos.ButtonDao
import verion.desing.launcher.database.daos.LanguageDao
import verion.desing.launcher.database.daos.SubmenuDao
import verion.desing.launcher.database.daos.TemplateDao
import verion.desing.launcher.database.tables.Button
import verion.desing.launcher.database.tables.Languages
import verion.desing.launcher.database.tables.Submenus
import verion.desing.launcher.database.tables.Templates

@TypeConverters(Converters::class)
@Database(entities = arrayOf(Languages::class,
        Submenus::class,
        Templates::class,
        Button::class), version = 6, exportSchema = false)
abstract class AppDataBase : RoomDatabase(){
    abstract fun languageDao():LanguageDao
    abstract fun templateDao(): TemplateDao
    abstract fun submenuDao(): SubmenuDao
    abstract fun buttonDao(): ButtonDao

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