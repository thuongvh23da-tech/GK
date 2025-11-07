package com.example.test

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MenuActivity()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuActivity() {
    val context = LocalContext.current
    val auth = remember { Firebase.auth }
    val db = remember { FirebaseFirestore.getInstance() }
    var noteTitle by remember { mutableStateOf("") }
    var noteDescription by remember { mutableStateOf("") }
    var notePrice by remember { mutableStateOf("") }
    var inputImageUrl by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(auth.currentUser) {
        if (auth.currentUser != null) {
            isCurrentUserAdmin(auth, db) { adminStatus ->
                isAdmin = adminStatus
            }
        } else {
            isAdmin = false
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
        inputImageUrl = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isAdmin) { // Chỉ Admin mới thấy nút Xem danh sách
                Button(
                    onClick = {
                        val intent = Intent(context, NoteListActivity::class.java)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text("Xem Danh Sách")
                }
            }
            Button(
                onClick = {
                    auth.signOut()
                    Toast.makeText(context, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                    context.startActivity(Intent(context, AuthActivity::class.java))
                    (context as? ComponentActivity)?.finish()
                },
                modifier = Modifier.weight(if (isAdmin) 1f else 2f)
            ) {
                Text("Đăng Xuất")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (isAdmin) { // Chỉ Admin mới thấy form Thêm Note
            OutlinedTextField(
                value = inputImageUrl,
                onValueChange = {
                    inputImageUrl = it
                    imageUri = null
                },
                label = { Text("URL ảnh bên ngoài (nếu có)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { imagePicker.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (imageUri != null) "Đã chọn ảnh" else "Hoặc Chọn ảnh/file để upload")
            }

            Spacer(modifier = Modifier.height(16.dp))

            val displayImage: Any? = when {
                inputImageUrl.isNotBlank() -> inputImageUrl
                imageUri != null -> imageUri
                else -> null
            }

            if (displayImage != null) {
                AsyncImage(
                    model = displayImage,
                    contentDescription = "Selected image",
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
                label = { Text("Price (Giá tiền)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (noteTitle.isBlank() || noteDescription.isBlank() || notePrice.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (imageUri == null && inputImageUrl.isBlank()) {
                        Toast.makeText(context, "Vui lòng chọn hoặc nhập URL ảnh/file", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    isLoading = true

                    if (inputImageUrl.isNotBlank()) {
                        saveNoteToFirestore(
                            title = noteTitle,
                            description = noteDescription,
                            price = notePrice,
                            imageUrl = inputImageUrl,
                            context = context
                        )
                        noteTitle = ""
                        noteDescription = ""
                        notePrice = ""
                        inputImageUrl = ""
                        imageUri = null
                        isLoading = false
                    } else if (imageUri != null) {
                        uploadFileToFirebaseStorage(
                            uri = imageUri!!,
                            context = context,
                            onSuccess = { downloadUrl ->
                                saveNoteToFirestore(
                                    title = noteTitle,
                                    description = noteDescription,
                                    price = notePrice,
                                    imageUrl = downloadUrl,
                                    context = context
                                )
                                noteTitle = ""
                                noteDescription = ""
                                notePrice = ""
                                imageUri = null
                                isLoading = false
                            },
                            onFailure = { e: Exception ->
                                Toast.makeText(context, "Lỗi tải ảnh/file: ${e.message}", Toast.LENGTH_LONG).show()
                                isLoading = false
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Đang tải lên..." else "THÊM NOTE")
            }
        } else {
            // Hiển thị thông báo nếu không phải Admin
            Text("Bạn không có quyền quản trị để thêm Note.", modifier = Modifier.padding(32.dp))
        }
    }
}