package app.coreme.messenger.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

/**
 * Wraps a suspend Retrofit call in a Result, mapping HTTP/network errors to
 * human-readable messages. Call from Repository layer only.
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (e: HttpException) {
        val bodyMessage = e.response()?.errorBody()?.parseErrorMessage()
        val message = bodyMessage ?: when (e.code()) {
            401 -> "Invalid credentials"
            409 -> "Username already taken"
            422 -> "Validation error"
            500 -> "Server error"
            else -> "Error ${e.code()}"
        }
        Result.failure(Exception(message))
    } catch (e: IOException) {
        Result.failure(Exception("No internet connection"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun ResponseBody.parseErrorMessage(): String? {
    return try {
        val raw = string()
        errorJson.parseToJsonElement(raw).jsonObject["message"]?.jsonPrimitive?.content
    } catch (_: Exception) {
        null
    }
}
