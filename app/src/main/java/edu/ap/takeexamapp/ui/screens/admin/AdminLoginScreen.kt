package edu.ap.takeexamapp.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@Composable
fun AdminLoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun migrateExistingAttemptLocks(onComplete: () -> Unit, onError: (Exception) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("attempts").get()
            .addOnSuccessListener { snapshot ->
                val attemptsToMigrate = snapshot.documents.mapNotNull { document ->
                    val examId = document.getString("examId").orEmpty()
                    val userId = document.getString("userId").orEmpty()
                    if (examId.isBlank() || userId.isBlank()) null
                    else Triple("${examId}_${userId}", examId, userId) to
                        (document.getLong("submittedAt") ?: System.currentTimeMillis())
                }

                if (attemptsToMigrate.isEmpty()) {
                    onComplete()
                    return@addOnSuccessListener
                }

                // A batch supports at most 500 writes. This school app is expected to stay
                // well below that limit, but chunking keeps the migration safe as data grows.
                val chunks = attemptsToMigrate.chunked(450)
                var completedChunks = 0
                chunks.forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { (attemptData, createdAt) ->
                        val (attemptId, examId, userId) = attemptData
                        batch.set(
                            firestore.collection("attemptLocks").document(attemptId),
                            mapOf(
                                "examId" to examId,
                                "userId" to userId,
                                "createdAt" to createdAt
                            ),
                            SetOptions.merge()
                        )
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            completedChunks++
                            if (completedChunks == chunks.size) onComplete()
                        }
                        .addOnFailureListener(onError)
                }
            }
            .addOnFailureListener(onError)
    }

    fun login() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Enter your email address and password."
            return
        }

        isLoading = true
        errorMessage = null

        if (FirebaseAuth.getInstance().currentUser?.isAnonymous == true) {
            FirebaseAuth.getInstance().signOut()
        }

        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    FirebaseAuth.getInstance().signOut()
                    isLoading = false
                    errorMessage = "Unable to verify administrator access."
                    return@addOnSuccessListener
                }

                FirebaseFirestore.getInstance()
                    .collection("admins")
                    .document(uid)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.getBoolean("enabled") == true) {
                            migrateExistingAttemptLocks(
                                onComplete = {
                                    isLoading = false
                                    onLoginSuccess()
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = error.localizedMessage
                                        ?: "Unable to prepare existing exam attempts."
                                }
                            )
                        } else {
                            FirebaseAuth.getInstance().signOut()
                            isLoading = false
                            errorMessage = "This account does not have administrator access."
                        }
                    }
                    .addOnFailureListener { error ->
                        FirebaseAuth.getInstance().signOut()
                        isLoading = false
                        errorMessage = error.localizedMessage
                            ?: "Unable to verify administrator access."
                    }
            }
            .addOnFailureListener { error ->
                isLoading = false
                errorMessage = error.localizedMessage
                    ?: "Unable to sign in."
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Admin login",
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            )
        }

        Button(
            onClick = { login() },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign in")
            }
        }
    }
}
