package edu.ap.takeexamapp.ui.screens.admin

import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.view.ViewOutlineProvider
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import edu.ap.takeexamapp.data.model.AttemptStatus
import edu.ap.takeexamapp.data.model.ExamAnswer
import edu.ap.takeexamapp.data.model.ExamAttempt
import edu.ap.takeexamapp.data.model.QuestionType
import edu.ap.takeexamapp.data.repository.ResultRepository
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun ResultDetailScreen(
    attemptId: String,
    onBack: () -> Unit,
    onReview: () -> Unit
) {
    val repository = remember { ResultRepository() }
    var attempt by remember { mutableStateOf<ExamAttempt?>(null) }
    var answers by remember { mutableStateOf<List<ExamAnswer>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(attemptId) {
        val attemptRegistration = repository.observeAttempt(attemptId, { attempt = it }, { error = it.localizedMessage })
        val answerRegistration = repository.observeAnswers(attemptId, { answers = it }, { error = it.localizedMessage })
        onDispose { attemptRegistration.remove(); answerRegistration.remove() }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        Text("Result details", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 16.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        attempt?.let { result ->
            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text(result.userName, style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 12.dp))
                    Text(result.examTitle, style = MaterialTheme.typography.titleMedium)
                    Text("Submitted: ${formatDate(result.submittedAt)}")
                    Text("Duration: ${formatDuration(result.durationSeconds)}")
                    Text(if (result.status == AttemptStatus.GRADED)
                        "Score: ${result.finalPoints}/${result.maximumPoints}"
                    else "Awaiting review",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp))
                    Text("Address: ${result.address}", modifier = Modifier.padding(top = 12.dp))
                    Text("Coordinates: ${result.latitude}, ${result.longitude}")
                    ExamLocationMap(result.latitude, result.longitude, result.userName)
                    Text("© OpenStreetMap contributors", style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp))
                    Text("Answers", style = MaterialTheme.typography.headlineSmall)
                }
                items(answers, key = { it.questionId }) { answer ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Text(answer.questionText, style = MaterialTheme.typography.titleMedium)
                        Text(if (answer.questionType == QuestionType.OPEN)
                            "Answer: ${answer.openAnswer.ifBlank { "No answer" }}"
                        else "Answer: ${answer.selectedOptionText.ifBlank { "No answer" }}")
                        Text("Points: ${answer.awardedPoints?.toString() ?: "Pending"}/${answer.maximumPoints}")
                    }
                    HorizontalDivider()
                }
            }
            Button(onClick = onReview, modifier = Modifier.fillMaxWidth()) {
                Text(if (result.status == AttemptStatus.GRADED) "Reopen grading" else "Review answers")
            }
        }
    }
}

@Composable
private fun ExamLocationMap(latitude: Double, longitude: Double, markerTitle: String) {
    val context = LocalContext.current
    Configuration.getInstance().userAgentValue = "TakeExamApp/1.0 (edu.ap.takeexamapp)"
    val mapView = remember { MapView(context) }
    val point = remember(latitude, longitude) { GeoPoint(latitude, longitude) }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val mapShape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(280.dp)
            .graphicsLayer {
                shape = mapShape
                clip = true
            }
            .clip(mapShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, mapShape)
    ) {
        AndroidView(
            factory = {
                mapView.apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        setColor(AndroidColor.TRANSPARENT)
                        cornerRadius = 12 * resources.displayMetrics.density
                    }
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                    clipToOutline = true
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    controller.setCenter(point)
                }
            },
            update = { map ->
                map.overlays.clear()
                map.overlays.add(Marker(map).apply {
                    position = point
                    title = markerTitle
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
                map.controller.setCenter(point)
                map.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
