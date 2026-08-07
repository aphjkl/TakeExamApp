package edu.ap.takeexamapp.data.repository

import android.content.Context
import edu.ap.takeexamapp.data.model.DraftAnswer
import edu.ap.takeexamapp.data.model.ExamDraft
import edu.ap.takeexamapp.data.model.Question
import org.json.JSONArray
import org.json.JSONObject

class ExamDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("exam_draft", Context.MODE_PRIVATE)

    fun save(draft: ExamDraft) {
        val root = JSONObject()
            .put("examId", draft.examId)
            .put("examTitle", draft.examTitle)
            .put("userId", draft.userId)
            .put("userName", draft.userName)
            .put("startedAt", draft.startedAt)
            .put("latitude", draft.latitude)
            .put("longitude", draft.longitude)
            .put("address", draft.address)
            .put("currentQuestionIndex", draft.currentQuestionIndex)

        root.put("questions", JSONArray().apply {
            draft.questions.forEach { question ->
                put(JSONObject()
                    .put("id", question.id)
                    .put("text", question.text)
                    .put("type", question.type)
                    .put("points", question.points)
                    .put("acceptedAnswers", JSONArray(question.acceptedAnswers))
                    .put("options", JSONArray(question.options))
                    .put("correctOptionIndex", question.correctOptionIndex)
                    .put("position", question.position))
            }
        })
        root.put("answers", JSONObject().apply {
            draft.answers.forEach { (questionId, answer) ->
                put(questionId, JSONObject()
                    .put("openAnswer", answer.openAnswer)
                    .put("selectedOptionIndex", answer.selectedOptionIndex))
            }
        })
        preferences.edit().putString(KEY, root.toString()).apply()
    }

    fun load(): ExamDraft? = runCatching {
        val root = JSONObject(preferences.getString(KEY, null) ?: return null)
        val questionsJson = root.getJSONArray("questions")
        val questions = (0 until questionsJson.length()).map { index ->
            val item = questionsJson.getJSONObject(index)
            Question(
                id = item.getString("id"), text = item.getString("text"), type = item.getString("type"),
                points = item.getInt("points"), acceptedAnswers = item.getJSONArray("acceptedAnswers").toStrings(),
                options = item.getJSONArray("options").toStrings(),
                correctOptionIndex = item.getInt("correctOptionIndex"), position = item.getInt("position")
            )
        }
        val answersJson = root.getJSONObject("answers")
        val answers = answersJson.keys().asSequence().associateWith { questionId ->
            val item = answersJson.getJSONObject(questionId)
            DraftAnswer(item.optString("openAnswer"), item.optInt("selectedOptionIndex", -1))
        }
        ExamDraft(
            examId = root.getString("examId"), examTitle = root.getString("examTitle"),
            userId = root.getString("userId"), userName = root.getString("userName"),
            startedAt = root.getLong("startedAt"), latitude = root.getDouble("latitude"),
            longitude = root.getDouble("longitude"), address = root.getString("address"),
            questions = questions, answers = answers,
            currentQuestionIndex = root.optInt("currentQuestionIndex", 0)
        )
    }.getOrNull()

    fun clear() = preferences.edit().remove(KEY).apply()

    private fun JSONArray.toStrings() = (0 until length()).map { getString(it) }

    private companion object { const val KEY = "active" }
}
