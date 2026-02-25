package com.example.mypazhonictest.data.local.entity

/**
 * Hierarchy level for [LocationEntity].
 * Order reflects depth: COUNTRY (root) → STATE → COUNTY → CITY.
 */
enum class LocationType {
    COUNTRY,
    STATE,
    COUNTY,
    CITY
}
