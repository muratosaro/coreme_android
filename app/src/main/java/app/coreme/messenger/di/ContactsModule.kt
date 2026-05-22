package app.coreme.messenger.di

import app.coreme.messenger.features.contacts.data.api.ContactsApi
import app.coreme.messenger.features.contacts.data.repository.ContactsRepositoryImpl
import app.coreme.messenger.features.contacts.domain.repository.ContactsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ContactsApiModule {
    @Provides
    @Singleton
    fun provideContactsApi(retrofit: Retrofit): ContactsApi = retrofit.create(ContactsApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ContactsRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindContactsRepository(impl: ContactsRepositoryImpl): ContactsRepository
}
