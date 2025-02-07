package com.example.app2

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Base64
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.transition.Visibility
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import com.applandeo.materialcalendarview.utils.calendar
import com.example.app2.ml.MobileNet4
import com.example.app2.showdataintable.firebaseData
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Arrays
import java.util.Calendar
import kotlin.math.max

class camera_page : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 100
    private val db = Firebase.firestore
    private lateinit var capturedImageView: ImageView
    private var firebase = mutableListOf<firebaseData>()
    private var toTakeFirebaseData = mutableListOf<String>()
    var userId = ""




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_camera_page_fragment)


        val openCameraButton = findViewById<Button>(R.id.openCameraButton)
        capturedImageView = findViewById(R.id.selectedImageView)
        val Id = intent?.getStringExtra("id") ?: "No ID Received"
        userId = Id
        val showId = findViewById<TextView>(R.id.showId)
        println(userId)
        showId.setText("You are Logged in as "+userId)


        openCameraButton.setOnClickListener {
            openCamera()
        }

        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_REQUEST_CODE
            )
        }
        val showTable = findViewById<Button>(R.id.showTable)

        showTable.setOnClickListener {
            val intent = Intent(this, showdataintable::class.java)
            intent.putExtra("id", userId)
            startActivity(intent)
        }
        retrieveToTakeFromFirestore()
    }

    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val photo: Bitmap = data?.extras?.get("data") as Bitmap

            val model = MobileNet4.newInstance(this.applicationContext)

            // Creates inputs for reference.
            val byteBuffer = convertBitmapToByteBuffer(photo)
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

            // Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

            val labels = listOf("Anarex" , "Atenelol" , "Calcium" , "Lozrasin" , "Paracetamol")

            val maxIndex = outputFeature0.indices.maxByOrNull { outputFeature0[it] } ?: -1

            if (maxIndex != -1) {
                println("Predicted class: ${labels[maxIndex]}")
                println("Confidence: ${outputFeature0[maxIndex]}")

                if (outputFeature0[maxIndex] > 0.75) {
                    // Display captured image
                    capturedImageView.setImageBitmap(photo)

                    // Convert Bitmap to Base64 string
                    val base64Image = convertBitmapToBase64(photo)

                    // Save Base64 string to Firestore
                    saveImageToFirestore(
                        base64Image,
                        labels[maxIndex],
                        String.format("%.2f", outputFeature0[maxIndex] * 100)
                    )
                    val confidence = outputFeature0[maxIndex] * 100
                    val showMetrics = findViewById<TextView>(R.id.showMetrics)
                    showMetrics.setText("Predicted classification: ${labels[maxIndex]} , Confidence: %${String.format("%.2f", confidence)}")

                    // Retrieve and display images from Firestore
                    model.close()
                    retrieveImagesFromFirestore()


                } else {
                    Toast.makeText(
                        this,
                        "Please try again ,take a clearer picture.",
                        Toast.LENGTH_LONG
                    ).show()
                    model.close()
                }
            }

        }
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveImageToFirestore(base64Image: String , classfication : String , confidence : String ) {
        val currentDateTime: LocalDateTime = LocalDateTime.now()
        //month will return 0 for Jan , 1 for Feb , 2 for March and so on till 13 for December
        val month = String.format("%02d", calendar.get(Calendar.MONTH))
        val year = String.format("%02d",currentDateTime.year)
        val day = String.format("%02d",currentDateTime.dayOfMonth)
        val hours = String.format("%02d",currentDateTime.hour)
        val minutes = String.format("%02d",currentDateTime.minute)
        val time = " ${hours}:${minutes}"

       val data = mapOf(
           "Image" to base64Image,
           "Time" to time,
           "Year" to year.toInt() ,
           "Month" to month.toInt(),
           "Day" to day.toInt(),
           "Class" to classfication,
           "Confidence" to confidence
       )
        //feature to add
        //add it so that each new month will store same month data , reduce search time if alot if data
        // if collection name is not found in firebase it will create a new collection
        db.collection(userId).document("Data").collection("Images")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(
                    this,
                    "Image saved with ID: ${documentReference.id}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }




    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val inputSize = 224  // Expected input size
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)  // Float32 size
        byteBuffer.order(ByteOrder.nativeOrder())

        // Resize image
        val resizedImage = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)

        // Normalize and write to buffer
        val pixels = IntArray(inputSize * inputSize)
        resizedImage.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }
    // will compare the medicine to take and what images is taken in the firestore base on day , month and year
    private fun toTakeformatSearchData() {
        val currentDateTime: LocalDateTime = LocalDateTime.now()
        val month = String.format("%02d", calendar.get(Calendar.MONTH)).toInt()
        val year = String.format("%02d",currentDateTime.year).toInt()
        val day = String.format("%02d",currentDateTime.dayOfMonth).toInt()

        val displayToTake = findViewById<TableLayout>(R.id.tableLayout) // Table to display data
        displayToTake.removeAllViews() // Clear previous data
        if (toTakeFirebaseData.size != 0 ) {
            toTakeFirebaseData.forEach { itemToTake ->
                var isFound = false

                firebase.forEach { data ->
                    if (data.Year == year && data.Month == month && data.Classification == itemToTake && data.Day == day) {
                        isFound = true
                    }
                }

                // Create a TextView for each item
                val textView = TextView(this)
                textView.text = itemToTake
                textView.textSize = 18f
                textView.setPadding(10, 10, 10, 10)

                // Set background color based on availability
                if (isFound) {
                    textView.setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.green
                        )
                    ) // ✅ Found
                } else {
                    textView.setBackgroundColor(
                        ContextCompat.getColor(
                            this,
                            R.color.red
                        )
                    ) // ❌ Not Found
                }

                // Add to TableLayout
                displayToTake.addView(textView)
            }
        }else{
            val toTake = findViewById<TextView>(R.id.totake)
            toTake.visibility = View.VISIBLE
            toTake.setText("No medicine to take currently")
        }
    }


    private fun retrieveImagesFromFirestore() {
        firebase.clear()
        db.collection(userId).document("Data").collection("Images")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val base64Image = document.getString("Image")
                    val time = document.getString("Time")
                    val confidence = document.getString("Confidence")
                    val classification = document.getString("Class")
                    val year = document.getLong("Year")?.toInt() ?: 0
                    val month = document.getLong("Month")?.toInt() ?: 0
                    val day = document.getLong("Day")?.toInt() ?: 0
                    val formatConfidence = "%${confidence.toString()}"

                    if (base64Image != null) {
                        firebase.add(
                            firebaseData(
                                Time = time.toString(),
                                Year = year,
                                Month = month,
                                Day = day,
                                Classification = classification.toString(),
                                Image = base64Image,
                                Confidence = formatConfidence,
                            )
                        )

                        // Send Notification on new upload
                    }
                }
                toTakeformatSearchData()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error retrieving images: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    // Function to send push notification


    private fun retrieveToTakeFromFirestore() {
        toTakeFirebaseData.clear()

        db.collection(userId)
            .document("toTake")
            .get()
            .addOnSuccessListener { result ->
                println(result)
                if (result.getString("totake") != null) {
                    // it will return "Anarex,Paracetamol,Calcium" for testing purpose but for future this will be set by doctor
                    val items = result.getString("totake")!!
                    val list = items.split(",")

                    list.forEachIndexed { index, item ->
                        toTakeFirebaseData.add(list[index])
                    }
                    println(toTakeFirebaseData)

                    retrieveImagesFromFirestore()
                }
                else{
                    toTakeFirebaseData.clear()
                    retrieveImagesFromFirestore()
                }

            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error retrieving images: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }



    data class firebaseData(
        val Time : String,
        val Year: Int,
        val Month: Int,
        val Day: Int,
        val Classification : String,
        val Image : String,
        val Confidence : String,
    )


}
