package com.example.mypazhonictest.data.repository

import com.example.mypazhonictest.data.local.dao.LocationDao
import com.example.mypazhonictest.data.local.entity.LocationEntity
import com.example.mypazhonictest.data.local.entity.LocationType
import javax.inject.Inject

/**
 * Read-only access to seeded location data (country, state, county, city).
 */
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao
) {

    /** All locations of a given type (e.g. all states). */
    suspend fun getByType(type: LocationType): List<LocationEntity> =
        locationDao.getByTypeOnce(type)

    /** All direct children of a parent (e.g. states of country 1, or counties of state 10). */
    suspend fun getByParentId(parentId: Long): List<LocationEntity> =
        locationDao.getByParentIdOnce(parentId)

    /** Locations of a given type with a specific parent (e.g. STATE with parentId = 1). */
    suspend fun getByTypeAndParentId(type: LocationType, parentId: Long): List<LocationEntity> =
        locationDao.getByTypeAndParentIdOnce(type, parentId)

    suspend fun getById(id: Long): LocationEntity? =
        locationDao.getById(id)

    /** All cities under a state (State → County → City). */
    suspend fun getCitiesByStateId(stateId: Long): List<LocationEntity> =
        locationDao.getCitiesByStateId(stateId)
}
