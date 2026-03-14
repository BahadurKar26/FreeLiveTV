package com.rancon.freelivetv.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromSeasonList(value: List<Season>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toSeasonList(value: String): List<Season> {
        val listType = object : TypeToken<List<Season>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
