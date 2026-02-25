package com.example.mypazhonictest.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "panel",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["folderId"]),
        Index(value = ["code"]),
        Index(value = ["locationId"]),
        Index(value = ["serialNumber"])
    ]
)
data class PanelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** Owner user id (session user). */
    val userId: Long,

    /** Optional folder/category id for Telegram-like grouping. */
    val folderId: Long? = null,

    /** Icon key for UI (e.g. "building", "home", "store"). */
    val icon: String? = null,

    val name: String,

    @ColumnInfo(name = "gsm_phone")
    val gsmPhone: String? = null,

    val ip: String? = null,
    val port: Int? = null,
    val code: String? = null,

    val description: String? = null,
    val serialNumber: String? = null,
    val isActive: Boolean = true,
    val locationId: Long? = null,
    val codeUD: String? = null,

    @ColumnInfo(name = "create_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
