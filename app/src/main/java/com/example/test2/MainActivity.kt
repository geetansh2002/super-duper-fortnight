package com.example.test2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.serialization.Serializable
import java.util.*
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var storage: FirebaseStorage
    private lateinit var statusText: TextView
    private val uploadedUrls = mutableListOf<String>()
    private var totalUploadsExpected = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        storage = Firebase.storage
        val selectImageBtn = findViewById<Button>(R.id.selectImageBtn)
        statusText = findViewById(R.id.statusText)

        selectImageBtn.setOnClickListener {
            uploadedUrls.clear()
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select Pictures"), 101)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            uploadedUrls.clear()
            totalUploadsExpected = 0

            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                totalUploadsExpected = count
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    uploadImageToFirebase(imageUri)
                }
            } else if (data?.data != null) {
                totalUploadsExpected = 1
                val imageUri = data.data!!
                uploadImageToFirebase(imageUri)
            } else {
                statusText.text = "No image selected"
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val filename = UUID.randomUUID().toString() + ".jpg"
        val ref = storage.reference.child("images/$filename")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    uploadedUrls.add(uri.toString())
                    statusText.text = "Uploaded ${uploadedUrls.size}/$totalUploadsExpected files."

                    if (uploadedUrls.size == totalUploadsExpected) {
                        statusText.append("\nSending to backend...")
                        sendToBackend(uploadedUrls)
                    }
                }
            }
            .addOnFailureListener {
                statusText.text = "Upload Failed: ${it.message}"
            }
    }

    private fun sendToBackend(bucketUrls: List<String>) {
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
                    setBody(MediaProcessRequest(bucket_urls = bucketUrls, is_video = false))
                }.bodyAsText()

                withContext(Dispatchers.Main) {
                    statusText.append("\n\nBackend Response:\n$responseText")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "API error: ${e.message}"
                }
            } finally {
                client.close()
            }
        }
    }
}

@Serializable
data class MediaProcessRequest(
    val bucket_urls: List<String>,
    val is_video: Boolean
)

@Serializable
data class ConfidenceScores(
    val vehicle_classification: Double,
    val cargo_detection: Double,
    val number_recognition: Double
)

@Serializable
data class MediaAnalysisResponse(
    val vehicle_number: String,
    val is_number_found: Boolean,
    val vehicle_type: String,
    val is_carrying_contents: Boolean,
    val contents_details: String? = null,
    val confidence_scores: ConfidenceScores
)