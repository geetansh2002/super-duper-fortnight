package com.example.test2

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID

class MediaProcessor(private val storage: FirebaseStorage) {

    private val TAG = "MediaProcessor"

    var statusCallback: ((String) -> Unit)? = null
    var responseCallback: ((List<MediaProcessResponse>) -> Unit)? = null
    private val uploadedUrls = mutableListOf<String>()
    var totalUploadsExpected = 0

    fun reset() {
        Log.d(TAG, "Resetting uploaded URLs and totalUploadsExpected.")
        uploadedUrls.clear()
        totalUploadsExpected = 0
        updateStatus("Wait for new uploads.")
    }

    fun uploadToFirebase(context: Context, imageUri: Uri) {
        val filename = UUID.randomUUID().toString() + ".jpg"
        Log.d(TAG, "Generated filename: $filename")

        val ref = storage.reference.child("images/$filename")
        Log.d(TAG, "Starting upload to Firebase for URI: $imageUri")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                Log.d(TAG, "Upload successful for: $filename")
                ref.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG, "Download URL: $uri")
                    uploadedUrls.add(uri.toString())
                    updateStatus("Uploaded ${uploadedUrls.size}/$totalUploadsExpected")

                    if (uploadedUrls.size == totalUploadsExpected) {
                        updateStatus("All uploaded. Sending to backend...")
                        sendToBackend()
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, "Upload failed for $filename: ${it.message}", it)
                updateStatus("Upload failed: ${it.message}")
            }
    }

    private fun sendToBackend() {
        CoroutineScope(Dispatchers.IO).launch {
            val client = HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

            try {
                val rawText = client.post("https://mine-tech-690683200486.us-central1.run.app/media/process/video") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(MediaProcessRequest(bucket_urls = uploadedUrls, is_video = false))
                }.bodyAsText()

                Log.d(TAG, "Raw backend response: $rawText")

                val json = Json { ignoreUnknownKeys = true }
                val parsed = json.decodeFromString<List<MediaProcessResponse>>(rawText)

                withContext(Dispatchers.Main) {
                    responseCallback?.invoke(parsed)
                    updateStatus("Backend processing complete.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "API error during backend call: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    updateStatus("API Error: ${e.message}")
                }
            } finally {
                client.close()
                Log.d(TAG, "Ktor client closed.")
            }
        }
    }

    private fun updateStatus(message: String) {
        Log.d(TAG, "Status update: $message")
        statusCallback?.invoke(message)
    }
}