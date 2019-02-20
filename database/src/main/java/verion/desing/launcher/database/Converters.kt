package verion.desing.launcher.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import verion.desing.launcher.database.tables.*


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
    fun fromStringToArrayListButton(value: String): ArrayList<Button> {
        val listType = object : TypeToken<ArrayList<Button>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListButtonToString(list: ArrayList<Button>): String {
        val listType = object : TypeToken<List<Button>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }

    @TypeConverter
    fun fromArrayListStringToArrayListSubmenu(value: String): ArrayList<Submenus> {
        val listType = object : TypeToken<List<Submenus>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListSubmenuToArrayListString(list: ArrayList<Submenus>): String {
        val listType = object : TypeToken<ArrayList<Submenus>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }

    @TypeConverter
    fun fromStringToArrayListPictures(value: String): ArrayList<Pictures> {
        val listType = object : TypeToken<ArrayList<Pictures>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromPicturesToArrayListString(list: ArrayList<Pictures>): String {
        val listType = object : TypeToken<ArrayList<Pictures>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }

    @TypeConverter
    fun fromStringToArrayListLanguages(value: String): List<Languages> {
        val listType = object : TypeToken<List<Languages>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromArrayListLanguagesToString(list: List<Languages>): String {
        val listType = object : TypeToken<List<Languages>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }
}