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

class MainActivity : ComponentActivity() {

    private val mediaProcessor by lazy { MediaProcessor(Firebase.storage) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri> ->
            mediaProcessor.reset()
            if (uris.isNotEmpty()) {
                mediaProcessor.totalUploadsExpected = uris.size
                uris.forEach { uri ->
                    mediaProcessor.uploadToFirebase(this, uri)
                }
            }
        }

        setContent {
            var statusText by remember { mutableStateOf("Select images to upload.") }

            mediaProcessor.statusCallback = { msg ->
                statusText = msg
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