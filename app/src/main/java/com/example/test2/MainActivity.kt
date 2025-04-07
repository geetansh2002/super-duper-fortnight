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
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var storage: FirebaseStorage
    private lateinit var statusText: TextView
    private val uploadedUrls = mutableListOf<String>()

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
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Select Pictures"), 101)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 101 && resultCode == RESULT_OK) {
            if (data?.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    uploadImageToFirebase(imageUri)
                }
            } else if (data?.data != null) {
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
                    statusText.text = "Uploaded:\n" + uploadedUrls.joinToString("\n\n")
                }
            }
            .addOnFailureListener {
                statusText.text = "Upload Failed: ${it.message}"
            }
    }
}

