package com.example.mypazhonictest.data.repository

import com.example.mypazhonictest.data.local.dao.PanelFolderDao
import com.example.mypazhonictest.data.local.entity.PanelFolderEntity
import javax.inject.Inject

class PanelFolderRepository @Inject constructor(
    private val panelFolderDao: PanelFolderDao
) {

    suspend fun getByUserId(userId: Long): List<PanelFolderEntity> =
        panelFolderDao.getByUserId(userId)

    suspend fun getById(id: Long): PanelFolderEntity? =
        panelFolderDao.getById(id)

    suspend fun insert(folder: PanelFolderEntity): Long =
        panelFolderDao.insert(folder)

    suspend fun update(folder: PanelFolderEntity) {
        panelFolderDao.update(folder)
    }

    suspend fun deleteById(id: Long) {
        panelFolderDao.deleteById(id)
    }
}
