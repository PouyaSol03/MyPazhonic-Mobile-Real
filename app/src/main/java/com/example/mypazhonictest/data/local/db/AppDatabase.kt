package com.example.mypazhonictest.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.mypazhonictest.data.local.dao.LocationDao
import com.example.mypazhonictest.data.local.dao.PanelDao
import com.example.mypazhonictest.data.local.dao.PanelFolderDao
import com.example.mypazhonictest.data.local.dao.UserDao
import com.example.mypazhonictest.data.local.entity.LocationEntity
import com.example.mypazhonictest.data.local.entity.PanelEntity
import com.example.mypazhonictest.data.local.entity.PanelFolderEntity
import com.example.mypazhonictest.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, LocationEntity::class, PanelEntity::class, PanelFolderEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(LocationTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun locationDao(): LocationDao
    abstract fun panelDao(): PanelDao
    abstract fun panelFolderDao(): PanelFolderDao
}
