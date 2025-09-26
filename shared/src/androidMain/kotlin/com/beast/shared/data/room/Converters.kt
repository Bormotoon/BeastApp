package com.beast.shared.data.room

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@ProvidedTypeConverter
class ListConverters(private val json: Json = Json) {
    @TypeConverter
    fun fromStringList(list: List<String>?): String = json.encodeToString(ListSerializer(String.serializer()), list ?: emptyList())

    @TypeConverter
    fun toStringList(data: String?): List<String> =
        if (data.isNullOrEmpty()) emptyList()
        else json.decodeFromString(ListSerializer(String.serializer()), data)
}

