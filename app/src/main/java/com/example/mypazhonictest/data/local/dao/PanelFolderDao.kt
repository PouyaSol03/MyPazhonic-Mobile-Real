package com.example.mypazhonictest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mypazhonictest.data.local.entity.PanelFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelFolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: PanelFolderEntity): Long

    @Update
    suspend fun update(folder: PanelFolderEntity)

    @Query("SELECT * FROM panel_folder WHERE id = :id")
    suspend fun getById(id: Long): PanelFolderEntity?

    @Query("SELECT * FROM panel_folder WHERE userId = :userId ORDER BY sortOrder ASC, id ASC")
    fun observeByUserId(userId: Long): Flow<List<PanelFolderEntity>>

    @Query("SELECT * FROM panel_folder WHERE userId = :userId ORDER BY sortOrder ASC, id ASC")
    suspend fun getByUserId(userId: Long): List<PanelFolderEntity>

    @Query("DELETE FROM panel_folder WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM panel_folder WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: Long)
}
