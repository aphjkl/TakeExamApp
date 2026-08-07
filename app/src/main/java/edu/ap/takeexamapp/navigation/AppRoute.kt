package edu.ap.takeexamapp.navigation

object AppRoute {
    const val HOME = "home"
    const val TAKE_EXAM = "take_exam"
    const val SELECT_STUDENT = "select_student/{examId}"
    const val CONFIRM_LOCATION = "confirm_location/{examId}/{userId}"
    const val EXAM_SESSION = "exam_session"
    const val SUBMISSION_RECEIPT = "submission_receipt/{attemptId}"
    const val ADMIN_LOGIN = "admin_login"
    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val MANAGE_USERS = "manage_users"
    const val MANAGE_EXAMS = "manage_exams"
    const val EDIT_EXAM = "edit_exam/{examId}"

    fun editExam(examId: String) = "edit_exam/$examId"
    fun selectStudent(examId: String) = "select_student/$examId"
    fun confirmLocation(examId: String, userId: String) = "confirm_location/$examId/$userId"
    fun submissionReceipt(attemptId: String) = "submission_receipt/$attemptId"
}
