package edu.ap.takeexamapp.data.model

object QuestionType {
    const val OPEN = "OPEN"
    const val MULTIPLE_CHOICE = "MULTIPLE_CHOICE"
}

data class Question(
    val id: String = "",
    val text: String = "",
    val type: String = QuestionType.OPEN,
    val points: Int = 1,
    val acceptedAnswers: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = -1,
    val position: Int = 0
)
