package com.example.mypazhonictest.data.local.seed

import com.example.mypazhonictest.data.local.dao.LocationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds the location table once at first run.
 * Data: full Iran hierarchy (country, 31 states, counties, cities) from LocationSeedData.
 * IDs are Long (same as UserEntity); no UUID. No CRUD from UI — read-only after seed.
 */
object LocationSeeder {

    suspend fun seedIfNeeded(locationDao: LocationDao) = withContext(Dispatchers.IO) {
        if (locationDao.count() > 0) return@withContext
        locationDao.insertAll(LocationSeedData.list())
    }
}
