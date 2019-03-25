package verion.desing.launcher.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import verion.desing.launcher.database.models.Child
import verion.desing.launcher.database.models.Translation
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
        val listType = object : TypeToken<List<Submenus>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }

    @TypeConverter
    fun fromStringToArrayListPictures(value: String): ArrayList<Translations> {
        val listType = object : TypeToken<ArrayList<Translations>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromPicturesToArrayListString(list: ArrayList<Translations>): String {
        val listType = object : TypeToken<ArrayList<Translations>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }

    @TypeConverter
    fun fromStringToArrayListTranslation(value: String): ArrayList<Translation> {
        val listType = object : TypeToken<List<Translation>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromTranslationToArrayListString(list: ArrayList<Translation>): String {
        val listType = object : TypeToken<List<Translation>>() {
        }.type
        val gson = Gson()
        return gson.toJson(list, listType)
    }

    @TypeConverter
    fun fromStringToArrayListInfoCards(value: String): ArrayList<InfoCards> {
        val listType = object : TypeToken<List<InfoCards>>() {
        }.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromInfoCardsToArrayListString(list: ArrayList<InfoCards>): String {
        val listType = object : TypeToken<List<InfoCards>>() {
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