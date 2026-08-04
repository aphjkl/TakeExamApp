package edu.ap.takeexamapp.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import edu.ap.takeexamapp.ui.screens.home.HomeScreen

import com.google.firebase.auth.FirebaseAuth
import edu.ap.takeexamapp.ui.screens.admin.AdminDashboardScreen
import edu.ap.takeexamapp.ui.screens.admin.AdminLoginScreen

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
            PlaceholderScreen(text = "Take an exam")
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

composable(AppRoute.ADMIN_DASHBOARD) {
    AdminDashboardScreen(
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
private fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text)
    }
}