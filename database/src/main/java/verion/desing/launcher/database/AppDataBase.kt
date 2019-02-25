package verion.desing.launcher.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import verion.desing.launcher.database.daos.*
import verion.desing.launcher.database.tables.*

@TypeConverters(Converters::class)
@Database(entities = arrayOf(Languages::class,
        Submenus::class,
        Templates::class,
        Button::class,
        Translations::class,
        InfoCards::class,
        Titles::class,
        Descriptions::class), version = 24, exportSchema = false)
abstract class AppDataBase : RoomDatabase(){
    abstract fun languageDao():LanguageDao
    abstract fun templateDao(): TemplateDao
    abstract fun submenuDao(): SubmenuDao
    abstract fun buttonDao(): ButtonDao
    abstract fun picturesDao(): PicturesDao
    abstract fun infoCardDao(): InfoCardDao
    abstract fun titlesDao(): TitlesDao
    abstract fun descriptionsDao(): DescriptionsDao

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