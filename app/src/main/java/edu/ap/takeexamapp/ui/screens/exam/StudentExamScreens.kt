package edu.ap.takeexamapp.ui.screens.exam

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import edu.ap.takeexamapp.data.model.DraftAnswer
import edu.ap.takeexamapp.data.model.Exam
import edu.ap.takeexamapp.data.model.ExamAttempt
import edu.ap.takeexamapp.data.model.ExamDraft
import edu.ap.takeexamapp.data.model.ExamUser
import edu.ap.takeexamapp.data.model.QuestionType
import edu.ap.takeexamapp.data.repository.AddressRepository
import edu.ap.takeexamapp.data.repository.AttemptRepository
import edu.ap.takeexamapp.data.repository.ExamDraftStore
import edu.ap.takeexamapp.data.repository.LocationRepository
import kotlinx.coroutines.delay

@Composable
fun StudentExamListScreen(
    onBack: () -> Unit,
    onSelectExam: (String) -> Unit,
    onResumeDraft: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AttemptRepository() }
    val draftStore = remember { ExamDraftStore(context) }
    var exams by remember { mutableStateOf<List<Exam>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf(draftStore.load()) }
    var authReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().signOut()
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnSuccessListener { authReady = true }
            .addOnFailureListener { error = it.localizedMessage }
    }
    DisposableEffect(repository, authReady) {
        if (!authReady) {
            onDispose { }
        } else {
            val registration = repository.observePublishedExams(
                { exams = it; loading = false },
                { error = it.localizedMessage; loading = false }
            )
            onDispose { registration.remove() }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Choose an exam", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 20.dp, bottom = 16.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        when {
            loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            exams.isEmpty() -> Text("No published exams are available.")
            else -> LazyColumn {
                items(exams, key = { it.id }) { exam ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(exam.title, style = MaterialTheme.typography.titleMedium)
                            Text("${exam.questionCount} questions · ${exam.totalPoints} points")
                        }
                        Button(onClick = { onSelectExam(exam.id) }) { Text("Select") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    draft?.let { active ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Resume exam?") },
            text = { Text("${active.userName} has an unfinished attempt for ${active.examTitle}.") },
            confirmButton = { TextButton(onClick = onResumeDraft) { Text("Resume") } },
            dismissButton = { TextButton(onClick = {
                draftStore.clear(); draft = null
            }) { Text("Discard") } }
        )
    }
}

@Composable
fun StudentSelectionScreen(examId: String, onBack: () -> Unit, onSelectUser: (String) -> Unit) {
    val repository = remember { AttemptRepository() }
    var users by remember { mutableStateOf<List<ExamUser>>(emptyList()) }
    var exam by remember { mutableStateOf<Exam?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(examId) { repository.loadExam(examId, { exam = it }, { error = it.localizedMessage }) }
    DisposableEffect(examId) {
        val registration = repository.observeEligibleUsers(examId,
            { users = it; loading = false }, { error = it.localizedMessage; loading = false })
        onDispose { registration.remove() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text(exam?.title ?: "Choose a student", style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 20.dp))
        Text("Tap your own name to continue.", modifier = Modifier.padding(vertical = 12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        else if (users.isEmpty()) Text("No eligible students are available.")
        else LazyColumn {
            items(users, key = { it.id }) { user ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(user.fullName, Modifier.weight(1f))
                    Button(onClick = { onSelectUser(user.id) }) { Text("Continue") }
                }
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun LocationConfirmationScreen(
    examId: String,
    userId: String,
    onBack: () -> Unit,
    onStartExam: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AttemptRepository() }
    val locationRepository = remember { LocationRepository(context) }
    val addressRepository = remember { AddressRepository(context) }
    val draftStore = remember { ExamDraftStore(context) }
    var exam by remember { mutableStateOf<Exam?>(null) }
    var user by remember { mutableStateOf<ExamUser?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun determineLocation() {
        loading = true; error = null; address = null
        locationRepository.getCurrentLocation({ location ->
            latitude = location.latitude; longitude = location.longitude
            addressRepository.reverseGeocode(location.latitude, location.longitude,
                { address = it; loading = false }, { error = it; loading = false })
        }, { error = it; loading = false })
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) determineLocation()
        else error = "Location permission was denied. It is required to start the exam."
    }

    LaunchedEffect(examId, userId) {
        repository.loadExam(examId, { exam = it }, { error = it.localizedMessage })
        repository.loadUser(userId, { user = it }, { error = it.localizedMessage })
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("Location check", style = MaterialTheme.typography.headlineLarge)
        Text("${user?.fullName.orEmpty()} · ${exam?.title.orEmpty()}", Modifier.padding(top = 8.dp))
        Text("Your location and address are required before the timer starts.", Modifier.padding(top = 16.dp))
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(16.dp))
        address?.let {
            Text("Location confirmed", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Text(it)
            Text("© OpenStreetMap contributors", style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }

        if (address == null) Button(
            enabled = !loading,
            onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
        ) { Text("Determine location") }
        else Button(onClick = {
            val selectedExam = exam ?: return@Button
            val selectedUser = user ?: return@Button
            loading = true
            repository.loadQuestions(examId, { questions ->
                if (questions.isEmpty()) { error = "This exam has no questions."; loading = false }
                else {
                    draftStore.save(ExamDraft(
                        examId, selectedExam.title, userId, selectedUser.fullName,
                        System.currentTimeMillis(), latitude!!, longitude!!, address!!, questions
                    ))
                    onStartExam()
                }
            }, { error = it.localizedMessage; loading = false })
        }, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) { Text("Start exam") }

        OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Location settings") }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
    }
}

@Composable
fun ExamSessionScreen(onCancel: () -> Unit, onSubmitted: (String) -> Unit) {
    val context = LocalContext.current
    val store = remember { ExamDraftStore(context) }
    val repository = remember { AttemptRepository() }
    var draft by remember { mutableStateOf(store.load()) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var showSubmit by remember { mutableStateOf(false) }
    var showCancel by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(draft?.startedAt) {
        while (draft != null) {
            elapsedSeconds = ((System.currentTimeMillis() - draft!!.startedAt) / 1000L).coerceAtLeast(0)
            delay(1000)
        }
    }
    val current = draft
    if (current == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("No unfinished exam was found.")
            Button(onClick = onCancel) { Text("Return") }
        }
        return
    }

    val index = current.currentQuestionIndex.coerceIn(current.questions.indices)
    val question = current.questions[index]
    val answer = current.answers[question.id] ?: DraftAnswer()
    fun updateAnswer(updated: DraftAnswer) {
        val changed = current.copy(answers = current.answers + (question.id to updated))
        draft = changed; store.save(changed)
    }
    fun moveTo(newIndex: Int) {
        val changed = current.copy(currentQuestionIndex = newIndex.coerceIn(current.questions.indices))
        draft = changed; store.save(changed)
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question ${index + 1} of ${current.questions.size}")
            Text(formatDuration(elapsedSeconds))
        }
        Text(current.examTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(question.text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 24.dp))
        Text("${question.points} points", style = MaterialTheme.typography.bodySmall)

        if (question.type == QuestionType.OPEN) {
            OutlinedTextField(
                value = answer.openAnswer,
                onValueChange = { updateAnswer(answer.copy(openAnswer = it)) },
                label = { Text("Your answer") }, minLines = 5,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
            )
        } else {
            Column(Modifier.padding(top = 16.dp)) {
                question.options.forEachIndexed { optionIndex, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(answer.selectedOptionIndex == optionIndex,
                            { updateAnswer(answer.copy(selectedOptionIndex = optionIndex)) })
                        Text(option)
                    }
                }
            }
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(enabled = index > 0 && !submitting, onClick = { moveTo(index - 1) }) { Text("Previous") }
            if (index < current.questions.lastIndex) Button(enabled = !submitting, onClick = { moveTo(index + 1) }) { Text("Next") }
            else Button(enabled = !submitting, onClick = { showSubmit = true }) { Text("Submit") }
        }
        TextButton(enabled = !submitting, onClick = { showCancel = true },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)) { Text("Cancel exam") }
    }

    if (showSubmit) {
        val unanswered = current.questions.count { q ->
            val a = current.answers[q.id]
            if (q.type == QuestionType.OPEN) a?.openAnswer.isNullOrBlank() else (a?.selectedOptionIndex ?: -1) < 0
        }
        AlertDialog(onDismissRequest = { showSubmit = false }, title = { Text("Submit exam?") },
            text = { Text(if (unanswered == 0) "You cannot change answers after submitting."
                else "$unanswered question(s) are unanswered. Submit anyway?") },
            confirmButton = { TextButton(onClick = {
                showSubmit = false; submitting = true
                repository.submitAttempt(current,
                    onSuccess = { attempt -> store.clear(); onSubmitted(attempt.id) },
                    onDuplicate = { store.clear(); error = "This student has already submitted this exam."; submitting = false },
                    onError = { error = it.localizedMessage; submitting = false })
            }) { Text("Submit") } },
            dismissButton = { TextButton(onClick = { showSubmit = false }) { Text("Continue exam") } })
    }
    if (showCancel) AlertDialog(onDismissRequest = { showCancel = false }, title = { Text("Discard attempt?") },
        text = { Text("Your saved answers and timer will be removed.") },
        confirmButton = { TextButton(onClick = { store.clear(); showCancel = false; onCancel() }) { Text("Discard") } },
        dismissButton = { TextButton(onClick = { showCancel = false }) { Text("Keep working") } })
}

@Composable
fun SubmissionReceiptScreen(attemptId: String, onDone: () -> Unit) {
    val repository = remember { AttemptRepository() }
    var attempt by remember { mutableStateOf<ExamAttempt?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(attemptId) { repository.loadAttempt(attemptId, { attempt = it }, { error = it.localizedMessage }) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Exam submitted", style = MaterialTheme.typography.headlineLarge)
        attempt?.let {
            Text(it.userName, Modifier.padding(top = 20.dp))
            Text(it.examTitle)
            Text("Duration: ${formatDuration(it.durationSeconds)}", Modifier.padding(top = 8.dp))
            Text("Awaiting review", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Done") }
    }
}

private fun formatDuration(totalSeconds: Long): String =
    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
