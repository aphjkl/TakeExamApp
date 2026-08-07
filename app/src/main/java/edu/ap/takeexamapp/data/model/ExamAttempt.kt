package edu.ap.takeexamapp.data.model

object AttemptStatus {
    const val PENDING_REVIEW = "PENDING_REVIEW"
    const val GRADED = "GRADED"
}

data class ExamAttempt(
    val id: String = "",
    val examId: String = "",
    val examTitle: String = "",
    val userId: String = "",
    val userName: String = "",
    val startedAt: Long = 0L,
    val submittedAt: Long = 0L,
    val durationSeconds: Long = 0L,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val maximumPoints: Int = 0,
    val automaticPoints: Int = 0,
    val finalPoints: Int? = null,
    val status: String = AttemptStatus.PENDING_REVIEW,
    val reviewedAt: Long? = null,
    val reviewedBy: String = ""
)

data class ExamAnswer(
    val questionId: String = "",
    val questionText: String = "",
    val questionType: String = QuestionType.OPEN,
    val maximumPoints: Int = 0,
    val openAnswer: String = "",
    val selectedOptionIndex: Int = -1,
    val selectedOptionText: String = "",
    val awardedPoints: Int? = null,
    val position: Int = 0
)

data class DraftAnswer(
    val openAnswer: String = "",
    val selectedOptionIndex: Int = -1
)

data class ExamDraft(
    val examId: String,
    val examTitle: String,
    val userId: String,
    val userName: String,
    val startedAt: Long,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val questions: List<Question>,
    val answers: Map<String, DraftAnswer> = emptyMap(),
    val currentQuestionIndex: Int = 0
)
