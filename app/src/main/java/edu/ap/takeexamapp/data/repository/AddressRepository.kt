package edu.ap.takeexamapp.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class AddressRepository(private val context: Context) {
    private val cache = context.getSharedPreferences("geocoding_cache", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()

    fun reverseGeocode(
        latitude: Double,
        longitude: Double,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val key = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
        cache.getString(key, null)?.let { onSuccess(it); return }

        firestore.collection("appConfig").document("public").get()
            .addOnSuccessListener { document ->
                val baseUrl = document.getString("nominatimBaseUrl")
                    ?.takeIf { it.startsWith("https://") }
                    ?: DEFAULT_BASE_URL
                requestAddress(baseUrl, latitude, longitude, key, onSuccess, onError)
            }
            .addOnFailureListener {
                requestAddress(DEFAULT_BASE_URL, latitude, longitude, key, onSuccess, onError)
            }
    }

    private fun requestAddress(
        baseUrl: String,
        latitude: Double,
        longitude: Double,
        cacheKey: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) = thread {
        var connection: HttpURLConnection? = null
        try {
            val endpoint = baseUrl.trimEnd('/') +
                "/reverse?format=jsonv2&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1"
            connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "TakeExamApp/1.0 (edu.ap.takeexamapp)")
            connection.setRequestProperty("Accept-Language", Locale.getDefault().toLanguageTag())

            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Address service returned ${connection.responseCode}.")
            }
            val address = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                .optString("display_name").trim()
            if (address.isEmpty()) throw IllegalStateException("No address was found for this location.")
            cache.edit().putString(cacheKey, address).apply()
            android.os.Handler(context.mainLooper).post { onSuccess(address) }
        } catch (error: Exception) {
            android.os.Handler(context.mainLooper).post {
                onError(error.localizedMessage ?: "Unable to retrieve the address.")
            }
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val DEFAULT_BASE_URL = "https://nominatim.openstreetmap.org"
    }
}
