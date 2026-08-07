package edu.ap.takeexamapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import edu.ap.takeexamapp.data.model.ExamUser

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val usersCollection = firestore.collection("users")

    fun observeUsers(
        onSuccess: (List<ExamUser>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return usersCollection
            .orderBy("lastName")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val users = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        document.toObject(ExamUser::class.java)?.copy(
                            id = document.id
                        )
                    }
                    .orEmpty()

                onSuccess(users)
            }
    }

    fun addUser(
        firstName: String,
        lastName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val user = hashMapOf(
            "firstName" to firstName.trim(),
            "lastName" to lastName.trim()
        )

        usersCollection
            .add(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun addUsers(
        users: List<Pair<String, String>>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (users.isEmpty()) {
            onSuccess()
            return
        }

        val batch = firestore.batch()

        users.forEach { (firstName, lastName) ->
            val document = usersCollection.document()

            batch.set(
                document,
                mapOf(
                    "firstName" to firstName.trim(),
                    "lastName" to lastName.trim()
                )
            )
        }

        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun deleteUser(
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        usersCollection
            .document(userId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }
}