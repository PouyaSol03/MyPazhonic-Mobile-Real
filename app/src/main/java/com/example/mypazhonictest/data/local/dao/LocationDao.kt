package com.example.mypazhonictest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mypazhonictest.data.local.entity.LocationEntity
import com.example.mypazhonictest.data.local.entity.LocationType
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LocationEntity>)

    @Query("SELECT COUNT(*) FROM location")
    suspend fun count(): Int

    @Query("SELECT * FROM location WHERE type = :type ORDER BY sortOrder ASC, name ASC")
    fun getByType(type: LocationType): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location WHERE type = :type ORDER BY sortOrder ASC, name ASC")
    suspend fun getByTypeOnce(type: LocationType): List<LocationEntity>

    @Query("SELECT * FROM location WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    fun getByParentId(parentId: Long): Flow<List<LocationEntity>>

    @Query("SELECT * FROM location WHERE parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    suspend fun getByParentIdOnce(parentId: Long): List<LocationEntity>

    @Query("SELECT * FROM location WHERE type = :type AND parentId = :parentId ORDER BY sortOrder ASC, name ASC")
    suspend fun getByTypeAndParentIdOnce(type: LocationType, parentId: Long): List<LocationEntity>

    @Query("SELECT * FROM location WHERE id = :id")
    suspend fun getById(id: Long): LocationEntity?

    /** All cities under a state: cities whose parentId is a county whose parentId = stateId. */
    @Query("SELECT c.* FROM location c INNER JOIN location county ON c.parentId = county.id WHERE county.parentId = :stateId AND c.type = 'CITY' ORDER BY c.sortOrder ASC, c.name ASC")
    suspend fun getCitiesByStateId(stateId: Long): List<LocationEntity>

    @Query("SELECT * FROM location WHERE type = :type AND (code = :code OR (code IS NULL AND :code IS NULL)) LIMIT 1")
    suspend fun getByTypeAndCode(type: LocationType, code: String?): LocationEntity?
}
