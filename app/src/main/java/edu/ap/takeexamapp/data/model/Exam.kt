package edu.ap.takeexamapp.data.model

data class Exam(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val published: Boolean = false,
    val questionCount: Int = 0,
    val totalPoints: Int = 0,
    val createdAt: Long = 0L
)
