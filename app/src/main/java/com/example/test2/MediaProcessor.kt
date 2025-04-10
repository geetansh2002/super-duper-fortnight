package com.example.test2

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import io.ktor.client.HttpClient
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

    var statusCallback: ((String) -> Unit)? = null
    private val uploadedUrls = mutableListOf<String>()
    var totalUploadsExpected = 0

    fun reset() {
        uploadedUrls.clear()
        totalUploadsExpected = 0
        updateStatus("Wait for new uploads.")
    }

    fun uploadToFirebase(context: Context, imageUri: Uri) {
        val filename = UUID.randomUUID().toString() + ".jpg"
        val ref = storage.reference.child("images/$filename")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    uploadedUrls.add(uri.toString())
                    updateStatus("Uploaded ${uploadedUrls.size}/$totalUploadsExpected")

                    if (uploadedUrls.size == totalUploadsExpected) {
                        updateStatus("All uploaded. Sending to backend...")
                        sendToBackend()
                    }
                }
            }
            .addOnFailureListener {
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
                val responseText = client.post("https://mine-tech-690683200486.us-central1.run.app/media/process/video") {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(MediaProcessRequest(bucket_urls = uploadedUrls, is_video = false))
                }.bodyAsText()

                withContext(Dispatchers.Main) {
                    updateStatus("Raw Backend Response:\n$responseText")
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateStatus("API Error: ${e.message}")
                }
            } finally {
                client.close()
            }
        }
    }

    private fun updateStatus(message: String) {
        statusCallback?.invoke(message)
    }
}