package com.example.mypazhonictest.di

import android.content.Context
import androidx.room.Room
import com.example.mypazhonictest.data.local.dao.LocationDao
import com.example.mypazhonictest.data.local.dao.PanelDao
import com.example.mypazhonictest.data.local.dao.PanelFolderDao
import com.example.mypazhonictest.data.local.dao.UserDao
import com.example.mypazhonictest.data.local.db.AppDatabase
import com.example.mypazhonictest.data.local.prefs.BiometricCredentialStore
import com.example.mypazhonictest.data.local.prefs.BiometricPrefs
import com.example.mypazhonictest.data.local.prefs.SessionPrefs
import com.example.mypazhonictest.data.repository.LocationRepository
import com.example.mypazhonictest.data.repository.PanelFolderRepository
import com.example.mypazhonictest.data.repository.PanelRepository
import com.example.mypazhonictest.data.repository.UserRepository
import com.example.mypazhonictest.panel.PanelSerialService
import com.example.mypazhonictest.panel.PanelTcpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val DATABASE_NAME = "mypazhonic.db"

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideLocationDao(database: AppDatabase): LocationDao {
        return database.locationDao()
    }

    @Provides
    @Singleton
    fun providePanelDao(database: AppDatabase): PanelDao {
        return database.panelDao()
    }

    @Provides
    @Singleton
    fun providePanelFolderDao(database: AppDatabase): PanelFolderDao {
        return database.panelFolderDao()
    }

    @Provides
    @Singleton
    fun providePanelRepository(panelDao: PanelDao): PanelRepository {
        return PanelRepository(panelDao)
    }

    @Provides
    @Singleton
    fun providePanelFolderRepository(panelFolderDao: PanelFolderDao): PanelFolderRepository {
        return PanelFolderRepository(panelFolderDao)
    }

    @Provides
    @Singleton
    fun provideSessionPrefs(
        @ApplicationContext context: Context
    ): SessionPrefs {
        return SessionPrefs(context)
    }

    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao, sessionPrefs: SessionPrefs): UserRepository {
        return UserRepository(userDao, sessionPrefs)
    }

    @Provides
    @Singleton
    fun provideLocationRepository(locationDao: LocationDao): LocationRepository {
        return LocationRepository(locationDao)
    }

    @Provides
    @Singleton
    fun provideBiometricPrefs(
        @ApplicationContext context: Context
    ): BiometricPrefs {
        return BiometricPrefs(context)
    }

    @Provides
    @Singleton
    fun provideBiometricCredentialStore(
        @ApplicationContext context: Context
    ): BiometricCredentialStore {
        return BiometricCredentialStore(context)
    }

    @Provides
    @Singleton
    fun providePanelTcpClient(): PanelTcpClient = PanelTcpClient()

    @Provides
    @Singleton
    fun providePanelSerialService(
        tcpClient: PanelTcpClient,
        userRepository: UserRepository
    ): PanelSerialService = PanelSerialService(tcpClient, userRepository)
}
