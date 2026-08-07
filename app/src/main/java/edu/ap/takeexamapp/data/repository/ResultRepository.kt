package edu.ap.takeexamapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.ap.takeexamapp.data.model.AttemptStatus
import edu.ap.takeexamapp.data.model.ExamAnswer
import edu.ap.takeexamapp.data.model.ExamAttempt
import edu.ap.takeexamapp.data.model.QuestionType

class ResultRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val attempts = firestore.collection("attempts")

    fun observeAll(
        onSuccess: (List<ExamAttempt>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = attempts.addSnapshotListener { snapshot, error ->
        if (error != null) { onError(error); return@addSnapshotListener }
        onSuccess(snapshot.toAttempts())
    }

    fun observePending(
        onSuccess: (List<ExamAttempt>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = attempts.whereEqualTo("status", AttemptStatus.PENDING_REVIEW)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error); return@addSnapshotListener }
            onSuccess(snapshot.toAttempts().sortedBy { it.submittedAt })
        }

    fun observeForUser(
        userId: String,
        onSuccess: (List<ExamAttempt>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = attempts.whereEqualTo("userId", userId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error); return@addSnapshotListener }
            onSuccess(snapshot.toAttempts().sortedByDescending { it.submittedAt })
        }

    fun observeForExam(
        examId: String,
        onSuccess: (List<ExamAttempt>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = attempts.whereEqualTo("examId", examId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error); return@addSnapshotListener }
            onSuccess(snapshot.toAttempts().sortedBy { it.userName.lowercase() })
        }

    fun observeAnswers(
        attemptId: String,
        onSuccess: (List<ExamAnswer>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = attempts.document(attemptId).collection("answers")
        .addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error); return@addSnapshotListener }
            val answers = snapshot?.documents?.mapNotNull { document ->
                document.toObject(ExamAnswer::class.java)?.copy(questionId = document.id)
            }.orEmpty().sortedWith(compareBy<ExamAnswer> { it.position }.thenBy { it.questionText })
            onSuccess(answers)
        }

    fun observeAttempt(
        attemptId: String,
        onSuccess: (ExamAttempt?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = attempts.document(attemptId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) { onError(error); return@addSnapshotListener }
            onSuccess(snapshot?.toObject(ExamAttempt::class.java)?.copy(id = attemptId))
        }

    fun finalizeGrade(
        attemptId: String,
        openScores: Map<String, Int>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val attemptRef = attempts.document(attemptId)
        attemptRef.collection("answers").get()
            .addOnSuccessListener { snapshot ->
                val answers = snapshot.documents.mapNotNull { document ->
                    document.toObject(ExamAnswer::class.java)?.copy(questionId = document.id)
                }
                val invalid = answers.any { answer ->
                    answer.questionType == QuestionType.OPEN &&
                        openScores[answer.questionId]?.let { it !in 0..answer.maximumPoints } != false
                }
                if (invalid) {
                    onError(IllegalArgumentException("Every open answer needs points between 0 and its maximum."))
                    return@addOnSuccessListener
                }

                firestore.runTransaction { transaction ->
                    transaction.get(attemptRef)
                    var total = 0
                    answers.forEach { answer ->
                        val points = if (answer.questionType == QuestionType.OPEN) {
                            openScores.getValue(answer.questionId)
                        } else answer.awardedPoints ?: 0
                        total += points
                        if (answer.questionType == QuestionType.OPEN) {
                            transaction.update(
                                attemptRef.collection("answers").document(answer.questionId),
                                "awardedPoints", points
                            )
                        }
                    }
                    val reviewer = FirebaseAuth.getInstance().currentUser
                    transaction.update(attemptRef, mapOf(
                        "finalPoints" to total,
                        "status" to AttemptStatus.GRADED,
                        "reviewedAt" to System.currentTimeMillis(),
                        "reviewedBy" to (reviewer?.email ?: reviewer?.uid.orEmpty())
                    ))
                }.addOnSuccessListener { onSuccess() }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    private fun com.google.firebase.firestore.QuerySnapshot?.toAttempts(): List<ExamAttempt> =
        this?.documents?.mapNotNull { document ->
            document.toObject(ExamAttempt::class.java)?.copy(id = document.id)
        }.orEmpty()
}
