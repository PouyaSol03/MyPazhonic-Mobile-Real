package com.example.mypazhonictest.di

import android.content.Context
import androidx.room.Room
import com.example.mypazhonictest.data.local.dao.UserDao
import com.example.mypazhonictest.data.local.db.AppDatabase
import com.example.mypazhonictest.data.local.prefs.BiometricPrefs
import com.example.mypazhonictest.data.local.prefs.SessionPrefs
import com.example.mypazhonictest.data.repository.UserRepository
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
    fun provideBiometricPrefs(
        @ApplicationContext context: Context
    ): BiometricPrefs {
        return BiometricPrefs(context)
    }
}
