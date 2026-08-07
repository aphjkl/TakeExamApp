package edu.ap.takeexamapp.navigation

object AppRoute {
    const val HOME = "home"
    const val TAKE_EXAM = "take_exam"
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val MANAGE_USERS = "manage_users"
    const val MANAGE_EXAMS = "manage_exams"
    const val EDIT_EXAM = "edit_exam/{examId}"

    fun editExam(examId: String) = "edit_exam/$examId"
}
