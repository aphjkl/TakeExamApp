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
    var showBulkImport by remember { mutableStateOf(false) }
    var bulkText by remember { mutableStateOf("") }
    var bulkError by remember { mutableStateOf<String?>(null) }


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

    OutlinedButton(
        onClick = {
            bulkText = ""
            bulkError = null
            showBulkImport = true
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text("Import multiple users")
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
    if (showBulkImport) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showBulkImport = false
                }
            },
            title = {
                Text("Import multiple users")
            },
            text = {
                Column {
                    Text(
                        "Enter one user per line. Put the first name first, " +
                                "followed by the last name."
                    )

                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = {
                            bulkText = it
                            bulkError = null
                        },
                        label = { Text("Users") },
                        placeholder = {
                            Text(
                                "Jan Janssens\n" +
                                        "Sara Peeters\n" +
                                        "Mohamed El Amrani"
                            )
                        },
                        minLines = 6,
                        maxLines = 12,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )

                    bulkError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        val parsedUsers = parseBulkUsers(bulkText)

                        if (parsedUsers.isEmpty()) {
                            bulkError = "Enter at least one complete name."
                            return@TextButton
                        }

                        isSaving = true
                        bulkError = null

                        repository.addUsers(
                            users = parsedUsers,
                            onSuccess = {
                                isSaving = false
                                showBulkImport = false
                                bulkText = ""
                            },
                            onError = {
                                isSaving = false
                                bulkError = it.localizedMessage
                                    ?: "Unable to import users."
                            }
                        )
                    }
                ) {
                    Text(if (isSaving) "Importing…" else "Import")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        showBulkImport = false
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
private fun parseBulkUsers(text: String): List<Pair<String, String>> {
    return text
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line
                .split(Regex("\\s+"), limit = 2)
                .map { it.trim() }

            if (parts.size != 2 ||
                parts[0].isBlank() ||
                parts[1].isBlank()
            ) {
                null
            } else {
                parts[0] to parts[1]
            }
        }
        .toList()
}