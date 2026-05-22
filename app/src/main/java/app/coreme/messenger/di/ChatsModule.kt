package app.coreme.messenger.di

import app.coreme.messenger.features.chats.data.repository.ChatsRepositoryImpl
import app.coreme.messenger.features.chats.domain.repository.ChatsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import app.coreme.messenger.features.chats.data.api.ChatsApi
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatsApiModule {
    @Provides
    @Singleton
    fun provideChatsApi(retrofit: Retrofit): ChatsApi = retrofit.create(ChatsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindChatsRepository(impl: ChatsRepositoryImpl): ChatsRepository
}
