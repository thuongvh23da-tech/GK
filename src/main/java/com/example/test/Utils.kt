package com.example.test

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.FirebaseAuth

fun getStoragePathFromUrl(downloadUrl: String): String? {
    return try {
        val uri = Uri.parse(downloadUrl)
        val path = uri.path
        path?.substringAfter("/o/")?.substringBefore("?")
    } catch (e: Exception) {
        null
    }
}

fun deleteImageFromStorage(imageUrl: String) {
    val storagePath = getStoragePathFromUrl(imageUrl)
    if (storagePath != null) {
        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child(storagePath)
        storageRef.delete()
            .addOnSuccessListener {
                android.util.Log.d("Storage", "Image deleted successfully: $storagePath")
            }
            .addOnFailureListener { e: Exception ->
                android.util.Log.e("Storage", "Error deleting image: ${e.message}")
            }
    } else {
        android.util.Log.e("Storage", "Cannot extract storage path from URL: $imageUrl")
    }
}

fun uploadFileToFirebaseStorage(
    uri: Uri,
    context: Context,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val storageRef = Firebase.storage.reference
    val fileRef = storageRef.child("images/${System.currentTimeMillis()}_${uri.lastPathSegment}")

    fileRef.putFile(uri)
        .addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { downloadUri ->
                onSuccess(downloadUri.toString())
            }
        }
        .addOnFailureListener { e: Exception ->
            onFailure(e)
        }
}

fun saveNoteToFirestore(
    title: String,
    description: String,
    price: String,
    imageUrl: String,
    context: Context
) {
    val db = Firebase.firestore
    val note = hashMapOf(
        "title" to title,
        "description" to description,
        "price" to price,
        "imageUrl" to imageUrl,
        "timestamp" to System.currentTimeMillis()
    )

    db.collection("Notes")
        .add(note)
        .addOnSuccessListener {
            Toast.makeText(context, "Thêm Note thành công!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e: Exception ->
            Toast.makeText(context, "Lỗi thêm Note: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun updateNoteInFirestore(
    id: String,
    title: String,
    description: String,
    price: String,
    imageUrl: String,
    context: Context
) {
    val db = FirebaseFirestore.getInstance()
    val noteUpdates = hashMapOf(
        "title" to title,
        "description" to description,
        "price" to price,
        "imageUrl" to imageUrl
    )

    db.collection("Notes").document(id).update(noteUpdates as Map<String, Any>)
        .addOnSuccessListener {
            Toast.makeText(context, "Cập nhật Note thành công!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e: Exception ->
            Toast.makeText(context, "Lỗi cập nhật Note: ${e.message}", Toast.LENGTH_SHORT).show()
        }
}

fun isCurrentUserAdmin(auth: FirebaseAuth, db: FirebaseFirestore, onResult: (Boolean) -> Unit) {
    val userId = auth.currentUser?.uid
    if (userId == null) {
        onResult(false)
        return
    }

    db.collection("Users").document(userId).get()
        .addOnSuccessListener { document ->
            val isAdmin = document.getBoolean("isAdmin") ?: false
            onResult(isAdmin)
        }
        .addOnFailureListener {
            onResult(false)
        }
}