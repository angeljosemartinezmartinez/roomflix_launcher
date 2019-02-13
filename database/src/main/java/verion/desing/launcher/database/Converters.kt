package verion.desing.launcher.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import verion.desing.launcher.database.models.Data
import verion.desing.launcher.database.models.TextsApp


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
    fun fromStringToArrayListData(value: String): ArrayList<Data> {
        val listType = object : TypeToken<ArrayList<Data>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListDataToString(list: ArrayList<Data>): String {
        val listType = object : TypeToken<ArrayList<Data>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }

    @TypeConverter
    fun fromStringToTextsApp(value: String): TextsApp {
        val listType = object : TypeToken<TextsApp>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromTextsAppToString(list: TextsApp): String {
        val listType = object : TypeToken<TextsApp>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }
}