package com.chemecador.passwordmemory.di

import android.content.Context
import androidx.room.Room
import com.chemecador.passwordmemory.data.db.PasswordDatabase
import com.chemecador.passwordmemory.data.db.PasswordEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PasswordDatabase =
        Room.databaseBuilder(context, PasswordDatabase::class.java, PasswordDatabase.NAME).build()

    @Provides
    fun provideEntryDao(database: PasswordDatabase): PasswordEntryDao = database.entryDao()
}
