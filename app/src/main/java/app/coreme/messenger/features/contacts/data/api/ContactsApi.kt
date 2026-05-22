package app.coreme.messenger.features.contacts.data.api

import app.coreme.messenger.features.contacts.data.dto.AddContactRequest
import app.coreme.messenger.features.contacts.data.dto.ContactDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ContactsApi {
    @GET("api/contacts")
    suspend fun getContacts(): List<ContactDto>

    @POST("api/contacts")
    suspend fun addContact(@Body request: AddContactRequest): ContactDto

    @DELETE("api/contacts/{id}")
    suspend fun removeContact(@Path("id") contactId: String)
}
