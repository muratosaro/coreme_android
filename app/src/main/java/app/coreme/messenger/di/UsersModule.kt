package app.coreme.messenger.di

import app.coreme.messenger.features.users.data.api.UsersApi
import app.coreme.messenger.features.users.data.repository.UsersRepositoryImpl
import app.coreme.messenger.features.users.domain.repository.UsersRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UsersApiModule {
    @Provides
    @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi = retrofit.create(UsersApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UsersRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindUsersRepository(impl: UsersRepositoryImpl): UsersRepository
}
