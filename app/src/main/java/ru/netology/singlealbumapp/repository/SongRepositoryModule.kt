package ru.netology.singlealbumapp.repository

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped
import okhttp3.OkHttpClient

@Module
@InstallIn(ViewModelComponent::class)
object SongRepositoryModule {

    @Provides
    @ViewModelScoped
    fun providesRepository(application: Application, client: OkHttpClient): SongRepository = SongRepositoryImpl(application, client)

}

