package edu.ap.takeexamapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import edu.ap.takeexamapp.data.model.Exam
import edu.ap.takeexamapp.data.model.Question

class ExamRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val examsCollection = firestore.collection("exams")

    fun observeExams(
        onSuccess: (List<Exam>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = examsCollection
        .orderBy("createdAt", Query.Direction.DESCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val exams = snapshot?.documents?.mapNotNull { document ->
                document.toObject(Exam::class.java)?.copy(id = document.id)
            }.orEmpty()
            onSuccess(exams)
        }

    fun observeExam(
        examId: String,
        onSuccess: (Exam?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = examsCollection.document(examId)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            onSuccess(snapshot?.toObject(Exam::class.java)?.copy(id = examId))
        }

    fun observeQuestions(
        examId: String,
        onSuccess: (List<Question>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = questions(examId)
        .orderBy("position")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val result = snapshot?.documents?.mapNotNull { document ->
                document.toObject(Question::class.java)?.copy(id = document.id)
            }.orEmpty()
            onSuccess(result)
        }

    fun createExam(
        title: String,
        description: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val document = examsCollection.document()
        document.set(
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "published" to false,
                "questionCount" to 0,
                "totalPoints" to 0,
                "createdAt" to System.currentTimeMillis()
            )
        ).addOnSuccessListener { onSuccess(document.id) }
            .addOnFailureListener(onError)
    }

    fun updateExam(
        examId: String,
        title: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = examsCollection.document(examId)
        .update(mapOf("title" to title.trim(), "description" to description.trim()))
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onError)

    fun setPublished(
        examId: String,
        published: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = examsCollection.document(examId)
        .update("published", published)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener(onError)

    fun addQuestion(
        examId: String,
        question: Question,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = mapOf(
            "text" to question.text.trim(),
            "type" to question.type,
            "points" to question.points,
            "acceptedAnswers" to question.acceptedAnswers,
            "options" to question.options,
            "correctOptionIndex" to question.correctOptionIndex,
            "position" to question.position
        )
        questions(examId).add(data)
            .addOnSuccessListener {
                recalculateSummary(examId, onSuccess, onError)
            }
            .addOnFailureListener(onError)
    }

    fun deleteQuestion(
        examId: String,
        questionId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) = questions(examId).document(questionId).delete()
        .addOnSuccessListener {
            recalculateSummary(examId, onSuccess, onError)
        }
        .addOnFailureListener(onError)

    fun deleteExam(
        examId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        questions(examId).get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(examsCollection.document(examId))
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    private fun recalculateSummary(
        examId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        questions(examId).get()
            .addOnSuccessListener { snapshot ->
                val points = snapshot.documents.sumOf {
                    (it.getLong("points") ?: 0L).toInt()
                }
                val summary = mutableMapOf<String, Any>(
                    "questionCount" to snapshot.size(),
                    "totalPoints" to points
                )
                if (snapshot.isEmpty) summary["published"] = false

                examsCollection.document(examId).update(summary)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    private fun questions(examId: String) =
        examsCollection.document(examId).collection("questions")
}
