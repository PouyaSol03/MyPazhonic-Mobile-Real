package com.example.mypazhonictest.data.local.db

import androidx.room.TypeConverter
import com.example.mypazhonictest.data.local.entity.LocationType

/** Room type converter for [LocationType] ↔ String. */
class LocationTypeConverter {

    @TypeConverter
    fun fromString(value: String?): LocationType? = value?.let { LocationType.valueOf(it) }

    @TypeConverter
    fun toString(type: LocationType?): String? = type?.name
}
