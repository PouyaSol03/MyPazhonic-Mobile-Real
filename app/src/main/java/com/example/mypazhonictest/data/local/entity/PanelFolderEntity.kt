package com.example.mypazhonictest.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User-defined folder (category) for grouping panels, Telegram-style.
 */
@Entity(
    tableName = "panel_folder",
    indices = [Index(value = ["userId"])]
)
data class PanelFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val userId: Long,
    val name: String,

    /** Display order (lower = first). */
    val sortOrder: Int = 0
)
