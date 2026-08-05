package edu.ap.takeexamapp.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.ap.takeexamapp.data.model.ExamUser
import edu.ap.takeexamapp.data.repository.UserRepository

@Composable
fun ManageUsersScreen(
    onBack: () -> Unit
) {
    val repository = remember { UserRepository() }

    var users by remember { mutableStateOf<List<ExamUser>>(emptyList()) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<ExamUser?>(null) }

    DisposableEffect(repository) {
        val registration = repository.observeUsers(
            onSuccess = {
                users = it
                isLoading = false
                errorMessage = null
            },
            onError = {
                isLoading = false
                errorMessage = it.localizedMessage
                    ?: "Unable to load users."
            }
        )

        onDispose {
            registration.remove()
        }
    }

    fun addUser() {
        if (firstName.isBlank() || lastName.isBlank()) {
            errorMessage = "Enter both a first name and last name."
            return
        }

        isSaving = true
        errorMessage = null

        repository.addUser(
            firstName = firstName,
            lastName = lastName,
            onSuccess = {
                firstName = ""
                lastName = ""
                isSaving = false
            },
            onError = {
                isSaving = false
                errorMessage = it.localizedMessage
                    ?: "Unable to add the user."
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Back")
        }

        Text(
            text = "Manage users",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 20.dp)
        )

        OutlinedTextField(
            value = firstName,
            onValueChange = {
                firstName = it
                errorMessage = null
            },
            label = { Text("First name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = {
                lastName = it
                errorMessage = null
            },
            label = { Text("Last name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )

        Button(
            onClick = { addUser() },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Add user")
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Text(
            text = "Users (${users.size})",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
        )

        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            users.isEmpty() -> {
                Text(
                    text = "No users have been added yet.",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = users,
                        key = { it.id }
                    ) { user ->
                        UserListItem(
                            user = user,
                            onDelete = {
                                userToDelete = user
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    userToDelete?.let { user ->
        AlertDialog(
            onDismissRequest = {
                userToDelete = null
            },
            title = {
                Text("Delete user?")
            },
            text = {
                Text("Delete ${user.fullName}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToDelete = null

                        repository.deleteUser(
                            userId = user.id,
                            onSuccess = {
                                errorMessage = null
                            },
                            onError = {
                                errorMessage = it.localizedMessage
                                    ?: "Unable to delete the user."
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        userToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserListItem(
    user: ExamUser,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = user.fullName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        TextButton(onClick = onDelete) {
            Text("Delete")
        }
    }
}