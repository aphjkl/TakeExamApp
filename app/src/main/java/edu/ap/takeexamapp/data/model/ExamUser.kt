package edu.ap.takeexamapp.data.model

data class ExamUser(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = ""
) {
    val fullName: String
        get() = "$firstName $lastName".trim()
}