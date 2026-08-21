package edu.ap.takeexamapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.ap.takeexamapp.data.model.AttemptStatus
import edu.ap.takeexamapp.data.model.Exam
import edu.ap.takeexamapp.data.model.ExamAnswer
import edu.ap.takeexamapp.data.model.ExamAttempt
import edu.ap.takeexamapp.data.model.ExamDraft
import edu.ap.takeexamapp.data.model.ExamUser
import edu.ap.takeexamapp.data.model.Question
import edu.ap.takeexamapp.data.model.QuestionType

class AttemptRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val attempts = firestore.collection("attempts")
    private val attemptLocks = firestore.collection("attemptLocks")

    fun loadExam(examId: String, onSuccess: (Exam?) -> Unit, onError: (Exception) -> Unit) {
        firestore.collection("exams").document(examId).get()
            .addOnSuccessListener { onSuccess(it.toObject(Exam::class.java)?.copy(id = it.id)) }
            .addOnFailureListener(onError)
    }

    fun loadUser(userId: String, onSuccess: (ExamUser?) -> Unit, onError: (Exception) -> Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { onSuccess(it.toObject(ExamUser::class.java)?.copy(id = it.id)) }
            .addOnFailureListener(onError)
    }

    fun loadAttempt(attemptId: String, onSuccess: (ExamAttempt?) -> Unit, onError: (Exception) -> Unit) {
        attempts.document(attemptId).get()
            .addOnSuccessListener { onSuccess(it.toObject(ExamAttempt::class.java)?.copy(id = it.id)) }
            .addOnFailureListener(onError)
    }

    fun observePublishedExams(
        onSuccess: (List<Exam>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = firestore.collection("exams")
        .whereEqualTo("published", true)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            val exams = snapshot?.documents?.mapNotNull { document ->
                document.toObject(Exam::class.java)?.copy(id = document.id)
            }.orEmpty().sortedByDescending { it.createdAt }
            onSuccess(exams)
        }

    fun observeEligibleUsers(
        examId: String,
        onSuccess: (List<ExamUser>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        var allUsers = emptyList<ExamUser>()
        var completedUserIds = emptySet<String>()

        fun publish() = onSuccess(allUsers.filterNot { it.id in completedUserIds })

        val usersListener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                allUsers = snapshot?.documents?.mapNotNull { document ->
                    document.toObject(ExamUser::class.java)?.copy(id = document.id)
                }.orEmpty().sortedWith(compareBy({ it.lastName.lowercase() }, { it.firstName.lowercase() }))
                publish()
            }

        val attemptsListener = attemptLocks.whereEqualTo("examId", examId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                completedUserIds = snapshot?.documents
                    ?.mapNotNull { it.getString("userId") }
                    ?.toSet().orEmpty()
                publish()
            }

        return ListenerRegistration {
            usersListener.remove()
            attemptsListener.remove()
        }
    }

    fun loadQuestions(
        examId: String,
        onSuccess: (List<Question>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        firestore.collection("exams").document(examId).collection("questions")
            .orderBy("position").get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.documents.mapNotNull { document ->
                    document.toObject(Question::class.java)?.copy(id = document.id)
                })
            }
            .addOnFailureListener(onError)
    }

    fun submitAttempt(
        draft: ExamDraft,
        onSuccess: (ExamAttempt) -> Unit,
        onDuplicate: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        ensureAnonymousSession(
            onSuccess = {
                submitAuthenticatedAttempt(draft, onSuccess, onDuplicate, onError)
            },
            onError = onError
        )
    }

    private fun ensureAnonymousSession(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser?.isAnonymous == true) {
            // Force-refresh the token so Firestore never submits with a stale or
            // missing credential after a longer exam.
            currentUser.getIdToken(true)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { error ->
                    auth.signOut()
                    auth.signInAnonymously()
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener(onError)
                }
            return
        }

        // Student submissions must never use a lingering administrator session.
        auth.signOut()
        auth.signInAnonymously()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    private fun submitAuthenticatedAttempt(
        draft: ExamDraft,
        onSuccess: (ExamAttempt) -> Unit,
        onDuplicate: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val lockId = "${draft.examId}_${draft.userId}"
        val attemptReference = attempts.document()
        val attemptId = attemptReference.id
        val lockReference = attemptLocks.document(lockId)
        val submittedAt = System.currentTimeMillis()
        val duration = ((submittedAt - draft.startedAt) / 1000L).coerceAtLeast(0L)

        val answers = draft.questions.map { question ->
            val draftAnswer = draft.answers[question.id]
            val selectedIndex = draftAnswer?.selectedOptionIndex ?: -1
            val automaticPoints = if (
                question.type == QuestionType.MULTIPLE_CHOICE &&
                selectedIndex == question.correctOptionIndex
            ) question.points else 0

            ExamAnswer(
                questionId = question.id,
                questionText = question.text,
                questionType = question.type,
                maximumPoints = question.points,
                openAnswer = draftAnswer?.openAnswer.orEmpty(),
                selectedOptionIndex = selectedIndex,
                selectedOptionText = question.options.getOrNull(selectedIndex).orEmpty(),
                awardedPoints = if (question.type == QuestionType.MULTIPLE_CHOICE) automaticPoints else null,
                position = question.position
            )
        }
        val automaticPoints = answers.sumOf { it.awardedPoints ?: 0 }
        val attempt = ExamAttempt(
            id = attemptId,
            examId = draft.examId,
            examTitle = draft.examTitle,
            userId = draft.userId,
            userName = draft.userName,
            startedAt = draft.startedAt,
            submittedAt = submittedAt,
            durationSeconds = duration,
            latitude = draft.latitude,
            longitude = draft.longitude,
            address = draft.address,
            maximumPoints = draft.questions.sumOf { it.points },
            automaticPoints = automaticPoints,
            finalPoints = null,
            status = AttemptStatus.PENDING_REVIEW
        )

        lockReference.get()
            .addOnSuccessListener { existingLock ->
                if (existingLock.exists()) {
                    onDuplicate()
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                batch.set(lockReference, mapOf(
                "examId" to draft.examId,
                "userId" to draft.userId,
                "attemptId" to attemptId,
                "createdAt" to submittedAt
                ))
                batch.set(attemptReference, attempt)
                answers.forEach { answer ->
                    batch.set(
                        attemptReference.collection("answers").document(answer.questionId),
                        answer
                    )
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess(attempt) }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }
}
