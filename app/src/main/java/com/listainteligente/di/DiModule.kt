package com.listainteligente.di

import android.content.Context
import androidx.room.Room
import com.listainteligente.data.AppDatabase
import com.listainteligente.data.ShoppingItemDao
import com.listainteligente.data.ShoppingListDao
import com.listainteligente.service.CameraService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "lista_inteligente.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideListDao(db: AppDatabase): ShoppingListDao = db.listDao()

    @Provides @Singleton
    fun provideItemDao(db: AppDatabase): ShoppingItemDao = db.itemDao()

    @Provides @Singleton
    fun provideCameraService(@ApplicationContext ctx: Context) = CameraService(ctx)
}
