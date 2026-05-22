package app.coreme.messenger.di

import app.coreme.messenger.features.channels.data.api.ChannelsApi
import app.coreme.messenger.features.channels.data.repository.ChannelsRepositoryImpl
import app.coreme.messenger.features.channels.domain.repository.ChannelsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChannelsApiModule {
    @Provides
    @Singleton
    fun provideChannelsApi(retrofit: Retrofit): ChannelsApi = retrofit.create(ChannelsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ChannelsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChannelsRepository(impl: ChannelsRepositoryImpl): ChannelsRepository
}
