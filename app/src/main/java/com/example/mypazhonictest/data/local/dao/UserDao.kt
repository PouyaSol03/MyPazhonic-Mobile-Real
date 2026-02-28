package com.example.mypazhonictest.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mypazhonictest.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM user ORDER BY id DESC LIMIT 1")
    fun observeLatestUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: Long): UserEntity?

    @Query("SELECT * FROM user ORDER BY id DESC LIMIT 1")
    suspend fun getLatestUser(): UserEntity?

    @Query("DELETE FROM user")
    suspend fun deleteAll()
}
