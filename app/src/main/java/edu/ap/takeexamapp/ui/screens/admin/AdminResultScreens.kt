package edu.ap.takeexamapp.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import edu.ap.takeexamapp.data.model.AttemptStatus
import edu.ap.takeexamapp.data.model.Exam
import edu.ap.takeexamapp.data.model.ExamAnswer
import edu.ap.takeexamapp.data.model.ExamAttempt
import edu.ap.takeexamapp.data.model.ExamUser
import edu.ap.takeexamapp.data.model.QuestionType
import edu.ap.takeexamapp.data.repository.ExamRepository
import edu.ap.takeexamapp.data.repository.ResultRepository
import edu.ap.takeexamapp.data.repository.UserRepository
import java.text.DateFormat
import java.util.Date

@Composable
fun ResultsHubScreen(
    onBack: () -> Unit,
    onPending: () -> Unit,
    onByUser: () -> Unit,
    onByExam: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Results", style = MaterialTheme.typography.headlineLarge)
        Button(onClick = onPending, modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
            Text("Awaiting review")
        }
        Button(onClick = onByUser, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("Results by user")
        }
        Button(onClick = onByExam, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("Results by exam")
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
            Text("Back")
        }
    }
}

@Composable
fun PendingReviewScreen(onBack: () -> Unit, onReview: (String) -> Unit) {
    val repository = remember { ResultRepository() }
    var attempts by remember { mutableStateOf<List<ExamAttempt>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(repository) {
        val registration = repository.observePending(
            { attempts = it; loading = false },
            { error = it.localizedMessage; loading = false })
        onDispose { registration.remove() }
    }
    ResultListLayout("Awaiting review", onBack, loading, error, attempts, "Nothing is awaiting review.") {
        AttemptRow(it, actionLabel = "Review", onAction = { onReview(it.id) })
    }
}

@Composable
fun GradeAttemptScreen(attemptId: String, onBack: () -> Unit, onSaved: () -> Unit) {
    val repository = remember { ResultRepository() }
    var attempt by remember { mutableStateOf<ExamAttempt?>(null) }
    var answers by remember { mutableStateOf<List<ExamAnswer>>(emptyList()) }
    var scores by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var initialized by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(attemptId) {
        val attemptRegistration = repository.observeAttempt(attemptId, { attempt = it }, { error = it.localizedMessage })
        val answersRegistration = repository.observeAnswers(attemptId, { loaded ->
            answers = loaded
            if (!initialized) {
                scores = loaded.filter { it.questionType == QuestionType.OPEN }.associate { answer ->
                    answer.questionId to when {
                        answer.awardedPoints != null -> answer.awardedPoints.toString()
                        answer.openAnswer.isBlank() -> "0"
                        else -> ""
                    }
                }
                initialized = true
            }
        }, { error = it.localizedMessage })
        onDispose { attemptRegistration.remove(); answersRegistration.remove() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Review attempt", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 16.dp))
        attempt?.let {
            Text("${it.userName} · ${it.examTitle}", style = MaterialTheme.typography.titleMedium)
            Text("Submitted ${formatDate(it.submittedAt)} · ${formatDuration(it.durationSeconds)}")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }

        LazyColumn(modifier = Modifier.weight(1f).padding(top = 12.dp)) {
            items(answers, key = { it.questionId }) { answer ->
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(answer.questionText, style = MaterialTheme.typography.titleMedium)
                    Text("Maximum: ${answer.maximumPoints} points")
                    if (answer.questionType == QuestionType.MULTIPLE_CHOICE) {
                        Text("Answer: ${answer.selectedOptionText.ifBlank { "No answer" }}")
                        Text("Awarded: ${answer.awardedPoints ?: 0}/${answer.maximumPoints}")
                    } else {
                        Text("Answer: ${answer.openAnswer.ifBlank { "No answer" }}",
                            modifier = Modifier.padding(vertical = 8.dp))
                        OutlinedTextField(
                            value = scores[answer.questionId].orEmpty(),
                            onValueChange = { value ->
                                scores = scores + (answer.questionId to value.filter(Char::isDigit))
                                error = null
                            },
                            label = { Text("Points (0-${answer.maximumPoints})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
                HorizontalDivider()
            }
        }

        Button(enabled = !saving && initialized, onClick = {
            val openAnswers = answers.filter { it.questionType == QuestionType.OPEN }
            val parsed = openAnswers.associate { it.questionId to scores[it.questionId]?.toIntOrNull() }
            if (parsed.values.any { it == null } || openAnswers.any {
                    val score = parsed[it.questionId] ?: -1
                    score !in 0..it.maximumPoints
                }) {
                error = "Enter points between 0 and the maximum for every open answer."
                return@Button
            }
            saving = true
            repository.finalizeGrade(attemptId, parsed.mapValues { it.value!! },
                onSuccess = { saving = false; onSaved() },
                onError = { saving = false; error = it.localizedMessage })
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (saving) "Saving…" else if (attempt?.status == AttemptStatus.GRADED) "Update final score" else "Finalize review")
        }
    }
}

@Composable
fun UserResultSelectorScreen(onBack: () -> Unit, onSelect: (String) -> Unit) {
    val repository = remember { UserRepository() }
    val resultRepository = remember { ResultRepository() }
    var users by remember { mutableStateOf<List<ExamUser>>(emptyList()) }
    var attempts by remember { mutableStateOf<List<ExamAttempt>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(repository) {
        val registration = repository.observeUsers({ users = it; loading = false }, { error = it.localizedMessage; loading = false })
        onDispose { registration.remove() }
    }
    DisposableEffect(resultRepository) {
        val registration = resultRepository.observeAll({ attempts = it }, { error = it.localizedMessage })
        onDispose { registration.remove() }
    }
    val options = (users.map { it.id to it.fullName } + attempts.map { it.userId to it.userName })
        .distinctBy { it.first }.sortedBy { it.second.lowercase() }
    SelectorLayout("Results by user", onBack, loading, error, options, "No users available.",
        label = { it.second }, id = { it.first }, onSelect = onSelect)
}

@Composable
fun ExamResultSelectorScreen(onBack: () -> Unit, onSelect: (String) -> Unit) {
    val repository = remember { ExamRepository() }
    val resultRepository = remember { ResultRepository() }
    var exams by remember { mutableStateOf<List<Exam>>(emptyList()) }
    var attempts by remember { mutableStateOf<List<ExamAttempt>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(repository) {
        val registration = repository.observeExams({ exams = it; loading = false }, { error = it.localizedMessage; loading = false })
        onDispose { registration.remove() }
    }
    DisposableEffect(resultRepository) {
        val registration = resultRepository.observeAll({ attempts = it }, { error = it.localizedMessage })
        onDispose { registration.remove() }
    }
    val options = (exams.map { it.id to it.title } + attempts.map { it.examId to it.examTitle })
        .distinctBy { it.first }.sortedBy { it.second.lowercase() }
    SelectorLayout("Results by exam", onBack, loading, error, options, "No exams available.",
        label = { it.second }, id = { it.first }, onSelect = onSelect)
}

@Composable
fun UserAttemptsScreen(userId: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    AttemptsForFilterScreen("User results", userId, true, onBack, onOpen)
}

@Composable
fun ExamAttemptsScreen(examId: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    AttemptsForFilterScreen("Exam results", examId, false, onBack, onOpen)
}

@Composable
private fun AttemptsForFilterScreen(
    title: String,
    filterId: String,
    byUser: Boolean,
    onBack: () -> Unit,
    onOpen: (String) -> Unit
) {
    val repository = remember { ResultRepository() }
    var attempts by remember { mutableStateOf<List<ExamAttempt>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(filterId, byUser) {
        val registration = if (byUser) repository.observeForUser(filterId,
            { attempts = it; loading = false }, { error = it.localizedMessage; loading = false })
        else repository.observeForExam(filterId,
            { attempts = it; loading = false }, { error = it.localizedMessage; loading = false })
        onDispose { registration.remove() }
    }
    ResultListLayout(title, onBack, loading, error, attempts, "No results found.") {
        AttemptRow(it, actionLabel = "Details", onAction = { onOpen(it.id) })
    }
}

@Composable
private fun AttemptRow(attempt: ExamAttempt, actionLabel: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text("${attempt.userName} · ${attempt.examTitle}", style = MaterialTheme.typography.titleMedium)
        Text(formatDate(attempt.submittedAt))
        Text(if (attempt.status == AttemptStatus.GRADED) "${attempt.finalPoints}/${attempt.maximumPoints} points"
            else "Awaiting review")
        Text("${attempt.address} · ${formatDuration(attempt.durationSeconds)}",
            style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) { Text(actionLabel) }
    }
}

@Composable
private fun ResultListLayout(
    title: String, onBack: () -> Unit, loading: Boolean, error: String?, attempts: List<ExamAttempt>,
    emptyText: String, row: @Composable (ExamAttempt) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        else if (attempts.isEmpty()) Text(emptyText)
        else LazyColumn { items(attempts, key = { it.id }) { row(it); HorizontalDivider() } }
    }
}

@Composable
private fun <T> SelectorLayout(
    title: String, onBack: () -> Unit, loading: Boolean, error: String?, values: List<T>, emptyText: String,
    label: (T) -> String, id: (T) -> String, onSelect: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        else if (values.isEmpty()) Text(emptyText)
        else LazyColumn { items(values, key = id) { value ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label(value), Modifier.weight(1f))
                Button(onClick = { onSelect(id(value)) }) { Text("View") }
            }
            HorizontalDivider()
        } }
    }
}

internal fun formatDate(timestamp: Long): String = DateFormat.getDateTimeInstance().format(Date(timestamp))

internal fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val remaining = seconds % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, remaining)
    else "%02d:%02d".format(minutes, remaining)
}
