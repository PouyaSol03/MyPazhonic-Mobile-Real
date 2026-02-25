package com.example.mypazhonictest.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mypazhonictest.data.local.dao.UserDao
import com.example.mypazhonictest.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
