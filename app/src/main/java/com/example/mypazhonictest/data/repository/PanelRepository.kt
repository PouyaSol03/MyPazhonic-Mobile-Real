package com.example.mypazhonictest.data.repository

import com.example.mypazhonictest.data.local.dao.PanelDao
import com.example.mypazhonictest.data.local.entity.PanelEntity
import javax.inject.Inject

class PanelRepository @Inject constructor(
    private val panelDao: PanelDao
) {

    suspend fun getByUserId(userId: Long): List<PanelEntity> =
        panelDao.getByUserId(userId)

    /** folderId == null means uncategorized; non-null means panels in that folder. */
    suspend fun getByUserIdAndFolderId(userId: Long, folderId: Long?): List<PanelEntity> =
        panelDao.getByUserIdAndFolderId(userId, folderId)

    suspend fun getById(id: Long): PanelEntity? =
        panelDao.getById(id)

    suspend fun insert(panel: PanelEntity): Long =
        panelDao.insert(panel)

    suspend fun update(panel: PanelEntity) {
        panelDao.update(panel)
    }

    suspend fun deleteById(id: Long) {
        panelDao.deleteById(id)
    }

    /** Set all panels in this folder to uncategorized (folderId = null). Call before deleting a folder. */
    suspend fun clearFolderIdForFolder(folderId: Long) {
        panelDao.clearFolderIdForFolder(folderId, System.currentTimeMillis())
    }

    suspend fun setPanelFolder(panelId: Long, folderId: Long?) {
        val panel = panelDao.getById(panelId) ?: return
        panelDao.update(panel.copy(folderId = folderId, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setPanelLastStatus(panelId: Long, lastStatus: String?) {
        val panel = panelDao.getById(panelId) ?: return
        panelDao.update(panel.copy(lastStatus = lastStatus?.takeIf { it.isNotBlank() }, updatedAt = System.currentTimeMillis()))
    }
}
