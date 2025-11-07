package com.example.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

class UpdateCourse : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val noteID = intent.getStringExtra("noteID") ?: ""
        val initialTitle = intent.getStringExtra("noteTitle") ?: ""
        val initialDescription = intent.getStringExtra("noteDescription") ?: ""
        val initialPrice = intent.getStringExtra("notePrice") ?: "" // Đã thêm: Đọc giá
        val initialImageUrl = intent.getStringExtra("noteImageUrl") ?: ""

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UpdateNoteScreen(
                        noteID,
                        initialTitle,
                        initialDescription,
                        initialPrice, // Truyền giá
                        initialImageUrl
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateNoteScreen(
    noteID: String,
    initialTitle: String,
    initialDescription: String,
    initialPrice: String, // Nhận giá
    initialImageUrl: String
) {
    val context = LocalContext.current
    var noteTitle by remember { mutableStateOf(initialTitle) }
    var noteDescription by remember { mutableStateOf(initialDescription) }
    var notePrice by remember { mutableStateOf(initialPrice) } // Biến state cho giá
    var inputNewUrl by remember { mutableStateOf("") }
    var currentImageUrl by remember { mutableStateOf(initialImageUrl) }
    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newImageUri = uri
        inputNewUrl = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Cập nhật Note", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = {
                        val intent = Intent(context, NoteListActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2))
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = inputNewUrl,
                onValueChange = {
                    inputNewUrl = it
                    newImageUri = null
                },
                label = { Text("URL ảnh bên ngoài mới (nếu có)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (newImageUri != null) "Đã chọn ảnh mới" else "Hoặc Chọn ảnh/file mới để upload")
            }

            Spacer(modifier = Modifier.height(16.dp))

            val displayImage: Any? = when {
                inputNewUrl.isNotBlank() -> inputNewUrl
                newImageUri != null -> newImageUri
                currentImageUrl.isNotBlank() -> currentImageUrl
                else -> null
            }

            if (displayImage != null) {
                AsyncImage(
                    model = displayImage,
                    contentDescription = "Note Image/File",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(bottom = 16.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Chưa có ảnh/file")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = noteTitle,
                onValueChange = { noteTitle = it },
                label = { Text("Title (Tiêu đề)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = noteDescription,
                onValueChange = { noteDescription = it },
                label = { Text("Description (Mô tả)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notePrice,
                onValueChange = { notePrice = it },
                label = { Text("Price (Giá tiền)") }, // Đã thêm: Input giá
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (noteTitle.isBlank() || noteDescription.isBlank() || notePrice.isBlank()) { // Kiểm tra giá
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true
                    var finalImageUrl = currentImageUrl

                    if (inputNewUrl.isNotBlank()) {
                        if (currentImageUrl.isNotEmpty() && currentImageUrl.startsWith("https://firebasestorage.googleapis.com")) {
                            deleteImageFromStorage(currentImageUrl)
                        }
                        finalImageUrl = inputNewUrl
                        updateNoteInFirestore(noteID, noteTitle, noteDescription, notePrice, finalImageUrl, context) // Truyền giá
                        isLoading = false
                        val intent = Intent(context, NoteListActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()

                    } else if (newImageUri != null) {
                        if (currentImageUrl.isNotEmpty() && currentImageUrl.startsWith("https://firebasestorage.googleapis.com")) {
                            deleteImageFromStorage(currentImageUrl)
                        }
                        uploadFileToFirebaseStorage(
                            uri = newImageUri!!,
                            context = context,
                            onSuccess = { downloadUrl ->
                                updateNoteInFirestore(noteID, noteTitle, noteDescription, notePrice, downloadUrl, context) // Truyền giá
                                isLoading = false
                                val intent = Intent(context, NoteListActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                context.startActivity(intent)
                                (context as? ComponentActivity)?.finish()
                            },
                            onFailure = { e: Exception ->
                                Toast.makeText(context, "Lỗi tải ảnh/file: ${e.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                        )
                    } else {
                        updateNoteInFirestore(noteID, noteTitle, noteDescription, notePrice, finalImageUrl, context) // Truyền giá
                        isLoading = false
                        val intent = Intent(context, NoteListActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Đang cập nhật..." else "CẬP NHẬT NOTE")
            }
        }
    }
}