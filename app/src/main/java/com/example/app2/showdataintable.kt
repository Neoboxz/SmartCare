package com.example.app2

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color

import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.strictmode.SetRetainInstanceUsageViolation
import com.applandeo.materialcalendarview.CalendarDay
import com.applandeo.materialcalendarview.CalendarView
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.time.LocalDateTime
import java.util.Calendar

class showdataintable : AppCompatActivity() {
    private val db = Firebase.firestore
    private lateinit var calendarView: CalendarView
    private var firebase = mutableListOf<firebaseData>()
    var userId = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_showdataintable)
        calendarView = findViewById(R.id.calender)
        val Id = intent?.getStringExtra("id") ?: "No ID Received"
        userId = Id

        retrieveImagesFromFirestore()


    }


    private fun formatSearchData (){
        val dataList : ArrayList<String> = ArrayList()
        val tableLayout = findViewById<TableLayout>(R.id.tableLayout)
        val currentDateTime: LocalDateTime = LocalDateTime.now()
        val month = currentDateTime.monthValue
        val year = currentDateTime.year
        val day = currentDateTime.dayOfMonth

        val calendars : ArrayList<CalendarDay> = ArrayList()
        val calendar = Calendar.getInstance()
        calendar.set(year,month,day)
        val calendarDay = CalendarDay(calendar)
        calendarDay.labelColor = R.color.purple_500
        calendars.add(calendarDay)
        calendarView.setCalendarDays(calendars)
        calendarView.setOnCalendarDayClickListener(object : OnCalendarDayClickListener{
            override fun onClick(calendarDay: CalendarDay){
                tableLayout.removeAllViews()
                val day = String.format("%02d", calendarDay.calendar.get(Calendar.DAY_OF_MONTH)).toInt()
                val month = String.format("%02d", calendarDay.calendar.get(Calendar.MONTH)).toInt()
                val year = String.format("%02d", calendarDay.calendar.get(Calendar.YEAR))
                val dateSelected = findViewById<TextView>(R.id.dateSelected)
                dateSelected.setText("${day}/${month+1}/${year}")
                firebase.forEach{ data ->
                    if (day == data.Day && month == data.Month && year == data.Year.toString()){
                        println("${data.Classification},${data.Time}, ${data.Confidence}")
                        dataList.add(data.Time)
                        dataList.add(data.Confidence)
                        dataList.add(data.Classification)
                        dataList.add(data.Image)

                    }


                }// Add headers to the table
                addTableHeader(tableLayout)

                // Add data rows to the table
                dataList?.let {
                    addTableData(tableLayout, it)
                    it.clear()
                }
            }
        })

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

                    }
                }
                formatSearchData()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error retrieving images: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun convertBase64ToBitmap(base64Str: String): Bitmap {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
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
    private fun addTableHeader(tableLayout: TableLayout) {
        val headers = arrayOf("Time", "Confidence" , "Classification" , "Photo")
        val headerRow = TableRow(this)

        headers.forEach { header ->
            val textView = TextView(this).apply {
                text = header
                setTextColor(Color.BLACK)
                textSize = 18f
                setPadding(16, 16, 16, 16)
                setBackgroundColor(Color.LTGRAY)
            }
            headerRow.addView(textView)
        }

        tableLayout.addView(headerRow)
    }

    private fun addTableData(tableLayout: TableLayout, dataList: ArrayList<String>) {
        // Ensure that dataList has rows with the required 4 elements (Time, Confidence, Classification, Image)
        val rows = dataList.chunked(4) // Split into groups of 4 elements (each row)

        rows.forEach { rowItems ->
            val row = TableRow(this)

            rowItems.forEachIndexed { index, item ->
                if (index == 3) { // If it's the Image column (4th column)
                    val imageView = ImageView(this).apply {
                        val bitmap = convertBase64ToBitmap(item) // Convert Base64 string to Bitmap
                        setImageBitmap(bitmap)
                        layoutParams = TableRow.LayoutParams(150, 150) // Set fixed image size
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setPadding(16, 16, 16, 16)
                    }
                    row.addView(imageView)
                } else { // For Time, Confidence, and Classification
                    val textView = TextView(this).apply {
                        text = item
                        setTextColor(Color.DKGRAY)
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                    }
                    row.addView(textView)
                }
            }

            tableLayout.addView(row)
        }
    }
}
