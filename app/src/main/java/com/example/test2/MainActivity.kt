package com.example.test2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import android.util.Log

class MainActivity : ComponentActivity() {

    private val TAG = "MainActivity"

    private val mediaProcessor by lazy { MediaProcessor(Firebase.storage) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity launched.")

        val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->
            Log.d(TAG, "Image Picker returned ${uris.size} URIs.")
            mediaProcessor.reset()
            if (uris.isNotEmpty()) {
                mediaProcessor.totalUploadsExpected = uris.size
                uris.forEach { uri ->
                    Log.d(TAG, "Uploading image URI: $uri")
                    mediaProcessor.uploadToFirebase(this, uri)
                }
            } else {
                Log.d(TAG, "No image selected.")
            }
        }

        setContent {
            var statusText by remember { mutableStateOf("Select images to upload.") }
            var results by remember { mutableStateOf<List<MediaProcessResponse>>(emptyList()) }

            mediaProcessor.statusCallback = { msg ->
                Log.d(TAG, "Status callback: $msg")
                statusText = msg
            }

            mediaProcessor.responseCallback = { responseList ->
                Log.d(TAG, "Received backend response with ${responseList.size} items.")
                responseList.forEachIndexed { index, item ->
                    Log.d(TAG, "Response[$index]: $item")
                }
                results = responseList
            }


            MaterialTheme(colorScheme = lightColorScheme()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(onClick = {
                            imagePickerLauncher.launch("image/*")
                        }) {
                            Text("Select Images")
                        }
                        Text(text = statusText, color = Color.White)

                        results.forEachIndexed { index, item ->
                            Text(
                                text = "Result #${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text("Vehicle Type: ${item.vehicle_type}", color = Color.White)
                            Text("Vehicle Number: ${item.vehicle_number}", color = Color.White)
                            Text("Is Number Found: ${item.is_number_found}", color = Color.White)
                            Text("Carrying Contents: ${item.is_carrying_contents}", color = Color.White)

                            item.contents_details?.let {
                                Text("Contents Category: ${it.category}", color = Color.White)
                                Text("Description: ${it.description}", color = Color.White)
                            }

                            Text("Confidence Scores:", color = Color.White)
                            Text("• Vehicle Classification: ${item.confidence_scores.vehicle_classification}", color = Color.White)
                            Text("• Cargo Detection: ${item.confidence_scores.cargo_detection}", color = Color.White)
                            Text("• Number Recognition: ${item.confidence_scores.number_recognition}", color = Color.White)

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(onClick = {
                            Toast.makeText(this@MainActivity, "Royalty Verified", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Verify Royalty")
                        }
                    }
                }
            }
        }
    }
}