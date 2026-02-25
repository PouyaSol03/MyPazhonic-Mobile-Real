package com.example.mypazhonictest.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user",
    indices = [
        Index(value = ["userName"], unique = true),
        Index(value = ["phoneNumber"], unique = true),
        Index(value = ["nationalCode"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val fullName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,

    val userName: String,
    val phoneNumber: String,
    val nationalCode: String? = null,

    val avatarUrl: String? = null,
    val ipAddress: String? = null,

    val password: String,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
