package edu.ap.takeexamapp.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import edu.ap.takeexamapp.data.model.Exam
import edu.ap.takeexamapp.data.repository.ExamRepository

@Composable
fun ManageExamsScreen(
    onBack: () -> Unit,
    onEditExam: (String) -> Unit
) {
    val repository = remember { ExamRepository() }
    var exams by remember { mutableStateOf<List<Exam>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredExams = exams.filter {
        it.title.contains(searchQuery.trim(), ignoreCase = true) ||
            it.description.contains(searchQuery.trim(), ignoreCase = true)
    }

    DisposableEffect(repository) {
        val registration = repository.observeExams(
            onSuccess = { exams = it; loading = false; error = null },
            onError = { loading = false; error = it.localizedMessage }
        )
        onDispose { registration.remove() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = { title = ""; description = ""; showCreate = true }) {
                Text("New exam")
            }
        }

        Text("Manage exams", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search exams") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            exams.isEmpty() -> Text("No exams have been created yet.")
            filteredExams.isEmpty() -> Text("No exams match your search.")
            else -> LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredExams, key = { it.id }) { exam ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(exam.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${exam.questionCount} questions · ${exam.totalPoints} points · " +
                                        if (exam.published) "Published" else "Draft",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { onEditExam(exam.id) }) { Text("Edit") }
                            TextButton(onClick = { examToDelete = exam }) { Text("Delete") }
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { if (!saving) showCreate = false },
            title = { Text("New exam") },
            text = {
                Column {
                    OutlinedTextField(title, { title = it; error = null },
                        label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(description, { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.padding(top = 12.dp))
                }
            },
            confirmButton = {
                TextButton(enabled = !saving, onClick = {
                    if (title.isBlank()) { error = "Enter an exam title."; return@TextButton }
                    saving = true
                    repository.createExam(title, description,
                        onSuccess = { id -> saving = false; showCreate = false; onEditExam(id) },
                        onError = { saving = false; error = it.localizedMessage })
                }) { Text(if (saving) "Creating…" else "Create") }
            },
            dismissButton = { TextButton(enabled = !saving, onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }

    examToDelete?.let { exam ->
        AlertDialog(
            onDismissRequest = { examToDelete = null },
            title = { Text("Delete exam?") },
            text = { Text("Delete ${exam.title} and all its questions?") },
            confirmButton = { TextButton(onClick = {
                examToDelete = null
                repository.deleteExam(exam.id, {}, { error = it.localizedMessage })
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { examToDelete = null }) { Text("Cancel") } }
        )
    }
}
