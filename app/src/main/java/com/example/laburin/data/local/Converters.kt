package com.example.laburin.data.local

import androidx.room.TypeConverter
import java.time.Instant

/** Instants are stored as epoch milliseconds (UTC). */
class Converters {
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
