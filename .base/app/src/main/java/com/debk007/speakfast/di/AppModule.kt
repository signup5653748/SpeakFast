package com.debk007.speakfast.di

import android.content.Context
import com.debk007.speakfast.data.SettingsManager
import com.debk007.speakfast.util.SelectionManager
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
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager {
        return SettingsManager(context)
    }

    @Provides
    @Singleton
    fun provideSelectionManager(): SelectionManager {
        return SelectionManager()
    }
}
