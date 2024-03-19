package com.example.dicp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var ourRequestCode: Int = 123
    private var galleryRequestCode: Int = 456
    private lateinit var imageView1: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check and request runtime permissions if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    galleryRequestCode
                )
            }
        }

        imageView1 = findViewById(R.id.imageView1)

        // Set click listener for the galleryButton
        findViewById<View>(R.id.galleryButton).setOnClickListener {
            openGallery()
        }
    }

    fun takePhoto(view: View) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, ourRequestCode)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, galleryRequestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ourRequestCode -> {
                if (resultCode == RESULT_OK) {
                    val imageView: ImageView = findViewById(R.id.imageView1)
                    val bitmap: Bitmap = data?.extras?.get("data") as Bitmap
                    imageView.setImageBitmap(bitmap)
                }
            }
            galleryRequestCode -> {
                if (resultCode == RESULT_OK && data != null) {
                    val selectedImage = data.data
                    // Handle the selected image as needed
                    // For example, you can set it to an ImageView
                    val imageView: ImageView = findViewById(R.id.imageView1)
                    imageView.setImageURI(selectedImage)
                    val bitmap = selectedImage?.let { loadBitmapFromUri(it) }
                    // Call fetchAndDisplayMessage with the loaded Bitmap
                    fetchAndDisplayMessage(bitmap)
                }
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        return MediaStore.Images.Media.getBitmap(contentResolver, uri)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == galleryRequestCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted, you can now use the galleryButton
        }
    }

    private fun fetchAndDisplayMessage(image: Bitmap?) {
        val base64Image = encodeImageToBase64(image)
        if (base64Image != null) {
           Toast.makeText(applicationContext, "Image encoded successfully!", Toast.LENGTH_LONG).show()
//            textView.text = base64Image
            val requestBody = JSONObject()
            requestBody.put("img_base64", base64Image)
            val request = Request.Builder()
                .url("https://seg-api-tens-pktsznq7fq-em.a.run.app/segment")
                .post(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
//                .post(requestBody.toString().toRequestBody("/segment".toMediaTypeOrNull()))
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Failed to connect to the server", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
//                    if (response.isSuccessful) {
//                        runOnUiThread {
//                            if (responseBody != null) {
//                                Toast.makeText(this@MainActivity, responseBody, Toast.LENGTH_LONG).show()
//                            } else {
//                                // Handle the case when response body is null
//                                Toast.makeText(this@MainActivity, "Response body is null", Toast.LENGTH_LONG).show()
//                            }
//                        }
//                    } else {
//                        // Handle unsuccessful response (e.g., show error message)
//                        runOnUiThread{
//                            Toast.makeText(this@MainActivity, "Unsuccessful response", Toast.LENGTH_LONG).show()
//                        }
//                    }
                    val jsonResponse = JSONObject(responseBody)
                    val mask = jsonResponse.optString("mask")

                    if (mask.isNotEmpty()) {
                        val decodedMask = decodeBase64ToBitmap(mask)
                        runOnUiThread {
                            imageView1.setImageBitmap(decodedMask)
                        }
                    } else {
                        runOnUiThread {
                          Toast.makeText(this@MainActivity, "Failed to decode the mask", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this@MainActivity, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun encodeImageToBase64(image: Bitmap?): String? {
        if (image != null) {
            val byteArrayOutputStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }
        return null
    }

    private fun decodeBase64ToBitmap(base64: String): Bitmap {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

}