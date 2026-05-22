package app.coreme.messenger.di

import app.coreme.messenger.features.calls.data.api.CallsApi
import app.coreme.messenger.features.calls.data.repository.CallsRepositoryImpl
import app.coreme.messenger.features.calls.domain.repository.CallsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CallsApiModule {
    @Provides
    @Singleton
    fun provideCallsApi(retrofit: Retrofit): CallsApi = retrofit.create(CallsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class CallsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindCallsRepository(impl: CallsRepositoryImpl): CallsRepository
}
