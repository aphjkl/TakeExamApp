package edu.ap.takeexamapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import edu.ap.takeexamapp.ui.screens.home.HomeScreen

import com.google.firebase.auth.FirebaseAuth
import edu.ap.takeexamapp.ui.screens.admin.AdminDashboardScreen
import edu.ap.takeexamapp.ui.screens.admin.AdminLoginScreen
import edu.ap.takeexamapp.ui.screens.admin.ManageUsersScreen
import edu.ap.takeexamapp.ui.screens.admin.ManageExamsScreen
import edu.ap.takeexamapp.ui.screens.admin.EditExamScreen
import edu.ap.takeexamapp.ui.screens.exam.ExamSessionScreen
import edu.ap.takeexamapp.ui.screens.exam.LocationConfirmationScreen
import edu.ap.takeexamapp.ui.screens.exam.StudentExamListScreen
import edu.ap.takeexamapp.ui.screens.exam.StudentSelectionScreen
import edu.ap.takeexamapp.ui.screens.exam.SubmissionReceiptScreen
import edu.ap.takeexamapp.ui.screens.admin.ExamAttemptsScreen
import edu.ap.takeexamapp.ui.screens.admin.ExamResultSelectorScreen
import edu.ap.takeexamapp.ui.screens.admin.GradeAttemptScreen
import edu.ap.takeexamapp.ui.screens.admin.PendingReviewScreen
import edu.ap.takeexamapp.ui.screens.admin.ResultDetailScreen
import edu.ap.takeexamapp.ui.screens.admin.ResultsHubScreen
import edu.ap.takeexamapp.ui.screens.admin.UserAttemptsScreen
import edu.ap.takeexamapp.ui.screens.admin.UserResultSelectorScreen

@Composable
fun TakeExamNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AppRoute.HOME
    ) {
        composable(AppRoute.HOME) {
            HomeScreen(
                onTakeExamClick = {
                    navController.navigate(AppRoute.TAKE_EXAM)
                },
                onAdminClick = {
                    navController.navigate(AppRoute.ADMIN_LOGIN)
                }
            )
        }

        composable(AppRoute.TAKE_EXAM) {
            StudentExamListScreen(
                onBack = { navController.popBackStack() },
                onSelectExam = { navController.navigate(AppRoute.selectStudent(it)) },
                onResumeDraft = { navController.navigate(AppRoute.EXAM_SESSION) }
            )
        }
        composable(AppRoute.SELECT_STUDENT) { entry ->
            val examId = entry.arguments?.getString("examId") ?: return@composable
            StudentSelectionScreen(
                examId = examId,
                onBack = { navController.popBackStack() },
                onSelectUser = { userId ->
                    navController.navigate(AppRoute.confirmLocation(examId, userId))
                }
            )
        }
        composable(AppRoute.CONFIRM_LOCATION) { entry ->
            val examId = entry.arguments?.getString("examId") ?: return@composable
            val userId = entry.arguments?.getString("userId") ?: return@composable
            LocationConfirmationScreen(
                examId = examId,
                userId = userId,
                onBack = { navController.popBackStack() },
                onStartExam = {
                    navController.navigate(AppRoute.EXAM_SESSION) {
                        popUpTo(AppRoute.TAKE_EXAM)
                    }
                }
            )
        }
        composable(AppRoute.EXAM_SESSION) {
            ExamSessionScreen(
                onCancel = {
                    navController.navigate(AppRoute.TAKE_EXAM) {
                        popUpTo(AppRoute.TAKE_EXAM) { inclusive = true }
                    }
                },
                onSubmitted = { attemptId ->
                    navController.navigate(AppRoute.submissionReceipt(attemptId)) {
                        popUpTo(AppRoute.TAKE_EXAM) { inclusive = true }
                    }
                }
            )
        }
        composable(AppRoute.SUBMISSION_RECEIPT) { entry ->
            val attemptId = entry.arguments?.getString("attemptId") ?: return@composable
            SubmissionReceiptScreen(attemptId = attemptId, onDone = {
                navController.navigate(AppRoute.HOME) {
                    popUpTo(AppRoute.HOME) { inclusive = true }
                }
            })
        }

        composable(AppRoute.ADMIN_LOGIN) {
            AdminLoginScreen(
                onLoginSuccess = {
                    navController.navigate(AppRoute.ADMIN_DASHBOARD) {
                        popUpTo(AppRoute.ADMIN_LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(AppRoute.MANAGE_USERS) {
            ManageUsersScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(AppRoute.MANAGE_EXAMS) {
            ManageExamsScreen(
                onBack = { navController.popBackStack() },
                onEditExam = { examId ->
                    navController.navigate(AppRoute.editExam(examId))
                }
            )
        }
        composable(AppRoute.EDIT_EXAM) { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: return@composable
            EditExamScreen(
                examId = examId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(AppRoute.RESULTS_HUB) {
            AdminRoute(navController) {
                ResultsHubScreen(
                    onBack = { navController.popBackStack() },
                    onPending = { navController.navigate(AppRoute.PENDING_RESULTS) },
                    onByUser = { navController.navigate(AppRoute.RESULT_USERS) },
                    onByExam = { navController.navigate(AppRoute.RESULT_EXAMS) }
                )
            }
        }
        composable(AppRoute.PENDING_RESULTS) {
            AdminRoute(navController) {
                PendingReviewScreen(
                    onBack = { navController.popBackStack() },
                    onReview = { navController.navigate(AppRoute.gradeAttempt(it)) }
                )
            }
        }
        composable(AppRoute.GRADE_ATTEMPT) { entry ->
            val attemptId = entry.arguments?.getString("attemptId") ?: return@composable
            AdminRoute(navController) {
                GradeAttemptScreen(
                    attemptId = attemptId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }
        composable(AppRoute.RESULT_USERS) {
            AdminRoute(navController) {
                UserResultSelectorScreen(
                    onBack = { navController.popBackStack() },
                    onSelect = { navController.navigate(AppRoute.userAttempts(it)) }
                )
            }
        }
        composable(AppRoute.USER_ATTEMPTS) { entry ->
            val userId = entry.arguments?.getString("userId") ?: return@composable
            AdminRoute(navController) {
                UserAttemptsScreen(userId, { navController.popBackStack() }) {
                    navController.navigate(AppRoute.resultDetail(it))
                }
            }
        }
        composable(AppRoute.RESULT_EXAMS) {
            AdminRoute(navController) {
                ExamResultSelectorScreen(
                    onBack = { navController.popBackStack() },
                    onSelect = { navController.navigate(AppRoute.examAttempts(it)) }
                )
            }
        }
        composable(AppRoute.EXAM_ATTEMPTS) { entry ->
            val examId = entry.arguments?.getString("examId") ?: return@composable
            AdminRoute(navController) {
                ExamAttemptsScreen(examId, { navController.popBackStack() }) {
                    navController.navigate(AppRoute.resultDetail(it))
                }
            }
        }
        composable(AppRoute.RESULT_DETAIL) { entry ->
            val attemptId = entry.arguments?.getString("attemptId") ?: return@composable
            AdminRoute(navController) {
                ResultDetailScreen(
                    attemptId = attemptId,
                    onBack = { navController.popBackStack() },
                    onReview = { navController.navigate(AppRoute.gradeAttempt(attemptId)) }
                )
            }
        }

        composable(AppRoute.ADMIN_DASHBOARD) {
            AdminDashboardScreen(
                onManageUsers = {
                    navController.navigate(AppRoute.MANAGE_USERS)
                },
                onManageExams = {
                    navController.navigate(AppRoute.MANAGE_EXAMS)
                },
                onViewResults = {
                    navController.navigate(AppRoute.RESULTS_HUB)
                },
                onSignOut = {
                    FirebaseAuth.getInstance().signOut()

                    navController.navigate(AppRoute.HOME) {
                        popUpTo(AppRoute.HOME) {
                            inclusive = true
                        }
                    }
                }
            )
        }


    }
}

@Composable
private fun AdminRoute(navController: NavHostController, content: @Composable () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    if (user == null || user.isAnonymous) {
        LaunchedEffect(Unit) {
            navController.navigate(AppRoute.ADMIN_LOGIN) {
                popUpTo(AppRoute.HOME)
            }
        }
    } else {
        content()
    }
}
