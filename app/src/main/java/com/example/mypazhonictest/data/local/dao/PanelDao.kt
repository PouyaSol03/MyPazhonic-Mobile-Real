package com.example.mypazhonictest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mypazhonictest.data.local.entity.PanelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(panel: PanelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(panels: List<PanelEntity>)

    @Update
    suspend fun update(panel: PanelEntity)

    @Query("SELECT * FROM panel WHERE id = :id")
    suspend fun getById(id: Long): PanelEntity?

    @Query("SELECT * FROM panel WHERE id = :id")
    fun observeById(id: Long): Flow<PanelEntity?>

    @Query("SELECT * FROM panel WHERE userId = :userId ORDER BY create_at DESC")
    fun observeByUserId(userId: Long): Flow<List<PanelEntity>>

    @Query("SELECT * FROM panel WHERE userId = :userId ORDER BY create_at DESC")
    suspend fun getByUserId(userId: Long): List<PanelEntity>

    /** For folderId == null returns uncategorized panels; else panels in that folder. */
    @Query("SELECT * FROM panel WHERE userId = :userId AND ((:folderId IS NULL AND folderId IS NULL) OR folderId = :folderId) ORDER BY name")
    suspend fun getByUserIdAndFolderId(userId: Long, folderId: Long?): List<PanelEntity>

    @Query("SELECT * FROM panel ORDER BY create_at DESC")
    fun observeAll(): Flow<List<PanelEntity>>

    @Query("SELECT * FROM panel ORDER BY create_at DESC")
    suspend fun getAll(): List<PanelEntity>

    @Query("SELECT * FROM panel WHERE locationId = :locationId ORDER BY name")
    suspend fun getByLocationId(locationId: Long): List<PanelEntity>

    @Query("SELECT * FROM panel WHERE locationId = :locationId ORDER BY name")
    fun observeByLocationId(locationId: Long): Flow<List<PanelEntity>>

    @Query("SELECT * FROM panel WHERE isActive = 1 ORDER BY name")
    suspend fun getActive(): List<PanelEntity>

    @Query("UPDATE panel SET folderId = NULL, updated_at = :now WHERE folderId = :folderId")
    suspend fun clearFolderIdForFolder(folderId: Long, now: Long)

    @Query("DELETE FROM panel WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM panel")
    suspend fun deleteAll()
}
