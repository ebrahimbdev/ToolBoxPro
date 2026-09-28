package com.toolbox.pro.core.di

import android.content.Context
import androidx.room.Room
import com.toolbox.pro.core.database.AppDatabase
import com.toolbox.pro.planner.data.PlannerDao
import com.toolbox.pro.qr.data.QrHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "toolbox_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideQrHistoryDao(database: AppDatabase): QrHistoryDao {
        return database.qrHistoryDao()
    }

    @Provides
    @Singleton
    fun providePlannerDao(database: AppDatabase): PlannerDao {
        return database.plannerDao()
    }
}
