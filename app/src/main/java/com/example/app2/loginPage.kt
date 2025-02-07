package com.example.app2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.applandeo.materialcalendarview.utils.calendar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Calendar


//testing credential id = Terence password = Password123
class loginPage : AppCompatActivity() {
    private val db = Firebase.firestore
    private var passwordHash = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_page)

        val loginButton = findViewById<Button>(R.id.login)
        loginButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    var inputID = findViewById<EditText>(R.id.id).text.toString()
                    var inputPassword = findViewById<EditText>(R.id.password).text.toString()
                    inputPassword = inputPassword.trim()
                    inputID = inputID.trim()

                    // Use coroutines to fetch password
                    passwordHash = retrievePasswordFromFirestore(inputID) ?: ""

                    println(hashPassword(inputPassword))
                    println(passwordHash)

                    if (passwordHash == hashPassword(inputPassword)) {
                        Toast.makeText(this@loginPage, "Success!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@loginPage , camera_page::class.java)
                        intent.putExtra("id", inputID)
                        startActivity(intent)

                    } else {
                        Toast.makeText(this@loginPage, "Invalid", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Toast.makeText(this@loginPage, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val genrateDataButton = findViewById<Button>(R.id.generateData)
        genrateDataButton.setOnClickListener{
            generateData()
        }
    }

    private fun generateData() {
        //if Testuser1 is already in firebase it will just override/add the exiting data with this
        val data = mapOf(
            "password" to "008c70392e3abfbd0fa47bbc2ed96aa99bd49e159727fcba0f2e6abeb3a9d601" //Password123 and user name is Testuser1
        )
        val totakedata = mapOf(
            "totake" to "Anarex,Paracetamol"
        )
        db.collection("Testuser1").document("credential")
            .set(data)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(
                    this,
                    "success on adding user and password",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
               println(e)
            }
        db.collection("Testuser1").document("toTake")
            .set(totakedata)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(
                    this,
                    "success on adding to take data",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                println(e)
            }
    }

    // Hash function (SHA-256)
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    // Fetch password asynchronously using coroutines
    private suspend fun retrievePasswordFromFirestore(id: String): String? {
        return try {
            val document = db.collection(id).document("credential").get().await()
            document.getString("password")
        } catch (e: Exception) {
            Toast.makeText(this, "Error retrieving password: ${e.message}", Toast.LENGTH_LONG).show()
            null
        }
    }
}
