package com.tdpro1612.textstoryreader.database

import androidx.room.TypeConverter

class Converters {

    // 1. Mapping: Enum -> String (Lúc Room GHI xuống DB)
    @TypeConverter
    fun fromBookStatus(status: BookStatus): String {
        return status.name
    }

    // 2. Mapping: String -> Enum (Lúc Room ĐỌC từ DB lên App)
    @TypeConverter
    fun toBookStatus(value: String): BookStatus {
        return try {
            BookStatus.valueOf(value)
        } catch (e: Exception) {
            BookStatus.UNKNOWN
        }
    }
}