package verion.desing.launcher.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import verion.desing.launcher.database.models.Language


class Converters {

    @TypeConverter
    fun fromStringToArrayList(value: String): ArrayList<String> {
        val listType = object : TypeToken<ArrayList<String>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListToString(list: ArrayList<String>): String {
        val listType = object : TypeToken<ArrayList<String>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list,listType)
    }

    @TypeConverter
    fun fromStringToArrayListLanguage(value: String): ArrayList<Language> {
        val listType = object : TypeToken<ArrayList<Language>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListLanguageToString(list: ArrayList<Language>): String {
        val listType = object : TypeToken<ArrayList<Language>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }
}