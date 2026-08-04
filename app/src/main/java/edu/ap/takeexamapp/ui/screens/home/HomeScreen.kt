package edu.ap.takeexamapp.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onTakeExamClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TakeExamApp",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Choose how you want to continue",
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = onTakeExamClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Take an exam")
        }

        OutlinedButton(
            onClick = onAdminClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text("Admin")
        }
    }
}