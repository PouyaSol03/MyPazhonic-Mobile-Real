package com.example.mypazhonictest.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Single-table hierarchy for geographic locations.
 * Types: COUNTRY (parentId=null), STATE (parent=country), COUNTY (parent=state), CITY (parent=county or state).
 * Seeded once at app init; no CRUD from UI.
 */
@Entity(
    tableName = "location",
    indices = [
        Index(value = ["type"]),
        Index(value = ["parentId"]),
        Index(value = ["code"])
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val name: String,
    val type: LocationType,
    val parentId: Long? = null,

    /** Optional code: ISO 3166-1 alpha-2 for country (e.g. "IR"), state/county/city code if needed. */
    val code: String? = null,

    /** Display order within same type/parent (lower = first). */
    val sortOrder: Int = 0
)
