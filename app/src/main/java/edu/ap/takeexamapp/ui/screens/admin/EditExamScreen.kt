package edu.ap.takeexamapp.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import edu.ap.takeexamapp.data.model.Exam
import edu.ap.takeexamapp.data.model.Question
import edu.ap.takeexamapp.data.model.QuestionType
import edu.ap.takeexamapp.data.repository.ExamRepository

@Composable
fun EditExamScreen(examId: String, onBack: () -> Unit) {
    val repository = remember { ExamRepository() }
    var exam by remember { mutableStateOf<Exam?>(null) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var initialized by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showQuestionDialog by remember { mutableStateOf(false) }
    var questionToDelete by remember { mutableStateOf<Question?>(null) }

    DisposableEffect(examId) {
        val examRegistration = repository.observeExam(examId, { loaded ->
            exam = loaded
            if (!initialized && loaded != null) {
                title = loaded.title; description = loaded.description; initialized = true
            }
        }, { error = it.localizedMessage })
        val questionRegistration = repository.observeQuestions(examId, { questions = it }, { error = it.localizedMessage })
        onDispose { examRegistration.remove(); questionRegistration.remove() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Edit exam", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 16.dp))

        OutlinedTextField(title, { title = it }, label = { Text("Title") },
            singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        OutlinedTextField(description, { description = it }, label = { Text("Description") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Button(onClick = {
            if (title.isBlank()) error = "Enter an exam title."
            else repository.updateExam(examId, title, description, {}, { error = it.localizedMessage })
        }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Save exam details") }

        exam?.let {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(if (it.published) "Published" else "Draft", Modifier.weight(1f))
                Switch(checked = it.published, onCheckedChange = { value ->
                    if (value && questions.isEmpty()) error = "Add at least one question before publishing."
                    else repository.setPublished(examId, value, {}, { e -> error = e.localizedMessage })
                })
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Questions (${questions.size}) · ${questions.sumOf { it.points }} points",
                style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(onClick = { showQuestionDialog = true }) { Text("Add question") }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(questions, key = { it.id }) { question ->
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${question.position + 1}. ${question.text}")
                        Text("${if (question.type == QuestionType.OPEN) "Open" else "Multiple choice"} · ${question.points} points",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { questionToDelete = question }) { Text("Delete") }
                }
                HorizontalDivider()
            }
        }
    }

    if (showQuestionDialog) {
        AddQuestionDialog(
            position = (questions.maxOfOrNull { it.position } ?: -1) + 1,
            onDismiss = { showQuestionDialog = false },
            onAdd = { question ->
                repository.addQuestion(examId, question,
                    onSuccess = { showQuestionDialog = false; error = null },
                    onError = { error = it.localizedMessage })
            }
        )
    }

    questionToDelete?.let { question ->
        AlertDialog(
            onDismissRequest = { questionToDelete = null },
            title = { Text("Delete question?") }, text = { Text(question.text) },
            confirmButton = { TextButton(onClick = {
                questionToDelete = null
                repository.deleteQuestion(examId, question.id, {}, { error = it.localizedMessage })
            }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { questionToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddQuestionDialog(position: Int, onDismiss: () -> Unit, onAdd: (Question) -> Unit) {
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(QuestionType.OPEN) }
    var pointsText by remember { mutableStateOf("1") }
    var answersText by remember { mutableStateOf("") }
    var optionsText by remember { mutableStateOf("") }
    var correctIndex by remember { mutableStateOf(-1) }
    var validation by remember { mutableStateOf<String?>(null) }
    val options = optionsText.lines().map { it.trim() }.filter { it.isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add question") },
        text = {
            LazyColumn {
                item {
                    OutlinedTextField(text, { text = it }, label = { Text("Question") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(type == QuestionType.OPEN, { type = QuestionType.OPEN })
                        Text("Open")
                        RadioButton(type == QuestionType.MULTIPLE_CHOICE, { type = QuestionType.MULTIPLE_CHOICE })
                        Text("Multiple choice")
                    }
                    OutlinedTextField(pointsText, { pointsText = it.filter(Char::isDigit) },
                        label = { Text("Points") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                    if (type == QuestionType.OPEN) {
                        OutlinedTextField(answersText, { answersText = it },
                            label = { Text("Accepted answers (one per line)") }, minLines = 3,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    } else {
                        OutlinedTextField(optionsText, { optionsText = it; correctIndex = -1 },
                            label = { Text("Options (one per line)") }, minLines = 4,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        Text("Select the correct answer:", modifier = Modifier.padding(top = 8.dp))
                        options.forEachIndexed { index, option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(correctIndex == index, { correctIndex = index })
                                Text(option)
                            }
                        }
                    }
                    validation?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = {
            val points = pointsText.toIntOrNull() ?: 0
            val accepted = answersText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            validation = when {
                text.isBlank() -> "Enter the question."
                points < 1 -> "Points must be at least 1."
                type == QuestionType.OPEN && accepted.isEmpty() -> "Enter at least one accepted answer."
                type == QuestionType.MULTIPLE_CHOICE && options.size < 2 -> "Enter at least two options."
                type == QuestionType.MULTIPLE_CHOICE && correctIndex !in options.indices -> "Select the correct answer."
                else -> null
            }
            if (validation == null) onAdd(Question(text = text.trim(), type = type, points = points,
                acceptedAnswers = if (type == QuestionType.OPEN) accepted else emptyList(),
                options = if (type == QuestionType.MULTIPLE_CHOICE) options else emptyList(),
                correctOptionIndex = if (type == QuestionType.MULTIPLE_CHOICE) correctIndex else -1,
                position = position))
        }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
